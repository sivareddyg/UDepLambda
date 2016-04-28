package deplambda.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;

/**
 * 
 * @author Siva Reddy
 *
 */
public class DependencyTree extends LabeledScoredTreeNode {
  private static final long serialVersionUID = 2203868333509512844L;
  public static String WORD_PREFIX = "w-";
  public static String VIRTUAL_PREFIX = "v-";
  public static String TAG_PREFIX = "t-";
  public static String DEP_PREFIX = "l-";

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
      Preconditions.checkArgument(word.has("word"));
      Preconditions.checkArgument(word.has("tag"));
      Preconditions.checkArgument(word.has("label"));


      DependencyTree word_node;
      if (word.has("lemma")) {
        word_node =
            new DependencyTree(new Word(WORD_PREFIX
                + word.get("lemma").getAsString()));
      } else {
        word_node =
            new DependencyTree(new Word(WORD_PREFIX
                + word.get("word").getAsString()));
      }

      DependencyTree tag_node =
          new DependencyTree(new Word(TAG_PREFIX
              + word.get("tag").getAsString()));

      DependencyTree dep_node =
          new DependencyTree(new Word(DEP_PREFIX
              + word.get("label").getAsString()));

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
            "Multiple root nodes present");
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
    return label().value().startsWith(TAG_PREFIX);
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
