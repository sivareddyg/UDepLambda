package deplambda.others;

import java.io.IOException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import in.sivareddy.util.ProcessStreamInterface;

public class ConvertGraphParserSentenceToConll extends ProcessStreamInterface {
  @Override
  public void processSentence(JsonObject sentence) {
    for (JsonElement wordElm : sentence.get(SentenceKeys.WORDS_KEY)
        .getAsJsonArray()) {
      JsonObject word = wordElm.getAsJsonObject();
      int index = word.get(SentenceKeys.INDEX_KEY).getAsInt();
      String wordStr = word.get(SentenceKeys.WORD_KEY).getAsString();
      String lemma =
          word.has(SentenceKeys.LEMMA_KEY) ? word.get(SentenceKeys.LEMMA_KEY)
              .getAsString() : "_";
      String pos =
          word.has(SentenceKeys.POS_KEY) ? word.get(SentenceKeys.POS_KEY)
              .getAsString() : "_";
      String fpos =
          word.has(SentenceKeys.FINE_POS_KEY) ? word.get(
              SentenceKeys.FINE_POS_KEY).getAsString() : "_";
      String feats =
          word.has(SentenceKeys.FEATS_KEY) ? word.get(SentenceKeys.FEATS_KEY)
              .getAsString() : "_";
      String head =
          word.has(SentenceKeys.HEAD_KEY) ? word.get(SentenceKeys.HEAD_KEY)
              .getAsString() : "_";
      String deprel =
          word.has(SentenceKeys.DEPENDENCY_KEY) ? word.get(
              SentenceKeys.DEPENDENCY_KEY).getAsString() : "_";
      String phead =
          word.has(SentenceKeys.PHEAD) ? word.get(SentenceKeys.PHEAD)
              .getAsString() : "_";
      String pdeprel =
          word.has(SentenceKeys.PDEPREL) ? word.get(SentenceKeys.PDEPREL)
              .getAsString() : "_";
      System.out.println(String.format(
          "%d\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s", index, wordStr, lemma, pos,
          fpos, feats, head, deprel, phead, pdeprel));
    }
    System.out.println();
  }

  public static void main(String[] args) throws IOException,
      InterruptedException {
    ConvertGraphParserSentenceToConll convertor =
        new ConvertGraphParserSentenceToConll();
    convertor.processStream(System.in, null, 1, false);
  }
}