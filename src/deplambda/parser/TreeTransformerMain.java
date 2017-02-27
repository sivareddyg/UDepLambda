package deplambda.parser;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import deplambda.others.SentenceKeys;
import deplambda.util.Sentence;
import deplambda.util.TransformationRuleGroups;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.utils.composites.Pair;
import in.sivareddy.util.ProcessStreamInterface;

public class TreeTransformerMain extends ProcessStreamInterface {

  private static Gson gson = new Gson();
  private static JsonParser jsonParser = new JsonParser();

  private final TransformationRuleGroups treeTransformationRules;
  private final TransformationRuleGroups lambdaAssignmentRules;
  private final TransformationRuleGroups relationPriorityRules;
  private final Logger logger;

  private final boolean lexicalizePredicates;

  public TreeTransformerMain(TransformationRuleGroups treeTransformationRules,
      TransformationRuleGroups relationPriorityRules,
      TransformationRuleGroups lambdaAssignmentRules, Logger logger,
      boolean lexicalizePredicates) {
    this.treeTransformationRules = treeTransformationRules;
    this.relationPriorityRules = relationPriorityRules;
    this.lambdaAssignmentRules = lambdaAssignmentRules;
    this.logger = logger;

    this.lexicalizePredicates = lexicalizePredicates;
  }

  @Override
  public void processSentence(JsonObject sent) {
    Sentence sentence = new Sentence(sent);
    // TreeTransformationRules for modifying the structure of a tree.
    TreeTransformer.applyRuleGroupsOnTree(treeTransformationRules,
        sentence.getRootNode());

    // Assign lambdas.
    TreeTransformer.applyRuleGroupsOnTree(lambdaAssignmentRules,
        sentence.getRootNode());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            relationPriorityRules.getRelationPriority(), logger, false);
    sent.addProperty(SentenceKeys.DEPLAMBDA_OBLIQUE_TREE,
        sentenceSemantics.first());
    if (sentenceSemantics.second().size() > 0) {
      sent.addProperty(SentenceKeys.DEPLAMBDA_EXPRESSION,
          sentenceSemantics.second().get(0).toString());
    }

    // Post processing lambdas.
    JsonArray jsonParses = new JsonArray();
    for (LogicalExpression parse : sentenceSemantics.second()) {
      List<String> cleaned =
          Lists.newArrayList(PostProcessLogicalForm.process(sentence, parse,
              lexicalizePredicates));

      // TODO: Better sorting function is required.
      Collections.sort(cleaned);
      jsonParses.add(jsonParser.parse(gson.toJson(cleaned)));
    }
    sent.add(SentenceKeys.DEPENDENCY_LAMBDA, jsonParses);
  }
}
