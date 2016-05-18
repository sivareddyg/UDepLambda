package deplambda.parser;

import static org.junit.Assert.*;
import in.sivareddy.graphparser.cli.CcgParseToUngroundedGraphs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
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
  static Logger logger = Logger.getLogger("TreeTransformerUniversalTest");

  static {
    try {
      types = new MutableTypeRepository("lib_data/ud.types.txt");

      LogicLanguageServices.setInstance(
          new LogicLanguageServices.Builder(types, new FlexibleTypeComparator())
              .closeOntology(false).setNumeralTypeName("i").build());

      treeTransformationRules = new TransformationRuleGroups(
          "lib_data/ud-tree-transformation-rules.proto.txt");
      relationRules = new TransformationRuleGroups(
          "lib_data/ud-relation-priorities.proto.txt");
      lambdaAssignmentRules = new TransformationRuleGroups(
          "lib_data/ud-lambda-assignment-rules.proto.txt");
      PatternLayout layout = new PatternLayout("%r [%t] %-5p: %m%n");
      logger.setLevel(Level.DEBUG);
      logger.setAdditivity(false);
      Appender stdoutAppender = new ConsoleAppender(layout);
      logger.addAppender(stdoutAppender);
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
    JsonObject jsonSentence = jsonParser
        .parse(
            "{\"sentence\":\"Yahoo!\",\"words\":[{\"word\":\"Yahoo\",\"lemma\":\"yahoo\",\"pos\":\"PROPN\",\"ner\":\"ORGANIZATION\",\"dep\":\"root\",\"head\":0,\"index\":1},{\"word\":\"!\",\"lemma\":\"!\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":2,\"head\":1,\"dep\":\"punct\",\"sentEnd\":true}]}")
        .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(treeTransformationRules,
        sentence.getRootNode());
    assertEquals("(l-root w-1-yahoo t-PROPN (l-punct w-2-! t-PUNCT))",
        sentence.getRootNode().toString());

    String binarizedTreeString = TreeTransformer.binarizeTree(
        sentence.getRootNode(), relationRules.getRelationPriority());
    assertEquals("(l-punct w-1-yahoo w-2-!)", binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(lambdaAssignmentRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            relationRules.getRelationPriority(), false);

    assertEquals(
        "(lambda $0:<a,e> (and:c (p_TYPE_w-1-yahoo:u $0) (p_EVENT_w-1-yahoo:u $0) (p_EVENT.ENTITY_arg0:b $0 $0)))",
        sentenceSemantics.second().get(0).toString());
  }

  @Test
  public final void testCase() {
    JsonObject jsonSentence = jsonParser
        .parse(
            "{\"sentence\":\"in India.\",\"words\":[{\"word\":\"in\",\"lemma\":\"in\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":1,\"head\":2,\"dep\":\"case\"},{\"word\":\"India\",\"lemma\":\"india\",\"pos\":\"PROPN\",\"ner\":\"LOCATION\",\"dep\":\"root\",\"head\":0,\"index\":2},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":3,\"head\":2,\"dep\":\"punct\",\"sentEnd\":true}]}")
        .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(treeTransformationRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-2-india t-PROPN (l-case w-1-in t-ADP) (l-punct w-3-. t-PUNCT))",
        sentence.getRootNode().toString());

    String binarizedTreeString = TreeTransformer.binarizeTree(
        sentence.getRootNode(), relationRules.getRelationPriority());
    assertEquals("(l-punct (l-case w-2-india w-1-in) w-3-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(lambdaAssignmentRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            relationRules.getRelationPriority(), false);

    assertEquals(
        "(lambda $0:<a,e> (and:c (p_TYPE_w-2-india:u $0) (p_EVENT_w-2-india:u $0) (p_EVENT.ENTITY_arg0:b $0 $0)))",
        sentenceSemantics.second().get(0).toString());
  }

  @Test
  public final void testNmod() {
    // nmod with a case attached to a noun.
    JsonObject jsonSentence = jsonParser
        .parse(
            "{\"sentence\":\"city in India .\",\"words\":[{\"word\":\"city\",\"lemma\":\"city\",\"pos\":\"NOUN\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":1},{\"word\":\"in\",\"lemma\":\"in\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":2,\"head\":3,\"dep\":\"case\"},{\"word\":\"India\",\"lemma\":\"india\",\"pos\":\"PROPN\",\"ner\":\"LOCATION\",\"index\":3,\"head\":1,\"dep\":\"nmod\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":4,\"head\":1,\"dep\":\"punct\",\"sentEnd\":true}]}")
        .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(treeTransformationRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-1-city t-NOUN (l-nmod w-3-india t-PROPN (l-case w-2-in t-ADP)) (l-punct w-4-. t-PUNCT))",
        sentence.getRootNode().toString());

    String binarizedTreeString = TreeTransformer.binarizeTree(
        sentence.getRootNode(), relationRules.getRelationPriority());
    assertEquals("(l-punct (l-nmod w-1-city (l-case w-3-india w-2-in)) w-4-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(lambdaAssignmentRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            relationRules.getRelationPriority(), false);

    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (and:c (p_TYPE_w-1-city:u $0) (p_EVENT_w-1-city:u $0) (p_EVENT.ENTITY_arg0:b $0 $0)) (and:c (p_TYPE_w-3-india:u $1) (p_EVENT_w-3-india:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (p_EVENT.ENTITY_l-nmod.w-2-in:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());

    // nmod with a case attached to a verb.
    jsonSentence = jsonParser
        .parse(
            "{\"sentence\":\"saw with telescope .\",\"words\":[{\"word\":\"saw\",\"lemma\":\"see\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":1},{\"word\":\"with\",\"lemma\":\"with\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":2,\"head\":3,\"dep\":\"case\"},{\"word\":\"telescope\",\"lemma\":\"telescope\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":3,\"head\":1,\"dep\":\"nmod\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":4,\"head\":1,\"dep\":\"punct\",\"sentEnd\":true}]}")
        .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(treeTransformationRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-1-see t-VERB (l-nmod w-3-telescope t-NOUN (l-case w-2-with t-ADP)) (l-punct w-4-. t-PUNCT))",
        sentence.getRootNode().toString());

    binarizedTreeString = TreeTransformer.binarizeTree(sentence.getRootNode(),
        relationRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nmod w-1-see (l-case w-3-telescope w-2-with)) w-4-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(lambdaAssignmentRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics = TreeTransformer.composeSemantics(sentence.getRootNode(),
        relationRules.getRelationPriority(), false);

    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (p_EVENT_w-1-see:u $0) (and:c (p_TYPE_w-3-telescope:u $1) (p_EVENT_w-3-telescope:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (p_EVENT.ENTITY_l-nmod.w-2-with:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());

    // nmod with possessive case.
    jsonSentence = jsonParser
        .parse(
            "{\"sentence\":\"Darwin's book\",\"words\":[{\"word\":\"Darwin\",\"lemma\":\"darwin\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":1,\"head\":3,\"dep\":\"nmod:poss\"},{\"word\":\"'s\",\"lemma\":\"'s\",\"pos\":\"PART\",\"ner\":\"O\",\"index\":2,\"head\":1,\"dep\":\"case\"},{\"word\":\"book\",\"lemma\":\"book\",\"pos\":\"NOUN\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":3,\"sentEnd\":true}]}")
        .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(treeTransformationRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-3-book t-NOUN (l-nmod:poss w-1-darwin t-PROPN (l-case w-2-'s t-PART)))",
        sentence.getRootNode().toString());

    binarizedTreeString = TreeTransformer.binarizeTree(sentence.getRootNode(),
        relationRules.getRelationPriority());
    assertEquals("(l-nmod:poss w-3-book (l-case w-1-darwin w-2-'s))",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(lambdaAssignmentRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics = TreeTransformer.composeSemantics(sentence.getRootNode(),
        relationRules.getRelationPriority(), false);

    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (and:c (p_TYPE_w-3-book:u $0) (p_EVENT_w-3-book:u $0) (p_EVENT.ENTITY_arg0:b $0 $0)) (and:c (p_TYPE_w-1-darwin:u $1) (p_EVENT_w-1-darwin:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (p_EVENT.ENTITY_l-nmod.w-2-'s:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());

    // Two nmods.
    jsonSentence = jsonParser
        .parse(
            "{\"sentence\":\"saw with telescope on mountain\",\"words\":[{\"word\":\"saw\",\"lemma\":\"see\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":1},{\"word\":\"with\",\"lemma\":\"with\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":2,\"head\":3,\"dep\":\"case\"},{\"word\":\"telescope\",\"lemma\":\"telescope\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":3,\"head\":1,\"dep\":\"nmod\"},{\"word\":\"on\",\"lemma\":\"on\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":4,\"head\":5,\"dep\":\"case\"},{\"word\":\"mountain\",\"lemma\":\"mountain\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":5,\"head\":1,\"dep\":\"nmod\",\"sentEnd\":true}]}")
        .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(treeTransformationRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-1-see t-VERB (l-nmod w-3-telescope t-NOUN (l-case w-2-with t-ADP)) (l-nmod w-5-mountain t-NOUN (l-case w-4-on t-ADP)))",
        sentence.getRootNode().toString());

    binarizedTreeString = TreeTransformer.binarizeTree(sentence.getRootNode(),
        relationRules.getRelationPriority());
    assertEquals(
        "(l-nmod (l-nmod w-1-see (l-case w-3-telescope w-2-with)) (l-case w-5-mountain w-4-on))",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(lambdaAssignmentRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics = TreeTransformer.composeSemantics(sentence.getRootNode(),
        relationRules.getRelationPriority(), false);

    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-1-see:u $0) (and:c (p_TYPE_w-3-telescope:u $2) (p_EVENT_w-3-telescope:u $2) (p_EVENT.ENTITY_arg0:b $2 $2)) (p_EVENT.ENTITY_l-nmod.w-2-with:b $0 $2))) (and:c (p_TYPE_w-5-mountain:u $1) (p_EVENT_w-5-mountain:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (p_EVENT.ENTITY_l-nmod.w-4-on:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());

    // All other nmods.
    jsonSentence = jsonParser
        .parse(
            "{\"sentence\":\"years old\",\"words\":[{\"word\":\"years\",\"lemma\":\"year\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":1,\"head\":2,\"dep\":\"nmod:npmod\"},{\"word\":\"old\",\"lemma\":\"old\",\"pos\":\"ADJ\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":2,\"sentEnd\":true}]}")
        .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(treeTransformationRules,
        sentence.getRootNode());
    assertEquals("(l-root w-2-old t-ADJ (l-nmod:npmod w-1-year t-NOUN))",
        sentence.getRootNode().toString());

    binarizedTreeString = TreeTransformer.binarizeTree(sentence.getRootNode(),
        relationRules.getRelationPriority());
    assertEquals("(l-nmod:npmod w-2-old w-1-year)", binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(lambdaAssignmentRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics = TreeTransformer.composeSemantics(sentence.getRootNode(),
        relationRules.getRelationPriority(), false);

    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (p_EVENT_w-2-old:u $0) (and:c (p_TYPE_w-1-year:u $1) (p_EVENT_w-1-year:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (p_EVENT.ENTITY_l-nmod:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
  }

  @Test
  public final void testNsubj() {
    // nsubj with a preposition.
    JsonObject jsonSentence = jsonParser
        .parse(
            "{\"sentence\":\"John slept on couch.\",\"words\":[{\"word\":\"John\",\"lemma\":\"john\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":1,\"head\":2,\"dep\":\"nsubj\"},{\"word\":\"slept\",\"lemma\":\"sleep\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":2},{\"word\":\"on\",\"lemma\":\"on\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":3,\"head\":4,\"dep\":\"case\"},{\"word\":\"couch\",\"lemma\":\"couch\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":4,\"head\":2,\"dep\":\"nmod\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":5,\"head\":2,\"dep\":\"punct\",\"sentEnd\":true}]}")
        .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(treeTransformationRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-2-sleep t-VERB (l-nsubj w-1-john t-PROPN) (l-nmod w-4-couch t-NOUN (l-case w-3-on t-ADP)) (l-punct w-5-. t-PUNCT))",
        sentence.getRootNode().toString());

    String binarizedTreeString = TreeTransformer.binarizeTree(
        sentence.getRootNode(), relationRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-nmod w-2-sleep (l-case w-4-couch w-3-on)) w-1-john) w-5-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(lambdaAssignmentRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            relationRules.getRelationPriority(), false);

    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-2-sleep:u $0) (and:c (p_TYPE_w-4-couch:u $2) (p_EVENT_w-4-couch:u $2) (p_EVENT.ENTITY_arg0:b $2 $2)) (p_EVENT.ENTITY_l-nmod.w-3-on:b $0 $2))) (and:c (p_TYPE_w-1-john:u $1) (p_EVENT_w-1-john:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());

    // nsubj with copula.
    jsonSentence = jsonParser
        .parse(
            "{\"sentence\":\"John is old.\",\"words\":[{\"word\":\"John\",\"lemma\":\"john\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":1,\"head\":3,\"dep\":\"nsubj\"},{\"word\":\"is\",\"lemma\":\"be\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":2,\"head\":3,\"dep\":\"cop\"},{\"word\":\"old\",\"lemma\":\"old\",\"pos\":\"ADJ\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":3},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":4,\"head\":3,\"dep\":\"punct\",\"sentEnd\":true}]}")
        .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(treeTransformationRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-3-old t-ADJ (l-nsubj w-1-john t-PROPN) (l-cop w-2-be t-VERB) (l-punct w-4-. t-PUNCT))",
        sentence.getRootNode().toString());

    binarizedTreeString = TreeTransformer.binarizeTree(sentence.getRootNode(),
        relationRules.getRelationPriority());
    assertEquals("(l-punct (l-nsubj (l-cop w-3-old w-2-be) w-1-john) w-4-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(lambdaAssignmentRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics = TreeTransformer.composeSemantics(sentence.getRootNode(),
        relationRules.getRelationPriority(), false);

    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (p_EVENT_w-3-old:u $0) (and:c (p_TYPE_w-1-john:u $1) (p_EVENT_w-1-john:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
  }

  @Test
  public final void testDet() {
    // Multiple det relations.
    JsonObject jsonSentence = jsonParser
        .parse(
            "{\"sentence\":\"which city is the capital of US?\",\"words\":[{\"word\":\"which\",\"lemma\":\"which\",\"pos\":\"DET\",\"ner\":\"O\",\"index\":1,\"head\":2,\"dep\":\"det\"},{\"word\":\"city\",\"lemma\":\"city\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":2,\"head\":5,\"dep\":\"nsubj\"},{\"word\":\"is\",\"lemma\":\"be\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":3,\"head\":5,\"dep\":\"cop\"},{\"word\":\"the\",\"lemma\":\"the\",\"pos\":\"DET\",\"ner\":\"O\",\"index\":4,\"head\":5,\"dep\":\"det\"},{\"word\":\"capital\",\"lemma\":\"capital\",\"pos\":\"NOUN\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":5},{\"word\":\"of\",\"lemma\":\"of\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":6,\"head\":7,\"dep\":\"case\"},{\"word\":\"US\",\"lemma\":\"US\",\"pos\":\"PROPN\",\"ner\":\"LOCATION\",\"index\":7,\"head\":5,\"dep\":\"nmod\"},{\"word\":\"?\",\"lemma\":\"?\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":8,\"head\":5,\"dep\":\"punct\",\"sentEnd\":true}]}")
        .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(treeTransformationRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-5-capital t-NOUN (l-nsubj w-2-city t-NOUN (l-det-wh w-1-which t-DET)) (l-cop w-3-be t-VERB) (l-det w-4-the t-DET) (l-nmod w-7-US t-PROPN (l-case w-6-of t-ADP)) (l-punct w-8-? t-PUNCT))",
        sentence.getRootNode().toString());

    String binarizedTreeString = TreeTransformer.binarizeTree(
        sentence.getRootNode(), relationRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-det (l-nmod (l-cop w-5-capital w-3-be) (l-case w-7-US w-6-of)) w-4-the) (l-det-wh w-2-city w-1-which)) w-8-?)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(lambdaAssignmentRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            relationRules.getRelationPriority(), false);

    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (and:c (p_TYPE_w-5-capital:u $0) (p_EVENT_w-5-capital:u $0) (p_EVENT.ENTITY_arg0:b $0 $0)) (and:c (p_TYPE_w-7-US:u $2) (p_EVENT_w-7-US:u $2) (p_EVENT.ENTITY_arg0:b $2 $2)) (p_EVENT.ENTITY_l-nmod.w-6-of:b $0 $2))) (and:c (and:c (p_TYPE_w-2-city:u $1) (p_EVENT_w-2-city:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (p_TYPE_w-1-which:u $1) (p_TARGET:u $1)) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
  }

  @Test
  public final void testDobj() {
    // Multiple det relations.
    JsonObject jsonSentence = jsonParser
        .parse(
            "{\"sentence\":\"Bush nominated Anderson as judge of the Court.\",\"words\":[{\"word\":\"Bush\",\"lemma\":\"bush\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":1,\"head\":2,\"dep\":\"nsubj\"},{\"word\":\"nominated\",\"lemma\":\"nominate\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":2},{\"word\":\"Anderson\",\"lemma\":\"anderson\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":3,\"head\":2,\"dep\":\"dobj\"},{\"word\":\"as\",\"lemma\":\"as\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":4,\"head\":5,\"dep\":\"case\"},{\"word\":\"judge\",\"lemma\":\"judge\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":5,\"head\":2,\"dep\":\"nmod\"},{\"word\":\"of\",\"lemma\":\"of\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":6,\"head\":8,\"dep\":\"case\"},{\"word\":\"the\",\"lemma\":\"the\",\"pos\":\"DET\",\"ner\":\"O\",\"index\":7,\"head\":8,\"dep\":\"det\"},{\"word\":\"Court\",\"lemma\":\"court\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":8,\"head\":5,\"dep\":\"nmod\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":9,\"head\":2,\"dep\":\"punct\",\"sentEnd\":true}]}")
        .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(treeTransformationRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-2-nominate t-VERB (l-nsubj w-1-bush t-PROPN) (l-dobj w-3-anderson t-PROPN) (l-nmod w-5-judge t-NOUN (l-case w-4-as t-ADP) (l-nmod w-8-court t-NOUN (l-case w-6-of t-ADP) (l-det w-7-the t-DET))) (l-punct w-9-. t-PUNCT))",
        sentence.getRootNode().toString());

    String binarizedTreeString = TreeTransformer.binarizeTree(
        sentence.getRootNode(), relationRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-nmod (l-dobj w-2-nominate w-3-anderson) (l-case (l-nmod w-5-judge (l-case (l-det w-8-court w-7-the) w-6-of)) w-4-as)) w-1-bush) w-9-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(lambdaAssignmentRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            relationRules.getRelationPriority(), logger, false);

    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (exists:ex $3:<a,e> (and:c (p_EVENT_w-2-nominate:u $0) (and:c (p_TYPE_w-3-anderson:u $3) (p_EVENT_w-3-anderson:u $3) (p_EVENT.ENTITY_arg0:b $3 $3)) (p_EVENT.ENTITY_arg2:b $0 $3))) (exists:ex $4:<a,e> (and:c (and:c (p_TYPE_w-5-judge:u $2) (p_EVENT_w-5-judge:u $2) (p_EVENT.ENTITY_arg0:b $2 $2)) (and:c (p_TYPE_w-8-court:u $4) (p_EVENT_w-8-court:u $4) (p_EVENT.ENTITY_arg0:b $4 $4)) (p_EVENT.ENTITY_l-nmod.w-6-of:b $2 $4))) (p_EVENT.ENTITY_l-nmod.w-4-as:b $0 $2))) (and:c (p_TYPE_w-1-bush:u $1) (p_EVENT_w-1-bush:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    PostProcessLogicalForm.process(sentence, sentenceSemantics.second().get(0),
        true);
  }

  @Test
  public final void testReader() {
    // Cameron directed Titanic.
    LogicalExpression e1 = SimpleLogicalExpressionReader.read(
        "(lambda $f:w (lambda $g:w (lambda $x:v (and:c ($f $x) ($g $x)))))");
    LogicalExpression e2 = SimpleLogicalExpressionReader.read(
        "(lambda $0:<v,t> (lambda $1:<v,t> (lambda $2:<a,e> (and:c ($0 $2) ($1 $2)))))");
    assertTrue(e1.equals(e2));
  }
}
