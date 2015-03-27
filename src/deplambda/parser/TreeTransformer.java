package deplambda.parser;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;

import deplambda.util.DependencyTree;
import edu.stanford.nlp.trees.Tree;

public class TreeTransformer {
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
    if (tree.isLeaf() && tree.isWord())
      return tree.label().value();

    if (tree.isLeaf())
      return null;

    // The tree has label at its node.
    List<Tree> children = tree.getChildrenAsList();

    // Sort the children based on the label priority. If the label is word, give
    // it the highest priority. Else either use the given priority or use the
    // lowest priority.
    children.sort(Comparator.comparing(child -> ((DependencyTree) child)
        .isWord() ? Integer.MIN_VALUE : relationPriority.getOrDefault(
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
