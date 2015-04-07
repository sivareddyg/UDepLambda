/**
 * 
 */
package uk.ac.ed.ilcc.deplambda.util;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import uk.ac.ed.ilcc.deplambda.util.Sentence;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author Siva Reddy
 *
 */
public class SentenceTest {

  private JsonObject jsonSentence;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    String dependencyTreeJson =
        "{\"entities\": [{\"start\": 0, \"ner\": \"ORGANIZATION\", \"end\": 1, \"entity\": \"m.0k8z\"}, {\"start\": 3, \"ner\": \"PERSON\", \"end\": 4, \"entity\": \"m.06y3r\"}, {\"start\": 8, \"ner\": \"MISC\", \"end\": 9, \"entity\": \"m.04yg_s\"}], \"words\": [{\"word\":\"Apple\",\"head\":2,\"tag\":\"NNP\",\"label\":\"nn\"}, {\"word\":\"Inc.\",\"head\":8,\"tag\":\"NNP\",\"label\":\"nsubj\"}, {\"word\":\"which\",\"head\":6,\"tag\":\"WP\",\"label\":\"dobj\"}, {\"word\":\"Steve\",\"head\":5,\"tag\":\"NNP\",\"label\":\"nn\"}, {\"word\":\"Jobs\",\"head\":6,\"tag\":\"NNP\",\"label\":\"nsubj\"}, {\"word\":\"founded\",\"head\":2,\"tag\":\"VBD\",\"label\":\"rcmod\", \"lemma\":\"found\"}, {\"word\":\",\",\"head\":2,\"tag\":\".\",\"label\":\"p\"}, {\"word\":\"developed\",\"head\":0,\"tag\":\"VBD\",\"label\":\"ROOT\"}, {\"word\":\"iPod\",\"head\":10,\"tag\":\"NNP\",\"label\":\"nn\"}, {\"word\":\"shuffle\",\"head\":8,\"tag\":\"NN\",\"label\":\"dobj\"}, {\"word\":\".\",\"head\":8,\"tag\":\".\",\"label\":\"p\"}]}";
    JsonParser json = new JsonParser();
    jsonSentence = json.parse(dependencyTreeJson).getAsJsonObject();
  }

  /**
   * Test method for
   * {@link uk.ac.ed.ilcc.deplambda.util.Sentence#Sentence(com.google.gson.JsonObject)}.
   */
  @Test
  public void testSentence() {
    Sentence sent = new Sentence(jsonSentence);
    assertEquals(
        "(l-ROOT w-developed t-VBD (l-nsubj w-Inc. t-NNP (l-rcmod w-found t-VBD (l-dobj w-which t-WP) (l-nsubj w-Jobs t-NNP)) (l-p w-, t-.)) (l-dobj w-shuffle t-NN) (l-p w-. t-.))",
        sent.getRootNode().toString());
  }

}
