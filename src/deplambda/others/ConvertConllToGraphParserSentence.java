package deplambda.others;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ConvertConllToGraphParserSentence {
  private static Gson gson = new Gson();

  public static void convert(InputStream input, PrintStream out)
      throws IOException {
    BufferedReader br =
        new BufferedReader(new InputStreamReader(input, "UTF8"));
    JsonObject sentence = getNextSentence(br);
    Writer writer = new OutputStreamWriter(out, "UTF-8");
    BufferedWriter fout = new BufferedWriter(writer);
    while (sentence != null) {
      fout.write(gson.toJson(sentence));
      fout.write("\n");
      sentence = getNextSentence(br);
    }
    br.close();
    fout.close();
  }

  public static JsonObject getNextSentence(BufferedReader br) {
    try {
      String line = br.readLine();
      JsonObject sentence = new JsonObject();
      JsonArray words = new JsonArray();
      List<String> wordStrs = new ArrayList<>();
      while (line != null && !line.trim().equals("")) {
        String[] parts = line.split("\t");
        JsonObject word = new JsonObject();
        word.addProperty(SentenceKeys.INDEX_KEY, Integer.parseInt(parts[0]));
        word.addProperty(SentenceKeys.WORD_KEY, parts[1]);
        wordStrs.add(parts[1]);

        if (parts.length > 2) {
          word.addProperty(SentenceKeys.LEMMA_KEY, parts[2]);
        }

        if (parts.length > 3) {
          word.addProperty(SentenceKeys.POS_KEY, parts[3]);
        }

        if (parts.length > 4) {
          word.addProperty(SentenceKeys.FINE_POS_KEY, parts[4]);
        }

        if (parts.length > 5) {
          word.addProperty(SentenceKeys.FEATS_KEY, parts[5]);
        }

        if (parts.length > 6) {
          if (parts[6].equals("-1"))
            parts[6] = "0";
          word.addProperty(SentenceKeys.HEAD_KEY, parts[6]);
        }

        if (parts.length > 7) {
          word.addProperty(SentenceKeys.DEPENDENCY_KEY, parts[7]);
        }

        if (parts.length > 8) {
          word.addProperty(SentenceKeys.PHEAD, parts[8]);
        }

        if (parts.length > 9) {
          word.addProperty(SentenceKeys.PDEPREL, parts[9]);
        }
        words.add(word);
        line = br.readLine();
      }

      if (words.size() > 0) {
        sentence.add(SentenceKeys.WORDS_KEY, words);
        sentence.addProperty(SentenceKeys.SENTENCE_KEY,
            Joiner.on(" ").join(wordStrs));
        return sentence;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static void main(String args[]) throws IOException {
    convert(System.in, System.out);
  }
}
