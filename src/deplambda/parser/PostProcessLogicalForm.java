package deplambda.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

import deplambda.others.PredicateKeys;
import deplambda.others.SentenceKeys;
import deplambda.util.Sentence;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.Literal;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.SimpleLogicalExpressionReader;
import edu.cornell.cs.nlp.spf.mr.lambda.Term;
import jregex.Matcher;
import jregex.Pattern;

public class PostProcessLogicalForm {
  public static Set<String> process(Sentence sentence, LogicalExpression parse,
      boolean lexicalizePredicates) {
    List<Literal> predicates = new ArrayList<>();
    traverse(parse, predicates);
    LogicalExpression[] predicatesList =
        new LogicalExpression[predicates.size()];
    LogicalExpression and = SimpleLogicalExpressionReader.read("and:c");
    Literal x = new Literal(and, predicates.toArray(predicatesList));
    System.out.println(x);

    Map<Term, List<Integer>> varToEvents = new HashMap<>();
    Map<Term, List<Integer>> varToEntities = new HashMap<>();
    process(predicates, varToEvents, varToEntities, sentence, parse,
        lexicalizePredicates);

    cleanVarToEntities(varToEntities, sentence);
    Set<String> cleanedPredicates =
        createCleanPredicates(predicates, varToEvents, varToEntities, sentence);
    System.out.println(cleanedPredicates);
    return cleanedPredicates;
  }

  private static Set<String> createCleanPredicates(List<Literal> predicates,
      Map<Term, List<Integer>> varToEvents,
      Map<Term, List<Integer>> varToEntities, Sentence sentence) {
    Set<String> cleanedPredicates = new HashSet<>();
    for (Literal predicate : predicates) {
      String basePredicate =
          ((LogicalConstant) predicate.getPredicate()).getBaseName();
      if (basePredicate.startsWith(PredicateKeys.EVENT_ENTITY_PREFIX)) {
        // (p_EVENT.ENTITY_arg1:b $0:<a,e> $1:<a,e>)
        // (p_EVENT.ENTITY_l-nmod.w-4-as:b $0:<a,e> $1:<a,e>)
        Iterable<String> parts = Splitter.on(".")
            .trimResults(CharMatcher.anyOf(":")).split(basePredicate
                .substring(PredicateKeys.EVENT_ENTITY_PREFIX.length()));
        List<String> cleanedParts = new ArrayList<>();
        for (String part : parts) {
          part = part.replaceFirst("^l-", "");
          part = part.replaceFirst("^w-[0-9]+-", "");
          cleanedParts.add(part);
        }
        String cleanedPredicate = Joiner.on(".").join(cleanedParts);
        Term eventTerm = (Term) predicate.getArg(0);
        Term entityTerm = (Term) predicate.getArg(1);
        for (int eventIndex : varToEvents.get(eventTerm)) {
          for (int entityIndex : varToEntities.get(entityTerm)) {
            cleanedPredicates.add(String.format("(%s %d:e %d:x)",
                cleanedPredicate, eventIndex, entityIndex));
          }
        }
      }
    }
    return cleanedPredicates;
  }

  private static void cleanVarToEntities(Map<Term, List<Integer>> varToEntities,
      Sentence sentence) {
    for (Term key : varToEntities.keySet()) {
      List<Integer> entityIds = new ArrayList<>();
      for (int wordId : varToEntities.get(key)) {
        JsonObject entity = sentence.getEntityAtWordIndex(wordId);
        if (entity != null) {
          int entityId = entity.has(SentenceKeys.ENTITY_INDEX)
              ? entity.get(SentenceKeys.ENTITY_INDEX).getAsInt()
              : entity.get(SentenceKeys.START).getAsInt();
          entityIds.add(entityId);
        }
      }
      if (entityIds.size() == 0)
        entityIds.add(varToEntities.get(key).get(0));
      varToEntities.put(key, entityIds);
    }
  }

  public static void traverse(LogicalExpression parse,
      List<Literal> predicates) {
    if (parse instanceof Lambda) {
      traverse(((Lambda) parse).getBody(), predicates);
    } else if (parse instanceof Literal) {
      if (((Literal) parse).getPredicate().toString().startsWith("p_")) {
        // System.out.println(parse);
        predicates.add((Literal) parse);
        ((Literal) parse).getArg(0).hashCode();
      } else {
        for (int i = 0; i < ((Literal) parse).numArgs(); i++) {
          traverse(((Literal) parse).getArg(i), predicates);
        }
      }
    }
  }

  /*-
  (p_EVENT_w-2-nominate:u $0:<a,e>)
  (p_TYPE_w-3-anderson:u $0:<a,e>)
  (p_EVENT_w-3-anderson:u $0:<a,e>)
  (p_EVENT.ENTITY_arg2:b $0:<a,e> $1:<a,e>)
  (p_TYPE_w-5-judge:u $0:<a,e>)
  (p_EVENT_w-5-judge:u $0:<a,e>)
  (p_TYPE_w-8-court:u $0:<a,e>)
  (p_EVENT_w-8-court:u $0:<a,e>)
  (p_EVENT.ENTITY_l-nmod.w-6-of:b $0:<a,e> $1:<a,e>)
  (p_EVENT.ENTITY_l-nmod.w-4-as:b $0:<a,e> $1:<a,e>)
  (p_TYPE_w-1-bush:u $0:<a,e>)
  (p_EVENT_w-1-bush:u $0:<a,e>)
  (p_EVENT.ENTITY_arg1:b $0:<a,e> $1:<a,e>)*/

  private static void process(List<Literal> predicates,
      Map<Term, List<Integer>> varToEvents,
      Map<Term, List<Integer>> varToEntities, Sentence sentence,
      LogicalExpression parse, boolean lexicalizePredicates) {
    if (parse instanceof Lambda) {
      process(predicates, varToEvents, varToEntities, sentence,
          ((Lambda) parse).getBody(), lexicalizePredicates);
    } else if (parse instanceof Literal) {
      if (((LogicalConstant) ((Literal) parse).getPredicate()).getBaseName()
          .startsWith("p_")) {
        predicates.add(((Literal) parse));
        processPredicate(((Literal) parse), varToEvents, varToEntities);
      } else {
        for (int i = 0; i < ((Literal) parse).numArgs(); i++) {
          process(predicates, varToEvents, varToEntities, sentence,
              ((Literal) parse).getArg(i), lexicalizePredicates);
        }
      }
    }
  }

  static Pattern eventIdPattern =
      new Pattern(String.format("%sw-([0-9]+)-.*", PredicateKeys.EVENT_PREFIX));
  static Pattern typeIdPattern =
      new Pattern(String.format("%sw-([0-9]+)-.*", PredicateKeys.TYPE_PREFIX));

  private static void processPredicate(Literal literal,
      Map<Term, List<Integer>> varToEvents,
      Map<Term, List<Integer>> varToEntities) {
    String predicate = ((LogicalConstant) literal.getPredicate()).getBaseName();
    if (predicate.startsWith(PredicateKeys.EVENT_PREFIX)) {
      // (p_EVENT_w-2-nominate:u $0:<a,e>)
      Matcher matcher = eventIdPattern.matcher(predicate);
      matcher.matches();
      int eventId = Integer.parseInt(matcher.group(1));
      Term key = (Term) literal.getArg(0);
      varToEvents.putIfAbsent(key, new ArrayList<>());
      varToEvents.get(key).add(eventId);
    } else if (predicate.startsWith(PredicateKeys.TYPE_PREFIX)) {
      Matcher matcher = typeIdPattern.matcher(predicate);
      matcher.matches();
      int typeId = Integer.parseInt(matcher.group(1));
      Term key = (Term) literal.getArg(0);
      varToEntities.putIfAbsent(key, new ArrayList<>());
      varToEntities.get(key).add(typeId);
    }
  }
}
