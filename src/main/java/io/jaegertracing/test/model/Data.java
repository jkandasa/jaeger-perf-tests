package io.jaegertracing.test.model;

import java.util.List;

/**
 * @author Pavol Loffay
 */
public class Data {

  private List<Span> spans;
  private String traceID;

  public String getTraceID() {
    return traceID;
  }

  public void setTraceID(String traceID) {
    this.traceID = traceID;
  }

  public List<Span> getSpans() {
    return spans;
  }

  public void setSpans(List<Span> spans) {
    this.spans = spans;
  }
}
