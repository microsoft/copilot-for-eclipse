// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.microsoft.copilot.eclipse.core.logger.GithubPanicErrorReport;
import com.microsoft.copilot.eclipse.core.utils.AnonymizeUtils;

/**
 * Parameter used for the checkStatus request.
 */
public class TelemetryExceptionParams {
  String transaction;
  String stacktrace;
  String platform = "java";
  @SerializedName("exception_detail")
  ArrayList<ExceptionDetail> exceptionDetail;

  /**
   * Default constructor.
   */
  public TelemetryExceptionParams() {
    this.exceptionDetail = new ArrayList<>();
  }

  /**
   * Constructor with exception.
   *
   * @param ex the exception.
   */
  public TelemetryExceptionParams(Throwable ex) {
    this.exceptionDetail = new ArrayList<>();

    do {
      var d = new ExceptionDetail();
      d.setType(ex.getClass().getName());
      d.setValue(AnonymizeUtils.removePii(ex.getMessage()));
      d.setStacktrace(ex.getStackTrace());
      this.exceptionDetail.add(d);
      ex = ex.getCause();
    } while (ex != null);
  }

  public String getTransaction() {
    return transaction;
  }

  public String getStacktrace() {
    return stacktrace;
  }

  public String getPlatform() {
    return platform;
  }

  public ArrayList<ExceptionDetail> getExceptionDetail() {
    return exceptionDetail;
  }

  public void setTransaction(String transaction) {
    this.transaction = transaction;
  }

  public void setStacktrace(String stacktrace) {
    this.stacktrace = stacktrace;
  }

  public void setPlatform(String platform) {
    this.platform = platform;
  }

  public void setExceptionDetail(ArrayList<ExceptionDetail> exceptionDetail) {
    this.exceptionDetail = exceptionDetail;
  }

  @Override
  public int hashCode() {
    return Objects.hash(exceptionDetail, platform, stacktrace, transaction);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    TelemetryExceptionParams other = (TelemetryExceptionParams) obj;
    return Objects.equals(exceptionDetail, other.exceptionDetail) && Objects.equals(platform, other.platform)
        && Objects.equals(stacktrace, other.stacktrace) && Objects.equals(transaction, other.transaction);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("transaction", transaction);
    builder.append("stacktrace", stacktrace);
    builder.append("platform", platform);
    builder.append("exceptionDetail", exceptionDetail);
    return builder.toString();
  }

  /**
   * The type of the exception.
   */
  public class ExceptionDetail {
    String type;
    String value;
    CopilotStackTraceElement[] stacktrace;

    /**
     * Default constructor.
     */
    public ExceptionDetail() {
      this.stacktrace = new CopilotStackTraceElement[0];
    }

    public String getType() {
      return type;
    }

    public String getValue() {
      return value;
    }

    public CopilotStackTraceElement[] getStacktrace() {
      return stacktrace;
    }

    public void setType(String type) {
      this.type = type;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public void setStacktrace(CopilotStackTraceElement[] stacktrace) {
      this.stacktrace = stacktrace;
    }

    /**
     * Set the stack trace.
     *
     * @param stacktrace the stack trace from exception.
     */
    public void setStacktrace(StackTraceElement[] stacktrace) {
      this.stacktrace = new CopilotStackTraceElement[stacktrace.length];
      int idx = 0;
      for (StackTraceElement element : stacktrace) {
        var cst = new CopilotStackTraceElement();
        cst.setFilename(GithubPanicErrorReport.computeFileName(element));
        cst.setLineno(String.valueOf(element.getLineNumber()));
        // colno is not available in StackTraceElement
        cst.setColno(String.valueOf(0));
        cst.setFunction(element.getMethodName());
        cst.setInApp(true);
        this.stacktrace[idx] = cst;
        idx++;
      }
    }

    @Override
    public int hashCode() {
      return Objects.hash(type, value, Arrays.hashCode(stacktrace));
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ExceptionDetail that = (ExceptionDetail) o;
      return Objects.equals(type, that.type) && Objects.equals(value, that.value)
          && Arrays.equals(stacktrace, that.stacktrace);
    }
  }

  /**
   * The stack trace element.
   */
  public class CopilotStackTraceElement {
    String filename;
    String lineno;
    String colno;
    String function;
    @SerializedName("in_app")
    boolean inApp;

    /**
     * Default constructor.
     */
    public CopilotStackTraceElement() {
    }

    public String getFilename() {
      return filename;
    }

    public String getLineno() {
      return lineno;
    }

    public String getColno() {
      return colno;
    }

    public String getFunction() {
      return function;
    }

    public boolean isInApp() {
      return inApp;
    }

    public void setFilename(String filename) {
      this.filename = filename;
    }

    public void setLineno(String lineno) {
      this.lineno = lineno;
    }

    public void setColno(String colno) {
      this.colno = colno;
    }

    public void setFunction(String function) {
      this.function = function;
    }

    public void setInApp(boolean inApp) {
      this.inApp = inApp;
    }

    @Override
    public int hashCode() {
      return Objects.hash(filename, lineno, colno, function, inApp);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CopilotStackTraceElement that = (CopilotStackTraceElement) o;
      return inApp == that.inApp && Objects.equals(filename, that.filename) && Objects.equals(lineno, that.lineno)
          && Objects.equals(colno, that.colno) && Objects.equals(function, that.function);
    }
  }
}
