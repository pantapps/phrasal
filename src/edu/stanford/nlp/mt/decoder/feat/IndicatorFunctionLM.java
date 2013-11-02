package edu.stanford.nlp.mt.decoder.feat;

import edu.stanford.nlp.mt.base.IString;
import edu.stanford.nlp.mt.base.Sequence;
import edu.stanford.nlp.mt.base.TokenUtils;
import edu.stanford.nlp.mt.lm.LanguageModel;

/**
 * 
 * @author danielcer
 * 
 */
public class IndicatorFunctionLM implements LanguageModel<IString> {
  public static final String NAME = "IndicatorFunctionLM";

  final int order;

  /**
   * 
   * @param <FV>
   */
  public <FV> IndicatorFunctionLM(int order) {
    this.order = order;
  }

  @Override
  public IString getEndToken() {
    return TokenUtils.END_TOKEN;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public IString getStartToken() {
    return TokenUtils.START_TOKEN;
  }

  @Override
  public int order() {
    return order;
  }

  @Override
  public boolean relevantPrefix(Sequence<IString> sequence) {
    // TODO: make this weight vector aware so we don't always need to return
    // 'true'
    // when sequence.size() <= order -1;
    return sequence.size() <= order - 1;
  }

  @Override
  public double score(Sequence<IString> sequence) {
    return 1.0;
  }

}
