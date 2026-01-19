package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jdi.ClassNotLoadedException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaExceptionBreakpoint;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.debug.eval.IAstEvaluationEngine;
import org.eclipse.jdt.debug.eval.ICompiledExpression;
import org.eclipse.jdt.debug.eval.IEvaluationListener;
import org.eclipse.jdt.debug.eval.IEvaluationResult;
import org.eclipse.jdt.internal.debug.core.JDIDebugPlugin;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConfirmationMessages;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InputSchema;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InputSchemaPropertyValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolInformation;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult.ToolInvocationStatus;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;

/**
 * Tool adapter for Java debugging operations without confirmation prompts.
 */
public class JavaDebuggerToolAdapter extends BaseTool {
  public static final String TOOL_NAME = "java_debugger";
  private static final String ACTION = "action";
  private static final String DEPTH = "depth";
  private static final String EXPRESSION = "expression";
  private static final String FILE = "file";
  private static final String LINE = "line";
  private static final String THREAD_ID = "threadId";
  private static final String CONDITION = "condition";
  private static final String HIT_COUNT = "hitCount";
  private static final String VARIABLE_NAME = "variableName";
  private static final String VALUE = "value";
  private static final String EXCEPTION_TYPE = "exceptionType";
  private static final String CAUGHT = "caught";
  private static final String UNCAUGHT = "uncaught";
  private static final int EVALUATION_TIMEOUT_MS = 5000;

  private final Gson gson;

  /**
   * Constructor for the JavaDebuggerToolAdapter.
   */
  public JavaDebuggerToolAdapter() {
    this.name = TOOL_NAME;
    this.gson = new GsonBuilder().setPrettyPrinting().create();
  }

  @Override
  public boolean needConfirmation() {
    return true;
  }

  @Override
  public ConfirmationMessages getConfirmationMessages() {
    return new ConfirmationMessages("Execute Java debugger action",
        "The tool is about to perform the following debug action:");
  }

  @Override
  public LanguageModelToolInformation getToolInformation() {
    LanguageModelToolInformation toolInfo = super.getToolInformation();

    toolInfo.setName(TOOL_NAME);
    toolInfo.setDescription("""
        Control and inspect Java debug sessions. Provides rich inspection of variables, stack traces,
        breakpoints (including conditional and exception breakpoints), expression evaluation with full
        Java syntax support, variable modification, and execution control. Requires user confirmation
        before executing any debug action.
        Available actions: get_state, get_variables, get_stack_trace, evaluate_expression, set_variable,
        set_breakpoint (supports conditions and hit counts), remove_breakpoint, list_breakpoints,
        set_exception_breakpoint, remove_exception_breakpoint, step_over, step_into, step_out,
        continue, suspend.
        """);

    InputSchema inputSchema = new InputSchema();
    inputSchema.setType("object");

    Map<String, InputSchemaPropertyValue> properties = new HashMap<>();

    InputSchemaPropertyValue actionProperty = new InputSchemaPropertyValue("string");
    actionProperty.setDescription("""
        The debug action to perform. Valid actions: get_state, get_variables, get_stack_trace,
        evaluate_expression (supports full Java expressions), set_variable (modify variable values),
        set_breakpoint (supports conditions and hit counts), remove_breakpoint, list_breakpoints,
        set_exception_breakpoint, remove_exception_breakpoint, step_over, step_into, step_out,
        continue, suspend.
        """);
    properties.put(ACTION, actionProperty);

    InputSchemaPropertyValue depthProperty = new InputSchemaPropertyValue("integer");
    depthProperty.setDescription(
        "For get_variables: maximum depth for nested object traversal (default: 2)");
    properties.put(DEPTH, depthProperty);

    InputSchemaPropertyValue expressionProperty = new InputSchemaPropertyValue("string");
    expressionProperty.setDescription(
        "For evaluate_expression: the Java expression to evaluate "
        + "(supports full Java syntax including method calls, operators, field access)");
    properties.put(EXPRESSION, expressionProperty);

    InputSchemaPropertyValue fileProperty = new InputSchemaPropertyValue("string");
    fileProperty.setDescription(
        "For set_breakpoint: the file path where the breakpoint should be set");
    properties.put(FILE, fileProperty);

    InputSchemaPropertyValue lineProperty = new InputSchemaPropertyValue("integer");
    lineProperty.setDescription(
        "For set_breakpoint/remove_breakpoint: the line number");
    properties.put(LINE, lineProperty);

    InputSchemaPropertyValue conditionProperty = new InputSchemaPropertyValue("string");
    conditionProperty.setDescription(
        "For set_breakpoint: optional Java boolean expression condition "
        + "(e.g., 'count > 100 && status == Status.ACTIVE')");
    properties.put(CONDITION, conditionProperty);

    InputSchemaPropertyValue hitCountProperty = new InputSchemaPropertyValue("integer");
    hitCountProperty.setDescription(
        "For set_breakpoint: optional hit count - break after N hits (0 = always break, default: 0)");
    properties.put(HIT_COUNT, hitCountProperty);

    InputSchemaPropertyValue variableNameProperty = new InputSchemaPropertyValue("string");
    variableNameProperty.setDescription(
        "For set_variable: the name of the variable to modify");
    properties.put(VARIABLE_NAME, variableNameProperty);

    InputSchemaPropertyValue valueProperty = new InputSchemaPropertyValue("string");
    valueProperty.setDescription(
        "For set_variable: the new value for the variable (as string, will be parsed based on variable type)");
    properties.put(VALUE, valueProperty);

    InputSchemaPropertyValue exceptionTypeProperty = new InputSchemaPropertyValue("string");
    exceptionTypeProperty.setDescription(
        "For set_exception_breakpoint/remove_exception_breakpoint: "
        + "fully qualified exception class name (e.g., 'java.lang.NullPointerException')");
    properties.put(EXCEPTION_TYPE, exceptionTypeProperty);

    InputSchemaPropertyValue caughtProperty = new InputSchemaPropertyValue("boolean");
    caughtProperty.setDescription(
        "For set_exception_breakpoint: break on caught exceptions (default: true)");
    properties.put(CAUGHT, caughtProperty);

    InputSchemaPropertyValue uncaughtProperty = new InputSchemaPropertyValue("boolean");
    uncaughtProperty.setDescription(
        "For set_exception_breakpoint: break on uncaught exceptions (default: true)");
    properties.put(UNCAUGHT, uncaughtProperty);

    InputSchemaPropertyValue threadIdProperty = new InputSchemaPropertyValue("integer");
    threadIdProperty.setDescription(
        "Thread unique ID to use for operations. "
        + "Use -1 or omit to automatically select the first suspended thread (recommended). "
        + "Use the threadId value from get_state results to select a specific thread.");
    properties.put(THREAD_ID, threadIdProperty);

    inputSchema.setProperties(properties);
    inputSchema.setRequired(List.of(ACTION));

    toolInfo.setInputSchema(inputSchema);

    return toolInfo;
  }

  @Override
  public CompletableFuture<LanguageModelToolResult[]> invoke(Map<String, Object> input, ChatView chatView) {
    String action = (String) input.get(ACTION);
    if (StringUtils.isBlank(action)) {
      String errorMsg = "Action parameter is required";
      CopilotCore.LOGGER.error(errorMsg, new IllegalArgumentException(errorMsg));
      return CompletableFuture.completedFuture(createErrorResult(errorMsg));
    }

    return CompletableFuture.supplyAsync(() -> {
      try {
        return executeAction(action, input);
      } catch (Exception e) {
        String errorMsg = "Failed to execute action '" + action + "': " + e.getMessage();
        CopilotCore.LOGGER.error(errorMsg, e);
        return createErrorResult(errorMsg);
      }
    });
  }

  private LanguageModelToolResult[] executeAction(String action, Map<String, Object> input) {
    return switch (action) {
      case "get_state" -> getState();
      case "get_variables" -> getVariables(input);
      case "get_stack_trace" -> getStackTrace(input);
      case "evaluate_expression" -> evaluateExpression(input);
      case "set_breakpoint" -> setBreakpoint(input);
      case "remove_breakpoint" -> removeBreakpoint(input);
      case "list_breakpoints" -> listBreakpoints();
      case "set_variable" -> setVariable(input);
      case "set_exception_breakpoint" -> setExceptionBreakpoint(input);
      case "remove_exception_breakpoint" -> removeExceptionBreakpoint(input);
      case "step_over" -> stepOver(input);
      case "step_into" -> stepInto(input);
      case "step_out" -> stepOut(input);
      case "continue" -> continueExecution(input);
      case "suspend" -> suspendExecution(input);
      default -> createErrorResult("Unknown action: " + action);
    };
  }

  private LanguageModelToolResult[] getState() {
    IJavaDebugTarget debugTarget = getActiveJavaDebugTarget();
    if (debugTarget == null) {
      return createErrorResult("No active Java debug session found. " + "Start debugging a Java application first.");
    }

    Map<String, Object> result = new HashMap<>();
    result.put("success", true);
    result.put("action", "get_state");

    Map<String, Object> data = new HashMap<>();
    try {
      data.put("terminated", debugTarget.isTerminated());
      data.put("suspended", debugTarget.isSuspended());
      data.put("canSuspend", debugTarget.canSuspend());
      data.put("canResume", debugTarget.canResume());
      data.put("canDisconnect", debugTarget.canDisconnect());
      data.put("name", debugTarget.getName());

      List<Map<String, Object>> threads = new ArrayList<>();
      IThread[] allThreads = debugTarget.getThreads();
      for (int i = 0; i < allThreads.length; i++) {
        IThread thread = allThreads[i];
        Map<String, Object> threadInfo = new HashMap<>();
        threadInfo.put("name", thread.getName());
        threadInfo.put("suspended", thread.isSuspended());
        threadInfo.put("canSuspend", thread.canSuspend());
        threadInfo.put("canResume", thread.canResume());
        threadInfo.put("canStepOver", thread.canStepOver());
        threadInfo.put("canStepInto", thread.canStepInto());
        threadInfo.put("canStepReturn", thread.canStepReturn());
        if (thread instanceof IJavaThread javaThread) {
          threadInfo.put("threadId", javaThread.getThreadObject().getUniqueId());
        }
        threads.add(threadInfo);
      }
      data.put("threads", threads);

    } catch (DebugException e) {
      data.put("error", "Failed to get debug state: " + e.getMessage());
    }

    result.put("data", data);
    return createResult(gson.toJson(result));
  }

  private LanguageModelToolResult[] getVariables(Map<String, Object> input) {
    IJavaDebugTarget debugTarget = getActiveJavaDebugTarget();
    if (debugTarget == null) {
      return createErrorResult("No active Java debug session found.");
    }

    int depth = getIntParameter(input, DEPTH, 2);
    long threadId = getLongParameter(input, THREAD_ID, -1L);

    Map<String, Object> result = new HashMap<>();
    result.put("success", true);
    result.put("action", "get_variables");

    try {
      IJavaStackFrame stackFrame = getSuspendedStackFrame(debugTarget, threadId);
      if (stackFrame == null) {
        return createErrorResult(
            "No suspended thread found. " + "The debug target must be suspended at a breakpoint to inspect variables. "
                + "Use get_state to see thread information.");
      }

      Map<String, Object> data = new HashMap<>();
      List<Map<String, Object>> variables = new ArrayList<>();

      for (IVariable variable : stackFrame.getVariables()) {
        if (variable instanceof IJavaVariable javaVar) {
          variables.add(buildVariableInfo(javaVar, depth, 0));
        }
      }

      data.put("variables", variables);
      data.put("stackFrame", stackFrame.getName());
      data.put("threadId", threadId);
      result.put("data", data);

    } catch (DebugException e) {
      result.put("success", false);
      result.put("error", "Failed to get variables: " + e.getMessage());
    }

    return createResult(gson.toJson(result));
  }

  private LanguageModelToolResult[] getStackTrace(Map<String, Object> input) {
    IJavaDebugTarget debugTarget = getActiveJavaDebugTarget();
    if (debugTarget == null) {
      return createErrorResult("No active Java debug session found.");
    }

    long threadId = getLongParameter(input, THREAD_ID, -1L);

    Map<String, Object> result = new HashMap<>();
    result.put("success", true);
    result.put("action", "get_stack_trace");

    try {
      IJavaThread thread = getSuspendedThread(debugTarget, threadId);
      if (thread == null) {
        return createErrorResult(
            "No suspended thread found. " + "The debug target must be suspended at a breakpoint to get stack trace. "
                + "Use get_state to see thread information.");
      }

      Map<String, Object> data = new HashMap<>();
      List<Map<String, Object>> frames = new ArrayList<>();

      for (IStackFrame frame : thread.getStackFrames()) {
        if (frame instanceof IJavaStackFrame javaFrame) {
          Map<String, Object> frameInfo = new HashMap<>();
          frameInfo.put("name", javaFrame.getName());
          frameInfo.put("lineNumber", javaFrame.getLineNumber());
          frameInfo.put("declaringTypeName", javaFrame.getDeclaringTypeName());
          frameInfo.put("methodName", javaFrame.getMethodName());
          frameInfo.put("signature", javaFrame.getSignature());
          frameInfo.put("sourceName", javaFrame.getSourceName());
          frameInfo.put("sourcePath", javaFrame.getSourcePath());
          frames.add(frameInfo);
        }
      }

      data.put("frames", frames);
      data.put("threadName", thread.getName());
      data.put("threadId", threadId);
      result.put("data", data);

    } catch (DebugException e) {
      result.put("success", false);
      result.put("error", "Failed to get stack trace: " + e.getMessage());
    }

    return createResult(gson.toJson(result));
  }

  private LanguageModelToolResult[] evaluateExpression(Map<String, Object> input) {
    IJavaDebugTarget debugTarget = getActiveJavaDebugTarget();
    if (debugTarget == null) {
      return createErrorResult("No active Java debug session found.");
    }

    String expression = (String) input.get(EXPRESSION);
    if (StringUtils.isBlank(expression)) {
      return createErrorResult("Expression parameter is required for evaluate_expression action.");
    }

    long threadId = getLongParameter(input, THREAD_ID, -1L);

    Map<String, Object> result = new HashMap<>();
    result.put("success", true);
    result.put("action", "evaluate_expression");

    try {
      IJavaStackFrame stackFrame = getSuspendedStackFrame(debugTarget, threadId);
      if (stackFrame == null) {
        return createErrorResult(
            "No suspended thread found. " + "The debug target must be suspended at a breakpoint to "
                + "evaluate expressions. Use get_state to see thread information.");
      }

      // Use AST evaluation engine for full Java expression support
      IJavaProject javaProject = findJavaProject(stackFrame);

      if (javaProject == null || !javaProject.exists()) {
        CopilotCore.LOGGER.error(new IllegalStateException(
            "Failed to find Java project for expression evaluation. "
            + "This error helps track how many users encounter project detection issues."));
        return createErrorResult("Unable to find Java project for expression evaluation. "
            + "Ensure a Java project is open in the workspace.");
      }

      IAstEvaluationEngine engine = JDIDebugPlugin.getDefault().getEvaluationEngine(javaProject, debugTarget);

      // Use CompletableFuture for cleaner async evaluation
      CompletableFuture<Map<String, Object>> evalFuture = new CompletableFuture<>();

      IEvaluationListener listener = new IEvaluationListener() {
        @Override
        public void evaluationComplete(IEvaluationResult evalResultObj) {
          Map<String, Object> evalResult = new HashMap<>();
          if (evalResultObj.hasErrors()) {
            String[] errors = evalResultObj.getErrorMessages();
            evalResult.put("error", String.join("; ", errors));
          } else {
            IValue value = evalResultObj.getValue();
            if (value instanceof IJavaValue javaValue) {
              try {
                evalResult.put("value", javaValue.getValueString());
                evalResult.put("type", javaValue.getReferenceTypeName());
              } catch (DebugException e) {
                evalResult.put("error", "Failed to get value: " + e.getMessage());
              }
            }
          }
          evalFuture.complete(evalResult);
        }
      };

      // Compile and evaluate the expression
      ICompiledExpression compiledExpression = engine.getCompiledExpression(expression, stackFrame);
      engine.evaluateExpression(compiledExpression, stackFrame, listener,
          DebugEvent.EVALUATION_IMPLICIT, false);

      // Wait for evaluation with timeout
      Map<String, Object> evalResult;
      try {
        evalResult = evalFuture.get(EVALUATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
      } catch (TimeoutException e) {
        return createErrorResult("Expression evaluation timed out");
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return createErrorResult("Expression evaluation interrupted");
      } catch (Exception e) {
        return createErrorResult("Expression evaluation failed: " + e.getMessage());
      }

      // Check if evaluation completed
      if (evalResult.containsKey("error")) {
        return createErrorResult("Expression evaluation failed: " + evalResult.get("error"));
      }

      if (!evalResult.containsKey("value")) {
        return createErrorResult("Expression evaluation timed out or produced no result");
      }

      Map<String, Object> data = new HashMap<>();
      data.put("expression", expression);
      data.put("value", evalResult.get("value"));
      data.put("type", evalResult.get("type"));
      data.put("threadId", threadId);
      result.put("data", data);

    } catch (Exception e) {
      result.put("success", false);
      result.put("error", "Failed to evaluate expression: " + e.getMessage());
    }

    return createResult(gson.toJson(result));
  }

  private LanguageModelToolResult[] setBreakpoint(Map<String, Object> input) {
    String filePath = (String) input.get(FILE);
    if (StringUtils.isBlank(filePath)) {
      return createErrorResult("File parameter is required for set_breakpoint action.");
    }

    Integer lineNumber = getIntParameter(input, LINE, null);
    if (lineNumber == null) {
      return createErrorResult("Line parameter is required for set_breakpoint action.");
    }

    String condition = (String) input.get(CONDITION);
    Integer hitCount = getIntParameter(input, HIT_COUNT, 0);

    Map<String, Object> result = new HashMap<>();

    try {
      IResource resource = findResource(filePath);
      if (resource == null || !resource.exists()) {
        return createErrorResult("File not found: " + filePath);
      }

      if (!(resource instanceof IFile)) {
        return createErrorResult("Resource is not a file: " + filePath);
      }

      // Get the type name from the Java file
      String typeName = getTypeNameFromResource(resource);

      // Create the breakpoint with proper registration
      IJavaLineBreakpoint breakpoint = JDIDebugModel.createLineBreakpoint(resource,
          typeName, // fully qualified type name (required for validation)
          lineNumber, -1, // char start (unknown)
          -1, // char end (unknown)
          hitCount, // hit count (0 = no restriction, always hit)
          true, // register with breakpoint manager
          null // attributes
      );

      // Set conditional breakpoint if condition is provided
      if (!StringUtils.isBlank(condition)) {
        breakpoint.setCondition(condition);
        breakpoint.setConditionEnabled(true);
      }

      // Validate the breakpoint was created successfully
      IMarker marker = breakpoint.getMarker();
      if (marker == null || !marker.exists()) {
        return createErrorResult("Failed to create breakpoint marker. " + "The line may not contain executable code.");
      }

      result.put("success", true);
      result.put("action", "set_breakpoint");

      // Convert breakpoint to info map
      Map<String, Object> data = breakpointToInfo(breakpoint, false);
      if (data == null) {
        return createErrorResult("Failed to create breakpoint marker. " + "The line may not contain executable code.");
      }

      // Override file path with the input path for consistency
      data.put("file", filePath);

      // Add type name information
      data.put("typeName", typeName);

      // Add warning if type name couldn't be resolved
      if (typeName == null) {
        data.put("warning", "Type name could not be resolved. " + "Breakpoint may not be validated properly.");
      }

      result.put("data", data);

    } catch (CoreException e) {
      result.put("success", false);
      result.put("error", "Failed to set breakpoint: " + e.getMessage());
    }

    return createResult(gson.toJson(result));
  }

  private LanguageModelToolResult[] removeBreakpoint(Map<String, Object> input) {
    String filePath = (String) input.get(FILE);
    Integer lineNumber = getIntParameter(input, LINE, null);

    Map<String, Object> result = new HashMap<>();

    try {
      IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints();
      boolean found = false;

      for (IBreakpoint breakpoint : breakpoints) {
        if (breakpoint instanceof IJavaLineBreakpoint lineBreakpoint) {
          IMarker marker = lineBreakpoint.getMarker();
          if (marker != null && marker.exists()) {
            boolean matches = true;

            if (!StringUtils.isBlank(filePath)) {
              IResource resource = marker.getResource();

              // Normalize paths for comparison (handle both absolute and workspace-relative paths)
              String normalizedInputPath = filePath.replace('\\', '/');

              String bpAbsolutePath = resource.getLocation() != null
                  ? resource.getLocation().toString().replace('\\', '/')
                  : null;
              String bpWorkspacePath = resource.getFullPath().toString();

              // Match against either absolute path or workspace-relative path
              matches = false;
              if (bpAbsolutePath != null && bpAbsolutePath.equals(normalizedInputPath)) {
                matches = true;
              } else if (bpWorkspacePath.equals(normalizedInputPath)) {
                matches = true;
              } else if (normalizedInputPath.endsWith(bpWorkspacePath)) {
                // Fallback: check if input path ends with workspace path
                matches = true;
              } else if (bpAbsolutePath != null && bpAbsolutePath.endsWith(normalizedInputPath)) {
                // Fallback: check if breakpoint absolute path ends with input path
                matches = true;
              }
            }

            if (lineNumber != null && matches) {
              int bpLine = lineBreakpoint.getLineNumber();
              matches = (bpLine == lineNumber);
            }

            if (matches) {
              breakpoint.delete();
              found = true;
              break;
            }
          }
        }
      }

      if (found) {
        result.put("success", true);
        result.put("action", "remove_breakpoint");
        Map<String, Object> data = new HashMap<>();
        data.put("file", filePath);
        data.put("line", lineNumber);
        result.put("data", data);
      } else {
        return createErrorResult("No matching breakpoint found.");
      }

    } catch (CoreException e) {
      result.put("success", false);
      result.put("error", "Failed to remove breakpoint: " + e.getMessage());
    }

    return createResult(gson.toJson(result));
  }

  private LanguageModelToolResult[] listBreakpoints() {
    Map<String, Object> result = new HashMap<>();
    result.put("success", true);
    result.put("action", "list_breakpoints");

    List<Map<String, Object>> breakpoints = new ArrayList<>();

    try {
      for (IBreakpoint breakpoint : DebugPlugin.getDefault().getBreakpointManager().getBreakpoints()) {
        if (breakpoint instanceof IJavaBreakpoint javaBreakpoint) {
          Map<String, Object> bpInfo = breakpointToInfo(javaBreakpoint, true);
          if (bpInfo != null) {
            breakpoints.add(bpInfo);
          }
        }
      }

      Map<String, Object> data = new HashMap<>();
      data.put("breakpoints", breakpoints);
      result.put("data", data);

    } catch (CoreException e) {
      result.put("success", false);
      result.put("error", "Failed to list breakpoints: " + e.getMessage());
    }

    return createResult(gson.toJson(result));
  }

  private LanguageModelToolResult[] setVariable(Map<String, Object> input) {
    IJavaDebugTarget debugTarget = getActiveJavaDebugTarget();
    if (debugTarget == null) {
      return createErrorResult("No active Java debug session found.");
    }

    String variableName = (String) input.get(VARIABLE_NAME);
    if (StringUtils.isBlank(variableName)) {
      return createErrorResult("variableName parameter is required for set_variable action.");
    }

    String value = (String) input.get(VALUE);
    if (value == null) {
      return createErrorResult("value parameter is required for set_variable action.");
    }

    long threadId = getLongParameter(input, THREAD_ID, -1L);

    Map<String, Object> result = new HashMap<>();
    result.put("success", true);
    result.put("action", "set_variable");

    try {
      IJavaStackFrame stackFrame = getSuspendedStackFrame(debugTarget, threadId);
      if (stackFrame == null) {
        return createErrorResult(
            "No suspended thread found. " + "The debug target must be suspended at a breakpoint to modify variables. "
                + "Use get_state to see thread information.");
      }

      // Find the variable in the current stack frame
      IVariable[] variables = stackFrame.getVariables();
      IJavaVariable targetVariable = null;

      for (IVariable var : variables) {
        if (var.getName().equals(variableName) && var instanceof IJavaVariable) {
          targetVariable = (IJavaVariable) var;
          break;
        }
      }

      if (targetVariable == null) {
        String availableVars = String.join(", ", java.util.Arrays.stream(variables).map(v -> {
          try {
            return v.getName();
          } catch (Exception e) {
            return "?";
          }
        }).toArray(String[]::new));
        return createErrorResult("Variable '" + variableName + "' not found in current scope. "
            + "Available variables: " + availableVars);
      }

      // Create new value based on variable type
      IJavaValue currentValue = (IJavaValue) targetVariable.getValue();
      String typeName = currentValue.getReferenceTypeName();
      IJavaValue newValue;

      // Handle primitive types and strings
      if ("java.lang.String".equals(typeName) || "String".equals(typeName)) {
        newValue = debugTarget.newValue(value);
      } else if ("int".equals(typeName) || "java.lang.Integer".equals(typeName)) {
        newValue = debugTarget.newValue(Integer.parseInt(value));
      } else if ("long".equals(typeName) || "java.lang.Long".equals(typeName)) {
        newValue = debugTarget.newValue(Long.parseLong(value));
      } else if ("double".equals(typeName) || "java.lang.Double".equals(typeName)) {
        newValue = debugTarget.newValue(Double.parseDouble(value));
      } else if ("float".equals(typeName) || "java.lang.Float".equals(typeName)) {
        newValue = debugTarget.newValue(Float.parseFloat(value));
      } else if ("boolean".equals(typeName) || "java.lang.Boolean".equals(typeName)) {
        newValue = debugTarget.newValue(Boolean.parseBoolean(value));
      } else if ("byte".equals(typeName) || "java.lang.Byte".equals(typeName)) {
        newValue = debugTarget.newValue(Byte.parseByte(value));
      } else if ("short".equals(typeName) || "java.lang.Short".equals(typeName)) {
        newValue = debugTarget.newValue(Short.parseShort(value));
      } else if ("char".equals(typeName) || "java.lang.Character".equals(typeName)) {
        if (value.length() != 1) {
          return createErrorResult("Invalid char value: must be a single character");
        }
        newValue = debugTarget.newValue(value.charAt(0));
      } else if ("null".equals(value)) {
        newValue = debugTarget.nullValue();
      } else {
        return createErrorResult("Unsupported type for value modification: " + typeName + ". "
            + "Only primitive types, strings, and null are supported.");
      }

      // Set the new value
      targetVariable.setValue(newValue);

      Map<String, Object> data = new HashMap<>();
      data.put("variableName", variableName);
      data.put("oldValue", currentValue.getValueString());
      data.put("newValue", value);
      data.put("type", typeName);
      data.put("threadId", threadId);
      result.put("data", data);

    } catch (NumberFormatException e) {
      result.put("success", false);
      result.put("error", "Invalid value format: " + e.getMessage());
    } catch (DebugException e) {
      result.put("success", false);
      result.put("error", "Failed to set variable: " + e.getMessage());
    }

    return createResult(gson.toJson(result));
  }

  private LanguageModelToolResult[] setExceptionBreakpoint(Map<String, Object> input) {
    String exceptionType = (String) input.get(EXCEPTION_TYPE);
    if (StringUtils.isBlank(exceptionType)) {
      return createErrorResult("exceptionType parameter is required for set_exception_breakpoint action.");
    }

    boolean caught = getBooleanParameter(input, CAUGHT, true);
    boolean uncaught = getBooleanParameter(input, UNCAUGHT, true);

    Map<String, Object> result = new HashMap<>();

    try {
      // Use workspace root as resource for exception breakpoints
      IResource resource = ResourcesPlugin.getWorkspace().getRoot();

      IJavaExceptionBreakpoint breakpoint = JDIDebugModel.createExceptionBreakpoint(
          resource,
          exceptionType,
          caught,
          uncaught,
          false, // checked exceptions
          true,  // enabled
          null   // attributes
      );

      IMarker marker = breakpoint.getMarker();
      if (marker == null || !marker.exists()) {
        return createErrorResult("Failed to create exception breakpoint marker.");
      }

      result.put("success", true);
      result.put("action", "set_exception_breakpoint");
      Map<String, Object> data = new HashMap<>();
      data.put("exceptionType", exceptionType);
      data.put("caught", caught);
      data.put("uncaught", uncaught);
      data.put("breakpointId", marker.getId());
      data.put("enabled", breakpoint.isEnabled());
      result.put("data", data);

    } catch (CoreException e) {
      result.put("success", false);
      result.put("error", "Failed to set exception breakpoint: " + e.getMessage());
    }

    return createResult(gson.toJson(result));
  }

  private LanguageModelToolResult[] removeExceptionBreakpoint(Map<String, Object> input) {
    String exceptionType = (String) input.get(EXCEPTION_TYPE);
    if (StringUtils.isBlank(exceptionType)) {
      return createErrorResult("exceptionType parameter is required for remove_exception_breakpoint action.");
    }

    Map<String, Object> result = new HashMap<>();

    try {
      IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints();
      boolean found = false;

      for (IBreakpoint breakpoint : breakpoints) {
        if (breakpoint instanceof IJavaExceptionBreakpoint exceptionBp) {
          String bpExceptionType = exceptionBp.getTypeName();
          if (exceptionType.equals(bpExceptionType)) {
            breakpoint.delete();
            found = true;
            break;
          }
        }
      }

      if (found) {
        result.put("success", true);
        result.put("action", "remove_exception_breakpoint");
        Map<String, Object> data = new HashMap<>();
        data.put("exceptionType", exceptionType);
        result.put("data", data);
      } else {
        return createErrorResult("No exception breakpoint found for: " + exceptionType);
      }

    } catch (CoreException e) {
      result.put("success", false);
      result.put("error", "Failed to remove exception breakpoint: " + e.getMessage());
    }

    return createResult(gson.toJson(result));
  }

  private LanguageModelToolResult[] stepOver(Map<String, Object> input) {
    return performStepAction("step_over", input, IThread::canStepOver, IThread::stepOver);
  }

  private LanguageModelToolResult[] stepInto(Map<String, Object> input) {
    return performStepAction("step_into", input, IThread::canStepInto, IThread::stepInto);
  }

  private LanguageModelToolResult[] stepOut(Map<String, Object> input) {
    return performStepAction("step_out", input, IThread::canStepReturn, IThread::stepReturn);
  }

  private LanguageModelToolResult[] continueExecution(Map<String, Object> input) {
    IJavaDebugTarget debugTarget = getActiveJavaDebugTarget();
    if (debugTarget == null) {
      return createErrorResult("No active Java debug session found.");
    }

    Map<String, Object> result = new HashMap<>();

    try {
      if (debugTarget.canResume()) {
        debugTarget.resume();
        result.put("success", true);
        result.put("action", "continue");
        result.put("data", Map.of("message", "Execution resumed"));
      } else {
        return createErrorResult("Debug target cannot be resumed.");
      }
    } catch (DebugException e) {
      result.put("success", false);
      result.put("error", "Failed to continue execution: " + e.getMessage());
    }

    return createResult(gson.toJson(result));
  }

  private LanguageModelToolResult[] suspendExecution(Map<String, Object> input) {
    IJavaDebugTarget debugTarget = getActiveJavaDebugTarget();
    if (debugTarget == null) {
      return createErrorResult("No active Java debug session found.");
    }

    Map<String, Object> result = new HashMap<>();

    try {
      if (debugTarget.canSuspend()) {
        debugTarget.suspend();
        result.put("success", true);
        result.put("action", "suspend");
        result.put("data", Map.of("message", "Execution suspended"));
      } else {
        return createErrorResult("Debug target cannot be suspended.");
      }
    } catch (DebugException e) {
      result.put("success", false);
      result.put("error", "Failed to suspend execution: " + e.getMessage());
    }

    return createResult(gson.toJson(result));
  }

  private LanguageModelToolResult[] performStepAction(String actionName, Map<String, Object> input,
      ThreadCapabilityChecker checker, ThreadAction action) {
    IJavaDebugTarget debugTarget = getActiveJavaDebugTarget();
    if (debugTarget == null) {
      return createErrorResult("No active Java debug session found.");
    }

    long threadId = getLongParameter(input, THREAD_ID, -1L);

    Map<String, Object> result = new HashMap<>();

    try {
      IJavaThread thread = getSuspendedThread(debugTarget, threadId);
      if (thread == null) {
        return createErrorResult("No suspended thread found. The thread must be suspended at a breakpoint to step. "
            + "Use get_state to see thread information.");
      }

      if (!checker.check(thread)) {
        return createErrorResult("Thread cannot perform " + actionName);
      }

      action.execute(thread);
      result.put("success", true);
      result.put("action", actionName);
      Map<String, Object> data = new HashMap<>();
      data.put("message", "Step action performed");
      data.put("threadId", threadId);
      result.put("data", data);

    } catch (DebugException e) {
      result.put("success", false);
      result.put("error", "Failed to " + actionName + ": " + e.getMessage());
    }

    return createResult(gson.toJson(result));
  }

  @FunctionalInterface
  private interface ThreadCapabilityChecker {
    boolean check(IThread thread) throws DebugException;
  }

  @FunctionalInterface
  private interface ThreadAction {
    void execute(IThread thread) throws DebugException;
  }

  private IJavaDebugTarget getActiveJavaDebugTarget() {
    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    IDebugTarget[] debugTargets = launchManager.getDebugTargets();

    for (IDebugTarget target : debugTargets) {
      if (target instanceof IJavaDebugTarget javaTarget && !target.isTerminated()) {
        return javaTarget;
      }
    }

    return null;
  }

  /**
   * Finds the Java project associated with the given stack frame.
   * Tries strategies in order of efficiency:
   * 1. Launch configuration (most reliable)
   * 2. Source path mapping
   * 3. Declaring type lookup (slower, searches all projects)
   *
   * @param stackFrame the stack frame to find the project for
   * @return the Java project, or null if not found
   */
  private IJavaProject findJavaProject(IJavaStackFrame stackFrame) {
    // Try 1: Get project from launch configuration (most reliable)
    try {
      IFile launchFile = stackFrame.getLaunch().getLaunchConfiguration().getFile();
      if (launchFile != null) {
        IProject foundProject = launchFile.getProject();
        if (foundProject.isOpen() && foundProject.hasNature(JavaCore.NATURE_ID)) {
          return JavaCore.create(foundProject);
        }
      }
    } catch (CoreException e) {
      // Continue to next approach
    }

    // Try 2: Get project from source path
    try {
      String sourcePath = stackFrame.getSourcePath();
      if (sourcePath != null) {
        IFile[] files = ResourcesPlugin.getWorkspace().getRoot()
            .findFilesForLocationURI(new File(sourcePath).toURI());
        if (files.length > 0) {
          IProject foundProject = files[0].getProject();
          if (foundProject.isOpen() && foundProject.hasNature(JavaCore.NATURE_ID)) {
            return JavaCore.create(foundProject);
          }
        }
      }
    } catch (Exception e) {
      // Continue to next approach
    }

    // Try 3: Get project from the declaring type (slower, searches all projects)
    try {
      String typeName = stackFrame.getDeclaringTypeName();
      if (typeName != null) {
        IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for (IProject p : allProjects) {
          if (p.isOpen() && p.hasNature(JavaCore.NATURE_ID)) {
            IJavaProject jp = JavaCore.create(p);
            if (jp != null && jp.exists()) {
              IType type = jp.findType(typeName);
              if (type != null && type.exists()) {
                return jp;
              }
            }
          }
        }
      }
    } catch (Exception e) {
      // All approaches failed
    }

    return null;
  }

  private IJavaThread getSuspendedThread(IJavaDebugTarget debugTarget, long threadId) throws DebugException {
    IThread[] threads = debugTarget.getThreads();

    // If threadId is -1 or negative, automatically select the first suspended thread
    if (threadId < 0) {
      for (IThread thread : threads) {
        if (thread instanceof IJavaThread javaThread && thread.isSuspended()) {
          return javaThread;
        }
      }
      return null;
    }

    // Otherwise, find the thread by its unique ID
    for (IThread thread : threads) {
      if (thread instanceof IJavaThread javaThread && javaThread.isSuspended()) {
        if (javaThread.getThreadObject().getUniqueId() == threadId) {
          return javaThread;
        }
      }
    }

    return null;
  }

  private IJavaStackFrame getSuspendedStackFrame(IJavaDebugTarget debugTarget, long threadId) throws DebugException {
    IJavaThread thread = getSuspendedThread(debugTarget, threadId);
    if (thread != null && thread.hasStackFrames()) {
      IStackFrame[] frames = thread.getStackFrames();
      if (frames.length > 0 && frames[0] instanceof IJavaStackFrame) {
        return (IJavaStackFrame) frames[0];
      }
    }
    return null;
  }

  private Map<String, Object> buildVariableInfo(IJavaVariable variable, int maxDepth, int currentDepth)
      throws DebugException {
    Map<String, Object> varInfo = new HashMap<>();
    varInfo.put("name", variable.getName());

    // Handle ClassNotLoadedException when getting type information
    try {
      varInfo.put("type", variable.getReferenceTypeName());
    } catch (DebugException e) {
      if (e.getCause() instanceof ClassNotLoadedException) {
        varInfo.put("type", "<type not loaded>");
      } else {
        throw e;
      }
    }

    if (variable.getValue() instanceof IJavaValue javaValue) {
      varInfo.put("value", javaValue.getValueString());

      // Only traverse nested fields if we haven't reached max depth
      if (currentDepth < maxDepth) {
        try {
          IVariable[] nestedVars = javaValue.getVariables();
          if (nestedVars.length > 0) {
            List<Map<String, Object>> fields = new ArrayList<>();
            for (IVariable field : nestedVars) {
              if (field instanceof IJavaVariable javaField) {
                try {
                  fields.add(buildVariableInfo(javaField, maxDepth, currentDepth + 1));
                } catch (DebugException e) {
                  // If we can't inspect this field (e.g., ClassNotLoadedException), add a placeholder
                  if (e.getCause() instanceof ClassNotLoadedException) {
                    Map<String, Object> errorField = new HashMap<>();
                    errorField.put("name", javaField.getName());
                    errorField.put("type", "<type not loaded>");
                    errorField.put("value", "<unavailable>");
                    fields.add(errorField);
                  } else {
                    throw e;
                  }
                }
              }
            }
            if (!fields.isEmpty()) {
              varInfo.put("fields", fields);
            }
          }
        } catch (DebugException e) {
          // If we can't get nested variables at all, just skip them
          if (!(e.getCause() instanceof ClassNotLoadedException)) {
            throw e;
          }
        }
      }
    }

    return varInfo;
  }

  private IResource findResource(String filePath) {
    IPath path = new Path(filePath);

    // Try as workspace-relative path first
    IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
    if (file.exists()) {
      return file;
    }

    // Try as absolute path
    IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(path.toFile().toURI());
    if (files.length > 0) {
      return files[0];
    }

    // Try finding by name
    IResource foundMember = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
    if (foundMember != null) {
      return foundMember;
    }

    return null;
  }

  /**
   * Get the fully qualified type name from a Java resource file. This is required for JDT to properly validate and
   * install breakpoints.
   *
   * @param resource The Java file resource
   * @return The fully qualified type name, or null if not found
   */
  private String getTypeNameFromResource(IResource resource) {
    if (!(resource instanceof IFile)) {
      return null;
    }

    try {
      IJavaElement javaElement = JavaCore.create(resource);
      if (javaElement instanceof ICompilationUnit cu) {
        // Try to get the primary type (main public class matching the file name)
        IType primaryType = cu.findPrimaryType();
        if (primaryType != null && primaryType.exists()) {
          return primaryType.getFullyQualifiedName();
        }

        // Fallback: For files with only secondary types (no primary type),
        // use the first available type
        IType[] types = cu.getTypes();
        if (types.length > 0) {
          return types[0].getFullyQualifiedName();
        }
      }
    } catch (JavaModelException e) {
      // If we can't determine the type name, return null
      // The breakpoint will still be created but may not have the checkmark
    }

    return null;
  }

  private Integer getIntParameter(Map<String, Object> input, String key, Integer defaultValue) {
    Object value = input.get(key);
    if (value instanceof Integer intValue) {
      return intValue;
    } else if (value instanceof String strValue) {
      try {
        return Integer.parseInt(strValue);
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    } else if (value instanceof Double doubleValue) {
      return doubleValue.intValue();
    }
    return defaultValue;
  }

  private Long getLongParameter(Map<String, Object> input, String key, Long defaultValue) {
    Object value = input.get(key);
    if (value instanceof Long longValue) {
      return longValue;
    } else if (value instanceof Integer intValue) {
      return intValue.longValue();
    } else if (value instanceof String strValue) {
      try {
        return Long.parseLong(strValue);
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    } else if (value instanceof Double doubleValue) {
      return doubleValue.longValue();
    }
    return defaultValue;
  }

  private boolean getBooleanParameter(Map<String, Object> input, String key, boolean defaultValue) {
    Object value = input.get(key);
    if (value instanceof Boolean boolValue) {
      return boolValue;
    } else if (value instanceof String strValue) {
      return Boolean.parseBoolean(strValue);
    }
    return defaultValue;
  }

  private LanguageModelToolResult[] createResult(String content) {
    LanguageModelToolResult result = new LanguageModelToolResult();
    result.addContent(content);
    return new LanguageModelToolResult[] { result };
  }

  private LanguageModelToolResult[] createErrorResult(String errorMessage) {
    CopilotCore.LOGGER.error("Java debugger tool error: " + errorMessage, new RuntimeException(errorMessage));
    LanguageModelToolResult result = new LanguageModelToolResult();
    result.addContent(errorMessage);
    result.setStatus(ToolInvocationStatus.error);
    return new LanguageModelToolResult[] { result };
  }

  /**
   * Converts a Java breakpoint into a map of breakpoint information.
   *
   * @param javaBreakpoint the breakpoint to convert
   * @param includeAbsolutePath whether to include the absolute file path
   * @return a map containing breakpoint information, or null if the marker doesn't exist
   * @throws CoreException if there's an error accessing breakpoint properties
   */
  private Map<String, Object> breakpointToInfo(IJavaBreakpoint javaBreakpoint, boolean includeAbsolutePath)
      throws CoreException {
    IMarker marker = javaBreakpoint.getMarker();

    if (marker == null || !marker.exists()) {
      return null;
    }

    Map<String, Object> bpInfo = new HashMap<>();
    IResource resource = marker.getResource();

    // Add file path information
    bpInfo.put("file", resource.getFullPath().toString());
    if (includeAbsolutePath && resource.getLocation() != null) {
      bpInfo.put("absolutePath", resource.getLocation().toString());
    }

    // Add line breakpoint specific information
    if (javaBreakpoint instanceof IJavaLineBreakpoint lineBreakpoint) {
      bpInfo.put("line", lineBreakpoint.getLineNumber());
      bpInfo.put("type", "line");

      // Add condition if present
      String condition = lineBreakpoint.getCondition();
      if (!StringUtils.isBlank(condition)) {
        bpInfo.put("condition", condition);
        bpInfo.put("conditionEnabled", lineBreakpoint.isConditionEnabled());
      }

      // Add hit count if present
      int hitCount = lineBreakpoint.getHitCount();
      if (hitCount > 0) {
        bpInfo.put("hitCount", hitCount);
      }
    }

    // Add common breakpoint properties
    bpInfo.put("enabled", javaBreakpoint.isEnabled());
    bpInfo.put("registered", javaBreakpoint.isRegistered());
    bpInfo.put("markerExists", marker.exists());
    bpInfo.put("id", marker.getId());

    return bpInfo;
  }
}
