package deplambda.others;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ConvertGraphParserSentenceToConll {

  private final BufferedWriter fout;
  private static JsonParser jsonParser = new JsonParser();

  public ConvertGraphParserSentenceToConll(BufferedWriter fout) {
    this.fout = fout;
  }

  public void processStream(InputStream stream) throws IOException,
      InterruptedException {
    BufferedReader br =
        new BufferedReader(new InputStreamReader(stream, "UTF8"));
    String line = null;
    try {
      line = br.readLine();
      while (line != null) {
        if (line.startsWith("#") || line.trim().equals("")) {
          line = br.readLine();
          continue;
        }
        JsonObject jsonSentence = jsonParser.parse(line).getAsJsonObject();
        processSentence(jsonSentence);
        line = br.readLine();
      }
    } catch (Exception e) {
      System.err.println("Could not process line: ");
      System.err.println(line);
    } finally {
      br.close();
    }
  }

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
        String lemma =
            word.has(SentenceKeys.LEMMA_KEY) ? word.get(SentenceKeys.LEMMA_KEY)
                .getAsString() : "_";
        String pos =
            word.has(SentenceKeys.POS_KEY) ? word.get(SentenceKeys.POS_KEY)
                .getAsString() : "_";
        String fpos =
            word.has(SentenceKeys.FINE_POS_KEY) ? word.get(
                SentenceKeys.FINE_POS_KEY).getAsString() : pos;
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

  public static void main(String[] args) throws IOException,
      InterruptedException {
    Writer writer = new OutputStreamWriter(System.out, "UTF-8");
    BufferedWriter fout = new BufferedWriter(writer);
    ConvertGraphParserSentenceToConll convertor =
        new ConvertGraphParserSentenceToConll(fout);
    convertor.processStream(System.in);
    fout.close();
  }
}
