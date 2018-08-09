package io.jaegertracing.test;

import io.opentracing.Span;

/**
 * @author Pavol Loffay
 */
public class NumberTagTagger {

  private int numberOfTags;

  public NumberTagTagger(int numberOfTags) {
    this.numberOfTags = numberOfTags;
  }

  public void setTag(Span span) {
    UniqueSpanTagger uniqueSpanTagger = new UniqueSpanTagger();
    for (int i = 0; i < numberOfTags; i++) {
      uniqueSpanTagger.setTag(span);
    }
  }

}
