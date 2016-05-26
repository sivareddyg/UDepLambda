package deplambda.util;

import in.sivareddy.util.SentenceKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;

/**
 * 
 * @author Siva Reddy
 *
 */
public class DependencyTree extends LabeledScoredTreeNode {
  private static final long serialVersionUID = 2203868333509512844L;
  public static String WORD_PREFIX = "w-";
  public static String VIRTUAL_PREFIX = "v-";
  public static String POS_PREFIX = "t-";
  public static String DEP_PREFIX = "l-";
  public static String LEXICAL_KEY = SentenceKeys.WORD_KEY;

  // Lambda function representing the semantics of this node.
  private List<LogicalExpression> nodeLambda;

  // Lambda function representing the semantics of the tree rooted at this node.
  private List<LogicalExpression> treeLambda;

  public DependencyTree(Label label) {
    super(label);
  }

  /**
   * Constructs a dependency tree from words in json format. All the words in an
   * entity are treated as a single word representing the entity.
   * 
   * @param words words in the sentence in json format.
   * @param wordIndexToEntity word to entity mapping
   * @param nodes list in which the all the dependency nodes have to be stored.
   * @return root node of the dependency tree.
   */
  public static DependencyTree parse(JsonArray words,
      Map<Integer, JsonObject> wordIndexToEntity, List<DependencyTree> nodes) {
    Preconditions.checkNotNull(nodes);

    // Create all nodes in the dependency tree.
    for (int i = 0; i < words.size(); i++) {
      JsonObject word = words.get(i).getAsJsonObject();
      Preconditions.checkArgument(word.has(SentenceKeys.WORD_KEY));
      Preconditions.checkArgument(word.has(SentenceKeys.POS_KEY));
      Preconditions.checkArgument(word.has(SentenceKeys.DEPENDENCY_KEY));

      DependencyTree word_node;
      if (word.has(LEXICAL_KEY)) {
        word_node =
            new DependencyTree(new Word(String.format(
                "%s%s%s",
                WORD_PREFIX,
                word.has(SentenceKeys.INDEX_KEY) ? String.format("%d-", word
                    .get(SentenceKeys.INDEX_KEY).getAsInt()) : "",
                word.get(LEXICAL_KEY).getAsString().toLowerCase())));
      } else {
        word_node =
            new DependencyTree(new Word(String.format(
                "%s%s%s",
                WORD_PREFIX,
                word.has(SentenceKeys.INDEX_KEY) ? String.format("%d-", word
                    .get(SentenceKeys.INDEX_KEY).getAsInt()) : "",
                word.get(SentenceKeys.WORD_KEY).getAsString().toLowerCase())));
      }

      DependencyTree tag_node =
          new DependencyTree(new Word(POS_PREFIX
              + word.get(SentenceKeys.POS_KEY).getAsString()));

      DependencyTree dep_node =
          new DependencyTree(new Word(DEP_PREFIX
              + word.get(SentenceKeys.DEPENDENCY_KEY).getAsString()));

      dep_node.addChild(word_node);
      dep_node.addChild(tag_node);
      nodes.add(dep_node);
    }

    DependencyTree rootNode = null;
    for (int i = 0; i < words.size(); i++) {
      JsonObject word = words.get(i).getAsJsonObject();
      int head = word.get("head").getAsInt() - 1;
      if (head == -1) {
        // ROOT node.
        Preconditions.checkArgument(rootNode == null,
            "Multiple root nodes present: " + words);
        rootNode = nodes.get(i);
      } else {
        if (wordIndexToEntity.containsKey(i)
            && wordIndexToEntity.containsKey(head)
            && wordIndexToEntity.get(i) == wordIndexToEntity.get(head)) {
          // words belong to the same entity. Ignore.
        } else {
          DependencyTree parent = nodes.get(head);
          DependencyTree current = nodes.get(i);
          parent.addChild(current);
        }
      }
    }
    return rootNode;
  }

  public boolean isWord() {
    return label().value().startsWith(WORD_PREFIX);
  }

  public boolean isVirtual() {
    return label().value().startsWith(VIRTUAL_PREFIX);
  }

  public boolean isWordOrVirtual() {
    return isWord() || isVirtual();
  }

  public boolean isTag() {
    return label().value().startsWith(POS_PREFIX);
  }

  public boolean isDepLabel() {
    return label().value().startsWith(DEP_PREFIX);
  }

  public List<LogicalExpression> getNodeLambda() {
    return nodeLambda;
  }

  public void addNodeLambda(LogicalExpression lambda) {
    if (nodeLambda == null)
      nodeLambda = new ArrayList<>();
    nodeLambda.add(lambda);
  }

  public List<LogicalExpression> getTreeLambda() {
    return treeLambda;
  }

  public void addTreeLambda(LogicalExpression lambda) {
    if (treeLambda == null)
      treeLambda = new ArrayList<>();
    treeLambda.add(lambda);
  }
}
