package deplambda.others;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.gson.JsonObject;

import in.sivareddy.util.ProcessStreamInterface;

public class PrintSentencesFromWords extends ProcessStreamInterface {

  @Override
  public void processSentence(JsonObject sentence) {
    if (sentence.has(SentenceKeys.FOREST))
      sentence.get(SentenceKeys.FOREST).getAsJsonArray()
          .forEach(x -> printIndividualSentence(x.getAsJsonObject()));
    else
      printIndividualSentence(sentence);
  }

  private void printIndividualSentence(JsonObject sentence) {
    List<String> words = new ArrayList<>();
    sentence
        .get(SentenceKeys.WORDS_KEY)
        .getAsJsonArray()
        .forEach(
            x -> words.add(x.getAsJsonObject().get(SentenceKeys.WORD_KEY)
                .getAsString()));
    System.out.println(Joiner.on(" ").join(words));
  }

  public static void main(String[] args) throws IOException,
      InterruptedException {
    PrintSentencesFromWords printer = new PrintSentencesFromWords();
    printer.processStream(System.in, null, 1, false);
  }
}