package deplambda.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import deplambda.others.SentenceKeys;

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

  public JsonObject getEntityAtWordIndex(int index) {
    return wordIndexToEntity.containsKey(index) ? wordIndexToEntity.get(index)
        : null;
  }

  /**
   * Constructs a map from word index to entity object.
   * 
   * @param sentence
   * @param wordIndexToEntity
   */
  private static void buildWordToEntityMap(JsonObject sentence,
      Map<Integer, JsonObject> wordIndexToEntity) {
    if (sentence.has(SentenceKeys.ENTITIES)) {
      JsonArray entities = sentence.get(SentenceKeys.ENTITIES).getAsJsonArray();
      for (JsonElement entityElement : entities) {
        JsonObject entity = entityElement.getAsJsonObject();

        Preconditions.checkArgument(
            entity.has(SentenceKeys.ENTITY_INDEX)
                || (entity.has(SentenceKeys.START) && entity
                    .has(SentenceKeys.END)),
            "An entity should have an index, or a start and an end.");

        if (!entity.has(SentenceKeys.START)) {
          int entityIndex = entity.get(SentenceKeys.ENTITY_INDEX).getAsInt();
          entity.addProperty(SentenceKeys.START, entityIndex);
          entity.addProperty(SentenceKeys.END, entityIndex);
        }

        int start = entity.get(SentenceKeys.START).getAsInt();
        int end = entity.get(SentenceKeys.END).getAsInt();

        if (!entity.has(SentenceKeys.ENTITY_INDEX)) {
          // The last word is the index of the whole entity.
          entity.addProperty(SentenceKeys.ENTITY_INDEX, end);
        }

        while (start <= end) {
          wordIndexToEntity.put(start, entity);
          start++;
        }
      }
    }
  }

  public JsonArray getWords() {
    return words;
  }

  public DependencyTree getRootNode() {
    return rootNode;
  }

  public String getLemma(int index) {
    JsonObject word = getWords().get(index).getAsJsonObject();
    return word.has(SentenceKeys.LEMMA_KEY)
        && !word.get(SentenceKeys.LEMMA_KEY).getAsString().equals("_") ? word
        .get(SentenceKeys.LEMMA_KEY).getAsString().toLowerCase() : word
        .get(SentenceKeys.WORD_KEY).getAsString().toLowerCase();
  }
}
