/**
 * 
 */
package deplambda.parser;

import static org.junit.Assert.*;

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

  TransformationRuleGroups treeTransformationRules;
  TransformationRuleGroups rules;
  JsonObject jsonSentence;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    String sentenceString =
        "{\"entities\": [{\"start\": 0, \"ner\": \"ORGANIZATION\", \"end\": 1, \"entity\": \"m.0k8z\"}, {\"start\": 3, \"ner\": \"PERSON\", \"end\": 4, \"entity\": \"m.06y3r\"}, {\"start\": 8, \"ner\": \"MISC\", \"end\": 9, \"entity\": \"m.04yg_s\"}], \"words\": [{\"word\":\"Apple\",\"head\":2,\"tag\":\"NNP\",\"label\":\"nn\"}, {\"word\":\"Inc.\",\"head\":8,\"tag\":\"NNP\",\"label\":\"nsubj\"}, {\"word\":\"which\",\"head\":6,\"tag\":\"WP\",\"label\":\"dobj\"}, {\"word\":\"Steve\",\"head\":5,\"tag\":\"NNP\",\"label\":\"nn\"}, {\"word\":\"Jobs\",\"head\":6,\"tag\":\"NNP\",\"label\":\"nsubj\"}, {\"word\":\"founded\",\"head\":2,\"tag\":\"VBD\",\"label\":\"rcmod\", \"lemma\":\"found\"}, {\"word\":\",\",\"head\":2,\"tag\":\".\",\"label\":\"p\"}, {\"word\":\"developed\",\"head\":0,\"tag\":\"VBD\",\"label\":\"ROOT\"}, {\"word\":\"iPod\",\"head\":10,\"tag\":\"NNP\",\"label\":\"nn\"}, {\"word\":\"shuffle\",\"head\":8,\"tag\":\"NN\",\"label\":\"dobj\"}, {\"word\":\".\",\"head\":8,\"tag\":\".\",\"label\":\"p\"}]}";
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
    
    
    String treeTransformationRulesString =
        "rulegroup {\n" + 
        "  name: \"relative clause (all rcmod)\"\n" + 
        "  priority: 1\n" + 
        "  rule {\n" + 
        "    name: \"dummy rule that should not to be applied\"\n" + 
        "    priority: 6\n" + 
        "    tregex: \"(l-rcmod=target < /t-V.*/)\"\n" + 
        "    transformation {\n" + 
        "      target: \"target\"\n" + 
        "      action: CHANGE_LABEL\n" + 
        "      label: \"l-rcmod-wrong\"\n" + 
        "    }\n" + 
        "  }\n" + 
        "  rule {\n" + 
        "    name: \"relative obj extraction\"\n" + 
        "    priority: 5\n" + 
        "    tregex: \"(l-rcmod=target < /t-V.*/) < (/l-(dobj|dep)/=whchild < /t-W.*/)\"\n" + 
        "    # Apple, which Jobs \"founded\", developed Iphone.\n" + 
        "    # (l-nsubj (l-p (l-p (l-dobj w-developed w-Iphone) w-,) w-.) (l-rcmod (l-p w-Apple w-,) (l-wh-dobj (l-BIND (l-nsubj (l-dobj w-founded v-f0) w-Jobs) v-f0) w-which)))\n" + 
        "    transformation {\n" + 
        "      target: \"whchild\"\n" + 
        "      action: CHANGE_LABEL\n" + 
        "      label: \"l-wh-dobj\"\n" + 
        "    }\n" + 
        "    transformation {\n" + 
        "      target: \"target\"\n" + 
        "      action: ADD_CHILD\n" + 
        "      label: \"l-dobj\"\n" + 
        "      child: \"v-f\"\n" + 
        "    }\n" + 
        "    transformation {\n" + 
        "      target: \"target\"\n" + 
        "      action: ADD_CHILD\n" + 
        "      label: \"l-BIND\"\n" + 
        "      child: \"v-f\"\n" + 
        "    }\n" + 
        "  }\n" +
        "  rule {\n" + 
        "    name: \"dummy rule that have to be applied\"\n" + 
        "    priority: 5\n" + 
        "    tregex: \"(l-rcmod=target < /t-V.*/=verb)\"\n" + 
        "    transformation {\n" + 
        "      target: \"verb\"\n" + 
        "      action: CHANGE_LABEL\n" + 
        "      label: \"l-VB\"\n" + 
        "    }\n" + 
        "  }\n" + 
        "}\n" + 
        "";
    // @formatter:on

    File treeTransformationRuleFile =
        File.createTempFile("tree-transfomration-rules", ".tmp");
    BufferedWriter bw =
        new BufferedWriter(new FileWriter(treeTransformationRuleFile));
    bw.write(treeTransformationRulesString);
    bw.close();
    treeTransformationRules =
        new TransformationRuleGroups(
            treeTransformationRuleFile.getAbsolutePath());
    treeTransformationRuleFile.delete();

    File ruleFile = File.createTempFile("test-rules", ".tmp");
    bw = new BufferedWriter(new FileWriter(ruleFile));
    bw.write(relations);
    bw.close();
    rules = new TransformationRuleGroups(ruleFile.getAbsolutePath());
    ruleFile.delete();
  }

  /**
   * Test method for
   * {@link deplambda.parser.TreeTransformer#ApplyRuleGroupsOnTree(TransformationRuleGroups, deplambda.util.DependencyTree)}
   * .
   */
  @Test
  public final void testApplyRuleGroupsOnTree() {
    Sentence sentence = new Sentence(jsonSentence);
    System.out.println(sentence.getRootNode());
    assertTrue(TreeTransformer.ApplyRuleGroupsOnTree(treeTransformationRules,
        sentence.getRootNode()));

    assertEquals(
        "(l-ROOT w-developed t-VBD (l-nsubj w-Inc. t-NNP (l-rcmod w-found l-VB (l-wh-dobj w-which t-WP) (l-nsubj w-Jobs t-NNP) (l-dobj v-f) (l-BIND v-f)) (l-p w-, t-.)) (l-dobj w-shuffle t-NN) (l-p w-. t-.))",
        sentence.getRootNode().toString());

    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            rules.getRelationPriority());
    assertEquals(
        "(l-nsubj (l-p (l-dobj w-developed w-shuffle) w-.) (l-p (l-rcmod w-Inc. (l-BIND (l-wh-dobj (l-nsubj (l-dobj w-found v-f) w-Jobs) w-which) v-f)) w-,))",
        binarizedTreeString);
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
        "(l-nsubj (l-p (l-dobj w-developed w-shuffle) w-.) (l-p (l-rcmod w-Inc. (l-nsubj (l-dobj w-found w-which) w-Jobs)) w-,))",
        binarizedTreeString);
  }
}
