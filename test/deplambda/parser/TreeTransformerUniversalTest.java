package deplambda.parser;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import deplambda.util.Sentence;
import deplambda.util.TransformationRuleGroups;
import edu.cornell.cs.nlp.spf.mr.lambda.FlexibleTypeComparator;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.SimpleLogicalExpressionReader;
import edu.cornell.cs.nlp.spf.mr.language.type.MutableTypeRepository;
import edu.cornell.cs.nlp.utils.composites.Pair;

public class TreeTransformerUniversalTest {

  static TransformationRuleGroups treeTransformationRules;
  static TransformationRuleGroups lambdaAssignmentRules;
  static TransformationRuleGroups relationRules;
  static MutableTypeRepository types;
  static JsonParser jsonParser = new JsonParser();

  static {
    try {
      types = new MutableTypeRepository("lib_data/ud.types.txt");

      LogicLanguageServices.setInstance(new LogicLanguageServices.Builder(
          types, new FlexibleTypeComparator()).closeOntology(false)
          .setNumeralTypeName("i").build());

      treeTransformationRules =
          new TransformationRuleGroups(
              "lib_data/ud-tree-transformation-rules.proto.txt");
      relationRules =
          new TransformationRuleGroups(
              "lib_data/ud-relation-priorities.proto.txt");
      lambdaAssignmentRules =
          new TransformationRuleGroups(
              "lib_data/ud-lambda-assignment-rules.proto.txt");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public final void testTypes() {
    assertEquals("<<<a,e>,t>,<<<a,e>,t>,<<a,e>,t>>>",
        types.unfoldType(types.getType("r")));
  }

  @Test
  public final void testPunct() {
    JsonObject jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"Yahoo!\",\"words\":[{\"word\":\"Yahoo\",\"lemma\":\"yahoo\",\"pos\":\"PROPN\",\"ner\":\"ORGANIZATION\",\"dep\":\"root\",\"head\":0,\"index\":1},{\"word\":\"!\",\"lemma\":\"!\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":2,\"head\":1,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(treeTransformationRules,
        sentence.getRootNode());
    assertEquals("(l-root w-1-yahoo t-PROPN (l-punct w-2-! t-PUNCT))", sentence
        .getRootNode().toString());

    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            relationRules.getRelationPriority());
    assertEquals("(l-punct w-1-yahoo w-2-!)", binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(lambdaAssignmentRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            relationRules.getRelationPriority(), false);

    assertEquals(
        "(lambda $0:<a,e> (and:c (and:c (p_TYPE_w-1-yahoo:u $0) (p_EVENT_w-1-yahoo:u $0) (p_EVENT.ENTITY_arg_1:b $0 $0)) (p_EMPTY:u $0)))",
        sentenceSemantics.second().get(0).toString());
  }

  @Test
  public final void testReader() {
    // Cameron directed Titanic.
    LogicalExpression e1 =
        SimpleLogicalExpressionReader
            .read("(lambda $f:w (lambda $g:w (lambda $x:v (and:c ($f $x) ($g $x)))))");
    LogicalExpression e2 =
        SimpleLogicalExpressionReader
            .read("(lambda $0:<v,t> (lambda $1:<v,t> (lambda $2:<a,e> (and:c ($0 $2) ($1 $2)))))");
    assertTrue(e1.equals(e2));
  }
}
