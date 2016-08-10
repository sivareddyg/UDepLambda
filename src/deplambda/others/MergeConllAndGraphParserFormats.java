package deplambda.others;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MergeConllAndGraphParserFormats {
  private static JsonParser parser = new JsonParser();
  private static Gson gson = new Gson();

  public static void merge(InputStream conllInputStream,
      InputStream graphParserInputStream, PrintStream out) throws IOException {
    Writer writer = new OutputStreamWriter(out, "UTF-8");
    BufferedWriter fout = new BufferedWriter(writer);
    BufferedReader conllReader =
        new BufferedReader(new InputStreamReader(conllInputStream, "UTF8"));
    BufferedReader graphParserReader = new BufferedReader(
        new InputStreamReader(graphParserInputStream, "UTF8"));

    String line = graphParserReader.readLine();
    while (line != null) {
      if (line.startsWith("#") || line.trim().equals("")) {
        line = graphParserReader.readLine();
        continue;
      }
      JsonObject sentence = parser.parse(line).getAsJsonObject();
      if (sentence.has(SentenceKeys.FOREST))
        mergeForestWithConll(sentence, conllReader);
      else
        mergeSentenceWithConll(sentence, conllReader);
      fout.write(gson.toJson(sentence));
      fout.newLine();
      line = graphParserReader.readLine();
    }
    conllReader.close();
    graphParserReader.close();
    fout.close();
  }

  public static void mergeForestWithConll(JsonObject forest,
      BufferedReader conllReader) {
    forest.get(SentenceKeys.FOREST).getAsJsonArray()
        .forEach(x -> mergeSentenceWithConll(x.getAsJsonObject(), conllReader));
  }

  public static void mergeSentenceWithConll(JsonObject sentence,
      BufferedReader conllReader) {
    JsonObject conllSentence =
        ConvertConllToGraphParserSentence.getNextSentence(conllReader);
    JsonArray conllWords =
        conllSentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray();
    JsonArray originalWords =
        sentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray();
    Preconditions.checkArgument(conllWords.size() == originalWords.size(),
        String.format(
            "Words in CONLL file and GraphParser file do not align\n CONLL: %s \n GraphParser: %s",
            conllWords, originalWords));
    sentence.add(SentenceKeys.WORDS_KEY, conllWords);
  }

  public static void main(String[] args) throws IOException {
    String conllFile = args[0];
    String graphParserFile = args[1];

    FileInputStream conllInputStream = new FileInputStream(conllFile);
    FileInputStream graphParserInputStream =
        new FileInputStream(graphParserFile);
    merge(conllInputStream, graphParserInputStream, System.out);
  }
}
