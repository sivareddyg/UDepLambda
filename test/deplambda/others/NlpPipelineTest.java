package deplambda.others;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class NlpPipelineTest {

  @Before
  public void setUp() throws Exception {}

  @After
  public void tearDown() throws Exception {}

  @Test
  public final void testProcessSentence() throws Exception {
    Map<String, String> options =
        ImmutableMap
            .of("annotators",
                "tokenize,ssplit,pos,lemma",
                "pos.model",
                "lib_data/utb-models/en/pos-tagger/utb-en-bidirectional-glove-distsim-lower.full.tagger");

    JsonParser jsonParser = new JsonParser();
    JsonObject sent =
        jsonParser
            .parse(
                "{\"sentence\":\"Obama is the president of United States. He won a Nobel prize. What sport does Sally Pearson compete in? Who was Titanic directed by?\"}")
            .getAsJsonObject();
    NlpPipeline englishPipeline = new NlpPipeline(options);
    englishPipeline.processSentence(sent);

    options = new HashMap<>();
    options.put("annotators", "tokenize, ssplit, pos, lemma");

    options
        .put(
            "pos.model",
            "lib_data/utb-models/en/pos-tagger/utb-en-bidirectional-glove-distsim-lower.full.tagger");
    options.put("tokenize.whitespace", "true");
    options.put("ssplit.newlineIsSentenceBreak", "always");

    options.put("maltparser",
        "lib_data/utb-models/en/parser/en-stackproj-coarse.mco");
    options.put(SentenceKeys.SVG_TREES, "true");

    englishPipeline = new NlpPipeline(options);
    englishPipeline.processSentence(sent);
    BufferedWriter bw =
        new BufferedWriter(new FileWriter("/tmp/malt_canvas.html"));
    bw.write(sent.get(SentenceKeys.SVG_TREES).getAsString());
    bw.close();

    options = new HashMap<>();
    options.put("annotators", "tokenize,ssplit,pos,lemma,depparse");

    options
        .put(
            "pos.model",
            "lib_data/utb-models/en/pos-tagger/utb-en-bidirectional-glove-distsim-lower.full.tagger");
    options.put("tokenize.whitespace", "true");
    options.put("ssplit.newlineIsSentenceBreak", "always");

    options
        .put("depparse.model",
            "lib_data/utb-models/en/neural-parser/en-glove50.6B.nndep.model.txt.gz");
    options.put(SentenceKeys.SVG_TREES, "true");

    englishPipeline = new NlpPipeline(options);
    englishPipeline.processSentence(sent);
    bw = new BufferedWriter(new FileWriter("/tmp/stanford_nn_dep_canvas.html"));
    bw.write(sent.get(SentenceKeys.SVG_TREES).getAsString());
    bw.close();
  }
}
