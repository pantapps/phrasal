package edu.stanford.nlp.mt.decoder.feat.sparse;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.stanford.nlp.mt.base.ConcreteRule;
import edu.stanford.nlp.mt.base.CoreNLPCache;
import edu.stanford.nlp.mt.base.FeatureValue;
import edu.stanford.nlp.mt.base.Featurizable;
import edu.stanford.nlp.mt.base.IString;
import edu.stanford.nlp.mt.base.PhraseAlignment;
import edu.stanford.nlp.mt.base.Sequence;
import edu.stanford.nlp.mt.base.Sequences;
import edu.stanford.nlp.mt.base.SimpleSequence;
import edu.stanford.nlp.mt.decoder.feat.DerivationFeaturizer;
import edu.stanford.nlp.mt.decoder.feat.FeatureUtils;
import edu.stanford.nlp.mt.decoder.feat.FeaturizerState;
import edu.stanford.nlp.mt.decoder.feat.NeedsCloneable;
import edu.stanford.nlp.mt.lm.LMState;
import edu.stanford.nlp.mt.lm.LanguageModel;
import edu.stanford.nlp.mt.lm.LanguageModelFactory;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.Generics;

/**
 * Featurizer for n-gram dependency language models.
 * 
 * @author Sebastian Schuster
 */
public class DependencyLanguageModelFeaturizer extends DerivationFeaturizer<IString, String> implements NeedsCloneable<IString, String> {

  public static String FEAT_NAME_WORD_PENALTY = "DEPLM_WORD_PENALTY";
  public static String FEAT_NAME = "DEPLM";
  private static LanguageModel<IString> leftLM ;
  private static LanguageModel<IString> rightLM;
  private static LanguageModel<IString> rootLM;

  
  
  private HashMap<Integer, HashSet<Integer>> forwardDependencies;
  private HashMap<Integer, Integer> reverseDependencies;
  
  public DependencyLanguageModelFeaturizer() {
  
  }
  
  public DependencyLanguageModelFeaturizer(String...args) throws IOException {
    Properties options = FeatureUtils.argsToProperties(args);
    if (!options.containsKey("leftLM")) {
        throw new RuntimeException(
              this.getClass().getName() + ": ERROR No left dependency LM file was specified!");
    }

    if (!options.containsKey("rightLM")) {
      throw new RuntimeException(
            this.getClass().getName() + ": ERROR No right dependency LM file was specified!");
    }
    
    if (!options.containsKey("rootLM")) {
      throw new RuntimeException(
            this.getClass().getName() + ": ERROR No root dependency LM file was specified!");
    }
    
    if (!options.containsKey("annotations")) {
      throw new RuntimeException(
            this.getClass().getName() + ": ERROR No dependency annotations file was specified!");
    }
    
    assert leftLM == null;
    leftLM = LanguageModelFactory.load(options.getProperty("leftLM"));
    assert rightLM == null;
    rightLM = LanguageModelFactory.load(options.getProperty("rightLM"));
    assert rootLM == null;
    rootLM = LanguageModelFactory.load(options.getProperty("rootLM"));

    
    CoreNLPCache.loadSerialized(options.getProperty("annotations"));

    

  }
       

  /*private SymmetricalWordAlignment partialAlignment(Featurizable<IString, String> f) {
    SymmetricalWordAlignment alignment = new SymmetricalWordAlignment(f.sourceSentence, f.targetPrefix);
    
    Featurizable<IString, String> prev = f;
    while (prev != null) {
      PhraseAlignment localAlignment = prev.rule.abstractRule.alignment;
      int sourceStart = prev.sourcePosition;
      int targetStart = prev.targetPosition;
      int targetPLen = prev.targetPhrase.size();
      for (int t = 0; t < targetPLen; t++) {
        for (int s : localAlignment.t2s(t)) {
          alignment.addAlign(sourceStart + s, targetStart + t);
        }
      }
      prev = prev.prior;
    }
    
    return alignment;
  }
  */
  /*private Map<Integer, NavigableSet<Integer>> partialDependencyProjection(CoreMap annotation, SymmetricalWordAlignment alignment, Featurizable<IString, String> f) {
    Map<Integer, NavigableSet<Integer>> projectedDependencies = Generics.newHashMap();

    SemanticGraph semanticGraph = annotation.get(BasicDependenciesAnnotation.class);

    Collection<TypedDependency> dependencies = semanticGraph.typedDependencies();
    HashMap<Integer, Integer> reverseDependencies = new HashMap<Integer, Integer>() ;
        
    for (TypedDependency dep : dependencies) {
      int govIndex = dep.gov().index() - 1;
      int depIndex = dep.dep().index() - 1;
      reverseDependencies.put(depIndex, govIndex);
    }
    

    int len = alignment.eSize();

    Integer tSentenceHeadIdx = -1;
    //add root
    Collection<IndexedWord> sentenceHeads = semanticGraph.getRoots();
    NavigableSet<Integer> tHeadIndices = null;
    if (!sentenceHeads.isEmpty()) {
      IndexedWord sentenceHead = semanticGraph.getFirstRoot();
      int sHeadIndex = sentenceHead.index() - 1;
      tHeadIndices = Generics.newTreeSet(alignment.f2e(sHeadIndex));
      if (tHeadIndices.size() == 1) {
        tSentenceHeadIdx = tHeadIndices.last();
        projectedDependencies.put(-1, tHeadIndices);
      } else if (tHeadIndices.size() > 1) {
        //make the right-most word the sentence head and make all the other words dependent of the head
        tSentenceHeadIdx = tHeadIndices.last();
        NavigableSet<Integer> depSet = Generics.newTreeSet();
        projectedDependencies.put(tSentenceHeadIdx, depSet);

        for (Integer idx : tHeadIndices ) {
          if (idx != tSentenceHeadIdx)
          projectedDependencies.get(tSentenceHeadIdx).add(idx);
        }
        
        NavigableSet<Integer> headSet = Generics.newTreeSet();
        headSet.add(tSentenceHeadIdx);
        projectedDependencies.put(-1, headSet);
      }
    }
    
    
    for (int j = 0; j < len; j++) {
      String word = alignment.e().get(j).word();
      //ignore garbage collection alignments to punction marks
      if (!Character.isAlphabetic(word.charAt(0))) 
        continue;

      //don't add dependencies for sentence heads
      if (tHeadIndices != null && tHeadIndices.contains(j))
        continue;
      
      SortedSet<Integer> sIdxs = alignment.e2f(j);
      Pair<Integer, Integer> dep = null; 
      //one-to-*      
      if (sIdxs.size() > 0) {
        //SortedSet<Integer> tIdxs = alignment.f2e(sIdxs.first());
        //one-to-1+
        //if (tIdxs.size()  > 0) {
          int depSIdx = sIdxs.first();
          
          
          int headIdx;
          while((reverseDependencies.get(depSIdx) != null) 
              && (headIdx = reverseDependencies.get(depSIdx)) > -1) {
            
            //check if this portion of the source sentence was already translated, otherwise stop
            if (!f.derivation.sourceCoverage.get(headIdx))
              break;

            SortedSet<Integer> headTIdxs = alignment.f2e(headIdx);
            //one-to-one and one-to-many
            if (headTIdxs.size() > 0 && (headTIdxs.first() !=  j || sIdxs.size() < 2)) {
              //prevent self loops and that the sentence head depends on any other word
              if (headTIdxs.first() !=  j && tSentenceHeadIdx != j) {
                dep = Generics.newPair(headTIdxs.last(), j);
              } 
              break;
            } else { //unaligned, try to map to transitive head
              depSIdx = headIdx;
            }
          }
      }
    
      if (dep != null) {
        if (projectedDependencies.get(dep.first()) == null) 
          projectedDependencies.put(dep.first(), new TreeSet<Integer>());
        projectedDependencies.get(dep.first()).add(dep.second());
      }
    }
    return projectedDependencies;
  }  

*/
  


  @Override
  public void initialize(int sourceInputId,
      List<ConcreteRule<IString, String>> ruleList, Sequence<IString> source) {
    SemanticGraph semanticGraph = CoreNLPCache.get(sourceInputId).get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
    this.forwardDependencies = new HashMap<Integer, HashSet<Integer>>();
    this.reverseDependencies = new HashMap<Integer, Integer>() ;
    for (TypedDependency dep : semanticGraph.typedDependencies()) {
      int govIndex = dep.gov().index() - 1;
      int depIndex = dep.dep().index() - 1;
      if (!this.forwardDependencies.containsKey(govIndex)) {
        this.forwardDependencies.put(govIndex, new HashSet<Integer>());
      }
      this.forwardDependencies.get(govIndex).add(depIndex);
      this.reverseDependencies.put(depIndex, govIndex);
    }
  }


  @Override
  public List<FeatureValue<String>> featurize(Featurizable<IString, String> f) {
    List<FeatureValue<String>> features = Generics.newLinkedList();
    DepLMState prevState = f.prior == null ? null : (DepLMState) f.prior.getState(this);
    DepLMState state = prevState == null ? new DepLMState() : prevState.clone();
    
    int targetLength = f.targetPhrase.size();
    PhraseAlignment alignment = f.rule.abstractRule.alignment;
    for (int i = 0; i < targetLength; i++) {
      IString token = f.targetPhrase.get(i);
      if (token.word().length() < 1 || !Character.isAlphabetic(token.word().charAt(0)))
        continue;
      if (alignment.t2s(i) == null || alignment.t2s(i).length < 1)
        continue;
      
      Integer sourceGovIndex = null;
      int sourceDepIndex = -1;
      for (int j = 0; j < alignment.t2s(i).length; j++) {
        sourceDepIndex = alignment.t2s(i)[j] + f.sourcePosition;
        if ((sourceGovIndex = this.reverseDependencies.get(sourceDepIndex)) != null)
          break;
      }
      
      //check if the current word has a head
      if (sourceGovIndex == null)
        continue; 

      //force 1:1 alignments, if the source token is already aligned to some other target token
      //then ignore the current token
      if (state.alignedSourceIndices.contains(sourceDepIndex)) {
        continue;
      }
      
      //check for root
      if (sourceGovIndex == -1) {
        double rootScore = rootLM.score(f.targetPhrase.subsequence(i, i+1), 0, null).getScore();
        features.add(new FeatureValue<String>(FEAT_NAME, rootScore));
        features.add(new FeatureValue<String>(FEAT_NAME_WORD_PENALTY, -1.0));
      } else {
        //check if head was already put down
        if (f.derivation.sourceCoverage.get(sourceGovIndex)) {
          //score right
          DepLMSubState subState = state.getSubState(sourceGovIndex);
          
          //if the substate exists but has no head token, then the head is not aligned
          //so clear the state
          if (subState == null || subState.headToken == null) {
            if (subState != null && subState.getLeftChildren().size() > 0) {
              subState.getLeftChildren().clear();
            }
            continue;
          }
      
          Sequence<IString> seq = f.targetPhrase.subsequence(i, i+1);
          //add start and end tokens
          int start = 0;
          if (subState.getRightLMState() == null) {
            seq = Sequences.wrapStart(seq, new IString(subState.headToken.word() + "<HEAD>"));
            seq = Sequences.wrapStart(seq, rightLM.getStartToken());
            start = 2;
          }
          LMState lmState = rightLM.score(seq, start, subState.getRightLMState());
          double rightScore = lmState.getScore();
          state.getSubState(sourceGovIndex).setRightLMState(lmState);
          features.add(new FeatureValue<String>(FEAT_NAME, rightScore));
          features.add(new FeatureValue<String>(FEAT_NAME_WORD_PENALTY, -1.0));
          //figure out how to score end token
        } else {
          //add the current token to the list that should be scored on the left
          DepLMSubState subState =  state.getSubState(sourceGovIndex);
          if (subState == null) {
            subState = state.addSubState(sourceGovIndex);
          }
          subState.getLeftChildren().add(token);
        }
          
      }
      DepLMSubState subState =  state.getSubState(sourceDepIndex);
      if (subState != null) {
        if (subState.getLeftChildren().size() > 0) {
          Collections.reverse(subState.getLeftChildren());
          Sequence<IString> seq = new SimpleSequence<IString>(subState.getLeftChildren());
          seq = Sequences.wrapStart(seq, new IString(token.word() + "<HEAD>"));
          seq = Sequences.wrapStartEnd(seq, leftLM.getStartToken(), leftLM.getEndToken());
          double leftScore = leftLM.score(seq, 2, null).getScore();
          features.add(new FeatureValue<String>(FEAT_NAME, leftScore));
          features.add(new FeatureValue<String>(FEAT_NAME_WORD_PENALTY, -1.0 * subState.getLeftChildren().size()));
          subState.getLeftChildren().clear();
        }
      } else if (this.forwardDependencies.get(sourceDepIndex) != null && !this.forwardDependencies.get(sourceDepIndex).isEmpty()) {
        subState = state.addSubState(sourceDepIndex);
      }
      
      if (subState != null) {
        subState.setHeadToken(token);
      }     
    }
    
    for (Integer i: state.getSubStates().keySet()) {
      if (state.getSubState(i) == null)
        continue;
      HashSet<Integer> deps = this.forwardDependencies.get(i);
      boolean del = f.derivation.sourceCoverage.get(i);
      if (del && deps != null) {
        for (Integer j : deps) {
          if (!f.derivation.sourceCoverage.get(j)) {
            del = false;
            break;
          }
        }
      }
      if (del) {
        //remove this head index from state
        state.setSubState(i, null);
      }
    }
    
    f.setState(this, state);
    
    return features;
  }
  
  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }
  
  public static class DepLMState extends FeaturizerState {

    private HashMap<Integer, DepLMSubState> subStates;
    private SortedSet<Integer> alignedSourceIndices;
    
    public DepLMState() {
      this.subStates = new HashMap<Integer, DepLMSubState>();
      this.alignedSourceIndices = Generics.newTreeSet();
    }
    
    @Override
    public boolean equals(Object other) {
      if (other == null)
        return false;
      if (other == this)
        return true;
      if (!(other instanceof DepLMState))
        return false;

      DepLMState oState = (DepLMState) other;
      
      if (oState.alignedSourceIndices.size() != this.alignedSourceIndices.size())
        return false;

      for (Integer i : this.alignedSourceIndices) {
        if (!oState.alignedSourceIndices.contains(i))
          return false;
      }
      
      for (Integer i : this.subStates.keySet()) {
        if ((oState.getSubState(i) == null || this.getSubState(i) == null)) {
          if (oState.getSubState(i) != this.getSubState(i))
            return false;
        } else {
          if (!this.subStates.get(i).equals(oState.getSubState(i)))
            return false;
        }
      }
      
      return true;
    }

    public HashMap<Integer, DepLMSubState> getSubStates() {
      return this.subStates;
    }

    public DepLMSubState getSubState(int i) {
      return this.subStates.get(i);
    }
    
    public DepLMSubState addSubState(int i) {
      this.subStates.put(i, new DepLMSubState());
      return this.subStates.get(i);
    }

    public void setSubState(int i, DepLMSubState subState) {
      this.subStates.put(i, subState);
    }

    
   
    
    
    @Override
    public int hashCode() {
      SortedSet<Integer> keys = new TreeSet<Integer>(this.getSubStates().keySet());
      int sz = this.alignedSourceIndices.size();
      for (int i : keys) {
        if (this.getSubState(i) != null)
          sz++;
      }

      if (sz == 0)
        return 0;
      int arr[] = new int[sz];
      int j = 0;
      for (int i: this.alignedSourceIndices) {
        arr[j] = i;
        j++;
      }
      for (int i : keys) {
        if (this.getSubState(i) != null) {
          arr[j] = this.getSubState(i).hashCode();
          j++;
        }
      }
      return arr.hashCode();
    }
    
    public DepLMState clone() {
      DepLMState copy = new DepLMState();
      for (Integer i : this.subStates.keySet()) {
        copy.setSubState(i, this.subStates.get(i) != null ? this.subStates.get(i).clone() : null);
      }
      copy.alignedSourceIndices.addAll(this.alignedSourceIndices);
      return copy;
    }
  }
  
  private static class DepLMSubState {
    
    private LMState rightLMState;
    private List<IString> leftChildren;
    private IString headToken;
    
    
    public DepLMSubState() {
      this.rightLMState = null;
      this.leftChildren = new LinkedList<IString>();
      this.headToken = null;
      
    }
    
    public void setHeadToken(IString token) {
      this.headToken = token;
    }
    
    public IString getHeadToken() {
      return this.headToken;
    }
    
    public void setRightLMState(LMState rightLMState){
      this.rightLMState = rightLMState;
    }
    
    @Override
    public boolean equals(Object other) {
      if (other == null)
        return false;
      if (other == this)
        return true;
      if (!(other instanceof DepLMSubState))
        return false;
      
      DepLMSubState oState = (DepLMSubState) other;
      
      if (rightLMState != null &&  oState.rightLMState != null && 
           !rightLMState.equals(oState.getRightLMState()))
        return false;
      
      if ((rightLMState == null || oState.rightLMState == null) &&
          oState.rightLMState != rightLMState)
        return false;

      int lChildrenCount = oState.getLeftChildren().size();
      if (this.leftChildren.size() != lChildrenCount)
        return false;
      
      for (int i = 0; i < lChildrenCount; i++) {
        if (!this.getLeftChildren().get(i).equals(oState.getLeftChildren().get(i)))
          return false;
      }
      
      return true;
    }
   
    //fix hash function such that it returns the same
    //val if equals returns true
    //arrays.hashCode
    
    public int hashCode() {
      int sz = 2 + this.getLeftChildren().size();
      int arr[] = new int[sz];
      arr[0] = this.headToken != null ? this.headToken.id : 0;
      arr[1] = this.rightLMState != null ? this.rightLMState.hashCode() : 0;
      int i = 2;
      for (IString c : this.getLeftChildren()) {
        arr[i] = c.id;
        i++;
      }
      return arr.hashCode();
    }
    
    public LMState getRightLMState() {
      return this.rightLMState;
    }
    
    public List<IString> getLeftChildren() {
      return this.leftChildren;
    }
    
    public DepLMSubState clone() {
      DepLMSubState copy = new DepLMSubState();
      copy.setRightLMState(this.rightLMState);
      for (IString child : this.getLeftChildren()) {
        copy.getLeftChildren().add(child);
      }
      copy.setHeadToken(this.headToken);
      return copy;
    }
    
  }
}