package io.jaegertracing.test.model;

/**
 * @author Pavol Loffay
 */
public class Span {

  private String operationName;
  private String spanID;
  private String traceID;

  public String getOperationName() {
    return operationName;
  }

  public void setOperationName(String operationName) {
    this.operationName = operationName;
  }

  public String getSpanID() {
    return spanID;
  }

  public void setSpanID(String spanID) {
    this.spanID = spanID;
  }

  public String getTraceID() {
    return traceID;
  }

  public void setTraceID(String traceID) {
    this.traceID = traceID;
  }
}
