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

public class TreeTransformerUDV2Test {

	static TransformationRuleGroups enhancementRules;
	static TransformationRuleGroups substitutionRules;
	static TransformationRuleGroups obliquenessHierarchyRules;
	static MutableTypeRepository types;
	static JsonParser jsonParser = new JsonParser();
	static Logger logger = Logger.getLogger("TreeTransformerUniversalTest");

	static {
		try {
			DependencyTree.LEXICAL_KEY = SentenceKeys.WORD_KEY;
			types = new MutableTypeRepository("lib_data/ud.types.txt");

			LogicLanguageServices.setInstance(new LogicLanguageServices.Builder(types, new FlexibleTypeComparator())
					.closeOntology(false).setNumeralTypeName("i").build());

			enhancementRules = new TransformationRuleGroups("lib_data/udv2-enhancement-rules.proto");
			obliquenessHierarchyRules = new TransformationRuleGroups("lib_data/udv2-obliqueness-hierarchy.proto");
			substitutionRules = new TransformationRuleGroups("lib_data/udv2-substitution-rules.proto");
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
		assertEquals("<<<a,e>,t>,<<<a,e>,t>,<<a,e>,t>>>", types.unfoldType(types.getType("r")));
	}

	@Test
	public final void testSimple() {
		// 
		JsonObject jsonSentence = jsonParser.parse(
				"{\"words\": [{\"index\": 1, \"word\": \"Did\", \"lemma\": \"do\", \"pos\": \"AUX\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"5\", \"dep\": \"aux\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 2, \"word\": \"Alice\", \"lemma\": \"Alice\", \"pos\": \"PROPN\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"4\", \"dep\": \"nmod\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"PERSON\", \"lang\": \"en\"}, {\"index\": 3, \"word\": \"'s\", \"lemma\": \"'s\", \"pos\": \"PART\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"2\", \"dep\": \"case\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 4, \"word\": \"writer\", \"lemma\": \"writer\", \"pos\": \"NOUN\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"5\", \"dep\": \"nsubj\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 5, \"word\": \"influence\", \"lemma\": \"influence\", \"pos\": \"VERB\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"0\", \"dep\": \"root\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 6, \"word\": \"a\", \"lemma\": \"a\", \"pos\": \"DET\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"8\", \"dep\": \"det\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 7, \"word\": \"film\", \"lemma\": \"film\", \"pos\": \"NOUN\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"8\", \"dep\": \"compound\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 8, \"word\": \"editor\", \"lemma\": \"editor\", \"pos\": \"NOUN\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"5\", \"dep\": \"obj\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 9, \"word\": \"?\", \"lemma\": \"?\", \"pos\": \"PUNCT\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"5\", \"dep\": \"punct\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\", \"sentEnd\": true}], \"sentence\": \"Did Alice 's writer influence a film editor ?\", \"entities\": [{\"phrase\": \"Alice\", \"start\": 1, \"end\": 1}]}")
				.getAsJsonObject();
		Sentence sentence = new Sentence(jsonSentence);

		// TreeTransformationRules for modifying the structure of a tree.
		TreeTransformer.applyRuleGroupsOnTree(enhancementRules, sentence.getRootNode());
		assertEquals(
				"(l-root w-5-influence t-VERB (l-aux w-1-did t-AUX) (l-nsubj w-4-writer t-NOUN (l-nmod w-2-alice t-PROPN (l-case w-3-'s t-PART))) (l-obj w-8-editor t-NOUN (l-det w-6-a t-DET) (l-compound w-7-film t-NOUN)) (l-punct w-9-? t-PUNCT))",
				sentence.getRootNode().toString());

		String binarizedTreeString = TreeTransformer.binarizeTree(sentence.getRootNode(),
				obliquenessHierarchyRules.getRelationPriority());
		assertEquals(
				"(l-punct (l-nsubj (l-aux (l-obj w-5-influence (l-det (l-compound w-8-editor w-7-film) w-6-a)) w-1-did) (l-nmod w-4-writer (l-case w-2-alice w-3-'s))) w-9-?)",
				binarizedTreeString);

		// Assign lambdas.
		TreeTransformer.applyRuleGroupsOnTree(substitutionRules, sentence.getRootNode());

		// Composing lambda.
		Pair<String, List<LogicalExpression>> sentenceSemantics = TreeTransformer
				.composeSemantics(sentence.getRootNode(), obliquenessHierarchyRules.getRelationPriority(), false);

		assertEquals(1, sentenceSemantics.second().size());
		assertEquals(
				"(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-5-influence:u $0) (and:c (and:c (and:c (p_TYPE_w-8-editor:u $2) (p_EVENT_w-8-editor:u $2) (p_EVENT.ENTITY_arg0:b $2 $2)) (p_TYPEMOD_w-7-film.w-8-editor:u $2)) (p_EMPTY:u $2)) (p_EVENT.ENTITY_arg2:b $0 $2))) (exists:ex $3:<a,e> (and:c (and:c (p_TYPE_w-4-writer:u $1) (p_EVENT_w-4-writer:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (and:c (p_TYPE_w-2-alice:u $3) (p_EVENT_w-2-alice:u $3) (p_EVENT.ENTITY_arg0:b $3 $3)) (p_EVENT.ENTITY_l-nmod.w-3-'s:b $1 $3))) (p_EVENT.ENTITY_arg1:b $0 $1))))",
				sentenceSemantics.second().get(0).toString());

		List<String> cleanedPredicates = Lists
				.newArrayList(PostProcessLogicalForm.process(sentence, sentenceSemantics.second().get(0), true));
		Collections.sort(cleanedPredicates);
		assertEquals(
				"[arg0(1:e , 1:m.Alice), editor(7:s , 7:x), editor.arg0(7:e , 7:x), film.editor(6:s , 7:x), influence.arg1(4:e , 3:x), influence.arg2(4:e , 7:x), writer(3:s , 3:x), writer.arg0(3:e , 3:x), writer.nmod.'s(3:e , 1:m.Alice)]",
				cleanedPredicates.toString());

	}

	@Test
	public final void testPassive() {
		// Did a film producer founded by a actor distribute Alice ?
		JsonObject jsonSentence = jsonParser.parse(
				"{\"words\": [{\"index\": 1, \"word\": \"Did\", \"lemma\": \"do\", \"pos\": \"AUX\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"9\", \"dep\": \"aux\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 2, \"word\": \"a\", \"lemma\": \"a\", \"pos\": \"DET\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"4\", \"dep\": \"det\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 3, \"word\": \"film\", \"lemma\": \"film\", \"pos\": \"NOUN\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"4\", \"dep\": \"compound\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 4, \"word\": \"producer\", \"lemma\": \"producer\", \"pos\": \"NOUN\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"9\", \"dep\": \"nsubj\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 5, \"word\": \"founded\", \"lemma\": \"found\", \"pos\": \"VERB\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"4\", \"dep\": \"acl\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 6, \"word\": \"by\", \"lemma\": \"by\", \"pos\": \"ADP\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"8\", \"dep\": \"case\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 7, \"word\": \"a\", \"lemma\": \"a\", \"pos\": \"DET\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"8\", \"dep\": \"det\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 8, \"word\": \"actor\", \"lemma\": \"actor\", \"pos\": \"NOUN\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"5\", \"dep\": \"obl\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 9, \"word\": \"distribute\", \"lemma\": \"distribute\", \"pos\": \"VERB\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"0\", \"dep\": \"root\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 10, \"word\": \"Alice\", \"lemma\": \"Alice\", \"pos\": \"NOUN\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"9\", \"dep\": \"obj\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"PERSON\", \"lang\": \"en\"}, {\"index\": 11, \"word\": \"?\", \"lemma\": \"?\", \"pos\": \"PUNCT\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"9\", \"dep\": \"punct\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\", \"sentEnd\": true}], \"sentence\": \"Did a film producer founded by a actor distribute Alice ?\", \"entities\": [{\"phrase\": \"Alice\", \"start\": 9, \"end\": 9}]}")
				.getAsJsonObject();
		Sentence sentence = new Sentence(jsonSentence);

		// TreeTransformationRules for modifying the structure of a tree.
		TreeTransformer.applyRuleGroupsOnTree(enhancementRules, sentence.getRootNode());
		assertEquals(
				"(l-root w-9-distribute t-VERB (l-aux w-1-did t-AUX) (l-nsubj w-4-producer t-NOUN (l-det w-2-a t-DET) (l-compound w-3-film t-NOUN) (l-acl w-5-founded t-VERB (l-obl w-8-actor t-NOUN (l-case w-6-by t-ADP) (l-det w-7-a t-DET)) (l-obj v-w-4-producer)) (l-BIND v-w-4-producer)) (l-obj w-10-alice t-NOUN) (l-punct w-11-? t-PUNCT))",
				sentence.getRootNode().toString());

		String binarizedTreeString = TreeTransformer.binarizeTree(sentence.getRootNode(),
				obliquenessHierarchyRules.getRelationPriority());
		assertEquals(
				"(l-punct (l-nsubj (l-aux (l-obj w-9-distribute w-10-alice) w-1-did) (l-acl (l-det (l-compound (l-BIND w-4-producer v-w-4-producer) w-3-film) w-2-a) (l-obl (l-obj w-5-founded v-w-4-producer) (l-case (l-det w-8-actor w-7-a) w-6-by)))) w-11-?)",
				binarizedTreeString);

		// Assign lambdas.
		TreeTransformer.applyRuleGroupsOnTree(substitutionRules, sentence.getRootNode());

		// Composing lambda.
		Pair<String, List<LogicalExpression>> sentenceSemantics = TreeTransformer
				.composeSemantics(sentence.getRootNode(), obliquenessHierarchyRules.getRelationPriority(), false);

		assertEquals(1, sentenceSemantics.second().size());
		assertEquals(
				"(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-9-distribute:u $0) (p_TYPE_w-10-alice:u $2) (p_EVENT.ENTITY_arg2:b $0 $2))) (exists:ex $3:<a,e> (and:c (and:c (and:c (and:c (and:c (p_TYPE_w-4-producer:u $1) (p_EVENT_w-4-producer:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (p_EQUAL:b $1 v-w-4-producer:v)) (p_TYPEMOD_w-3-film.w-4-producer:u $1)) (p_EMPTY:u $1)) (exists:ex $4:<a,e> (and:c (exists:ex $5:<a,e> (and:c (p_EVENT_w-5-founded:u $3) (p_EQUAL:b $5 v-w-4-producer:v) (p_EVENT.ENTITY_arg2:b $3 $5))) (and:c (and:c (p_TYPE_w-8-actor:u $4) (p_EVENT_w-8-actor:u $4) (p_EVENT.ENTITY_arg0:b $4 $4)) (p_EMPTY:u $4)) (p_EVENT.ENTITY_l-obl.w-6-by:b $3 $4))))) (p_EVENT.ENTITY_arg1:b $0 $1))))",
				sentenceSemantics.second().get(0).toString());

		List<String> cleanedPredicates = Lists
				.newArrayList(PostProcessLogicalForm.process(sentence, sentenceSemantics.second().get(0), true));
		Collections.sort(cleanedPredicates);
		assertEquals(
				"[actor(7:s , 7:x), actor.arg0(7:e , 7:x), distribute.arg1(8:e , 3:x), distribute.arg2(8:e , 9:m.Alice), film.producer(2:s , 3:x), found.arg2(4:e , 3:x), found.obl.by(4:e , 7:x), producer(3:s , 3:x), producer.arg0(3:e , 3:x)]",
				cleanedPredicates.toString());
		
	}
	
	@Test
	public final void testConjVP() {
		// Did a film producer founded by a actor distribute Alice ?
		JsonObject jsonSentence = jsonParser.parse(
				"{\"words\": [{\"index\": 1, \"word\": \"Did\", \"lemma\": \"do\", \"pos\": \"AUX\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"3\", \"dep\": \"aux\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 2, \"word\": \"Alice\", \"lemma\": \"Alice\", \"pos\": \"PROPN\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"3\", \"dep\": \"nsubj\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"PERSON\", \"lang\": \"en\"}, {\"index\": 3, \"word\": \"star\", \"lemma\": \"star\", \"pos\": \"VERB\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"0\", \"dep\": \"root\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 4, \"word\": \"Bob\", \"lemma\": \"Bob\", \"pos\": \"PROPN\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"3\", \"dep\": \"obj\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"PERSON\", \"lang\": \"en\"}, {\"index\": 5, \"word\": \"and\", \"lemma\": \"and\", \"pos\": \"CONJ\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"6\", \"dep\": \"cc\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 6, \"word\": \"star\", \"lemma\": \"star\", \"pos\": \"VERB\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"3\", \"dep\": \"conj\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 7, \"word\": \"a\", \"lemma\": \"a\", \"pos\": \"DET\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"9\", \"dep\": \"det\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 8, \"word\": \"film\", \"lemma\": \"film\", \"pos\": \"NOUN\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"9\", \"dep\": \"compound\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 9, \"word\": \"editor\", \"lemma\": \"editor\", \"pos\": \"NOUN\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"6\", \"dep\": \"obj\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 10, \"word\": \"?\", \"lemma\": \"?\", \"pos\": \"PUNCT\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"3\", \"dep\": \"punct\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\", \"sentEnd\": true}], \"sentence\": \"Did Alice star Bob and star a film editor ?\", \"entities\": [{\"phrase\": \"Alice\", \"start\": 1, \"end\": 1}, {\"phrase\": \"Bob\", \"start\": 3, \"end\": 3}]}")
				.getAsJsonObject();
		Sentence sentence = new Sentence(jsonSentence);

		// TreeTransformationRules for modifying the structure of a tree.
		TreeTransformer.applyRuleGroupsOnTree(enhancementRules, sentence.getRootNode());
		assertEquals(
				"(l-root w-3-star t-VERB (l-aux w-1-did t-AUX) (l-nsubj w-2-alice t-PROPN) (l-obj w-4-bob t-PROPN) (l-conj-vp w-6-star t-VERB (l-cc w-5-and t-CONJ) (l-obj w-9-editor t-NOUN (l-det w-7-a t-DET) (l-compound w-8-film t-NOUN))) (l-punct w-10-? t-PUNCT))",
				sentence.getRootNode().toString());

		String binarizedTreeString = TreeTransformer.binarizeTree(sentence.getRootNode(),
				obliquenessHierarchyRules.getRelationPriority());
		assertEquals(
				"(l-punct (l-nsubj (l-conj-vp (l-aux (l-obj w-3-star w-4-bob) w-1-did) (l-obj (l-cc w-6-star w-5-and) (l-det (l-compound w-9-editor w-8-film) w-7-a))) w-2-alice) w-10-?)",
				binarizedTreeString);

		// Assign lambdas.
		TreeTransformer.applyRuleGroupsOnTree(substitutionRules, sentence.getRootNode());

		// Composing lambda.
		Pair<String, List<LogicalExpression>> sentenceSemantics = TreeTransformer
				.composeSemantics(sentence.getRootNode(), obliquenessHierarchyRules.getRelationPriority(), false);

		List<String> cleanedPredicates = Lists
				.newArrayList(PostProcessLogicalForm.process(sentence, sentenceSemantics.second().get(0), true));
		Collections.sort(cleanedPredicates);
		assertEquals(
				"[editor(8:s , 8:x), editor.arg0(8:e , 8:x), film.editor(7:s , 8:x), star.arg1(2:e , 1:m.Alice), star.arg1(5:e , 1:m.Alice), star.arg2(2:e , 3:m.Bob), star.arg2(5:e , 8:x)]",
				cleanedPredicates.toString());
		
	}
	
	@Test
	public final void testConjNP() {
		// Did a film producer founded by a actor distribute Alice ?
		JsonObject jsonSentence = jsonParser.parse(
				"{\"words\": [{\"index\": 1, \"word\": \"Did\", \"lemma\": \"do\", \"pos\": \"AUX\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"18\", \"dep\": \"aux\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 2, \"word\": \"Alice\", \"lemma\": \"Alice\", \"pos\": \"PROPN\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"4\", \"dep\": \"nmod\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"PERSON\", \"lang\": \"en\"}, {\"index\": 3, \"word\": \"'s\", \"lemma\": \"'s\", \"pos\": \"PART\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"2\", \"dep\": \"case\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 4, \"word\": \"writer\", \"lemma\": \"writer\", \"pos\": \"NOUN\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"18\", \"dep\": \"nsubj\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 5, \"word\": \",\", \"lemma\": \",\", \"pos\": \"PUNCT\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"6\", \"dep\": \"punct\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 6, \"word\": \"editor\", \"lemma\": \"editor\", \"pos\": \"NOUN\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"4\", \"dep\": \"conj\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 7, \"word\": \",\", \"lemma\": \",\", \"pos\": \"PUNCT\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"8\", \"dep\": \"punct\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 8, \"word\": \"director\", \"lemma\": \"director\", \"pos\": \"NOUN\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"4\", \"dep\": \"conj\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 9, \"word\": \",\", \"lemma\": \",\", \"pos\": \"PUNCT\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"11\", \"dep\": \"punct\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 10, \"word\": \"costume\", \"lemma\": \"costume\", \"pos\": \"ADJ\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"11\", \"dep\": \"compound\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 11, \"word\": \"designer\", \"lemma\": \"designer\", \"pos\": \"NOUN\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"4\", \"dep\": \"conj\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 12, \"word\": \",\", \"lemma\": \",\", \"pos\": \"PUNCT\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"13\", \"dep\": \"punct\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 13, \"word\": \"cinematographer\", \"lemma\": \"cinematographer\", \"pos\": \"NOUN\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"4\", \"dep\": \"conj\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 14, \"word\": \",\", \"lemma\": \",\", \"pos\": \"PUNCT\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"16\", \"dep\": \"punct\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 15, \"word\": \"and\", \"lemma\": \"and\", \"pos\": \"CONJ\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"16\", \"dep\": \"cc\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 16, \"word\": \"star\", \"lemma\": \"star\", \"pos\": \"NOUN\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"4\", \"dep\": \"conj\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 17, \"word\": \"executive\", \"lemma\": \"executive\", \"pos\": \"NOUN\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"18\", \"dep\": \"compound\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 18, \"word\": \"produce\", \"lemma\": \"produce\", \"pos\": \"VERB\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"0\", \"dep\": \"root\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 19, \"word\": \"and\", \"lemma\": \"and\", \"pos\": \"CONJ\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"20\", \"dep\": \"cc\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 20, \"word\": \"direct\", \"lemma\": \"direct\", \"pos\": \"VERB\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"18\", \"dep\": \"conj\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\"}, {\"index\": 21, \"word\": \"Bob\", \"lemma\": \"Bob\", \"pos\": \"PROPN\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"18\", \"dep\": \"obj\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"PERSON\", \"lang\": \"en\"}, {\"index\": 22, \"word\": \"?\", \"lemma\": \"?\", \"pos\": \"PUNCT\", \"fpos\": \"foo\", \"feats\": \"_\", \"head\": \"18\", \"dep\": \"punct\", \"phead\": \"_\", \"pdep\": \"_\", \"ner\": \"O\", \"lang\": \"en\", \"sentEnd\": true}], \"sentence\": \"Did Alice 's writer , editor , director , costume designer , cinematographer , and star executive produce and direct Bob ?\", \"entities\": [{\"phrase\": \"Alice\", \"start\": 1, \"end\": 1}, {\"phrase\": \"Bob\", \"start\": 20, \"end\": 20}]}")
				.getAsJsonObject();
		Sentence sentence = new Sentence(jsonSentence);

		// TreeTransformationRules for modifying the structure of a tree.
		TreeTransformer.applyRuleGroupsOnTree(enhancementRules, sentence.getRootNode());
		assertEquals(
				"(l-root w-18-produce t-VERB (l-aux w-1-did t-AUX) (l-nsubj w-4-writer t-NOUN (l-nmod w-2-alice t-PROPN (l-case w-3-'s t-PART)) (l-conj-np w-6-editor t-NOUN (l-punct w-5-, t-PUNCT)) (l-conj-np w-8-director t-NOUN (l-punct w-7-, t-PUNCT)) (l-conj-np w-11-designer t-NOUN (l-punct w-9-, t-PUNCT) (l-compound w-10-costume t-ADJ)) (l-conj-np w-13-cinematographer t-NOUN (l-punct w-12-, t-PUNCT)) (l-conj-np w-16-star t-NOUN (l-punct w-14-, t-PUNCT) (l-cc w-15-and t-CONJ))) (l-compound w-17-executive t-NOUN) (l-conj-verb w-20-direct t-VERB (l-cc w-19-and t-CONJ)) (l-obj w-21-bob t-PROPN) (l-punct w-22-? t-PUNCT))",
				sentence.getRootNode().toString());

		String binarizedTreeString = TreeTransformer.binarizeTree(sentence.getRootNode(),
				obliquenessHierarchyRules.getRelationPriority());
		assertEquals(
				"(l-punct (l-nsubj (l-aux (l-obj (l-conj-verb (l-compound w-18-produce w-17-executive) (l-cc w-20-direct w-19-and)) w-21-bob) w-1-do) (l-nmod (l-conj-np (l-conj-np (l-conj-np (l-conj-np (l-conj-np w-4-writer (l-punct w-6-editor w-5-,)) (l-punct w-8-director w-7-,)) (l-punct (l-compound w-11-designer w-10-costume) w-9-,)) (l-punct w-13-cinematographer w-12-,)) (l-punct (l-cc w-16-star w-15-and) w-14-,)) (l-case w-2-alice w-3-'s))) w-22-?)",
				binarizedTreeString);

		// Assign lambdas.
		TreeTransformer.applyRuleGroupsOnTree(substitutionRules, sentence.getRootNode());

		// Composing lambda.
		Pair<String, List<LogicalExpression>> sentenceSemantics = TreeTransformer
				.composeSemantics(sentence.getRootNode(), obliquenessHierarchyRules.getRelationPriority(), false);

		List<String> cleanedPredicates = Lists
				.newArrayList(PostProcessLogicalForm.process(sentence, sentenceSemantics.second().get(0), true));
		Collections.sort(cleanedPredicates);
		assertEquals(
				"[arg0(1:e , 1:m.Alice), cinematographer(12:s , 12:x), cinematographer.arg0(12:e , 12:x), costume(9:s , 10:x), designer(10:s , 10:x), designer.arg0(10:e , 10:x), direct.arg2(19:e , 20:m.Bob), director(7:s , 7:x), director.arg0(7:e , 7:x), editor(5:s , 3:x), produce.arg2(17:e , 20:m.Bob), star(15:s , 15:x), star.arg0(15:e , 15:x), writer(3:s , 3:x), writer.arg0(3:e , 3:x)]",
				cleanedPredicates.toString());
		
	}
	
	@Test
	public final void testConjNP2() {
		// Did a film producer founded by a actor distribute Alice ?
		JsonObject jsonSentence = jsonParser.parse(
				"{\"sentence\":\"Alice\\u0027s brother and sister ate burgers.\",\"words\":[{\"word\":\"Alice\",\"lemma\":\"Alice\",\"pos\":\"PROPN\",\"ner\":\"PERSON\",\"index\":1,\"head\":3,\"dep\":\"nmod:poss\",\"lang\":\"en\"},{\"word\":\"\\u0027s\",\"lemma\":\"\\u0027s\",\"pos\":\"PART\",\"ner\":\"O\",\"index\":2,\"head\":1,\"dep\":\"case\",\"lang\":\"en\"},{\"word\":\"brother\",\"lemma\":\"brother\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":3,\"head\":6,\"dep\":\"nsubj\",\"lang\":\"en\"},{\"word\":\"and\",\"lemma\":\"and\",\"pos\":\"CONJ\",\"ner\":\"O\",\"index\":4,\"head\":3,\"dep\":\"cc\",\"lang\":\"en\"},{\"word\":\"sister\",\"lemma\":\"sister\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":5,\"head\":3,\"dep\":\"conj\",\"lang\":\"en\"},{\"word\":\"ate\",\"lemma\":\"eat\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":6,\"lang\":\"en\"},{\"word\":\"burgers\",\"lemma\":\"burger\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":7,\"head\":6,\"dep\":\"obj\",\"lang\":\"en\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":8,\"head\":6,\"dep\":\"punct\",\"sentEnd\":true,\"lang\":\"en\"}],\"entities\":[{\"phrase\":\"Alice\",\"start\":0,\"end\":0,\"index\":0}],\"deplambda_oblique_tree\":\"(l-punct (l-nsubj (l-dobj w-6-ate w-7-burgers) (l-nmod:poss (l-conj-np (l-cc w-3-brother w-4-and) w-5-sister) (l-case w-1-alice w-2-\\u0027s))) w-8-.)\",\"deplambda_expression\":\"(lambda $0:\\u003ca,e\\u003e (exists:ex $1:\\u003ca,e\\u003e (and:c (exists:ex $2:\\u003ca,e\\u003e (and:c (p_EVENT_w-6-ate:u $0) (p_TYPE_w-7-burgers:u $2) (p_EVENT.ENTITY_arg2:b $0 $2))) (exists:ex $3:\\u003ca,e\\u003e (and:c (exists:ex $4:\\u003ca,e\\u003e (exists:ex $5:\\u003ca,e\\u003e (and:c (exists:ex $6:\\u003ca,e\\u003e (and:c (and:c (p_TYPE_w-3-brother:u $4) (p_EVENT_w-3-brother:u $4) (p_EVENT.ENTITY_arg0:b $4 $4)) (p_EMPTY:u $6))) (p_TYPE_w-5-sister:u $5) (p_CONJ:tri $1 $4 $5)))) (and:c (p_TYPE_w-1-alice:u $3) (p_EVENT_w-1-alice:u $3) (p_EVENT.ENTITY_arg0:b $3 $3)) (p_EVENT.ENTITY_l-nmod.w-2-\\u0027s:b $1 $3))) (p_EVENT.ENTITY_arg1:b $0 $1))))\",\"dependency_lambda\":[[\"arg0(0:e , 0:m.Alice)\",\"brother(2:s , 2:x)\",\"brother.arg0(2:e , 2:x)\",\"brother.nmod.\\u0027s(2:e , 0:m.Alice)\",\"burgers(6:s , 6:x)\",\"eat.arg1(5:e , 2:x)\",\"eat.arg1(5:e , 4:x)\",\"eat.arg2(5:e , 6:x)\",\"sister(4:s , 4:x)\"]]}")
				.getAsJsonObject();
		Sentence sentence = new Sentence(jsonSentence);

		// TreeTransformationRules for modifying the structure of a tree.
		TreeTransformer.applyRuleGroupsOnTree(enhancementRules, sentence.getRootNode());
		assertEquals(
				"(l-root w-6-ate t-VERB (l-nsubj w-3-brother t-NOUN (l-nmod:poss w-1-alice t-PROPN (l-case w-2-'s t-PART)) (l-cc w-4-and t-CONJ) (l-conj-np w-5-sister t-NOUN)) (l-obj w-7-burgers t-NOUN) (l-punct w-8-. t-PUNCT))",
				sentence.getRootNode().toString());

		String binarizedTreeString = TreeTransformer.binarizeTree(sentence.getRootNode(),
				obliquenessHierarchyRules.getRelationPriority());
		assertEquals(
				"(l-punct (l-nsubj (l-obj w-6-ate w-7-burgers) (l-nmod:poss (l-conj-np (l-cc w-3-brother w-4-and) w-5-sister) (l-case w-1-alice w-2-'s))) w-8-.)",
				binarizedTreeString);

		// Assign lambdas.
		TreeTransformer.applyRuleGroupsOnTree(substitutionRules, sentence.getRootNode());

		// Composing lambda.
		Pair<String, List<LogicalExpression>> sentenceSemantics = TreeTransformer
				.composeSemantics(sentence.getRootNode(), obliquenessHierarchyRules.getRelationPriority(), false);
		
		assertEquals(1, sentenceSemantics.second().size());
		assertEquals(
				"(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-6-ate:u $0) (p_TYPE_w-7-burgers:u $2) (p_EVENT.ENTITY_arg2:b $0 $2))) (exists:ex $3:<a,e> (and:c (exists:ex $4:<a,e> (exists:ex $5:<a,e> (and:c (exists:ex $6:<a,e> (and:c (and:c (p_TYPE_w-3-brother:u $4) (p_EVENT_w-3-brother:u $4) (p_EVENT.ENTITY_arg0:b $4 $4)) (p_EMPTY:u $6))) (p_TYPE_w-5-sister:u $5) (p_CONJ:tri $1 $4 $5)))) (and:c (p_TYPE_w-1-alice:u $3) (p_EVENT_w-1-alice:u $3) (p_EVENT.ENTITY_arg0:b $3 $3)) (p_EVENT.ENTITY_l-nmod.w-2-'s:b $1 $3))) (p_EVENT.ENTITY_arg1:b $0 $1))))",
				sentenceSemantics.second().get(0).toString());

		List<String> cleanedPredicates = Lists
				.newArrayList(PostProcessLogicalForm.process(sentence, sentenceSemantics.second().get(0), true));
		Collections.sort(cleanedPredicates);
		assertEquals(
				"[arg0(0:e , 0:m.Alice), brother(2:s , 2:x), brother.arg0(2:e , 2:x), burger(6:s , 6:x), eat.arg2(5:e , 6:x), sister(4:s , 2:x), sister.arg0(4:e , 4:x),]",
				cleanedPredicates.toString());
		
	}


}
