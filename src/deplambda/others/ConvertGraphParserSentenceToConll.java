package deplambda.others;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import in.sivareddy.util.ProcessStreamInterface;

public class ConvertGraphParserSentenceToConll extends ProcessStreamInterface {
  private static BufferedWriter fout;

  static {
    try {
      Writer writer = new OutputStreamWriter(System.out, "UTF-8");
      fout = new BufferedWriter(writer);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void processSentence(JsonObject sentence) {
    if (sentence.has(SentenceKeys.FOREST)) {
      for (JsonElement individualSentence : sentence.get(SentenceKeys.FOREST)
          .getAsJsonArray()) {
        processIndividualSentence(individualSentence.getAsJsonObject());
      }
    } else {
      processIndividualSentence(sentence);
    }
  }

  public void processIndividualSentence(JsonObject sentence) {
    try {
      int index = 1;
      for (JsonElement wordElm : sentence.get(SentenceKeys.WORDS_KEY)
          .getAsJsonArray()) {
        JsonObject word = wordElm.getAsJsonObject();
        String wordStr = word.get(SentenceKeys.WORD_KEY).getAsString();
        String lemma = word.has(SentenceKeys.LEMMA_KEY)
            ? word.get(SentenceKeys.LEMMA_KEY).getAsString() : "_";
        String pos = word.has(SentenceKeys.POS_KEY)
            ? word.get(SentenceKeys.POS_KEY).getAsString() : "_";
        String fpos = word.has(SentenceKeys.FINE_POS_KEY)
            ? word.get(SentenceKeys.FINE_POS_KEY).getAsString() : pos;
        String feats = word.has(SentenceKeys.FEATS_KEY)
            ? word.get(SentenceKeys.FEATS_KEY).getAsString() : "_";
        String head = word.has(SentenceKeys.HEAD_KEY)
            ? word.get(SentenceKeys.HEAD_KEY).getAsString() : "_";
        String deprel = word.has(SentenceKeys.DEPENDENCY_KEY)
            ? word.get(SentenceKeys.DEPENDENCY_KEY).getAsString() : "_";
        String phead = word.has(SentenceKeys.PHEAD)
            ? word.get(SentenceKeys.PHEAD).getAsString() : "_";
        String pdeprel = word.has(SentenceKeys.PDEPREL)
            ? word.get(SentenceKeys.PDEPREL).getAsString() : "_";

        fout.write(String.format("%d\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n",
            index, wordStr, lemma, pos, fpos, feats, head, deprel, phead,
            pdeprel));
        index++;
      }
      fout.newLine();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args)
      throws IOException, InterruptedException {
    ConvertGraphParserSentenceToConll convertor =
        new ConvertGraphParserSentenceToConll();
    convertor.processStream(System.in, null, 1, false);
  }
}
