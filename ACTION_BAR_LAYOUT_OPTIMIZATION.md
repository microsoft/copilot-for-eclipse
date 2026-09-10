# Action Bar Layout Optimization

## Context

`ActionBar.updateButtonsLayout()` currently disposes and recreates the Send or
Cancel button whenever the chat view or feature-flag-driven button topology is
refreshed. The state refactor intentionally keeps this layout behavior and
restores the current `ActionButtonState` after each rebuild.

Layout optimization is deferred so state correctness and widget lifecycle
changes can be reviewed independently.

## Current Costs and Risks

- Recreating controls churns selection listeners and accessibility metadata.
- Send button images are disposed and reloaded with the control.
- Rebuilds can cause unnecessary layout work and visible flicker.
- Future code can accidentally initialize a recreated control instead of
  rendering the current state.
- Asynchronous feature flag notifications can queue redundant rebuilds.

## Preferred Follow-up

Keep both action controls stable after construction:

1. Create the primary Send or Cancel button once.
2. Create the optional coding-agent button once.
3. Toggle visibility and `GridData.exclude` when preview availability changes.
4. Adjust the parent column count without disposing either control.
5. Render the existing `ActionButtonState` after topology changes.
6. Coalesce redundant asynchronous refresh requests when practical.

## Validation

- Switch between Ask and Agent while a turn is running.
- Deliver repeated feature flag notifications while a turn is running.
- Toggle preview availability in both idle and running states.
- Confirm keyboard focus, accessibility names, and tooltips remain correct.
- Confirm images and listeners are disposed exactly once with the ActionBar.
- Compare layout and repaint frequency before and after the optimization.
