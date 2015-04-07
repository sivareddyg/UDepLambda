package uk.ac.ed.ilcc.deplambda.parser;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

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

/**
 * Class containing methods to transform a tree using
 * {@link TransformationRuleGroups}. Transformation could be either changing the
 * tree, or assinging lambda functions to nodes in the tree.
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
    Preconditions.checkNotNull(tree);

    // Apply ruleGroups at the current node.
    boolean ruleGroupFound = ApplyRuleGroupsOnRootNode(ruleGroups, tree);

    // Apply ruleGroups at children trees.
    for (Tree child : tree.children()) {
      ruleGroupFound |=
          ApplyRuleGroupsOnTree(ruleGroups, (DependencyTree) child);
    }
    return ruleGroupFound;
  }

  /**
   * Applies rulegroups on the root node. Rulegroups are applied in priority
   * order. If a rulegroup of higher priority applies successfully to a node, no
   * rulegroup of lower priority will be applied again.
   * 
   * @param ruleGroups the ruleGroups that have to be applied.
   * @param tree the root node on which the rulegroups have to be applied.
   * @return Returns true if at least one rule group is applied.
   */
  public static boolean ApplyRuleGroupsOnRootNode(
      TransformationRuleGroups ruleGroups, DependencyTree tree) {
    int bestRuleSoFar = 0;
    boolean ruleGroupFound = false;
    for (TransformationRuleGroup ruleGroup : ruleGroups.getRuleGroupList()) {
      boolean ruleGroupApplied = ApplyRuleGroupOnRootNode(ruleGroup, tree);
      if (ruleGroupFound && ruleGroup.getPriority() > bestRuleSoFar)
        break;
      else if (ruleGroupApplied) {
        bestRuleSoFar = ruleGroup.getPriority();
        ruleGroupFound = true;
      }
    }
    return ruleGroupFound;
  }

  /**
   * Applies rulegroup on the root node.
   * 
   * @param ruleGroup the ruleGroup that have to be applied.
   * @param tree the root node on which the rules have to be applied.
   * @return Returns true if a rulegroup is successfully applied.
   */
  public static boolean ApplyRuleGroupOnRootNode(
      TransformationRuleGroup ruleGroup, DependencyTree tree) {
    int bestRuleSoFar = 0;
    boolean ruleFound = false;
    for (TransformationRule rule : ruleGroup.getRuleList()) {
      boolean ruleApplied = ApplyRuleOnRootNode(rule, tree);
      if (ruleFound && rule.getPriority() > bestRuleSoFar)
        break;
      else if (ruleApplied) {
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
   * @param tree root node on which the rule has to be applied.
   * @return Returns true if a rule group is successfully applied on the root
   *         node.
   */
  private static boolean ApplyRuleOnRootNode(TransformationRule rule,
      DependencyTree tree) {
    TregexPattern tregex = rule.getTregex();
    TregexMatcher matcher = tregex.matcher(tree);
    if (matcher.matchesAt(tree)) {
      for (Transformation transformation : rule.getTransformationList()) {
        ApplyTransformation(transformation, matcher, tree);
      }
      return true;
    }
    return false;
  }


  /**
   * Applies transformation on the nodes in the tree. The nodes on which
   * transformation has to be applied are retrieved from the
   * {@link TregexMatcher}.
   *
   * @param transformation the transformation that have to be applied.
   * @param matcher the matcher that contains all the named nodes.
   * @param tree the tree on which transformation have to be applied.
   */
  private static void ApplyTransformation(Transformation transformation,
      TregexMatcher matcher, DependencyTree tree) {
    String targetName = transformation.getTarget();
    Tree targetNode = matcher.getNode(targetName);
    switch (transformation.getAction()) {
      case ADD_CHILD: {
        String child = transformation.getChild();
        String label = transformation.getLabel();
        DependencyTree labelTree = new DependencyTree(new Word(label));
        DependencyTree childTree = new DependencyTree(new Word(child));
        labelTree.addChild(childTree);
        tree.addChild(labelTree);
        break;
      }
      case ASSIGN_LAMBDA: {
        
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
