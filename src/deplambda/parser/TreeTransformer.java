package deplambda.parser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;

import deplambda.others.PredicateKeys;
import deplambda.protos.TransformationRulesProto.RuleGroups.RuleGroup.Rule.Transformation;
import deplambda.util.DependencyTree;
import deplambda.util.TransformationRule;
import deplambda.util.TransformationRuleGroup;
import deplambda.util.TransformationRuleGroups;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.SimpleLogicalExpressionReader;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ApplyAndSimplify;
import edu.cornell.cs.nlp.spf.mr.language.type.MutableTypeRepository;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;

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
  public static boolean applyRuleGroupsOnTree(
      TransformationRuleGroups ruleGroups, DependencyTree tree) {
    return applyRuleGroupsOnSubTree(ruleGroups, tree, tree);
  }

  private static boolean applyRuleGroupsOnSubTree(
      TransformationRuleGroups ruleGroups, DependencyTree tree,
      DependencyTree subTree) {
    Preconditions.checkNotNull(tree);

    // Apply ruleGroups at the current node.
    boolean ruleGroupFound = applyRuleGroupsOnNode(ruleGroups, tree, subTree);

    // Apply ruleGroups at children trees.
    for (Tree child : subTree.children()) {
      ruleGroupFound |=
          applyRuleGroupsOnSubTree(ruleGroups, tree, (DependencyTree) child);
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
  public static boolean applyRuleGroupsOnNode(
      TransformationRuleGroups ruleGroups, DependencyTree tree,
      DependencyTree targetNode) {
    int bestRuleSoFar = 0;
    boolean ruleGroupFound = false;
    for (TransformationRuleGroup ruleGroup : ruleGroups.getRuleGroupList()) {
      if (ruleGroupFound && ruleGroup.getPriority() > bestRuleSoFar)
        break;
      boolean ruleGroupApplied =
          applyRuleGroupOnNode(ruleGroup, tree, targetNode);
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
  public static boolean applyRuleGroupOnNode(TransformationRuleGroup ruleGroup,
      DependencyTree tree, DependencyTree targetNode) {
    int bestRuleSoFar = 0;
    boolean ruleFound = false;
    for (TransformationRule rule : ruleGroup.getRuleList()) {
      if (ruleFound && rule.getPriority() > bestRuleSoFar)
        break;
      boolean ruleApplied = applyRuleOnNode(rule, tree, targetNode);
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
  private static boolean applyRuleOnNode(TransformationRule rule,
      DependencyTree tree, DependencyTree targetNode) {
    TregexPattern tregex = rule.getTregex();
    TregexMatcher matcher = tregex.matcher(tree);
    if (matcher.matchesAt(targetNode)) {
      for (Transformation transformation : rule.getTransformationList()) {
        applyTransformation(transformation, matcher);
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
  private static void applyTransformation(Transformation transformation,
      TregexMatcher matcher) {
    String targetName = transformation.getTarget();
    DependencyTree targetNode = (DependencyTree) matcher.getNode(targetName);
    switch (transformation.getAction()) {
      case ADD_CHILD: {
        String child = replaceNamedVars(matcher, transformation.getChild());
        String label = replaceNamedVars(matcher, transformation.getLabel());
        DependencyTree labelTree = new DependencyTree(new Word(label));
        DependencyTree childTree = new DependencyTree(new Word(child));
        labelTree.addChild(childTree);
        targetNode.addChild(labelTree);
        break;
      }
      case ASSIGN_LAMBDA: {
        String lambda = replaceNamedVars(matcher, transformation.getLambda());
        LogicalExpression expr = SimpleLogicalExpressionReader.read(lambda);
        targetNode.addNodeLambda(expr);
        break;
      }
      case CHANGE_LABEL: {
        String label = replaceNamedVars(matcher, transformation.getLabel());
        targetNode.setLabel(new Word(label));
        break;
      }
      default:
        break;
    }
  }
  
  private static String replaceNamedVars(TregexMatcher matcher, String original) {
    Pattern namedNodePattern = Pattern.compile("\\{(.+?)\\}");
    Matcher namedNodematcher = namedNodePattern.matcher(original);
    while (namedNodematcher.find()) {
      String namedNodeString = namedNodematcher.group(1);
      Tree namedNode = matcher.getNode(namedNodeString);
      original =
          original.replace(String.format("{%s}", namedNodeString),
              replaceSpecialChars(namedNode.label().value()));
      namedNodematcher = namedNodePattern.matcher(original);
    }
    return original;
  }

  private static String replaceSpecialChars(String name) {
    return name.replaceAll("[\\(\\)\\:,#]+", "-SPL-");
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

  /**
   * Composes semantics of a given dependency tree using its binarized
   * representation. A binarized tree is created by sorting labels based on the
   * priority. To compute tree lambda expression of a node, the node's {code
   * lambda expression ({@link DependencyTree#getNodeLambda()}) is applied to
   * left and right child lambda expressions.
   * 
   * @param tree the tree to be binarized.
   * @param relationPriority a map containing relation names and their priority.
   *        If a label is not present in the map, it is assigned the lowest
   *        priority.
   * @param heuristicJoinIfFailed if set to true, semantics is composed using
   *        heuristic join when there are type-mismatches.
   * @return Returns a pair containing the binarized form the tree along with
   *         its lambda expressions.
   */
  public static Pair<String, List<LogicalExpression>> composeSemantics(
      DependencyTree tree, Map<String, Integer> relationPriority,
      boolean heuristicJoinIfFailed) {
    return composeSemantics(tree, relationPriority, null, heuristicJoinIfFailed);
  }


  public static Pair<String, List<LogicalExpression>> composeSemantics(
      DependencyTree tree, Map<String, Integer> relationPriority,
      Logger logger, boolean heuristicJoinIfFailed) {
    if (tree.isLeaf())
      return Pair.of(tree.label().value(), tree.getNodeLambda());

    List<Tree> children = tree.getChildrenAsList();

    // Sort the children based on the label priority. If the label is word, give
    // it the highest priority. Else either use the given priority or use the
    // lowest priority.
    children.sort(Comparator.comparing(child -> ((DependencyTree) child)
        .isWordOrVirtual() ? Integer.MIN_VALUE : relationPriority.getOrDefault(
        ((DependencyTree) child).label().value(), Integer.MAX_VALUE)));

    Pair<String, List<LogicalExpression>> left =
        composeSemantics((DependencyTree) children.get(0), relationPriority,
            logger, heuristicJoinIfFailed);
    String leftTreeString = left.first();
    List<LogicalExpression> leftTreeParses = left.second();
    Preconditions.checkNotNull(leftTreeParses);
    Preconditions.checkArgument(leftTreeParses.size() != 0);

    for (int i = 1; i < children.size(); i++) {
      DependencyTree child = (DependencyTree) children.get(i);
      Pair<String, List<LogicalExpression>> right =
          composeSemantics(child, relationPriority, logger,
              heuristicJoinIfFailed);
      String rightTreeString = right.first();
      List<LogicalExpression> rightTreeParses = right.second();

      if (rightTreeParses == null)
        continue;

      leftTreeString =
          String.format("(%s %s %s)", child.label().value(), leftTreeString,
              rightTreeString);

      if (logger != null) {
        logger.debug(leftTreeString);
      }

      List<LogicalExpression> parses = new ArrayList<>();
      List<LogicalExpression> depParses = child.getNodeLambda();
      try {
        for (LogicalExpression depParse : depParses) {
          for (LogicalExpression leftTreeParse : leftTreeParses) {
            for (LogicalExpression rightTreeParse : rightTreeParses) {
              LogicalExpression depAndLeftAndRight = null;
              if (leftTreeParse instanceof LogicalConstant
                  && leftTreeParse.getType().equals(
                      MutableTypeRepository.NULL_TYPE)) {
                depAndLeftAndRight = rightTreeParse;
              } else if (rightTreeParse instanceof LogicalConstant
                  && rightTreeParse.getType().equals(
                      MutableTypeRepository.NULL_TYPE)) {
                depAndLeftAndRight = leftTreeParse;
              } else if (depParse instanceof LogicalConstant) {
                if (depParse.getType().equals(
                    MutableTypeRepository.BACKWARD_APPLICATION)) {
                  depAndLeftAndRight =
                      ApplyAndSimplify.of(rightTreeParse, leftTreeParse);
                } else if (depParse.getType().equals(
                    MutableTypeRepository.FORWARD_APPLICATION)) {
                  depAndLeftAndRight =
                      ApplyAndSimplify.of(leftTreeParse, rightTreeParse);

                } else if (depParse.getType().equals(
                    MutableTypeRepository.BIND_OPERATION)) {
                  depAndLeftAndRight =
                      bindOperation(leftTreeParse, rightTreeParse);
                } else if (depParse.getType().equals(
                    MutableTypeRepository.BIND_OPERATION_OLD)) {
                  depAndLeftAndRight =
                      bindOperationOld(leftTreeParse, rightTreeParse);
                }
              } else {
                LogicalExpression depAndLeft =
                    ApplyAndSimplify.of(depParse, leftTreeParse);
                if (depAndLeft != null) {
                  depAndLeftAndRight =
                      ApplyAndSimplify.of(depAndLeft, rightTreeParse);
                }
              }

              if (depAndLeftAndRight == null && heuristicJoinIfFailed) {
                // Type mismatches.
                depAndLeftAndRight =
                    heuristicJoin(leftTreeParse, rightTreeParse);
              }



              if (depAndLeftAndRight != null) {
                if (logger != null) {
                  logger.debug(depParse);
                  logger.debug(leftTreeParse);
                  logger.debug(rightTreeParse);
                  logger.debug(depAndLeftAndRight);
                  logger.debug("\n");
                }
                parses.add(depAndLeftAndRight);
              }
            }
          }
        }
      } catch (Exception e) {
        System.err.println("Cannot compose tree: " + leftTreeString);
        System.out.print("\tLabel: ");
        System.err.println(depParses);
        System.err.print("\tLeft: ");
        System.out.print(leftTreeParses);
        System.out.println("\tRight: ");
        System.err.println(rightTreeParses);
        e.printStackTrace();
      }
      leftTreeParses = parses;
    }
    return Pair.of(leftTreeString, leftTreeParses);
  }

  /**
   * Combines two logical expressions and returns a new expression of the type
   * headExpression. ChildExpression is converted to type <code>t</code> by
   * appending an empty predicate in-front of the expression. The
   * childExpression is then inserted inside the headExpression at a position
   * where a subexpression of type <code>t</code> is found in the
   * headExpression.
   * 
   * @param headExpression the expression into which childExpression is to be
   *        inserted.
   * @param childExpression the expression that is to be inserted in the
   *        headExpression.
   * @return a new expression of type headExpression.
   */
  protected static LogicalExpression heuristicJoin(
      LogicalExpression headExpression, LogicalExpression childExpression) {
    LogicalExpression headSubExpression = headExpression;
    Stack<Variable> headLambdaVairables = new Stack<>();
    while (headSubExpression != null && headSubExpression instanceof Lambda) {
      headLambdaVairables.push(((Lambda) headSubExpression).getArgument());
      headSubExpression = ((Lambda) headSubExpression).getBody();
    }

    Type truthType = LogicLanguageServices.getTypeRepository().getType("t");
    if (headSubExpression == null
        || !headSubExpression.getType().equals(truthType)) {
      return headExpression;
    }

    // Create a truth expression from childExpression.
    String childType = childExpression.getType().toString();
    LogicalConstant emptyPredicate =
        (LogicalConstant) SimpleLogicalExpressionReader.read(String.format(
            "empty:<%s,t>", childType));
    LogicalExpression[] childArray = {childExpression};
    LogicalExpression childTruthExpression =
        new Literal(emptyPredicate, childArray);

    LogicalConstant conjunctionPredicate =
        (LogicalConstant) SimpleLogicalExpressionReader
            .read(SimpleLogicalExpressionReader.CONJUNCTION_PREDICATE);
    LogicalExpression[] headAndChildArguments =
        {headSubExpression, childTruthExpression};
    Literal headAndChildTruthExpression =
        new Literal(conjunctionPredicate,
            (LogicalExpression[]) headAndChildArguments);

    LogicalExpression returnExpression = headAndChildTruthExpression;
    while (!headLambdaVairables.isEmpty()) {
      returnExpression =
          new Lambda(headLambdaVairables.pop(), returnExpression);
    }

    return returnExpression;
  }

  private static LogicalExpression bindOperation(
      LogicalExpression leftTreeParse, LogicalExpression rightTreeParse) {
    Preconditions.checkNotNull(leftTreeParse);
    Preconditions.checkNotNull(rightTreeParse);
    Preconditions.checkArgument(rightTreeParse instanceof LogicalConstant);

    // Dirty implementation. A better way is to work with objects and not the
    // strings.
    Type leftType = leftTreeParse.getType();
    String existsExpression =
        String.format("(lambda $f:%s (exists:<%s,<%s,%s>> $x:%s ($f $x))",
            leftType, leftType.getDomain(), leftType.getRange(),
            leftType.getRange(), leftType.getDomain());
    leftTreeParse =
        ApplyAndSimplify
            .of(SimpleLogicalExpressionReader.read(existsExpression),
                leftTreeParse);

    String variable = rightTreeParse.toString();
    String leftParse = leftTreeParse.toString();
    String variableType = rightTreeParse.getType().getDomain().toString();
    String returnType = rightTreeParse.getType().getRange().toString();
    Pattern variablePattern =
        Pattern.compile(String.format("([\\(\\)\\s])(%s)([\\(\\)\\s])",
            variable));
    Matcher matcher = variablePattern.matcher(leftParse);

    String finalString =
        matcher
            .replaceAll(String.format("$1%s:<%s,<%s,%s>> \\$x$3",
                PredicateKeys.EQUAL_PREFIX, variableType, variableType,
                returnType));
    finalString = String.format("(lambda $x:%s %s)", variableType, finalString);
    return SimpleLogicalExpressionReader.read(finalString);
  }

  /**
   * Converts the right argument as a lambda variable and passes it to Bind
   * operation is defined as
   * 
   * (BIND (A) B) = (lambda B (A))
   * 
   * @param leftTreeParse
   * @param rightTreeParse
   * @return
   */
  private static LogicalExpression bindOperationOld(
      LogicalExpression leftTreeParse, LogicalExpression rightTreeParse) {
    Preconditions.checkNotNull(leftTreeParse);
    Preconditions.checkNotNull(rightTreeParse);
    Preconditions.checkArgument(rightTreeParse instanceof LogicalConstant);

    // Dirty implementation. A better way is to work with objects and not the
    // strings.
    String variable = rightTreeParse.toString();
    String variableType = rightTreeParse.getType().toString();
    String leftParse = leftTreeParse.toString();
    Pattern variablePattern =
        Pattern.compile(String.format("([\\(\\)\\s])(%s)([\\(\\)\\s])",
            variable));
    Matcher matcher = variablePattern.matcher(leftParse);
    String finalString = matcher.replaceAll(String.format("$1\\$x$3"));
    finalString = String.format("(lambda $x:%s %s)", variableType, finalString);
    return SimpleLogicalExpressionReader.read(finalString);
  }
}
