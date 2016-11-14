package deplambda.others;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import deplambda.parser.TreeTransformerMain;
import deplambda.util.TransformationRuleGroups;
import edu.cornell.cs.nlp.spf.mr.lambda.FlexibleTypeComparator;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.language.type.MutableTypeRepository;

public class NlpPipeline extends in.sivareddy.graphparser.util.NlpPipeline {
  public static String DEPLAMBDA = "deplambda";
  public static String DEPLAMBDA_LEXICALIZE_PREDICATES =
      "deplambda.lexicalizePredicates";
  public static String DEPLAMBDA_TREE_TRANSFORMATIONS_FILE =
      "deplambda.treeTransformationsFile";
  public static String DEPLAMBDA_DEFINED_TYPES_FILE =
      "deplambda.definedTypesFile";
  public static String DEPLAMBDA_RELATION_PRORITIES_FILE =
      "deplambda.relationPrioritiesFile";
  public static String DEPLAMBDA_LAMBDA_ASSIGNMENT_RULES_FILE =
      "deplambda.lambdaAssignmentRulesFile";
  public static String DEPLAMBDA_DEBUG = "deplambda.debug";

  TreeTransformerMain treeTransformer = null;
  private Map<String, String> options;

  public NlpPipeline(Map<String, String> options) throws Exception {
    super(options);
    System.err.println(options);
    this.options = options;

    if (options.containsKey(DEPLAMBDA)) {
      System.err.println("Loading DepLambda Model.. ");
      try {
        MutableTypeRepository types =
            new MutableTypeRepository(options.get(DEPLAMBDA_DEFINED_TYPES_FILE));
        System.err.println(String.format("%s=%s", DEPLAMBDA_DEFINED_TYPES_FILE,
            options.get(DEPLAMBDA_DEFINED_TYPES_FILE)));

        LogicLanguageServices.setInstance(new LogicLanguageServices.Builder(
            types, new FlexibleTypeComparator()).closeOntology(false)
            .setNumeralTypeName("i").build());

        TransformationRuleGroups treeTransformationRules;
        treeTransformationRules =
            new TransformationRuleGroups(
                options.get(DEPLAMBDA_TREE_TRANSFORMATIONS_FILE));
        System.err.println(String.format("%s=%s",
            DEPLAMBDA_TREE_TRANSFORMATIONS_FILE,
            options.get(DEPLAMBDA_TREE_TRANSFORMATIONS_FILE)));

        TransformationRuleGroups relationPrioritiesRules =
            new TransformationRuleGroups(
                options.get(DEPLAMBDA_RELATION_PRORITIES_FILE));
        System.err.println(String.format("%s=%s",
            DEPLAMBDA_RELATION_PRORITIES_FILE,
            options.get(DEPLAMBDA_RELATION_PRORITIES_FILE)));

        TransformationRuleGroups lambdaAssignmentRules =
            new TransformationRuleGroups(
                options.get(DEPLAMBDA_LAMBDA_ASSIGNMENT_RULES_FILE));
        System.err.println(String.format("%s=%s",
            DEPLAMBDA_LAMBDA_ASSIGNMENT_RULES_FILE,
            options.get(DEPLAMBDA_LAMBDA_ASSIGNMENT_RULES_FILE)));
        Boolean lexicalizePredicates =
            Boolean.parseBoolean(options.getOrDefault(
                DEPLAMBDA_LEXICALIZE_PREDICATES, "true"));

        treeTransformer =
            new TreeTransformerMain(treeTransformationRules,
                relationPrioritiesRules, lambdaAssignmentRules, null,
                lexicalizePredicates);
        System.err.println("Loaded DepLambda Model.. ");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void processSentence(JsonObject jsonSentence) {
    if (jsonSentence.has(SentenceKeys.FOREST)) {
      for (JsonElement individualSentence : jsonSentence.get(
          SentenceKeys.FOREST).getAsJsonArray()) {
        processIndividualSentence(individualSentence.getAsJsonObject());
      }
    } else {
      processIndividualSentence(jsonSentence);
    }
  }

  public void processIndividualSentence(JsonObject jsonSentence) {
    super.processIndividualSentence(jsonSentence);
    if (options.containsKey(DEPLAMBDA)) {
      System.err.println("Runnning deplambda ...");
      treeTransformer.processSentence(jsonSentence);
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0 || args.length % 2 != 0) {
      System.err
          .println("Specify pipeline arguments, e.g., annotator, languageCode, preprocess.capitalize. See the NlpPipelineTest file.");
      System.exit(0);
    }

    Map<String, String> options = new HashMap<>();
    for (int i = 0; i < args.length; i += 2) {
      options.put(args[i], args[i + 1]);
    }

    NlpPipeline englishPipeline = new NlpPipeline(options);
    int nthreads =
        options.containsKey(SentenceKeys.NTHREADS) ? Integer.parseInt(options
            .get(SentenceKeys.NTHREADS)) : 20;
    englishPipeline.processStream(System.in, System.out, nthreads, true);
  }
}
