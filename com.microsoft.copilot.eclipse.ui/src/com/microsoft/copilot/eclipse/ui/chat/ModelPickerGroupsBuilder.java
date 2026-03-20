package com.microsoft.copilot.eclipse.ui.chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.preferences.ByokPreferencePage;
import com.microsoft.copilot.eclipse.ui.swt.DropdownItem;
import com.microsoft.copilot.eclipse.ui.swt.DropdownItemGroup;
import com.microsoft.copilot.eclipse.ui.swt.ModelHoverContentProvider;
import com.microsoft.copilot.eclipse.ui.utils.ModelUtils;
import com.microsoft.copilot.eclipse.ui.utils.PreferencesUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Builds model picker dropdown groups for the chat UI.
 */
public final class ModelPickerGroupsBuilder {

  private ModelPickerGroupsBuilder() {
  }

  /**
   * Builds grouped dropdown items for the model picker.
   *
   * @param modelMap available models keyed by id
   * @param showAddPremiumModelOption whether to include the premium upsell action
   * @param showByokManageOption whether to include the BYOK manage action
   * @return grouped dropdown items for the model picker
   */
  public static List<DropdownItemGroup> build(Map<String, CopilotModel> modelMap,
      boolean showAddPremiumModelOption, boolean showByokManageOption) {
    List<CopilotModel> otherModels = new ArrayList<>();
    List<CopilotModel> standardModels = new ArrayList<>();
    List<CopilotModel> premiumModels = new ArrayList<>();
    List<CopilotModel> customModels = new ArrayList<>();

    for (CopilotModel model : modelMap.values()) {
      if (model.getProviderName() != null) {
        customModels.add(model);
      } else if (model.getBilling() != null) {
        if (model.getBilling().isPremium()) {
          premiumModels.add(model);
        } else {
          standardModels.add(model);
        }
      } else {
        otherModels.add(model);
      }
    }

    standardModels.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getModelName(), b.getModelName()));
    premiumModels.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getModelName(), b.getModelName()));
    customModels.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getModelName(), b.getModelName()));

    List<DropdownItemGroup> groups = new ArrayList<>();
    if (!otherModels.isEmpty()) {
      groups.add(DropdownItemGroup.of(buildModelDropdownItems(otherModels)));
    }
    if (!standardModels.isEmpty()) {
      groups.add(DropdownItemGroup.of(Messages.chat_standardModels, buildModelDropdownItems(standardModels)));
    }
    if (!premiumModels.isEmpty()) {
      String header = standardModels.isEmpty() ? Messages.chat_copilotModels : Messages.chat_premiumModels;
      groups.add(DropdownItemGroup.of(header, buildModelDropdownItems(premiumModels)));
    }
    if (!customModels.isEmpty()) {
      groups.add(DropdownItemGroup.of(Messages.chat_customModels, buildModelDropdownItems(customModels)));
    }

    List<DropdownItem> actionItems = new ArrayList<>();
    if (showAddPremiumModelOption) {
      actionItems.add(new DropdownItem.Builder().label(Messages.chat_addPremiumModels).onAction(
          () -> UiUtils.executeCommandWithParameters("com.microsoft.copilot.eclipse.commands.upgradeCopilotPlan", null))
          .build());
    }
    if (showByokManageOption) {
      actionItems.add(new DropdownItem.Builder().label(Messages.chat_actionBar_modelPicker_manageModels)
          .onAction(ModelPickerGroupsBuilder::openManageModelsPreferences)
          .build());
    }
    if (!actionItems.isEmpty()) {
      groups.add(DropdownItemGroup.of(actionItems));
    }
    return groups;
  }

  private static List<DropdownItem> buildModelDropdownItems(List<CopilotModel> models) {
    List<DropdownItem> items = new ArrayList<>();
    for (CopilotModel model : models) {
      String suffix = ModelUtils.getModelSuffix(model);
      items.add(new DropdownItem.Builder().id(model.getModelName()).label(model.getModelName()).suffix(suffix)
          .hoverProvider(new ModelHoverContentProvider(model)).build());
    }
    return items;
  }

  private static void openManageModelsPreferences() {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("com.microsoft.copilot.eclipse.commands.openPreferences.activePageId", ByokPreferencePage.ID);
    parameters.put("com.microsoft.copilot.eclipse.commands.openPreferences.pageIds",
        String.join(",", PreferencesUtils.getAllPreferenceIds()));
    UiUtils.executeCommandWithParameters("com.microsoft.copilot.eclipse.commands.openPreferences", parameters);
  }
}