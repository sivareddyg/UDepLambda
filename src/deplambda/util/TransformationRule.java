package deplambda.util;

import java.util.ArrayList;
import java.util.List;

import deplambda.protos.TransformationRulesProto.RuleGroups.RuleGroup.Rule;
import deplambda.protos.TransformationRulesProto.RuleGroups.RuleGroup.Rule.Transformation;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.trees.tregex.TregexPatternCompiler;

public class TransformationRule {
  private List<Rule> ruleList;

  // Priority of the rule.
  private int priority;

  // Name of the rule.
  private String name;

  // Tregex pattern.
  private TregexPattern tregex;

  // List of transformations.
  private List<Transformation> transformationList;

  /**
   * Similar to {@link Rule} but with cache to store tregex patterns.
   * 
   * @param ruleGroup
   */
  public TransformationRule(Rule rule) {

    if (rule.hasPriority())
      priority = rule.getPriority();

    if (rule.hasName())
      name = rule.getName();

    if (rule.hasTregex()) {
      tregex = TregexPatternCompiler.defaultCompiler.compile(rule.getTregex());
    }

    transformationList = new ArrayList<>(rule.getTransformationList());
  }

  public List<Rule> getRuleList() {
    return ruleList;
  }

  public int getPriority() {
    return priority;
  }

  public String getName() {
    return name;
  }

  public TregexPattern getTregex() {
    return tregex;
  }

  public List<Transformation> getTransformationList() {
    return transformationList;
  }
}
