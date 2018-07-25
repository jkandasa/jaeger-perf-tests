package io.jaegertracing.test.model;


import java.util.List;

/**
 * @author Pavol Loffay
 */
public class Result {

  private List<Span> data;

  public List<Span> getData() {
    return data;
  }

  public void setData(List<Span> data) {
    this.data = data;
  }
}
