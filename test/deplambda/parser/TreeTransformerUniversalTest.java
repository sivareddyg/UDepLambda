package deplambda.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import deplambda.others.SentenceKeys;
import deplambda.util.DependencyTree;
import deplambda.util.Sentence;
import deplambda.util.TransformationRuleGroups;
import edu.cornell.cs.nlp.spf.mr.lambda.FlexibleTypeComparator;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.SimpleLogicalExpressionReader;
import edu.cornell.cs.nlp.spf.mr.language.type.MutableTypeRepository;
import edu.cornell.cs.nlp.utils.composites.Pair;

public class TreeTransformerUniversalTest {

  static TransformationRuleGroups enhancementRules;
  static TransformationRuleGroups substitutionRules;
  static TransformationRuleGroups obliquenessHierarchyRules;
  static MutableTypeRepository types;
  static JsonParser jsonParser = new JsonParser();
  static Logger logger = Logger.getLogger("TreeTransformerUniversalTest");

  static {
    try {
      DependencyTree.LEXICAL_KEY = SentenceKeys.LEMMA_KEY;
      types = new MutableTypeRepository("lib_data/ud.types.txt");

      LogicLanguageServices.setInstance(new LogicLanguageServices.Builder(
          types, new FlexibleTypeComparator()).closeOntology(false)
          .setNumeralTypeName("i").build());

      enhancementRules =
          new TransformationRuleGroups(
              "lib_data/ud-enhancement-rules.proto");
      obliquenessHierarchyRules =
          new TransformationRuleGroups(
              "lib_data/ud-obliqueness-hierarchy.proto");
      substitutionRules =
          new TransformationRuleGroups(
              "lib_data/ud-substitution-rules.proto");
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
    JsonObject jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"Yahoo!\",\"words\":[{\"word\":\"Yahoo\",\"lemma\":\"yahoo\",\"pos\":\"PROPN\",\"ner\":\"ORGANIZATION\",\"dep\":\"root\",\"head\":0,\"index\":1},{\"word\":\"!\",\"lemma\":\"!\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":2,\"head\":1,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals("(l-root w-1-yahoo t-PROPN (l-punct w-2-! t-PUNCT))", sentence
        .getRootNode().toString());

    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals("(l-punct w-1-yahoo w-2-!)", binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (and:c (p_TYPE_w-1-yahoo:u $0) (p_EVENT_w-1-yahoo:u $0) (p_EVENT.ENTITY_arg0:b $0 $0)))",
        sentenceSemantics.second().get(0).toString());
  }

  @Test
  public final void testCase() {
    JsonObject jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"in India.\",\"words\":[{\"word\":\"in\",\"lemma\":\"in\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":1,\"head\":2,\"dep\":\"case\"},{\"word\":\"India\",\"lemma\":\"india\",\"pos\":\"PROPN\",\"ner\":\"LOCATION\",\"dep\":\"root\",\"head\":0,\"index\":2},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":3,\"head\":2,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-2-india t-PROPN (l-case w-1-in t-ADP) (l-punct w-3-. t-PUNCT))",
        sentence.getRootNode().toString());

    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals("(l-punct (l-case w-2-india w-1-in) w-3-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (and:c (p_TYPE_w-2-india:u $0) (p_EVENT_w-2-india:u $0) (p_EVENT.ENTITY_arg0:b $0 $0)))",
        sentenceSemantics.second().get(0).toString());
  }

  @Test
  public final void testNmod() {
    // nmod with a case attached to a noun.
    JsonObject jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"city in India .\",\"words\":[{\"word\":\"city\",\"lemma\":\"city\",\"pos\":\"NOUN\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":1},{\"word\":\"in\",\"lemma\":\"in\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":2,\"head\":3,\"dep\":\"case\"},{\"word\":\"India\",\"lemma\":\"india\",\"pos\":\"PROPN\",\"ner\":\"LOCATION\",\"index\":3,\"head\":1,\"dep\":\"nmod\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":4,\"head\":1,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-1-city t-NOUN (l-nmod w-3-india t-PROPN (l-case w-2-in t-ADP)) (l-punct w-4-. t-PUNCT))",
        sentence.getRootNode().toString());

    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals("(l-punct (l-nmod w-1-city (l-case w-3-india w-2-in)) w-4-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (and:c (p_TYPE_w-1-city:u $0) (p_EVENT_w-1-city:u $0) (p_EVENT.ENTITY_arg0:b $0 $0)) (and:c (p_TYPE_w-3-india:u $1) (p_EVENT_w-3-india:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (p_EVENT.ENTITY_l-nmod.w-2-in:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());

    // nmod with a case attached to a verb.
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"saw with telescope .\",\"words\":[{\"word\":\"saw\",\"lemma\":\"see\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":1},{\"word\":\"with\",\"lemma\":\"with\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":2,\"head\":3,\"dep\":\"case\"},{\"word\":\"telescope\",\"lemma\":\"telescope\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":3,\"head\":1,\"dep\":\"nmod\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":4,\"head\":1,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-1-see t-VERB (l-nmod w-3-telescope t-NOUN (l-case w-2-with t-ADP)) (l-punct w-4-. t-PUNCT))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nmod w-1-see (l-case w-3-telescope w-2-with)) w-4-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (p_EVENT_w-1-see:u $0) (and:c (p_TYPE_w-3-telescope:u $1) (p_EVENT_w-3-telescope:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (p_EVENT.ENTITY_l-nmod.w-2-with:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());

    // nmod with possessive case.
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"Darwin's book\",\"words\":[{\"word\":\"Darwin\",\"lemma\":\"darwin\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":1,\"head\":3,\"dep\":\"nmod:poss\"},{\"word\":\"'s\",\"lemma\":\"'s\",\"pos\":\"PART\",\"ner\":\"O\",\"index\":2,\"head\":1,\"dep\":\"case\"},{\"word\":\"book\",\"lemma\":\"book\",\"pos\":\"NOUN\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":3,\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-3-book t-NOUN (l-nmod:poss w-1-darwin t-PROPN (l-case w-2-'s t-PART)))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals("(l-nmod:poss w-3-book (l-case w-1-darwin w-2-'s))",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (and:c (p_TYPE_w-3-book:u $0) (p_EVENT_w-3-book:u $0) (p_EVENT.ENTITY_arg0:b $0 $0)) (and:c (p_TYPE_w-1-darwin:u $1) (p_EVENT_w-1-darwin:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (p_EVENT.ENTITY_l-nmod.w-2-'s:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());

    // Two nmods.
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"saw with telescope on mountain\",\"words\":[{\"word\":\"saw\",\"lemma\":\"see\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":1},{\"word\":\"with\",\"lemma\":\"with\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":2,\"head\":3,\"dep\":\"case\"},{\"word\":\"telescope\",\"lemma\":\"telescope\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":3,\"head\":1,\"dep\":\"nmod\"},{\"word\":\"on\",\"lemma\":\"on\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":4,\"head\":5,\"dep\":\"case\"},{\"word\":\"mountain\",\"lemma\":\"mountain\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":5,\"head\":1,\"dep\":\"nmod\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-1-see t-VERB (l-nmod w-3-telescope t-NOUN (l-case w-2-with t-ADP)) (l-nmod w-5-mountain t-NOUN (l-case w-4-on t-ADP)))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-nmod (l-nmod w-1-see (l-case w-3-telescope w-2-with)) (l-case w-5-mountain w-4-on))",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-1-see:u $0) (and:c (p_TYPE_w-3-telescope:u $2) (p_EVENT_w-3-telescope:u $2) (p_EVENT.ENTITY_arg0:b $2 $2)) (p_EVENT.ENTITY_l-nmod.w-2-with:b $0 $2))) (and:c (p_TYPE_w-5-mountain:u $1) (p_EVENT_w-5-mountain:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (p_EVENT.ENTITY_l-nmod.w-4-on:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());

    // All other nmods.
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"years old\",\"words\":[{\"word\":\"years\",\"lemma\":\"year\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":1,\"head\":2,\"dep\":\"nmod:npmod\"},{\"word\":\"old\",\"lemma\":\"old\",\"pos\":\"ADJ\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":2,\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals("(l-root w-2-old t-ADJ (l-nmod:npmod w-1-year t-NOUN))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals("(l-nmod:npmod w-2-old w-1-year)", binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (p_EVENT_w-2-old:u $0) (p_TYPE_w-1-year:u $1) (p_EVENT.ENTITY_l-nmod:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
  }

  @Test
  public final void testNsubj() {
    // nsubj with a preposition.
    JsonObject jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"John slept on couch.\",\"words\":[{\"word\":\"John\",\"lemma\":\"john\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":1,\"head\":2,\"dep\":\"nsubj\"},{\"word\":\"slept\",\"lemma\":\"sleep\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":2},{\"word\":\"on\",\"lemma\":\"on\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":3,\"head\":4,\"dep\":\"case\"},{\"word\":\"couch\",\"lemma\":\"couch\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":4,\"head\":2,\"dep\":\"nmod\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":5,\"head\":2,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-2-sleep t-VERB (l-nsubj w-1-john t-PROPN) (l-nmod w-4-couch t-NOUN (l-case w-3-on t-ADP)) (l-punct w-5-. t-PUNCT))",
        sentence.getRootNode().toString());

    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-nmod w-2-sleep (l-case w-4-couch w-3-on)) w-1-john) w-5-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-2-sleep:u $0) (and:c (p_TYPE_w-4-couch:u $2) (p_EVENT_w-4-couch:u $2) (p_EVENT.ENTITY_arg0:b $2 $2)) (p_EVENT.ENTITY_l-nmod.w-3-on:b $0 $2))) (p_TYPE_w-1-john:u $1) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());

    // nsubj with copula.
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"John is old.\",\"words\":[{\"word\":\"John\",\"lemma\":\"john\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":1,\"head\":3,\"dep\":\"nsubj\"},{\"word\":\"is\",\"lemma\":\"be\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":2,\"head\":3,\"dep\":\"cop\"},{\"word\":\"old\",\"lemma\":\"old\",\"pos\":\"ADJ\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":3},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":4,\"head\":3,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-3-old t-ADJ (l-nsubj w-1-john t-PROPN) (l-cop w-2-be t-VERB) (l-punct w-4-. t-PUNCT))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals("(l-punct (l-nsubj (l-cop w-3-old w-2-be) w-1-john) w-4-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (p_EVENT_w-3-old:u $0) (p_TYPE_w-1-john:u $1) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());

    List<String> cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals("[old.arg1(2:e , 0:m.john)]", cleanedPredicates.toString());
  }

  @Test
  public final void testDet() {
    // Multiple det relations.
    JsonObject jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"which city is the capital of US?\",\"words\":[{\"word\":\"which\",\"lemma\":\"which\",\"pos\":\"DET\",\"ner\":\"O\",\"index\":1,\"head\":2,\"dep\":\"det\"},{\"word\":\"city\",\"lemma\":\"city\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":2,\"head\":5,\"dep\":\"nsubj\"},{\"word\":\"is\",\"lemma\":\"be\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":3,\"head\":5,\"dep\":\"cop\"},{\"word\":\"the\",\"lemma\":\"the\",\"pos\":\"DET\",\"ner\":\"O\",\"index\":4,\"head\":5,\"dep\":\"det\"},{\"word\":\"capital\",\"lemma\":\"capital\",\"pos\":\"NOUN\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":5},{\"word\":\"of\",\"lemma\":\"of\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":6,\"head\":7,\"dep\":\"case\"},{\"word\":\"US\",\"lemma\":\"US\",\"pos\":\"PROPN\",\"ner\":\"LOCATION\",\"index\":7,\"head\":5,\"dep\":\"nmod\"},{\"word\":\"?\",\"lemma\":\"?\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":8,\"head\":5,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-5-capital t-NOUN (l-nsubj w-2-city t-NOUN (l-det w-1-which t-DET:WH)) (l-cop w-3-be t-VERB) (l-det w-4-the t-DET) (l-nmod w-7-us t-PROPN (l-case w-6-of t-ADP)) (l-punct w-8-? t-PUNCT))",
        sentence.getRootNode().toString());

    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-nmod (l-cop (l-det w-5-capital w-4-the) w-3-be) (l-case w-7-us w-6-of)) (l-det w-2-city w-1-which)) w-8-?)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (and:c (and:c (p_TYPE_w-5-capital:u $0) (p_EVENT_w-5-capital:u $0) (p_EVENT.ENTITY_arg0:b $0 $0)) (p_EMPTY:u $0)) (and:c (p_TYPE_w-7-us:u $2) (p_EVENT_w-7-us:u $2) (p_EVENT.ENTITY_arg0:b $2 $2)) (p_EVENT.ENTITY_l-nmod.w-6-of:b $0 $2))) (and:c (and:c (p_TYPE_w-2-city:u $1) (p_EVENT_w-2-city:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (and:c (p_TYPEMOD_w-1-which:u $1) (p_TARGET:u $1))) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());

    List<String> cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[QUESTION(1:x), arg0(6:e , 6:m.us), capital(4:s , 4:x), capital.arg0(4:e , 4:x), capital.arg1(4:e , 1:x), capital.nmod.of(4:e , 6:m.us), city(1:s , 1:x), city.arg0(1:e , 1:x), which(0:s , 1:x)]",
        cleanedPredicates.toString());


    // question with copula.
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"What is the capital of UK?\",\"words\":[{\"word\":\"What\",\"lemma\":\"what\",\"pos\":\"PRON\",\"index\":1,\"head\":4,\"dep\":\"nsubj\"},{\"word\":\"is\",\"lemma\":\"be\",\"pos\":\"VERB\",\"index\":2,\"head\":4,\"dep\":\"cop\"},{\"word\":\"the\",\"lemma\":\"the\",\"pos\":\"DET\",\"index\":3,\"head\":4,\"dep\":\"det\"},{\"word\":\"capital\",\"lemma\":\"capital\",\"pos\":\"NOUN\",\"dep\":\"root\",\"head\":0,\"index\":4},{\"word\":\"of\",\"lemma\":\"of\",\"pos\":\"ADP\",\"index\":5,\"head\":6,\"dep\":\"case\"},{\"word\":\"UK\",\"lemma\":\"uk\",\"pos\":\"PROPN\",\"index\":6,\"head\":4,\"dep\":\"nmod\"},{\"word\":\"?\",\"lemma\":\"?\",\"pos\":\"PUNCT\",\"index\":7,\"head\":4,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-4-capital t-NOUN (l-nsubj w-1-what t-PRON:WH) (l-cop w-2-be t-VERB) (l-det w-3-the t-DET) (l-nmod w-6-uk t-PROPN (l-case w-5-of t-ADP)) (l-punct w-7-? t-PUNCT))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-nmod (l-cop (l-det w-4-capital w-3-the) w-2-be) (l-case w-6-uk w-5-of)) w-1-what) w-7-?)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (and:c (and:c (p_TYPE_w-4-capital:u $0) (p_EVENT_w-4-capital:u $0) (p_EVENT.ENTITY_arg0:b $0 $0)) (p_EMPTY:u $0)) (and:c (p_TYPE_w-6-uk:u $2) (p_EVENT_w-6-uk:u $2) (p_EVENT.ENTITY_arg0:b $2 $2)) (p_EVENT.ENTITY_l-nmod.w-5-of:b $0 $2))) (and:c (p_TYPE_w-1-what:u $1) (p_TARGET:u $1)) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());

    cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[QUESTION(0:x), arg0(5:e , 5:m.uk), capital(3:s , 3:x), capital.arg0(3:e , 3:x), capital.arg1(3:e , 0:x), capital.nmod.of(3:e , 5:m.uk), what(0:s , 0:x)]",
        cleanedPredicates.toString());
  }

  @Test
  public final void testDobj() {
    // Multiple det relations.
    JsonObject jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"Bush nominated Anderson as judge of the Court.\",\"words\":[{\"word\":\"Bush\",\"lemma\":\"bush\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":1,\"head\":2,\"dep\":\"nsubj\"},{\"word\":\"nominated\",\"lemma\":\"nominate\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":2},{\"word\":\"Anderson\",\"lemma\":\"anderson\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":3,\"head\":2,\"dep\":\"dobj\"},{\"word\":\"as\",\"lemma\":\"as\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":4,\"head\":5,\"dep\":\"case\"},{\"word\":\"judge\",\"lemma\":\"judge\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":5,\"head\":2,\"dep\":\"nmod\"},{\"word\":\"of\",\"lemma\":\"of\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":6,\"head\":8,\"dep\":\"case\"},{\"word\":\"the\",\"lemma\":\"the\",\"pos\":\"DET\",\"ner\":\"O\",\"index\":7,\"head\":8,\"dep\":\"det\"},{\"word\":\"Court\",\"lemma\":\"court\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":8,\"head\":5,\"dep\":\"nmod\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":9,\"head\":2,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-2-nominate t-VERB (l-nsubj w-1-bush t-PROPN) (l-dobj w-3-anderson t-PROPN) (l-nmod w-5-judge t-NOUN (l-case w-4-as t-ADP) (l-nmod w-8-court t-NOUN (l-case w-6-of t-ADP) (l-det w-7-the t-DET))) (l-punct w-9-. t-PUNCT))",
        sentence.getRootNode().toString());

    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-nmod (l-dobj w-2-nominate w-3-anderson) (l-case (l-nmod w-5-judge (l-case (l-det w-8-court w-7-the) w-6-of)) w-4-as)) w-1-bush) w-9-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (exists:ex $3:<a,e> (and:c (p_EVENT_w-2-nominate:u $0) (p_TYPE_w-3-anderson:u $3) (p_EVENT.ENTITY_arg2:b $0 $3))) (exists:ex $4:<a,e> (and:c (and:c (p_TYPE_w-5-judge:u $2) (p_EVENT_w-5-judge:u $2) (p_EVENT.ENTITY_arg0:b $2 $2)) (and:c (and:c (p_TYPE_w-8-court:u $4) (p_EVENT_w-8-court:u $4) (p_EVENT.ENTITY_arg0:b $4 $4)) (p_EMPTY:u $4)) (p_EVENT.ENTITY_l-nmod.w-6-of:b $2 $4))) (p_EVENT.ENTITY_l-nmod.w-4-as:b $0 $2))) (p_TYPE_w-1-bush:u $1) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    List<String> cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[court(7:s , 7:x), court.arg0(7:e , 7:x), judge(4:s , 4:x), judge.arg0(4:e , 4:x), judge.nmod.of(4:e , 7:x), nominate.arg1(1:e , 0:m.bush), nominate.arg2(1:e , 2:m.anderson), nominate.nmod.as(1:e , 4:x)]",
        cleanedPredicates.toString());
  }

  @Test
  public final void testAdvmod() {
    JsonObject jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"where was cameron born?\",\"words\":[{\"word\":\"where\",\"lemma\":\"where\",\"pos\":\"ADV\",\"ner\":\"O\",\"index\":1,\"head\":4,\"dep\":\"advmod\"},{\"word\":\"was\",\"lemma\":\"was\",\"pos\":\"AUX\",\"ner\":\"O\",\"index\":2,\"head\":4,\"dep\":\"auxpass\"},{\"word\":\"cameron\",\"lemma\":\"cameron\",\"pos\":\"PROPN\",\"ner\":\"O\",\"index\":3,\"head\":4,\"dep\":\"nsubj\"},{\"word\":\"born\",\"lemma\":\"bear\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":4},{\"word\":\"?\",\"lemma\":\"?\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":5,\"head\":4,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-4-bear t-VERB (l-advmod w-1-where t-ADV:WH) (l-auxpass w-2-was t-AUX) (l-nsubj w-3-cameron t-PROPN) (l-punct w-5-? t-PUNCT))",
        sentence.getRootNode().toString());

    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-advmod (l-auxpass w-4-bear w-2-was) w-1-where) w-3-cameron) w-5-?)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-4-bear:u $0) (and:c (p_TYPE_w-1-where:u $2) (p_TARGET:u $2)) (p_EVENT.ENTITY_l-advmod:b $0 $2))) (p_TYPE_w-3-cameron:u $1) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    List<String> cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[QUESTION(0:x), bear.advmod(3:e , 0:x), bear.arg1(3:e , 2:m.cameron), where(0:s , 0:x)]",
        cleanedPredicates.toString());

    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"I quickly ate bananas.\",\"words\":[{\"word\":\"I\",\"lemma\":\"I\",\"pos\":\"PRON\",\"ner\":\"O\",\"index\":1,\"head\":3,\"dep\":\"nsubj\"},{\"word\":\"quickly\",\"lemma\":\"quickly\",\"pos\":\"ADV\",\"ner\":\"O\",\"index\":2,\"head\":3,\"dep\":\"advmod\"},{\"word\":\"ate\",\"lemma\":\"eat\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":3},{\"word\":\"bananas\",\"lemma\":\"bananas\",\"pos\":\"PROPN\",\"ner\":\"O\",\"index\":4,\"head\":3,\"dep\":\"dobj\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":5,\"head\":3,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-3-eat t-VERB (l-nsubj w-1-i t-PRON) (l-advmod w-2-quickly t-ADV) (l-dobj w-4-bananas t-PROPN) (l-punct w-5-. t-PUNCT))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-advmod (l-dobj w-3-eat w-4-bananas) w-2-quickly) w-1-i) w-5-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-3-eat:u $0) (p_TYPE_w-4-bananas:u $2) (p_EVENT.ENTITY_arg2:b $0 $2))) (p_EVENTMOD_w-2-quickly:u $0)) (p_TYPE_w-1-i:u $1) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[eat.arg1(2:e , 0:x), eat.arg2(2:e , 3:m.bananas), i(0:s , 0:x), quickly(1:s , 2:e)]",
        cleanedPredicates.toString());

    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"When did Aldi originate?\",\"words\":[{\"word\":\"When\",\"lemma\":\"when\",\"pos\":\"ADV\",\"index\":1,\"head\":4,\"dep\":\"mark\"},{\"word\":\"did\",\"lemma\":\"did\",\"pos\":\"AUX\",\"index\":2,\"head\":4,\"dep\":\"aux\"},{\"word\":\"Aldi\",\"lemma\":\"aldi\",\"pos\":\"PROPN\",\"index\":3,\"head\":4,\"dep\":\"nsubj\"},{\"word\":\"originate\",\"lemma\":\"originate\",\"pos\":\"VERB\",\"dep\":\"root\",\"head\":0,\"index\":4},{\"word\":\"?\",\"lemma\":\"?\",\"pos\":\"PUNCT\",\"index\":5,\"head\":4,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-4-originate t-VERB (l-advmod w-1-when t-ADV:WH) (l-aux w-2-did t-AUX) (l-nsubj w-3-aldi t-PROPN) (l-punct w-5-? t-PUNCT))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-advmod (l-aux w-4-originate w-2-did) w-1-when) w-3-aldi) w-5-?)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-4-originate:u $0) (and:c (p_TYPE_w-1-when:u $2) (p_TARGET:u $2)) (p_EVENT.ENTITY_l-advmod:b $0 $2))) (p_TYPE_w-3-aldi:u $1) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[QUESTION(0:x), originate.advmod(3:e , 0:x), originate.arg1(3:e , 2:m.aldi), when(0:s , 0:x)]",
        cleanedPredicates.toString());
  }

  @Test
  public final void testAmod() {
    // Two different types of amod.
    JsonObject jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"He saw a fast provoking horse .\",\"words\":[{\"word\":\"He\",\"lemma\":\"he\",\"pos\":\"PRON\",\"ner\":\"O\",\"index\":1,\"head\":2,\"dep\":\"nsubj\"},{\"word\":\"saw\",\"lemma\":\"see\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":2},{\"word\":\"a\",\"lemma\":\"a\",\"pos\":\"DET\",\"ner\":\"O\",\"index\":3,\"head\":6,\"dep\":\"det\"},{\"word\":\"fast\",\"lemma\":\"fast\",\"pos\":\"ADJ\",\"ner\":\"O\",\"index\":4,\"head\":6,\"dep\":\"amod\"},{\"word\":\"provoking\",\"lemma\":\"provoke\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":5,\"head\":6,\"dep\":\"amod\"},{\"word\":\"horse\",\"lemma\":\"horse\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":6,\"head\":2,\"dep\":\"dobj\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":7,\"head\":2,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-2-see t-VERB (l-nsubj w-1-he t-PRON) (l-dobj w-6-horse t-NOUN (l-det w-3-a t-DET) (l-amod w-4-fast t-ADJ) (l-amod w-5-provoke t-VERB)) (l-punct w-7-. t-PUNCT))",
        sentence.getRootNode().toString());

    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-dobj w-2-see (l-det (l-amod (l-amod w-6-horse w-4-fast) w-5-provoke) w-3-a)) w-1-he) w-7-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-2-see:u $0) (and:c (exists:ex $3:<a,e> (and:c (and:c (and:c (p_TYPE_w-6-horse:u $2) (p_EVENT_w-6-horse:u $2) (p_EVENT.ENTITY_arg0:b $2 $2)) (p_TYPEMOD_w-4-fast:u $2)) (p_EVENT_w-5-provoke:u $3) (p_EVENT.ENTITY_l-amod:b $3 $2))) (p_EMPTY:u $2)) (p_EVENT.ENTITY_arg2:b $0 $2))) (p_TYPE_w-1-he:u $1) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    List<String> cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[fast(3:s , 5:x), he(0:s , 0:x), horse(5:s , 5:x), horse.arg0(5:e , 5:x), provoke.amod(4:e , 5:x), see.arg1(1:e , 0:x), see.arg2(1:e , 5:x)]",
        cleanedPredicates.toString());
  }

  @Test
  public final void testCount() {
    JsonObject jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"what is the number of cities?\",\"words\":[{\"word\":\"what\",\"lemma\":\"what\",\"pos\":\"PRON\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":1},{\"word\":\"is\",\"lemma\":\"be\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":2,\"head\":1,\"dep\":\"cop\"},{\"word\":\"the\",\"lemma\":\"the\",\"pos\":\"DET\",\"ner\":\"O\",\"index\":3,\"head\":4,\"dep\":\"det\"},{\"word\":\"number\",\"lemma\":\"number\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":4,\"head\":1,\"dep\":\"nsubj\"},{\"word\":\"of\",\"lemma\":\"of\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":5,\"head\":6,\"dep\":\"case\"},{\"word\":\"cities\",\"lemma\":\"cities\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":6,\"head\":4,\"dep\":\"nmod\"},{\"word\":\"?\",\"lemma\":\"?\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":7,\"head\":1,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-1-what t-PRON:WH (l-cop w-2-be t-VERB) (l-nsubj w-4-number t-NOUN (l-det w-3-the t-DET) (l-nmod:count w-6-cities t-NOUN (l-case w-5-of t-ADP))) (l-punct w-7-? t-PUNCT))",
        sentence.getRootNode().toString());

    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-cop w-1-what w-2-be) (l-nmod:count (l-det w-4-number w-3-the) (l-case w-6-cities w-5-of))) w-7-?)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(2, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (and:c (and:c (p_TYPE_w-1-what:u $0) (p_EVENT_w-1-what:u $0) (p_EVENT.ENTITY_arg0:b $0 $0) (p_TARGET:u $0)) (exists:ex $1:<a,e> (and:c (and:c (and:c (p_TYPE_w-4-number:u $0) (p_EVENT_w-4-number:u $0) (p_EVENT.ENTITY_arg0:b $0 $0)) (p_EMPTY:u $0)) (and:c (p_TYPE_w-6-cities:u $1) (p_EVENT_w-6-cities:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (p_COUNT:b $1 $0)))))",
        sentenceSemantics.second().get(1).toString());
    List<String> cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(1), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[COUNT(5:x , 0:x), QUESTION(0:x), cities(5:s , 5:x), cities.arg0(5:e , 5:x), number(3:s , 0:x), what(0:s , 0:x), what.arg0(0:e , 0:x)]",
        cleanedPredicates.toString());
    
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"how many people live in US?\",\"words\":[{\"word\":\"how\",\"lemma\":\"how\",\"pos\":\"ADV\",\"ner\":\"O\",\"index\":1,\"head\":2,\"dep\":\"advmod\"},{\"word\":\"many\",\"lemma\":\"many\",\"pos\":\"ADJ\",\"ner\":\"O\",\"index\":2,\"head\":3,\"dep\":\"amod\"},{\"word\":\"people\",\"lemma\":\"people\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":3,\"head\":4,\"dep\":\"nsubj\"},{\"word\":\"live\",\"lemma\":\"live\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":4},{\"word\":\"in\",\"lemma\":\"in\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":5,\"head\":6,\"dep\":\"case\"},{\"word\":\"US\",\"lemma\":\"us\",\"pos\":\"PROPN\",\"ner\":\"LOCATION\",\"index\":6,\"head\":4,\"dep\":\"nmod\"},{\"word\":\"?\",\"lemma\":\"?\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":7,\"head\":4,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);
    
    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-4-live t-VERB (l-nsubj w-3-people t-NOUN (l-amod w-2-many t-ADJ (l-advmod:count w-1-how t-ADV:WH))) (l-nmod w-6-us t-PROPN (l-case w-5-in t-ADP)) (l-punct w-7-? t-PUNCT))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-nmod w-4-live (l-case w-6-us w-5-in)) (l-amod w-3-people (l-advmod:count w-2-many w-1-how))) w-7-?)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);
    
    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-4-live:u $0) (and:c (p_TYPE_w-6-us:u $2) (p_EVENT_w-6-us:u $2) (p_EVENT.ENTITY_arg0:b $2 $2)) (p_EVENT.ENTITY_l-nmod.w-5-in:b $0 $2))) (and:c (and:c (p_TYPE_w-3-people:u $1) (p_EVENT_w-3-people:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (exists:ex $3:<a,e> (and:c (p_TYPEMOD_w-2-many:u $1) (and:c (p_TYPE_w-1-how:u $3) (p_TARGET:u $3)) (p_COUNT:b $1 $3)))) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[COUNT(2:x , 0:x), QUESTION(0:x), arg0(5:e , 5:m.us), how(0:s , 0:x), live.arg1(3:e , 2:x), live.nmod.in(3:e , 5:m.us), many(1:s , 2:x), people(2:s , 2:x), people.arg0(2:e , 2:x)]",
        cleanedPredicates.toString());
    
    jsonSentence =
        jsonParser
            .parse(
                "{\"words\": [{\"index\": 1, \"head\": 2, \"word\": \"Cu\\u00e1ntos\", \"dep\": \"det\", \"pos\": \"DET\", \"lemma\": \"cu\\u00e1ntos\", \"ner\": \"O\"}, {\"index\": 2, \"head\": 3, \"word\": \"personas\", \"dep\": \"nsubj\", \"pos\": \"NOUN\", \"lemma\": \"personas\", \"ner\": \"O\"}, {\"index\": 3, \"head\": 0, \"word\": \"viven\", \"dep\": \"root\", \"pos\": \"VERB\", \"lemma\": \"viven\", \"ner\": \"O\"}, {\"index\": 4, \"head\": 5, \"word\": \"en\", \"dep\": \"case\", \"pos\": \"ADP\", \"lemma\": \"en\", \"ner\": \"O\"}, {\"index\": 5, \"head\": 3, \"word\": \"Italia\", \"dep\": \"nmod\", \"pos\": \"PROPN\", \"lemma\": \"italia\", \"ner\": \"LUG\"}, {\"index\": 6, \"head\": 3, \"word\": \"?\", \"dep\": \"punct\", \"pos\": \"PUNCT\", \"sentEnd\": true, \"lemma\": \"?\", \"ner\": \"O\"}], \"sentence\": \"Cu\\u00e1ntas personas viven en Italia?\"}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);
    
    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-3-viven t-VERB (l-nsubj w-2-personas t-NOUN (l-det w-1-cuántos t-DET:COUNT:WH)) (l-nmod w-5-italia t-PROPN (l-case w-4-en t-ADP)) (l-punct w-6-? t-PUNCT))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-nmod w-3-viven (l-case w-5-italia w-4-en)) (l-det w-2-personas w-1-cuántos)) w-6-?)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);
    
    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-3-viven:u $0) (and:c (p_TYPE_w-5-italia:u $2) (p_EVENT_w-5-italia:u $2) (p_EVENT.ENTITY_arg0:b $2 $2)) (p_EVENT.ENTITY_l-nmod.w-4-en:b $0 $2))) (exists:ex $3:<a,e> (and:c (and:c (p_TYPE_w-2-personas:u $1) (p_EVENT_w-2-personas:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (and:c (p_TYPEMOD_w-1-cuántos:u $3) (p_TARGET:u $3)) (p_COUNT:b $1 $3) (p_TYPE_w-1-cuántos:u $3))) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[COUNT(1:x , 0:x), QUESTION(0:x), arg0(4:e , 4:m.italia), cuántos(0:s , 0:x), personas(1:s , 1:x), personas.arg0(1:e , 1:x), viven.arg1(2:e , 1:x), viven.nmod.en(2:e , 4:m.italia)]",
        cleanedPredicates.toString());
  }
  
  @Test
  public final void testCompound() {
    JsonObject jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"I bought Hilton coffee table.\",\"words\":[{\"word\":\"I\",\"lemma\":\"I\",\"pos\":\"PRON\",\"ner\":\"O\",\"index\":1,\"head\":2,\"dep\":\"nsubj\"},{\"word\":\"bought\",\"lemma\":\"buy\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":2},{\"word\":\"Hilton\",\"lemma\":\"hilton\",\"pos\":\"PROPN\",\"ner\":\"O\",\"index\":3,\"head\":5,\"dep\":\"compound\"},{\"word\":\"coffee\",\"lemma\":\"coffee\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":4,\"head\":5,\"dep\":\"compound\"},{\"word\":\"table\",\"lemma\":\"table\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":5,\"head\":2,\"dep\":\"dobj\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":6,\"head\":2,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-2-buy t-VERB (l-nsubj w-1-i t-PRON) (l-dobj w-5-table t-NOUN (l-compound w-3-hilton t-PROPN) (l-compound w-4-coffee t-NOUN)) (l-punct w-6-. t-PUNCT))",
        sentence.getRootNode().toString());

    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-dobj w-2-buy (l-compound (l-compound w-5-table w-3-hilton) w-4-coffee)) w-1-i) w-6-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-2-buy:u $0) (and:c (exists:ex $3:<a,e> (and:c (and:c (p_TYPE_w-5-table:u $2) (p_EVENT_w-5-table:u $2) (p_EVENT.ENTITY_arg0:b $2 $2)) (p_TYPE_w-3-hilton:u $3) (p_EVENT.ENTITY_l-compound:b $2 $3))) (p_TYPEMOD_w-4-coffee.w-5-table:u $2)) (p_EVENT.ENTITY_arg2:b $0 $2))) (p_TYPE_w-1-i:u $1) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    List<String> cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[buy.arg1(1:e , 0:x), buy.arg2(1:e , 4:x), coffee.table(3:s , 4:x), i(0:s , 0:x), table(4:s , 4:x), table.arg0(4:e , 4:x), table.compound(4:e , 2:m.hilton)]",
        cleanedPredicates.toString());

    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"I like a boy Harry\",\"words\":[{\"word\":\"I\",\"lemma\":\"I\",\"pos\":\"PRON\",\"ner\":\"O\",\"index\":1,\"head\":2,\"dep\":\"nsubj\"},{\"word\":\"like\",\"lemma\":\"like\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":2},{\"word\":\"a\",\"lemma\":\"a\",\"pos\":\"DET\",\"ner\":\"O\",\"index\":3,\"head\":5,\"dep\":\"det\"},{\"word\":\"boy\",\"lemma\":\"boy\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":4,\"head\":5,\"dep\":\"compound\"},{\"word\":\"Harry\",\"lemma\":\"harry\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":5,\"head\":2,\"dep\":\"dobj\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-2-like t-VERB (l-nsubj w-1-i t-PRON) (l-dobj w-5-harry t-PROPN (l-det w-3-a t-DET) (l-compound w-4-boy t-NOUN)))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-nsubj (l-dobj w-2-like (l-det (l-compound w-5-harry w-4-boy) w-3-a)) w-1-i)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-2-like:u $0) (and:c (and:c (and:c (p_TYPE_w-5-harry:u $2) (p_EVENT_w-5-harry:u $2) (p_EVENT.ENTITY_arg0:b $2 $2)) (p_TYPEMOD_w-4-boy:u $2)) (p_EMPTY:u $2)) (p_EVENT.ENTITY_arg2:b $0 $2))) (p_TYPE_w-1-i:u $1) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[arg0(4:e , 4:m.harry), boy(3:s , 4:m.harry), i(0:s , 0:x), like.arg1(1:e , 0:x), like.arg2(1:e , 4:m.harry)]",
        cleanedPredicates.toString());
  }

  @Test
  public final void testAppos() {
    JsonObject jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"Reverend John, husband of May, won lottery.\",\"words\":[{\"word\":\"Reverend\",\"lemma\":\"reverend\",\"pos\":\"ADJ\",\"ner\":\"O\",\"index\":1,\"head\":2,\"dep\":\"amod\"},{\"word\":\"John\",\"lemma\":\"john\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":2,\"head\":8,\"dep\":\"nsubj\"},{\"word\":\",\",\"lemma\":\",\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":3,\"head\":2,\"dep\":\"punct\"},{\"word\":\"husband\",\"lemma\":\"husband\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":4,\"head\":2,\"dep\":\"appos\"},{\"word\":\"of\",\"lemma\":\"of\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":5,\"head\":6,\"dep\":\"case\"},{\"word\":\"May\",\"lemma\":\"may\",\"pos\":\"PROPN\",\"ner\":\"DATE\",\"index\":6,\"head\":4,\"dep\":\"nmod\"},{\"word\":\",\",\"lemma\":\",\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":7,\"head\":8,\"dep\":\"punct\"},{\"word\":\"won\",\"lemma\":\"win\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":8},{\"word\":\"lottery\",\"lemma\":\"lottery\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":9,\"head\":8,\"dep\":\"dobj\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":10,\"head\":8,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-8-win t-VERB (l-nsubj w-2-john t-PROPN (l-amod w-1-reverend t-ADJ) (l-punct w-3-, t-PUNCT) (l-appos w-4-husband t-NOUN (l-nmod w-6-may t-PROPN (l-case w-5-of t-ADP)))) (l-punct w-7-, t-PUNCT) (l-dobj w-9-lottery t-NOUN) (l-punct w-10-. t-PUNCT))",
        sentence.getRootNode().toString());

    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-punct (l-nsubj (l-dobj w-8-win w-9-lottery) (l-punct (l-appos (l-amod w-2-john w-1-reverend) (l-nmod w-4-husband (l-case w-6-may w-5-of))) w-3-,)) w-7-,) w-10-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-8-win:u $0) (p_TYPE_w-9-lottery:u $2) (p_EVENT.ENTITY_arg2:b $0 $2))) (and:c (and:c (and:c (p_TYPE_w-2-john:u $1) (p_EVENT_w-2-john:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (p_TYPEMOD_w-1-reverend:u $1)) (exists:ex $3:<a,e> (and:c (and:c (p_TYPE_w-4-husband:u $1) (p_EVENT_w-4-husband:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (and:c (p_TYPE_w-6-may:u $3) (p_EVENT_w-6-may:u $3) (p_EVENT.ENTITY_arg0:b $3 $3)) (p_EVENT.ENTITY_l-nmod.w-5-of:b $1 $3)))) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    List<String> cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[arg0(5:e , 5:m.may), husband(3:s , 1:m.john), husband.arg0(3:e , 1:m.john), husband.nmod.of(3:e , 5:m.may), lottery(8:s , 8:x), reverend(0:s , 1:m.john), win.arg1(7:e , 1:m.john), win.arg2(7:e , 8:x)]",
        cleanedPredicates.toString());
  }

  @Test
  public final void testConj() {
    // NP conjunction.
    JsonObject jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"Bill and Dave founded HP.\",\"words\":[{\"word\":\"Bill\",\"lemma\":\"bill\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":1,\"head\":4,\"dep\":\"nsubj\"},{\"word\":\"and\",\"lemma\":\"and\",\"pos\":\"CONJ\",\"ner\":\"O\",\"index\":2,\"head\":1,\"dep\":\"cc\"},{\"word\":\"Dave\",\"lemma\":\"dave\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":3,\"head\":1,\"dep\":\"conj\"},{\"word\":\"founded\",\"lemma\":\"found\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":4},{\"word\":\"HP\",\"lemma\":\"hp\",\"pos\":\"PROPN\",\"ner\":\"O\",\"index\":5,\"head\":4,\"dep\":\"dobj\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":6,\"head\":4,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-4-found t-VERB (l-nsubj w-1-bill t-PROPN (l-cc w-2-and t-CONJ) (l-conj-np w-3-dave t-PROPN)) (l-dobj w-5-hp t-PROPN) (l-punct w-6-. t-PUNCT))",
        sentence.getRootNode().toString());

    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-dobj w-4-found w-5-hp) (l-conj-np (l-cc w-1-bill w-2-and) w-3-dave)) w-6-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-4-found:u $0) (p_TYPE_w-5-hp:u $2) (p_EVENT.ENTITY_arg2:b $0 $2))) (exists:ex $3:<a,e> (exists:ex $4:<a,e> (and:c (exists:ex $5:<a,e> (and:c (and:c (p_TYPE_w-1-bill:u $3) (p_EVENT_w-1-bill:u $3) (p_EVENT.ENTITY_arg0:b $3 $3)) (p_EMPTY:u $5))) (p_TYPE_w-3-dave:u $4) (p_CONJ:tri $1 $3 $4)))) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    List<String> cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[arg0(0:e , 0:m.bill), found.arg1(3:e , 0:m.bill), found.arg1(3:e , 2:m.dave), found.arg2(3:e , 4:m.hp)]",
        cleanedPredicates.toString());

    // Adj conjunction.
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"quick and fast reply\",\"words\":[{\"word\":\"quick\",\"lemma\":\"quick\",\"pos\":\"ADJ\",\"ner\":\"O\",\"index\":1,\"head\":4,\"dep\":\"amod\"},{\"word\":\"and\",\"lemma\":\"and\",\"pos\":\"CONJ\",\"ner\":\"O\",\"index\":2,\"head\":1,\"dep\":\"cc\"},{\"word\":\"fast\",\"lemma\":\"fast\",\"pos\":\"ADJ\",\"ner\":\"O\",\"index\":3,\"head\":1,\"dep\":\"conj\"},{\"word\":\"reply\",\"lemma\":\"reply\",\"pos\":\"NOUN\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":4,\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-4-reply t-NOUN (l-amod w-1-quick t-ADJ (l-cc w-2-and t-CONJ) (l-conj-adj w-3-fast t-ADJ)))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-amod w-4-reply (l-conj-adj (l-cc w-1-quick w-2-and) w-3-fast))",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (and:c (and:c (p_TYPE_w-4-reply:u $0) (p_EVENT_w-4-reply:u $0) (p_EVENT.ENTITY_arg0:b $0 $0)) (and:c (exists:ex $1:<a,e> (and:c (p_TYPEMOD_w-1-quick:u $0) (p_EMPTY:u $1))) (p_TYPEMOD_w-3-fast:u $0))))",
        sentenceSemantics.second().get(0).toString());
    cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[fast(2:s , 3:x), quick(0:s , 3:x), reply(3:s , 3:x), reply.arg0(3:e , 3:x)]",
        cleanedPredicates.toString());


    // VP conjunction.
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"Cameron directed Titanic and produced Avatar.\",\"words\":[{\"word\":\"Cameron\",\"lemma\":\"cameron\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":1,\"head\":2,\"dep\":\"nsubj\"},{\"word\":\"directed\",\"lemma\":\"direct\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":2},{\"word\":\"Titanic\",\"lemma\":\"titanic\",\"pos\":\"PROPN\",\"ner\":\"O\",\"index\":3,\"head\":2,\"dep\":\"dobj\"},{\"word\":\"and\",\"lemma\":\"and\",\"pos\":\"CONJ\",\"ner\":\"O\",\"index\":4,\"head\":2,\"dep\":\"cc\"},{\"word\":\"produced\",\"lemma\":\"produce\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":5,\"head\":2,\"dep\":\"conj\"},{\"word\":\"Avatar\",\"lemma\":\"avatar\",\"pos\":\"PROPN\",\"ner\":\"O\",\"index\":6,\"head\":5,\"dep\":\"dobj\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":7,\"head\":2,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-2-direct t-VERB (l-nsubj w-1-cameron t-PROPN) (l-dobj w-3-titanic t-PROPN) (l-cc w-4-and t-CONJ) (l-conj-vp w-5-produce t-VERB (l-dobj w-6-avatar t-PROPN)) (l-punct w-7-. t-PUNCT))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-conj-vp (l-dobj (l-cc w-2-direct w-4-and) w-3-titanic) (l-dobj w-5-produce w-6-avatar)) w-1-cameron) w-7-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (exists:ex $3:<a,e> (and:c (exists:ex $4:<a,e> (and:c (exists:ex $5:<a,e> (and:c (p_EVENT_w-2-direct:u $2) (p_EMPTY:u $5))) (p_TYPE_w-3-titanic:u $4) (p_EVENT.ENTITY_arg2:b $2 $4))) (exists:ex $6:<a,e> (and:c (p_EVENT_w-5-produce:u $3) (p_TYPE_w-6-avatar:u $6) (p_EVENT.ENTITY_arg2:b $3 $6))) (p_CONJ:tri $0 $2 $3)))) (p_TYPE_w-1-cameron:u $1) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[direct.arg1(1:e , 0:m.cameron), direct.arg2(1:e , 2:m.titanic), produce.arg1(4:e , 0:m.cameron), produce.arg2(4:e , 5:m.avatar)]",
        cleanedPredicates.toString());

    // VP conjunction tricky.
    // issue: https://github.com/sivareddyg/UDepLambda/issues/10
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"John ate apple and suffered.\",\"words\":[{\"word\":\"John\",\"lemma\":\"John\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":1,\"head\":2,\"dep\":\"nsubj\"},{\"word\":\"ate\",\"lemma\":\"eat\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":2},{\"word\":\"apple\",\"lemma\":\"apple\",\"pos\":\"PROPN\",\"ner\":\"O\",\"index\":3,\"head\":2,\"dep\":\"dobj\"},{\"word\":\"and\",\"lemma\":\"and\",\"pos\":\"CONJ\",\"ner\":\"O\",\"index\":4,\"head\":2,\"dep\":\"cc\"},{\"word\":\"suffered\",\"lemma\":\"suffer\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":5,\"head\":2,\"dep\":\"conj\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":6,\"head\":2,\"dep\":\"punct\",\"sentEnd\":true}],\"entities\":[{\"phrase\":\"John\",\"start\":0,\"end\":0,\"index\":0}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-2-eat t-VERB (l-nsubj w-1-john t-PROPN) (l-dobj w-3-apple t-PROPN) (l-cc w-4-and t-CONJ) (l-conj-vp w-5-suffer t-VERB) (l-punct w-6-. t-PUNCT))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-conj-vp (l-dobj (l-cc w-2-eat w-4-and) w-3-apple) w-5-suffer) w-1-john) w-6-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (exists:ex $3:<a,e> (and:c (exists:ex $4:<a,e> (and:c (exists:ex $5:<a,e> (and:c (p_EVENT_w-2-eat:u $2) (p_EMPTY:u $5))) (p_TYPE_w-3-apple:u $4) (p_EVENT.ENTITY_arg2:b $2 $4))) (p_EVENT_w-5-suffer:u $3) (p_CONJ:tri $0 $2 $3)))) (p_TYPE_w-1-john:u $1) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[eat.arg1(1:e , 0:m.John), eat.arg2(1:e , 2:m.apple), suffer.arg1(4:e , 0:m.John)]",
        cleanedPredicates.toString());
    
    // Verb conjunction.
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"Cameron directed and produced Titanic.\",\"words\":[{\"word\":\"Cameron\",\"lemma\":\"cameron\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":1,\"head\":2,\"dep\":\"nsubj\"},{\"word\":\"directed\",\"lemma\":\"direct\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":2},{\"word\":\"and\",\"lemma\":\"and\",\"pos\":\"CONJ\",\"ner\":\"O\",\"index\":3,\"head\":2,\"dep\":\"cc\"},{\"word\":\"produced\",\"lemma\":\"produce\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":4,\"head\":2,\"dep\":\"conj\"},{\"word\":\"Titanic\",\"lemma\":\"titanic\",\"pos\":\"PROPN\",\"ner\":\"O\",\"index\":5,\"head\":2,\"dep\":\"dobj\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":6,\"head\":2,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-2-direct t-VERB (l-nsubj w-1-cameron t-PROPN) (l-cc w-3-and t-CONJ) (l-conj-verb w-4-produce t-VERB) (l-dobj w-5-titanic t-PROPN) (l-punct w-6-. t-PUNCT))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-dobj (l-cc (l-conj-verb w-2-direct w-4-produce) w-3-and) w-5-titanic) w-1-cameron) w-6-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (exists:ex $3:<a,e> (and:c (exists:ex $4:<a,e> (exists:ex $5:<a,e> (and:c (p_EVENT_w-2-direct:u $4) (p_EVENT_w-4-produce:u $5) (p_CONJ:tri $0 $4 $5)))) (p_EMPTY:u $3))) (p_TYPE_w-5-titanic:u $2) (p_EVENT.ENTITY_arg2:b $0 $2))) (p_TYPE_w-1-cameron:u $1) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[direct.arg1(1:e , 0:m.cameron), direct.arg2(1:e , 4:m.titanic), produce.arg1(3:e , 0:m.cameron), produce.arg2(3:e , 4:m.titanic)]",
        cleanedPredicates.toString());

    // sentence conjunction.
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"Cameron ate sandwich; and Speilberg drank coke.\",\"words\":[{\"word\":\"Cameron\",\"lemma\":\"cameron\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":1,\"head\":2,\"dep\":\"nsubj\"},{\"word\":\"ate\",\"lemma\":\"eat\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":2},{\"word\":\"sandwich\",\"lemma\":\"sandwich\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":3,\"head\":2,\"dep\":\"dobj\"},{\"word\":\";\",\"lemma\":\";\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":4,\"head\":2,\"dep\":\"punct\"},{\"word\":\"and\",\"lemma\":\"and\",\"pos\":\"CONJ\",\"ner\":\"O\",\"index\":5,\"head\":2,\"dep\":\"cc\"},{\"word\":\"Speilberg\",\"lemma\":\"speilberg\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":6,\"head\":7,\"dep\":\"nsubj\"},{\"word\":\"drank\",\"lemma\":\"drink\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":7,\"head\":2,\"dep\":\"conj\"},{\"word\":\"coke\",\"lemma\":\"coke\",\"pos\":\"PROPN\",\"ner\":\"O\",\"index\":8,\"head\":7,\"dep\":\"dobj\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":9,\"head\":2,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-2-eat t-VERB (l-nsubj w-1-cameron t-PROPN) (l-dobj w-3-sandwich t-NOUN) (l-punct w-4-; t-PUNCT) (l-cc w-5-and t-CONJ) (l-conj-sent w-7-drink t-VERB (l-nsubj w-6-speilberg t-PROPN) (l-dobj w-8-coke t-PROPN)) (l-punct w-9-. t-PUNCT))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-punct (l-conj-sent (l-nsubj (l-dobj (l-cc w-2-eat w-5-and) w-3-sandwich) w-1-cameron) (l-nsubj (l-dobj w-7-drink w-8-coke) w-6-speilberg)) w-4-;) w-9-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (exists:ex $2:<a,e> (and:c (exists:ex $3:<a,e> (and:c (exists:ex $4:<a,e> (and:c (exists:ex $5:<a,e> (and:c (p_EVENT_w-2-eat:u $1) (p_EMPTY:u $5))) (p_TYPE_w-3-sandwich:u $4) (p_EVENT.ENTITY_arg2:b $1 $4))) (p_TYPE_w-1-cameron:u $3) (p_EVENT.ENTITY_arg1:b $1 $3))) (exists:ex $6:<a,e> (and:c (exists:ex $7:<a,e> (and:c (p_EVENT_w-7-drink:u $2) (p_TYPE_w-8-coke:u $7) (p_EVENT.ENTITY_arg2:b $2 $7))) (p_TYPE_w-6-speilberg:u $6) (p_EVENT.ENTITY_arg1:b $2 $6))) (p_CONJ:tri $0 $1 $2)))))",
        sentenceSemantics.second().get(0).toString());
    cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[drink.arg1(6:e , 5:m.speilberg), drink.arg2(6:e , 7:m.coke), eat.arg1(1:e , 0:m.cameron), eat.arg2(1:e , 2:x), sandwich(2:s , 2:x)]",
        cleanedPredicates.toString());
  }

  @Test
  public final void testRelatives() {
    // pobj extraction
    JsonObject jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"country which Darwin was born at\",\"words\":[{\"word\":\"country\",\"lemma\":\"country\",\"pos\":\"NOUN\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":1},{\"word\":\"which\",\"lemma\":\"which\",\"pos\":\"DET\",\"ner\":\"O\",\"index\":2,\"head\":5,\"dep\":\"dobj\"},{\"word\":\"Darwin\",\"lemma\":\"darwin\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":3,\"head\":5,\"dep\":\"nsubjpass\"},{\"word\":\"was\",\"lemma\":\"was\",\"pos\":\"AUX\",\"ner\":\"O\",\"index\":4,\"head\":5,\"dep\":\"auxpass\"},{\"word\":\"born\",\"lemma\":\"bear\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":5,\"head\":1,\"dep\":\"acl\"},{\"word\":\"at\",\"lemma\":\"at\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":6,\"head\":5,\"dep\":\"nmod\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-1-country t-NOUN (l-acl w-5-bear t-VERB (l-dobj w-2-which t-DET) (l-nsubjpass w-3-darwin t-PROPN) (l-auxpass w-4-was t-AUX) (l-nmod w-6-at t-ADP) (l-nmod v-w-1-country)) (l-BIND v-w-1-country))",
        sentence.getRootNode().toString());

    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-acl (l-BIND w-1-country v-w-1-country) (l-nsubjpass (l-nmod (l-nmod (l-auxpass (l-dobj w-5-bear w-2-which) w-4-was) w-6-at) v-w-1-country) w-3-darwin))",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (and:c (and:c (p_TYPE_w-1-country:u $0) (p_EVENT_w-1-country:u $0) (p_EVENT.ENTITY_arg0:b $0 $0)) (p_EQUAL:b $0 v-w-1-country:v)) (exists:ex $2:<a,e> (and:c (exists:ex $3:<a,e> (and:c (exists:ex $4:<a,e> (and:c (exists:ex $5:<a,e> (and:c (p_EVENT_w-5-bear:u $1) (p_EMPTY:u $5) (p_EVENT.ENTITY_arg2:b $1 $5))) (p_EMPTY:u $4) (p_EVENT.ENTITY_l-nmod:b $1 $4))) (p_EQUAL:b $3 v-w-1-country:v) (p_EVENT.ENTITY_l-nmod:b $1 $3))) (p_TYPE_w-3-darwin:u $2) (p_EVENT.ENTITY_arg2:b $1 $2))))))",
        sentenceSemantics.second().get(0).toString());
    List<String> cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[bear.arg2(4:e , 2:m.darwin), bear.nmod(4:e , 0:x), country(0:s , 0:x), country.arg0(0:e , 0:x)]",
        cleanedPredicates.toString());

    // subj extraction
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"company which bought Pixar\",\"words\":[{\"word\":\"company\",\"lemma\":\"company\",\"pos\":\"NOUN\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":1},{\"word\":\"which\",\"lemma\":\"which\",\"pos\":\"DET\",\"ner\":\"O\",\"index\":2,\"head\":3,\"dep\":\"nsubj\"},{\"word\":\"bought\",\"lemma\":\"buy\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":3,\"head\":1,\"dep\":\"acl:relcl\"},{\"word\":\"Pixar\",\"lemma\":\"pixar\",\"pos\":\"PROPN\",\"ner\":\"ORGANIZATION\",\"index\":4,\"head\":3,\"dep\":\"dobj\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-1-company t-NOUN (l-acl:relcl w-3-buy t-VERB (l-nsubj w-2-which t-DET) (l-dobj w-4-pixar t-PROPN) (l-nsubj v-w-1-company)) (l-BIND v-w-1-company))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-acl:relcl (l-BIND w-1-company v-w-1-company) (l-nsubj (l-nsubj (l-dobj w-3-buy w-4-pixar) w-2-which) v-w-1-company))",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (and:c (and:c (p_TYPE_w-1-company:u $0) (p_EVENT_w-1-company:u $0) (p_EVENT.ENTITY_arg0:b $0 $0)) (p_EQUAL:b $0 v-w-1-company:v)) (exists:ex $2:<a,e> (and:c (exists:ex $3:<a,e> (and:c (exists:ex $4:<a,e> (and:c (p_EVENT_w-3-buy:u $1) (p_TYPE_w-4-pixar:u $4) (p_EVENT.ENTITY_arg2:b $1 $4))) (p_EMPTY:u $3) (p_EVENT.ENTITY_arg1:b $1 $3))) (p_EQUAL:b $2 v-w-1-company:v) (p_EVENT.ENTITY_arg1:b $1 $2))))))",
        sentenceSemantics.second().get(0).toString());
    cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[buy.arg1(2:e , 0:x), buy.arg2(2:e , 3:m.pixar), company(0:s , 0:x), company.arg0(0:e , 0:x)]",
        cleanedPredicates.toString());

    // obj extraction
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"movie I saw\",\"words\":[{\"word\":\"movie\",\"lemma\":\"movie\",\"pos\":\"NOUN\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":1},{\"word\":\"I\",\"lemma\":\"I\",\"pos\":\"PRON\",\"ner\":\"O\",\"index\":2,\"head\":3,\"dep\":\"nsubj\"},{\"word\":\"saw\",\"lemma\":\"see\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":3,\"head\":1,\"dep\":\"acl:relcl\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-1-movie t-NOUN (l-acl:relcl w-3-see t-VERB (l-nsubj w-2-i t-PRON) (l-dobj v-w-1-movie)) (l-BIND v-w-1-movie))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-acl:relcl (l-BIND w-1-movie v-w-1-movie) (l-nsubj (l-dobj w-3-see v-w-1-movie) w-2-i))",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (and:c (and:c (p_TYPE_w-1-movie:u $0) (p_EVENT_w-1-movie:u $0) (p_EVENT.ENTITY_arg0:b $0 $0)) (p_EQUAL:b $0 v-w-1-movie:v)) (exists:ex $2:<a,e> (and:c (exists:ex $3:<a,e> (and:c (p_EVENT_w-3-see:u $1) (p_EQUAL:b $3 v-w-1-movie:v) (p_EVENT.ENTITY_arg2:b $1 $3))) (p_TYPE_w-2-i:u $2) (p_EVENT.ENTITY_arg1:b $1 $2))))))",
        sentenceSemantics.second().get(0).toString());
    cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[i(1:s , 1:x), movie(0:s , 0:x), movie.arg0(0:e , 0:x), see.arg1(2:e , 1:x), see.arg2(2:e , 0:x)]",
        cleanedPredicates.toString());

    // no extraction
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"the issues as he sees them\",\"words\":[{\"word\":\"the\",\"lemma\":\"the\",\"pos\":\"DET\",\"ner\":\"O\",\"index\":1,\"head\":2,\"dep\":\"det\"},{\"word\":\"issues\",\"lemma\":\"issue\",\"pos\":\"NOUN\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":2},{\"word\":\"as\",\"lemma\":\"as\",\"pos\":\"SCONJ\",\"ner\":\"O\",\"index\":3,\"head\":5,\"dep\":\"mark\"},{\"word\":\"he\",\"lemma\":\"he\",\"pos\":\"PRON\",\"ner\":\"O\",\"index\":4,\"head\":5,\"dep\":\"nsubj\"},{\"word\":\"sees\",\"lemma\":\"see\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":5,\"head\":2,\"dep\":\"acl\"},{\"word\":\"them\",\"lemma\":\"they\",\"pos\":\"PRON\",\"ner\":\"O\",\"index\":6,\"head\":5,\"dep\":\"dobj\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-2-issue t-NOUN (l-det w-1-the t-DET) (l-acl-other w-5-see t-VERB (l-mark w-3-as t-SCONJ) (l-nsubj w-4-he t-PRON) (l-dobj w-6-they t-PRON)))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-acl-other (l-det w-2-issue w-1-the) (l-mark (l-nsubj (l-dobj w-5-see w-6-they) w-4-he) w-3-as))",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (and:c (and:c (p_TYPE_w-2-issue:u $0) (p_EVENT_w-2-issue:u $0) (p_EVENT.ENTITY_arg0:b $0 $0)) (p_EMPTY:u $0)) (exists:ex $2:<a,e> (and:c (exists:ex $3:<a,e> (and:c (p_EVENT_w-5-see:u $1) (p_TYPE_w-6-they:u $3) (p_EVENT.ENTITY_arg2:b $1 $3))) (p_TYPE_w-4-he:u $2) (p_EVENT.ENTITY_arg1:b $1 $2))) (p_EVENT.ENTITY_l-acl-other:b $1 $0))))",
        sentenceSemantics.second().get(0).toString());
    cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[he(3:s , 3:x), issue(1:s , 1:x), issue.arg0(1:e , 1:x), see.acl-other(4:e , 1:x), see.arg1(4:e , 3:x), see.arg2(4:e , 5:x), they(5:s , 5:x)]",
        cleanedPredicates.toString());

    // Case to handle bad parse.
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"Where to hang out in Chicago?\",\"words\":[{\"word\":\"Where\",\"lemma\":\"where\",\"pos\":\"ADV\",\"dep\":\"root\",\"head\":0,\"index\":1},{\"word\":\"to\",\"lemma\":\"to\",\"pos\":\"PART\",\"index\":2,\"head\":3,\"dep\":\"mark\"},{\"word\":\"hang\",\"lemma\":\"hang\",\"pos\":\"VERB\",\"index\":3,\"head\":1,\"dep\":\"advcl\"},{\"word\":\"out\",\"lemma\":\"out\",\"pos\":\"ADP\",\"index\":4,\"head\":3,\"dep\":\"compound:prt\"},{\"word\":\"in\",\"lemma\":\"in\",\"pos\":\"ADP\",\"index\":5,\"head\":6,\"dep\":\"case\"},{\"word\":\"Chicago\",\"lemma\":\"chicago\",\"pos\":\"PROPN\",\"index\":6,\"head\":3,\"dep\":\"nmod\"},{\"word\":\"?\",\"lemma\":\"?\",\"pos\":\"PUNCT\",\"index\":7,\"head\":1,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-1-where t-ADV:WH (l-acl w-3-hang t-VERB (l-mark w-2-to t-PART) (l-compound:prt w-4-out t-ADP) (l-nmod w-6-chicago t-PROPN (l-case w-5-in t-ADP)) (l-nsubj v-w-1-where)) (l-punct w-7-? t-PUNCT) (l-BIND v-w-1-where))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-acl (l-BIND w-1-where v-w-1-where) (l-compound:prt (l-mark (l-nsubj (l-nmod w-3-hang (l-case w-6-chicago w-5-in)) v-w-1-where) w-2-to) w-4-out)) w-7-?)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (and:c (and:c (p_TYPE_w-1-where:u $0) (p_EVENT_w-1-where:u $0) (p_EVENT.ENTITY_arg0:b $0 $0) (p_TARGET:u $0)) (p_EQUAL:b $0 v-w-1-where:v)) (exists:ex $2:<a,e> (and:c (exists:ex $3:<a,e> (and:c (p_EVENT_w-3-hang:u $1) (and:c (p_TYPE_w-6-chicago:u $3) (p_EVENT_w-6-chicago:u $3) (p_EVENT.ENTITY_arg0:b $3 $3)) (p_EVENT.ENTITY_l-nmod.w-5-in:b $1 $3))) (p_EQUAL:b $2 v-w-1-where:v) (p_EVENT.ENTITY_arg1:b $1 $2))))))",
        sentenceSemantics.second().get(0).toString());
    cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[QUESTION(0:x), arg0(5:e , 5:m.chicago), hang.arg1(2:e , 0:x), hang.nmod.in(2:e , 5:m.chicago), where(0:s , 0:x), where.arg0(0:e , 0:x)]",
        cleanedPredicates.toString());

  }

  @Test
  public final void testQuestions() {
    // pobj extraction
    JsonObject jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"what city did Obama come from?\",\"words\":[{\"word\":\"what\",\"lemma\":\"what\",\"pos\":\"DET\",\"ner\":\"O\",\"index\":1,\"head\":2,\"dep\":\"det\"},{\"word\":\"city\",\"lemma\":\"city\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":2,\"head\":5,\"dep\":\"dobj\"},{\"word\":\"did\",\"lemma\":\"did\",\"pos\":\"AUX\",\"ner\":\"O\",\"index\":3,\"head\":5,\"dep\":\"aux\"},{\"word\":\"Obama\",\"lemma\":\"obama\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":4,\"head\":5,\"dep\":\"nsubj\"},{\"word\":\"come\",\"lemma\":\"come\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":5},{\"word\":\"from\",\"lemma\":\"from\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":6,\"head\":5,\"dep\":\"nmod\"},{\"word\":\"?\",\"lemma\":\"?\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":7,\"head\":5,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-5-come t-VERB (l-nmod w-2-city t-NOUN (l-det w-1-what t-DET:WH)) (l-aux w-3-did t-AUX) (l-nsubj w-4-obama t-PROPN) (l-nmod w-6-from t-ADP) (l-punct w-7-? t-PUNCT))",
        sentence.getRootNode().toString());

    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-nmod (l-nmod (l-aux w-5-come w-3-did) (l-det w-2-city w-1-what)) w-6-from) w-4-obama) w-7-?)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (exists:ex $3:<a,e> (and:c (p_EVENT_w-5-come:u $0) (and:c (and:c (p_TYPE_w-2-city:u $3) (p_EVENT_w-2-city:u $3) (p_EVENT.ENTITY_arg0:b $3 $3)) (and:c (p_TYPEMOD_w-1-what:u $3) (p_TARGET:u $3))) (p_EVENT.ENTITY_l-nmod:b $0 $3))) (p_EMPTY:u $2) (p_EVENT.ENTITY_l-nmod:b $0 $2))) (p_TYPE_w-4-obama:u $1) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    List<String> cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[QUESTION(1:x), city(1:s , 1:x), city.arg0(1:e , 1:x), come.arg1(4:e , 3:m.obama), come.nmod(4:e , 1:x), what(0:s , 1:x)]",
        cleanedPredicates.toString());

    // pobj extraction.
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"where did Obama come from?\",\"words\":[{\"word\":\"where\",\"lemma\":\"where\",\"pos\":\"ADV\",\"ner\":\"O\",\"index\":1,\"head\":4,\"dep\":\"advmod\"},{\"word\":\"did\",\"lemma\":\"did\",\"pos\":\"AUX\",\"ner\":\"O\",\"index\":2,\"head\":4,\"dep\":\"aux\"},{\"word\":\"Obama\",\"lemma\":\"obama\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":3,\"head\":4,\"dep\":\"nsubj\"},{\"word\":\"come\",\"lemma\":\"come\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":4},{\"word\":\"from\",\"lemma\":\"from\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":5,\"head\":4,\"dep\":\"nmod\"},{\"word\":\"?\",\"lemma\":\"?\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":6,\"head\":4,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-4-come t-VERB (l-nmod w-1-where t-ADV:WH) (l-aux w-2-did t-AUX) (l-nsubj w-3-obama t-PROPN) (l-nmod w-5-from t-ADP) (l-punct w-6-? t-PUNCT))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-nmod (l-nmod (l-aux w-4-come w-2-did) w-1-where) w-5-from) w-3-obama) w-6-?)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (exists:ex $3:<a,e> (and:c (p_EVENT_w-4-come:u $0) (and:c (p_TYPE_w-1-where:u $3) (p_TARGET:u $3)) (p_EVENT.ENTITY_l-nmod:b $0 $3))) (p_EMPTY:u $2) (p_EVENT.ENTITY_l-nmod:b $0 $2))) (p_TYPE_w-3-obama:u $1) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[QUESTION(0:x), come.arg1(3:e , 2:m.obama), come.nmod(3:e , 0:x), where(0:s , 0:x)]",
        cleanedPredicates.toString());

    // subj
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"Who directed Titanic?\",\"words\":[{\"word\":\"Who\",\"lemma\":\"who\",\"pos\":\"PRON\",\"ner\":\"O\",\"index\":1,\"head\":2,\"dep\":\"nsubj\"},{\"word\":\"directed\",\"lemma\":\"direct\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":2},{\"word\":\"Titanic\",\"lemma\":\"titanic\",\"pos\":\"PROPN\",\"ner\":\"O\",\"index\":3,\"head\":2,\"dep\":\"dobj\"},{\"word\":\"?\",\"lemma\":\"?\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":4,\"head\":2,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-2-direct t-VERB (l-nsubj w-1-who t-PRON:WH) (l-dobj w-3-titanic t-PROPN) (l-punct w-4-? t-PUNCT))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-dobj w-2-direct w-3-titanic) w-1-who) w-4-?)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-2-direct:u $0) (p_TYPE_w-3-titanic:u $2) (p_EVENT.ENTITY_arg2:b $0 $2))) (and:c (p_TYPE_w-1-who:u $1) (p_TARGET:u $1)) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[QUESTION(0:x), direct.arg1(1:e , 0:x), direct.arg2(1:e , 2:m.titanic), who(0:s , 0:x)]",
        cleanedPredicates.toString());

    // obj (the sentence is made ungrammatical to produce correct parse)
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"Who  Jim married?\",\"words\":[{\"word\":\"Who\",\"lemma\":\"who\",\"pos\":\"PRON\",\"ner\":\"O\",\"index\":1,\"head\":3,\"dep\":\"dobj\"},{\"word\":\"Jim\",\"lemma\":\"jim\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":2,\"head\":3,\"dep\":\"nsubj\"},{\"word\":\"married\",\"lemma\":\"marry\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":3},{\"word\":\"?\",\"lemma\":\"?\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":4,\"head\":3,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-3-marry t-VERB (l-dobj w-1-who t-PRON:WH) (l-nsubj w-2-jim t-PROPN) (l-punct w-4-? t-PUNCT))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-dobj w-3-marry w-1-who) w-2-jim) w-4-?)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-3-marry:u $0) (and:c (p_TYPE_w-1-who:u $2) (p_TARGET:u $2)) (p_EVENT.ENTITY_arg2:b $0 $2))) (p_TYPE_w-2-jim:u $1) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[QUESTION(0:x), marry.arg1(2:e , 1:m.jim), marry.arg2(2:e , 0:x), who(0:s , 0:x)]",
        cleanedPredicates.toString());

    // Special cases when adverbs act as verbs.
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"Is he in?\",\"words\":[{\"word\":\"Is\",\"lemma\":\"be\",\"pos\":\"VERB\",\"index\":1,\"head\":3,\"dep\":\"cop\"},{\"word\":\"he\",\"lemma\":\"he\",\"pos\":\"PRON\",\"index\":2,\"head\":3,\"dep\":\"nsubj\"},{\"word\":\"in\",\"lemma\":\"in\",\"pos\":\"ADV\",\"dep\":\"root\",\"head\":0,\"index\":3},{\"word\":\"?\",\"lemma\":\"?\",\"pos\":\"PUNCT\",\"index\":4,\"head\":3,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-3-in t-ADV (l-cop w-1-be t-VERB) (l-nsubj w-2-he t-PRON) (l-punct w-4-? t-PUNCT))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals("(l-punct (l-nsubj (l-cop w-3-in w-1-be) w-2-he) w-4-?)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (p_EVENT_w-3-in:u $0) (p_TYPE_w-2-he:u $1) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals("[he(1:s , 1:x), in.arg1(2:e , 1:x)]",
        cleanedPredicates.toString());
    
    // dobj acting as a question word
    jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"give me the capital of UK?\",\"words\":[{\"word\":\"give\",\"lemma\":\"give\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":1},{\"word\":\"me\",\"lemma\":\"I\",\"pos\":\"PRON\",\"ner\":\"O\",\"index\":2,\"head\":1,\"dep\":\"iobj\"},{\"word\":\"the\",\"lemma\":\"the\",\"pos\":\"DET\",\"ner\":\"O\",\"index\":3,\"head\":4,\"dep\":\"det\"},{\"word\":\"capital\",\"lemma\":\"capital\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":4,\"head\":1,\"dep\":\"dobj\"},{\"word\":\"of\",\"lemma\":\"of\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":5,\"head\":6,\"dep\":\"case\"},{\"word\":\"UK\",\"lemma\":\"uk\",\"pos\":\"PROPN\",\"ner\":\"LOCATION\",\"index\":6,\"head\":4,\"dep\":\"nmod\"},{\"word\":\"?\",\"lemma\":\"?\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":7,\"head\":1,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-1-give t-VERB (l-iobj w-2-i t-PRON) (l-dobj:wh w-4-capital t-NOUN (l-det w-3-the t-DET) (l-nmod w-6-uk t-PROPN (l-case w-5-of t-ADP))) (l-punct w-7-? t-PUNCT))",
        sentence.getRootNode().toString());

    binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals("(l-iobj (l-punct (l-dobj:wh w-1-give (l-nmod (l-det w-4-capital w-3-the) (l-case w-6-uk w-5-of))) w-7-?) w-2-i)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-1-give:u $0) (exists:ex $3:<a,e> (and:c (and:c (and:c (p_TYPE_w-4-capital:u $2) (p_EVENT_w-4-capital:u $2) (p_EVENT.ENTITY_arg0:b $2 $2)) (p_EMPTY:u $2)) (and:c (p_TYPE_w-6-uk:u $3) (p_EVENT_w-6-uk:u $3) (p_EVENT.ENTITY_arg0:b $3 $3)) (p_EVENT.ENTITY_l-nmod.w-5-of:b $2 $3))) (p_EVENT.ENTITY_arg2:b $0 $2) (p_TARGET:u $2))) (p_TYPE_w-2-i:u $1) (p_EVENT.ENTITY_l-iobj:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals("[QUESTION(3:x), arg0(5:e , 5:m.uk), capital(3:s , 3:x), capital.arg0(3:e , 3:x), capital.nmod.of(3:e , 5:m.uk), give.arg2(0:e , 3:x), give.iobj(0:e , 1:x), i(1:s , 1:x)]",
        cleanedPredicates.toString());
  }

  @Test
  public final void testXcomp() {
    JsonObject jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"Sue asked George to respond to her offer.\",\"words\":[{\"word\":\"Sue\",\"lemma\":\"sue\",\"pos\":\"PROPN\",\"ner\":\"O\",\"index\":1,\"head\":2,\"dep\":\"nsubj\"},{\"word\":\"asked\",\"lemma\":\"ask\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":2},{\"word\":\"George\",\"lemma\":\"george\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":3,\"head\":2,\"dep\":\"dobj\"},{\"word\":\"to\",\"lemma\":\"to\",\"pos\":\"PART\",\"ner\":\"O\",\"index\":4,\"head\":5,\"dep\":\"mark\"},{\"word\":\"respond\",\"lemma\":\"respond\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":5,\"head\":2,\"dep\":\"xcomp\"},{\"word\":\"to\",\"lemma\":\"to\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":6,\"head\":8,\"dep\":\"case\"},{\"word\":\"her\",\"lemma\":\"she\",\"pos\":\"PRON\",\"ner\":\"O\",\"index\":7,\"head\":8,\"dep\":\"nmod:poss\"},{\"word\":\"offer\",\"lemma\":\"offer\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":8,\"head\":5,\"dep\":\"nmod\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":9,\"head\":2,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());

    assertEquals(
        "(l-root w-2-ask t-VERB (l-nsubj w-1-sue t-PROPN) (l-dobj w-3-george t-PROPN (l-BIND v-w-3-george)) (l-xcomp w-5-respond t-VERB (l-mark w-4-to t-PART) (l-nmod w-8-offer t-NOUN (l-case w-6-to t-ADP) (l-nmod:poss w-7-she t-PRON)) (l-nsubj v-w-3-george)) (l-punct w-9-. t-PUNCT))",
        sentence.getRootNode().toString());

    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-xcomp (l-dobj w-2-ask (l-BIND w-3-george v-w-3-george)) (l-mark (l-nsubj (l-nmod w-5-respond (l-case (l-nmod:poss w-8-offer w-7-she) w-6-to)) v-w-3-george) w-4-to)) w-1-sue) w-9-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (exists:ex $3:<a,e> (and:c (p_EVENT_w-2-ask:u $0) (and:c (and:c (p_TYPE_w-3-george:u $3) (p_EVENT_w-3-george:u $3) (p_EVENT.ENTITY_arg0:b $3 $3)) (p_EQUAL:b $3 v-w-3-george:v)) (p_EVENT.ENTITY_arg2:b $0 $3))) (exists:ex $4:<a,e> (and:c (exists:ex $5:<a,e> (and:c (p_EVENT_w-5-respond:u $2) (exists:ex $6:<a,e> (and:c (and:c (p_TYPE_w-8-offer:u $5) (p_EVENT_w-8-offer:u $5) (p_EVENT.ENTITY_arg0:b $5 $5)) (p_TYPE_w-7-she:u $6) (p_EVENT.ENTITY_l-nmod:b $5 $6))) (p_EVENT.ENTITY_l-nmod.w-6-to:b $2 $5))) (p_EQUAL:b $4 v-w-3-george:v) (p_EVENT.ENTITY_arg1:b $2 $4))) (p_EVENT.EVENT_l-xcomp:b $0 $2))) (p_TYPE_w-1-sue:u $1) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    List<String> cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[arg0(2:e , 2:m.george), ask.arg1(1:e , 0:m.sue), ask.arg2(1:e , 2:m.george), ask.xcomp(1:e , 4:e), offer(7:s , 7:x), offer.arg0(7:e , 7:x), offer.nmod(7:e , 6:x), respond.arg1(4:e , 2:m.george), respond.nmod.to(4:e , 7:x), she(6:s , 6:x)]",
        cleanedPredicates.toString());
  }

  @Test
  public final void testOtherComplements() {
    JsonObject jsonSentence =
        jsonParser
            .parse(
                "{\"sentence\":\"I am certain that he did it.\",\"words\":[{\"word\":\"I\",\"lemma\":\"I\",\"pos\":\"PRON\",\"ner\":\"O\",\"index\":1,\"head\":3,\"dep\":\"nsubj\"},{\"word\":\"am\",\"lemma\":\"be\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":2,\"head\":3,\"dep\":\"cop\"},{\"word\":\"certain\",\"lemma\":\"certain\",\"pos\":\"ADJ\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":3},{\"word\":\"that\",\"lemma\":\"that\",\"pos\":\"SCONJ\",\"ner\":\"O\",\"index\":4,\"head\":6,\"dep\":\"mark\"},{\"word\":\"he\",\"lemma\":\"he\",\"pos\":\"PRON\",\"ner\":\"O\",\"index\":5,\"head\":6,\"dep\":\"nsubj\"},{\"word\":\"did\",\"lemma\":\"do\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":6,\"head\":3,\"dep\":\"ccomp\"},{\"word\":\"it\",\"lemma\":\"it\",\"pos\":\"PRON\",\"ner\":\"O\",\"index\":7,\"head\":6,\"dep\":\"dobj\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":8,\"head\":3,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    Sentence sentence = new Sentence(jsonSentence);

    // Comment this test case if you are handling ccomp properly.
    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(enhancementRules,
        sentence.getRootNode());
    assertEquals(
        "(l-root w-3-certain t-ADJ (l-nsubj w-1-i t-PRON) (l-cop w-2-be t-VERB) (l-ccomp w-6-do t-VERB (l-mark w-4-that t-SCONJ) (l-nsubj w-5-he t-PRON) (l-dobj w-7-it t-PRON)) (l-punct w-8-. t-PUNCT))",
        sentence.getRootNode().toString());

    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority());
    assertEquals(
        "(l-punct (l-nsubj (l-ccomp (l-cop w-3-certain w-2-be) (l-mark (l-nsubj (l-dobj w-6-do w-7-it) w-5-he) w-4-that)) w-1-i) w-8-.)",
        binarizedTreeString);

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(substitutionRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            obliquenessHierarchyRules.getRelationPriority(), false);

    assertEquals(1, sentenceSemantics.second().size());
    assertEquals(
        "(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-3-certain:u $0) (exists:ex $3:<a,e> (and:c (exists:ex $4:<a,e> (and:c (p_EVENT_w-6-do:u $2) (p_TYPE_w-7-it:u $4) (p_EVENT.ENTITY_arg2:b $2 $4))) (p_TYPE_w-5-he:u $3) (p_EVENT.ENTITY_arg1:b $2 $3))) (p_EVENT.EVENT_l-ccomp:b $0 $2))) (p_TYPE_w-1-i:u $1) (p_EVENT.ENTITY_arg1:b $0 $1))))",
        sentenceSemantics.second().get(0).toString());
    List<String> cleanedPredicates =
        Lists.newArrayList(PostProcessLogicalForm.process(sentence,
            sentenceSemantics.second().get(0), true));
    Collections.sort(cleanedPredicates);
    assertEquals(
        "[certain.arg1(2:e , 0:x), certain.ccomp(2:e , 5:e), do.arg1(5:e , 4:x), do.arg2(5:e , 6:x), he(4:s , 4:x), i(0:s , 0:x), it(6:s , 6:x)]",
        cleanedPredicates.toString());
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
