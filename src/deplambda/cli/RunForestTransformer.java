package deplambda.cli;

import in.sivareddy.graphparser.cli.AbstractCli;
import in.sivareddy.util.SentenceUtils;

import java.io.IOException;
import java.io.InputStream;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import deplambda.parser.ForestTransformerMain;
import deplambda.parser.TreeTransformerMain;
import deplambda.util.TransformationRuleGroups;
import edu.cornell.cs.nlp.spf.mr.lambda.FlexibleTypeComparator;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.language.type.MutableTypeRepository;

public class RunForestTransformer extends AbstractCli {
  private OptionSpec<String> definedTypesFile;
  private OptionSpec<String> relationPrioritiesFile;
  private OptionSpec<String> lambdaAssignmentRulesFile;
  private OptionSpec<String> treeTransformationsFile;

  private OptionSpec<String> inputFile;
  private OptionSpec<String> debugToFile;

  private OptionSpec<Boolean> lexicalizePredicates;

  private OptionSpec<Boolean> inputTypeIsForest;

  private OptionSpec<Integer> nthreads;

  @Override
  public void initializeOptions(OptionParser parser) {
    lexicalizePredicates =
        parser
            .accepts(
                "lexicalizePredicates",
                "lexicalize predicates with the source event, e.g. eat(1:e) ^ arg1(1:e , 2:x) -> eat.arg1(1:e , 2:x)")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    inputTypeIsForest =
        parser
            .accepts(
                "inputTypeIsForest",
                "input type can be a forest or a sentence object. Set this to false if you want to process individual sentences.")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    treeTransformationsFile =
        parser
            .accepts(
                "treeTransformationsFile",
                "file containing rules that modify the structure of the tree, e.g., making dependency relations fine grained, or adding traces")
            .withRequiredArg().ofType(String.class).required();

    definedTypesFile =
        parser
            .accepts("definedTypesFile",
                "file containing defined lambda variable types")
            .withRequiredArg().ofType(String.class).required();


    relationPrioritiesFile =
        parser
            .accepts("relationPrioritiesFile",
                "file specifying obliqueness hierarchy of dependenecy relations")
            .withRequiredArg().ofType(String.class).required();

    lambdaAssignmentRulesFile =
        parser
            .accepts(
                "lambdaAssignmentRulesFile",
                "file containing rules to assign lambda expressions to nodes in a dependency tree")
            .withRequiredArg().ofType(String.class).required();

    nthreads =
        parser.accepts("nthreads", "number of threads").withRequiredArg()
            .ofType(Integer.class).defaultsTo(20);

    inputFile =
        parser.accepts("inputFile", "input file containing forest of trees")
            .withRequiredArg().ofType(String.class).defaultsTo("stdin");

    debugToFile =
        parser
            .accepts("debugToFile",
                "sets the debugging mode. Note the debugging is asynchronous. Use nthread=1")
            .withRequiredArg().ofType(String.class).defaultsTo("");
  }

  @Override
  public void run(OptionSet options) {
    try {
      int nthreadsVal = options.valueOf(nthreads);
      String inputFileVal = options.valueOf(inputFile);

      InputStream inputStream = SentenceUtils.getInputStream(inputFileVal);
      MutableTypeRepository types =
          new MutableTypeRepository(options.valueOf(definedTypesFile));

      LogicLanguageServices.setInstance(new LogicLanguageServices.Builder(
          types, new FlexibleTypeComparator()).closeOntology(false)
          .setNumeralTypeName("i").build());

      TransformationRuleGroups treeTransformationRules =
          new TransformationRuleGroups(options.valueOf(treeTransformationsFile));
      TransformationRuleGroups relationPrioritiesRules =
          new TransformationRuleGroups(options.valueOf(relationPrioritiesFile));
      TransformationRuleGroups lambdaAssignmentRules =
          new TransformationRuleGroups(
              options.valueOf(lambdaAssignmentRulesFile));

      Logger logger = null;
      if (!options.valueOf(debugToFile).equals("")) {
        logger = Logger.getLogger(getClass());
        PatternLayout layout = new PatternLayout("%r [%t] %-5p: %m%n");
        logger.setLevel(Level.DEBUG);
        logger.setAdditivity(false);
        Appender fileAppender =
            new FileAppender(layout, options.valueOf(debugToFile));
        logger.addAppender(fileAppender);
      }

      TreeTransformerMain treeTransformer =
          new TreeTransformerMain(treeTransformationRules,
              relationPrioritiesRules, lambdaAssignmentRules, logger,
              options.valueOf(lexicalizePredicates));

      if (options.valueOf(inputTypeIsForest)) {
        ForestTransformerMain forestTransoformer =
            new ForestTransformerMain(treeTransformer);
        forestTransoformer.processStream(inputStream, System.out, nthreadsVal,
            true);
      } else {
        treeTransformer.processStream(inputStream, System.out, nthreadsVal,
            true);
      }
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    new RunForestTransformer().run(args);
  }
}
