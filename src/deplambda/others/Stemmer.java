package deplambda.others;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.tartarus.snowball.SnowballProgram;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import in.sivareddy.util.ProcessStreamInterface;

public class Stemmer extends ProcessStreamInterface {

  final Class<? extends SnowballProgram> stemClass;

  public static Map<String, String> languageCodeToKey = new HashMap<>();
  static {
    languageCodeToKey.put("en", "English");
    languageCodeToKey.put("es", "Spanish");
    languageCodeToKey.put("de", "German");
  }


  public Stemmer(String languageCode) throws ClassNotFoundException {
    Preconditions.checkArgument(languageCodeToKey.containsKey(languageCode),
        "Language code to stemming Analyzer absent");
    stemClass =
        Class.forName(
            "org.tartarus.snowball.ext." + languageCodeToKey.get(languageCode)
                + "Stemmer").asSubclass(SnowballProgram.class);
  }

  @Override
  public void processSentence(JsonObject sentence) {
    try {
      SnowballProgram stemmer = stemClass.newInstance();
      if (sentence.has(SentenceKeys.FOREST)) {
        for (JsonElement sent : sentence.get(SentenceKeys.FOREST)
            .getAsJsonArray()) {
          processSentence(sent.getAsJsonObject());
        }
      } else {
        JsonArray words = sentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray();
        for (JsonElement word : words) {
          JsonObject wordObj = word.getAsJsonObject();
          String wordString = wordObj.get(SentenceKeys.WORD_KEY).getAsString();
          stemmer.setCurrent(wordString);
          String stem = wordString;
          if (stemmer.stem()) {
            stem = stemmer.getCurrent();
          }
          stem = stem.toLowerCase();
          wordObj.addProperty(SentenceKeys.LEMMA_KEY, stem);
        }
      }
    } catch (InstantiationException | IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) throws ClassNotFoundException,
      IOException, InterruptedException {
    String languageCode = args[0];
    int nthreads = args.length > 1 ? Integer.parseInt(args[1]) : 20;
    Stemmer stemmer = new Stemmer(languageCode);
    stemmer.processStream(System.in, System.out, nthreads, true);
  }
}
