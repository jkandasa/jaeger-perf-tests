package io.jaegertracing.test.model;


import java.util.List;

/**
 * @author Pavol Loffay
 */
public class Result {

  private List<Data> data;

  public List<Data> getData() {
    return data;
  }

  public void setData(List<Data> data) {
    this.data = data;
  }
}
