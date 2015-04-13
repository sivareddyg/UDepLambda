package uk.ac.ed.ilcc.deplambda.parser;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import jregex.Matcher;
import jregex.Pattern;
import uk.ac.ed.ilcc.deplambda.protos.TransformationRulesProto.RuleGroups.RuleGroup.Rule.Transformation;
import uk.ac.ed.ilcc.deplambda.util.DependencyTree;
import uk.ac.ed.ilcc.deplambda.util.TransformationRule;
import uk.ac.ed.ilcc.deplambda.util.TransformationRuleGroup;
import uk.ac.ed.ilcc.deplambda.util.TransformationRuleGroups;

import com.google.common.base.Preconditions;

import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.SimpleLogicalExpressionReader;

/**
 * Class containing methods to transform a tree using
 * {@link TransformationRuleGroups}. Transformation could be either changing the
 * tree, or assigning lambda functions to nodes in the tree.
 * 
 * @author Siva Reddy
 *
 */
public class TreeTransformer {


  /**
   * Applies rulegroups on all nodes in the tree in a pre-order fashion.
   * 
   * @param ruleGroups the ruleGroups that have to be applied.
   * @param tree the tree on which the ruleGroups have to be applied.
   * @return Returns true if at least one rulegroup is applied successfully on
   *         any node in the tree.
   */
  public static boolean ApplyRuleGroupsOnTree(
      TransformationRuleGroups ruleGroups, DependencyTree tree) {
    return ApplyRuleGroupsOnSubTree(ruleGroups, tree, tree);
  }

  private static boolean ApplyRuleGroupsOnSubTree(
      TransformationRuleGroups ruleGroups, DependencyTree tree,
      DependencyTree subTree) {
    Preconditions.checkNotNull(tree);

    // Apply ruleGroups at the current node.
    boolean ruleGroupFound = ApplyRuleGroupsOnNode(ruleGroups, tree, subTree);

    // Apply ruleGroups at children trees.
    for (Tree child : subTree.children()) {
      ruleGroupFound |=
          ApplyRuleGroupsOnSubTree(ruleGroups, tree, (DependencyTree) child);
    }
    return ruleGroupFound;
  }

  /**
   * Applies rulegroups on the target node. Rulegroups are applied in priority
   * order. If a rulegroup of higher priority applies successfully to a node, no
   * rulegroup of lower priority will be applied again.
   * 
   * @param ruleGroups the ruleGroups that have to be applied.
   * @param tree the tree in which the target node is present.
   * @param targetNode the target node on which the rulegroups have to be
   *        applied.
   * @return Returns true if at least one rule group is applied.
   */
  public static boolean ApplyRuleGroupsOnNode(
      TransformationRuleGroups ruleGroups, DependencyTree tree,
      DependencyTree targetNode) {
    int bestRuleSoFar = 0;
    boolean ruleGroupFound = false;
    for (TransformationRuleGroup ruleGroup : ruleGroups.getRuleGroupList()) {
      if (ruleGroupFound && ruleGroup.getPriority() > bestRuleSoFar)
        break;
      boolean ruleGroupApplied =
          ApplyRuleGroupOnNode(ruleGroup, tree, targetNode);
      if (ruleGroupApplied) {
        bestRuleSoFar = ruleGroup.getPriority();
        ruleGroupFound = true;
      }
    }
    return ruleGroupFound;
  }

  /**
   * Applies rulegroup on the specified node.
   * 
   * @param ruleGroup the ruleGroup that have to be applied.
   * @param tree the tree in which the target node is present.
   * @param targetNode the target node on which the rulegroup have to be
   *        applied.
   * @return Returns true if a rulegroup is successfully applied.
   */
  public static boolean ApplyRuleGroupOnNode(TransformationRuleGroup ruleGroup,
      DependencyTree tree, DependencyTree targetNode) {
    int bestRuleSoFar = 0;
    boolean ruleFound = false;
    for (TransformationRule rule : ruleGroup.getRuleList()) {
      if (ruleFound && rule.getPriority() > bestRuleSoFar)
        break;
      boolean ruleApplied = ApplyRuleOnNode(rule, tree, targetNode);
      if (ruleApplied) {
        bestRuleSoFar = rule.getPriority();
        ruleFound = true;
      }
    }
    return ruleFound;
  }

  /**
   * Applies rule on the root node.
   * 
   * @param rule the rule to be applied.
   * @param tree the tree in which the target node is present.
   * @param targetNode the target node on which the rule has to be applied.
   * @return Returns true if a rule group is successfully applied on the root
   *         node.
   */
  private static boolean ApplyRuleOnNode(TransformationRule rule,
      DependencyTree tree, DependencyTree targetNode) {
    TregexPattern tregex = rule.getTregex();
    TregexMatcher matcher = tregex.matcher(tree);
    if (matcher.matchesAt(targetNode)) {
      for (Transformation transformation : rule.getTransformationList()) {
        ApplyTransformation(transformation, matcher);
      }
      return true;
    }
    return false;
  }


  /**
   * Applies transformation on the nodes in the tree. The nodes on which
   * transformation has to be applied are retrieved from the
   * {@link TregexMatcher} argument.
   *
   * @param transformation the transformation that have to be applied.
   * @param matcher the matcher that contains all the named nodes.
   */
  private static void ApplyTransformation(Transformation transformation,
      TregexMatcher matcher) {
    String targetName = transformation.getTarget();
    DependencyTree targetNode = (DependencyTree) matcher.getNode(targetName);
    switch (transformation.getAction()) {
      case ADD_CHILD: {
        String child = transformation.getChild();
        String label = transformation.getLabel();
        DependencyTree labelTree = new DependencyTree(new Word(label));
        DependencyTree childTree = new DependencyTree(new Word(child));
        labelTree.addChild(childTree);
        targetNode.addChild(labelTree);
        break;
      }
      case ASSIGN_LAMBDA: {
        String lambda = transformation.getLambda();
        Pattern namedNodePattern = new Pattern("\\{(.+?)\\}");
        Matcher namedNodematcher = namedNodePattern.matcher(lambda);
        while (namedNodematcher.find()) {
          String namedNodeString = namedNodematcher.group(1);
          Tree namedNode = matcher.getNode(namedNodeString);
          lambda =
              lambda.replaceAll(String.format("\\{%s\\}", namedNodeString),
                  namedNode.label().value());
          namedNodematcher = namedNodePattern.matcher(lambda);
        }
        LogicalExpression expr = SimpleLogicalExpressionReader.read(lambda);
        targetNode.addNodeLambda(expr);
        break;
      }
      case CHANGE_LABEL: {
        String newLabel = transformation.getLabel();
        targetNode.setLabel(new Word(newLabel));
        break;
      }
      default:
        break;
    }
  }

  /**
   * Returns a binarized form of a tree. The labels in the tree are sorted based
   * on the priority.
   * 
   * @param tree the tree to be binarized.
   * @param relationPriority a map containing relation names and their priority.
   *        If a label is not present in the map, it is assigned the lowest
   *        priority.
   * @return string form of the binarized tree.
   */
  public static String binarizeTree(DependencyTree tree,
      Map<String, Integer> relationPriority) {
    if (tree.isLeaf() && tree.isWordOrVirtual())
      return tree.label().value();

    if (tree.isLeaf())
      return null;

    // The tree has label at its node.
    List<Tree> children = tree.getChildrenAsList();

    // Sort the children based on the label priority. If the label is word, give
    // it the highest priority. Else either use the given priority or use the
    // lowest priority.
    children.sort(Comparator.comparing(child -> ((DependencyTree) child)
        .isWordOrVirtual() ? Integer.MIN_VALUE : relationPriority.getOrDefault(
        ((DependencyTree) child).label().value(), Integer.MAX_VALUE)));

    String left =
        binarizeTree((DependencyTree) children.get(0), relationPriority);
    Preconditions.checkNotNull(left);
    for (int i = 1; i < children.size(); i++) {
      DependencyTree child = (DependencyTree) children.get(i);
      String right = binarizeTree(child, relationPriority);
      if (right == null)
        continue;
      left = String.format("(%s %s %s)", child.label().value(), left, right);
    }
    return left;
  }
}
