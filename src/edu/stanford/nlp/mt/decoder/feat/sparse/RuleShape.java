package edu.stanford.nlp.mt.decoder.feat.sparse;

import java.util.LinkedList;
import java.util.List;

import edu.stanford.nlp.mt.decoder.feat.RuleFeaturizer;
import edu.stanford.nlp.mt.util.FeatureValue;
import edu.stanford.nlp.mt.util.Featurizable;
import edu.stanford.nlp.mt.util.IString;


/**
 * Shape of the translation rule.
 * 
 * @author Spence Green
 *
 */
public class RuleShape implements RuleFeaturizer<IString, String> {

  private static final String FEATURE_NAME = "RSHP";
  
  @Override
  public void initialize() {}

  @Override
  public List<FeatureValue<String>> ruleFeaturize(
      Featurizable<IString, String> f) {
    List<FeatureValue<String>> features = new LinkedList<>();
    String featureString = String.format("%s:%d-%d",FEATURE_NAME, 
        f.sourcePhrase.size(), f.targetPhrase.size());
    features.add(new FeatureValue<String>(featureString, 1.0));
    return features;
  }

  @Override
  public boolean isolationScoreOnly() {
    return false;
  }
}
