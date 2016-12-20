package deplambda.others;

import in.sivareddy.graphparser.parsing.GroundedGraphs;
import in.sivareddy.graphparser.parsing.LexicalGraph;
import in.sivareddy.graphparser.util.GroundedLexicon;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBaseCached;
import in.sivareddy.util.ProcessStreamInterface;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MergeTwoForestsIfDisconnected extends ProcessStreamInterface {
  private static GroundedGraphs graphCreator = null;
  static {
    String[] relationLexicalIdentifiers = {"word"};
    String[] relationTypingIdentifiers = {};
    try {
      KnowledgeBaseCached kb = new KnowledgeBaseCached(null, null);
      GroundedLexicon groundedLexicon = new GroundedLexicon(null);
      graphCreator = new GroundedGraphs(null, kb, groundedLexicon, null, null,
          relationLexicalIdentifiers, relationTypingIdentifiers, null, null, 1,
          false, false, false, false, false, false, false, false, false, false,
          false, false, false, false, false, false, false, false, false, false,
          false, false, false, false, false, false, false, false, false, false,
          false, false, false, false, false, 10.0, 1.0, 0.0, 0.0, 0.0); 
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private final Map<String, JsonObject> sentToForest;

  public MergeTwoForestsIfDisconnected(Map<String, JsonObject> sentToForest) {
    this.sentToForest = sentToForest;
  }

  @Override
  public void processSentence(JsonObject sentence) {
    String key = sentence.get(SentenceKeys.SENTENCE_KEY).getAsString();
    Preconditions.checkArgument(sentToForest.containsKey(key),
        "Sentence could not be found: " + key);
    JsonObject forest2 = sentToForest.get(key);
    mergeForest(sentence, forest2);
  }

  private void mergeForest(JsonObject forest1, JsonObject forest2) {
    JsonArray forestArray = new JsonArray();

    JsonArray sentences1 = forest1.get(SentenceKeys.FOREST).getAsJsonArray();
    JsonArray sentences2 = forest2.get(SentenceKeys.FOREST).getAsJsonArray();

    Preconditions.checkArgument(sentences1.size() == sentences2.size(),
        "Forests are different in sentences");

    for (int i = 0; i < sentences1.size(); i++) {
      JsonObject sentence1 = sentences1.get(i).getAsJsonObject();
      List<LexicalGraph> graphs1 =
          graphCreator.buildUngroundedGraph(sentence1,
              SentenceKeys.DEPENDENCY_LAMBDA, 1);
      boolean parseIsValid = false;

      // Check if the current semantic parse is a connected graph.
      for (LexicalGraph graph : graphs1) {
        parseIsValid |= graph.hasPathBetweenQuestionAndEntityNodes();
        if (parseIsValid)
          break;
      }
      forestArray.add(sentence1);

      // If the semantic parse is not a connected graph, add the sentence
      // from the other forest if it has a connected graph.
      if (!parseIsValid) {
        JsonObject sentence2 = sentences2.get(i).getAsJsonObject();
        List<LexicalGraph> graphs2 =
            graphCreator.buildUngroundedGraph(sentence2,
                SentenceKeys.DEPENDENCY_LAMBDA, 1);
        for (LexicalGraph graph : graphs2) {
          parseIsValid |= graph.hasPathBetweenQuestionAndEntityNodes();
          if (parseIsValid)
            break;
        }

        if (parseIsValid)
          forestArray.add(sentence2);
      }
    }

    forest1.add(SentenceKeys.FOREST, forestArray);
  }

  public static void main(String[] args) throws IOException,
      InterruptedException {
    BufferedReader br = new BufferedReader(new FileReader(args[0]));
    JsonParser jsonParser = new JsonParser();

    Map<String, JsonObject> sentToForest = new HashMap<>();
    try {
      String line = br.readLine();
      while (line != null) {
        if (!line.startsWith("#") && !line.trim().equals("")) {
          JsonObject forest = jsonParser.parse(line).getAsJsonObject();
          String key = forest.get(SentenceKeys.SENTENCE_KEY).getAsString();
          sentToForest.put(key, forest);
        }
        line = br.readLine();
      }
    } finally {
      br.close();
    }

    MergeTwoForestsIfDisconnected merger =
        new MergeTwoForestsIfDisconnected(sentToForest);
    merger.processStream(System.in, System.out, 20, true);
  }
}
