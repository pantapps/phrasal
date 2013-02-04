package edu.stanford.nlp.mt.decoder.efeat;

import java.util.LinkedList;
import java.util.List;

import edu.stanford.nlp.mt.base.CacheableFeatureValue;
import edu.stanford.nlp.mt.base.ConcreteTranslationOption;
import edu.stanford.nlp.mt.base.FeatureValue;
import edu.stanford.nlp.mt.base.Featurizable;
import edu.stanford.nlp.mt.base.Sequence;
import edu.stanford.nlp.mt.base.IString;
import edu.stanford.nlp.mt.decoder.feat.IncrementalFeaturizer;
import edu.stanford.nlp.mt.decoder.feat.IsolatedPhraseFeaturizer;
import edu.stanford.nlp.util.Index;

/**
 * 
 * @author danielcer
 * 
 */
public class DiscriminativePhraseIdentity implements
    IncrementalFeaturizer<IString, String>,
    IsolatedPhraseFeaturizer<IString, String> {
  
  public static final String FEATURE_PREFIX = "DPI";
  public static final String SOURCE = ":src";
  public static final String TARGET = ":trg";
  public static final String SOURCE_AND_TARGET = ":s+t";
  public static final boolean DEFAULT_DO_SOURCE = true;
  public static final boolean DEFAULT_DO_TARGET = true;

  final boolean doSource;
  final boolean doTarget;

  public DiscriminativePhraseIdentity() {
    doSource = DEFAULT_DO_SOURCE;
    doTarget = DEFAULT_DO_TARGET;
  }

  public DiscriminativePhraseIdentity(String... args) {
    doSource = Boolean.parseBoolean(args[0]);
    doTarget = Boolean.parseBoolean(args[1]);
  }

  @Override
  public List<FeatureValue<String>> phraseListFeaturize(
      Featurizable<IString, String> f) {

    List<FeatureValue<String>> fvalues = new LinkedList<FeatureValue<String>>();

    if (doSource && doTarget) {
      fvalues.add(new CacheableFeatureValue<String>(FEATURE_PREFIX + SOURCE_AND_TARGET
          + ":" + f.foreignPhrase.toString("_") + "=>"
          + f.translatedPhrase.toString("_"), 1.0));
    } else if (doSource) {
      fvalues.add(new CacheableFeatureValue<String>(FEATURE_PREFIX + SOURCE + ":"
          + f.foreignPhrase.toString("_"), 1.0));
    } else if (doTarget) {
      fvalues.add(new CacheableFeatureValue<String>(FEATURE_PREFIX + TARGET + ":"
          + f.translatedPhrase.toString("_"), 1.0));
    }

    return fvalues;
  }

  @Override
  public void initialize(Index<String> featureIndex) {
  }

  @Override
  public FeatureValue<String> phraseFeaturize(Featurizable<IString, String> f) {
    return null;
  }

  @Override
  public void initialize(
      List<ConcreteTranslationOption<IString, String>> options,
      Sequence<IString> foreign, Index<String> featureIndex) {
  }

  @Override
  public void reset() {
  }

  @Override
  public List<FeatureValue<String>> listFeaturize(
      Featurizable<IString, String> f) {
    return null;
  }

  @Override
  public FeatureValue<String> featurize(Featurizable<IString, String> f) {
    return null;
  }
}
