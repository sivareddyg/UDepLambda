package uk.ac.ed.ilcc.deplambda.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Siva Reddy
 *
 */
public class Sentence {
  private List<DependencyTree> nodes;
  private DependencyTree rootNode;
  private Map<Integer, JsonObject> wordIndexToEntity;
  private JsonArray words;

  /**
   * Constructs {@link Sentence} from json object.
   * 
   * @param sentence
   */
  public Sentence(JsonObject sentence) {
    wordIndexToEntity = new HashMap<>();
    nodes = new ArrayList<>();
    buildWordToEntityMap(sentence, wordIndexToEntity);
    Preconditions.checkArgument(sentence.has("words"),
        "Sentence should have the key 'words'");
    words = sentence.get("words").getAsJsonArray();
    rootNode = DependencyTree.parse(words, wordIndexToEntity, nodes);
  }

  /**
   * Constructs a map from word index to entity object.
   * 
   * @param sentence
   * @param wordIndexToEntity
   */
  private static void buildWordToEntityMap(JsonObject sentence,
      Map<Integer, JsonObject> wordIndexToEntity) {
    if (sentence.has("entities")) {
      JsonArray entities = sentence.get("entities").getAsJsonArray();
      for (JsonElement entityElement : entities) {
        JsonObject entity = entityElement.getAsJsonObject();
        int start = entity.get("start").getAsInt();
        int end = entity.get("end").getAsInt();
        while (start <= end) {
          wordIndexToEntity.put(start, entity);
          start++;
        }
      }
    }
  }

  public DependencyTree getRootNode() {
    return rootNode;
  }
}
