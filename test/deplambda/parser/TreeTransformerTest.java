/**
 * 
 */
package deplambda.parser;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import deplambda.util.Sentence;
import deplambda.util.TransformationRuleGroups;

/**
 * @author Siva Reddy
 *
 */
public class TreeTransformerTest {

  TransformationRuleGroups rules;
  JsonObject jsonSentence;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    String sentenceString =
        "{\"entities\": [{\"start\": 0, \"ner\": \"ORGANIZATION\", \"end\": 1, \"entity\": \"m.0k8z\"}, {\"start\": 4, \"ner\": \"PERSON\", \"end\": 5, \"entity\": \"m.06y3r\"}, {\"start\": 8, \"ner\": \"MISC\", \"end\": 9, \"entity\": \"m.04yg_s\"}], \"words\": [{\"word\":\"Apple\",\"head\":2,\"tag\":\"NNP\",\"label\":\"nn\"}, {\"word\":\"Inc.\",\"head\":8,\"tag\":\"NNP\",\"label\":\"nsubj\"}, {\"word\":\"which\",\"head\":6,\"tag\":\"WP\",\"label\":\"dobj\"}, {\"word\":\"Steve\",\"head\":5,\"tag\":\"NNP\",\"label\":\"nn\"}, {\"word\":\"Jobs\",\"head\":6,\"tag\":\"NNP\",\"label\":\"nsubj\"}, {\"word\":\"founded\",\"head\":2,\"tag\":\"VBD\",\"label\":\"rcmod\", \"lemma\":\"found\"}, {\"word\":\",\",\"head\":2,\"tag\":\".\",\"label\":\"p\"}, {\"word\":\"developed\",\"head\":0,\"tag\":\"VBD\",\"label\":\"ROOT\"}, {\"word\":\"iPod\",\"head\":10,\"tag\":\"NNP\",\"label\":\"nn\"}, {\"word\":\"shuffle\",\"head\":8,\"tag\":\"NN\",\"label\":\"dobj\"}, {\"word\":\".\",\"head\":8,\"tag\":\".\",\"label\":\"p\"}]}";
    JsonParser jsonParser = new JsonParser();
    jsonSentence = jsonParser.parse(sentenceString).getAsJsonObject();

    // @formatter:off
    String relations =
        "relation {\n" + 
        "  name: \"l-dobj\"\n" +  
        "  priority: 1\n" + 
        "}\n" +
        "relation {\n" + 
        "  name: \"l-p\"\n" +  
        "  priority: 3\n" + 
        "}\n" +
        "relation {\n" + 
        "  name: \"l-rcmod\"\n" +  
        "  priority: 2\n" + 
        "}\n" +
        "relation {\n" + 
        "  name: \"l-nsubj\"\n" +  
        "  priority: 4\n" + 
        "}\n";
    // @formatter:on

    File ruleFile = File.createTempFile("test-rules", ".tmp");
    BufferedWriter bw = new BufferedWriter(new FileWriter(ruleFile));
    bw.write(relations);
    bw.close();

    rules = new TransformationRuleGroups(ruleFile.getAbsolutePath());
    ruleFile.delete();
  }

  /**
   * Test method for
   * {@link deplambda.parser.TreeTransformer#ApplyRules(deplambda.util.TransformationRuleGroups, deplambda.util.DependencyTree)}
   * .
   */
  @Test
  public final void testApplyRules() {
    // TODO
  }

  /**
   * Test method for
   * {@link deplambda.parser.TreeTransformer#binarizeTree(deplambda.util.DependencyTree, java.util.Map)}
   * .
   */
  @Test
  public final void testBinarizeTree() {
    Sentence sentence = new Sentence(jsonSentence);
    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            rules.getRelationPriority());
    assertEquals(
        "(l-nsubj (l-p (l-dobj w-developed w-shuffle) w-.) (l-p (l-rcmod w-Inc. (l-dobj w-found w-which)) w-,))",
        binarizedTreeString);
  }
}
