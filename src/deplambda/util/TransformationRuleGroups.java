package deplambda.util;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.TextFormat;

import deplambda.protos.TransformationRulesProto.RuleGroups;
import deplambda.protos.TransformationRulesProto.RuleGroups.Relation;
import deplambda.protos.TransformationRulesProto.RuleGroups.RuleGroup;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.trees.tregex.TregexPatternCompiler;

/**
 * Similar to {@link RuleGroups} but the rule groups inside this object are
 * sorted.
 * 
 * @author Siva Reddy
 *
 */
public class TransformationRuleGroups {
  private List<TransformationRuleGroup> ruleGroupList;
  private List<Relation> relationList;

  private Map<String, TregexPattern> tregexCache;
  TregexPatternCompiler patternCompiler;
  private Map<String, Integer> relationPriority;

  public TransformationRuleGroups(String ruleFile)
      throws FileNotFoundException, IOException {
    patternCompiler = new TregexPatternCompiler();
    RuleGroups.Builder rulesBuilder = RuleGroups.newBuilder();
    TextFormat.merge(new FileReader(ruleFile), rulesBuilder);
    RuleGroups ruleGroups = rulesBuilder.build();

    ruleGroupList = new ArrayList<>();
    for (RuleGroup ruleGroup : ruleGroups.getRulegroupList()) {
      TransformationRuleGroup transformationRuleGroup =
          new TransformationRuleGroup(ruleGroup);
      ruleGroupList.add(transformationRuleGroup);
    }
    
    // Sort the groups based on the priority.
    ruleGroupList.sort(Comparator.comparing(r -> r.getPriority()));

    // Create relation priority map.
    relationPriority = new HashMap<>();
    relationList = new ArrayList<>(ruleGroups.getRelationList());
    relationList.sort(Comparator.comparing(r -> r.getPriority()));

    relationList.forEach(relation -> relationPriority.put(relation.getName(),
        relation.getPriority()));
  }

  public TregexPattern getTregex(String pattern) {
    tregexCache.putIfAbsent(pattern, patternCompiler.compile(pattern));
    return tregexCache.get(pattern);
  }

  public List<TransformationRuleGroup> getRuleGroupList() {
    return ruleGroupList;
  }

  public Map<String, Integer> getRelationPriority() {
    return relationPriority;
  }
}
