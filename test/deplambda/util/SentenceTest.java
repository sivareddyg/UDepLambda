/**
 * 
 */
package deplambda.util;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import deplambda.util.Sentence;

/**
 * @author Siva Reddy
 *
 */
public class SentenceTest {

  private static JsonParser json = new JsonParser();

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() {
  }

  /**
   * Test method for
   * {@link deplambda.util.Sentence#Sentence(com.google.gson.JsonObject)}.
   */
  @Test
  public void testSentence() {
    JsonObject jsonSentence = json
        .parse(
            "{\"entities\": [{\"start\": 0, \"ner\": \"ORGANIZATION\", \"end\": 1, \"entity\": \"m.0k8z\"}, {\"start\": 3, \"ner\": \"PERSON\", \"end\": 4, \"entity\": \"m.06y3r\"}, {\"start\": 8, \"ner\": \"MISC\", \"end\": 9, \"entity\": \"m.04yg_s\"}], \"words\": [{\"word\":\"Apple\",\"head\":2,\"pos\":\"NNP\",\"dep\":\"nn\"}, {\"word\":\"Inc.\",\"head\":8,\"pos\":\"NNP\",\"dep\":\"nsubj\"}, {\"word\":\"which\",\"head\":6,\"pos\":\"WP\",\"dep\":\"dobj\"}, {\"word\":\"Steve\",\"head\":5,\"pos\":\"NNP\",\"dep\":\"nn\"}, {\"word\":\"Jobs\",\"head\":6,\"pos\":\"NNP\",\"dep\":\"nsubj\"}, {\"word\":\"founded\",\"head\":2,\"pos\":\"VBD\",\"dep\":\"rcmod\", \"lemma\":\"found\"}, {\"word\":\",\",\"head\":2,\"pos\":\".\",\"dep\":\"p\"}, {\"word\":\"developed\",\"head\":0,\"pos\":\"VBD\",\"dep\":\"ROOT\"}, {\"word\":\"iPod\",\"head\":10,\"pos\":\"NNP\",\"dep\":\"nn\"}, {\"word\":\"shuffle\",\"head\":8,\"pos\":\"NN\",\"dep\":\"dobj\"}, {\"word\":\".\",\"head\":8,\"pos\":\".\",\"dep\":\"p\"}]}")
        .getAsJsonObject();
    Sentence sent = new Sentence(jsonSentence);
    assertEquals(
        "(l-ROOT w-developed t-VBD (l-nsubj w-inc. t-NNP (l-rcmod w-founded t-VBD (l-dobj w-which t-WP) (l-nsubj w-jobs t-NNP)) (l-p w-, t-.)) (l-dobj w-shuffle t-NN) (l-p w-. t-.))",
        sent.getRootNode().toString());
  }

}
