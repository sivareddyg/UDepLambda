package deplambda.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import deplambda.others.SentenceKeys;
import in.sivareddy.util.ProcessStreamInterface;

public class ForestTransformerMain extends ProcessStreamInterface {
  private final TreeTransformerMain treeTransformerMain;

  public ForestTransformerMain(TreeTransformerMain treeTransformerMain) {
    this.treeTransformerMain = treeTransformerMain;
  }

  @Override
  public void processSentence(JsonObject forest) {
    if (!forest.has(SentenceKeys.FOREST))
      return;

    for (JsonElement sentenceElm : forest.get(SentenceKeys.FOREST)
        .getAsJsonArray()) {
      JsonObject sentence = sentenceElm.getAsJsonObject();
      treeTransformerMain.processSentence(sentence);
    }
  }
}
