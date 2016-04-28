package deplambda.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import deplambda.protos.TransformationRulesProto.RuleGroups.RuleGroup;
import deplambda.protos.TransformationRulesProto.RuleGroups.RuleGroup.Rule;

public class TransformationRuleGroup {
  private List<TransformationRule> ruleList;

  // Priority of the RuleGroup.
  private int priority;

  // Name of the RuleGroup.
  private String name;

  /**
   * Mutable form of {@link RuleGroup}. The main difference is that the rules in
   * this group are sorted based on their priority.
   * 
   * @param ruleGroup
   */
  public TransformationRuleGroup(RuleGroup ruleGroup) {
    ruleList = new ArrayList<>();
    if (ruleGroup.hasPriority())
      priority = ruleGroup.getPriority();

    if (ruleGroup.hasName())
      name = ruleGroup.getName();

    ruleList = new ArrayList<>();
    for (Rule rule : ruleGroup.getRuleList()) {
      ruleList.add(new TransformationRule(rule));
    }
    ruleList.sort(Comparator.comparing(r -> r.getPriority()));
  }

  public List<TransformationRule> getRuleList() {
    return ruleList;
  }

  public int getPriority() {
    return priority;
  }

  public String getName() {
    return name;
  }
}
