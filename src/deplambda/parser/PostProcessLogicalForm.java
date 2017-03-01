package deplambda.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import jregex.Matcher;
import jregex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
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

/**
 * Class to post process a logical form and make it more readable.
 * 
 * @author Siva Reddy
 *
 */
public class PostProcessLogicalForm {

  static Pattern EVENT_ID_PATTERN = new Pattern(String.format(
      "%sw-([0-9]+)-.*", PredicateKeys.EVENT_PREFIX));
  static Pattern TYPE_ID_PATTERN = new Pattern(String.format("%sw-([0-9]+)-.*",
      PredicateKeys.TYPE_PREFIX));
  static Pattern TYPEMOD_ID_PATTERN = new Pattern(String.format(
      "%sw-([0-9]+)-.*", PredicateKeys.TYPEMOD_PREFIX));
  static Pattern EVENTMOD_ID_PATTERN = new Pattern(String.format(
      "%sw-([0-9]+)-.*", PredicateKeys.EVENTMOD_PREFIX));


  /**
   * From a given logical expression, the main predicates are extracted, make
   * them more readable, link the variables to the sources from which they
   * originated.
   * 
   * @param sentence the source sentence
   * @param parse the logical expression to be processed
   * @param lexicalizePredicates lexicalize predicates by appending the event,
   *        e.g., eat(1:e) ^ arg1(1:e , 1:x) becomes eat.arg1(1:e , 1:x)
   * @return the set of main predicates in readable format
   */
  public static Set<String> process(Sentence sentence, LogicalExpression parse,
      boolean lexicalizePredicates) {
    List<Literal> mainPredicates = new ArrayList<>();
    Map<Term, List<Integer>> varToEvents = new HashMap<>();
    Map<Term, List<Integer>> varToEntities = new HashMap<>();
    Map<Term, List<Term>> varToConj = new HashMap<>();
    List<Pair<Term, Term>> equalPairs = new ArrayList<>();
    process(mainPredicates, varToEvents, varToEntities, varToConj, equalPairs,
        sentence, parse);

    // TODO(sivareddyg) handle predicates p_CONJ and p_EQUAL in both varToEvents
    // and varToEntities.
    cleanVarToEntities(varToEntities, sentence);
    cleanVarToEvents(varToEvents, sentence);
    populateConj(varToConj, varToEntities, varToEvents);
    populateEquals(equalPairs, varToEntities, varToEvents);
    Set<String> cleanedPredicates =
        createCleanPredicates(mainPredicates, varToEvents, varToEntities,
            sentence, lexicalizePredicates);
    return cleanedPredicates;
  }

  private static void populateEquals(List<Pair<Term, Term>> equalPairs,
      Map<Term, List<Integer>> varToEntities,
      Map<Term, List<Integer>> varToEvents) {
    Map<Term, Set<Term>> varToAllEquals = new HashMap<>();
    for (Pair<Term, Term> varPair : equalPairs) {
      Set<Term> unifiedSet = new HashSet<>();
      unifiedSet.add(varPair.getLeft());
      unifiedSet.add(varPair.getRight());
      unifiedSet.addAll(varToAllEquals.getOrDefault(varPair.getLeft(),
          new HashSet<>()));
      unifiedSet.addAll(varToAllEquals.getOrDefault(varPair.getRight(),
          new HashSet<>()));
      for (Term var : unifiedSet) {
        varToAllEquals.put(var, unifiedSet);
      }
    }

    Set<Set<Term>> unifiedSets = Sets.newHashSet(varToAllEquals.values());
    for (Set<Term> unifiedSet : unifiedSets) {
      Set<Integer> entityIndices = new HashSet<>();
      unifiedSet.forEach(x -> entityIndices.addAll(varToEntities.getOrDefault(
          x, new ArrayList<>())));
      List<Integer> entityIndicesList = new ArrayList<>(entityIndices);
      unifiedSet.forEach(x -> varToEntities.put(x, entityIndicesList));

      Set<Integer> eventIndices = new HashSet<>();
      unifiedSet.forEach(x -> eventIndices.addAll(varToEvents.getOrDefault(x,
          new ArrayList<>())));
      List<Integer> eventIndicesList = new ArrayList<>(eventIndices);
      unifiedSet.forEach(x -> varToEvents.put(x, eventIndicesList));
    }
  }

  /**
   * Unfolds a conjunction variable to basic variables and populates this info
   * into entityToVar and entityToEvent.
   * 
   * @param varToConj
   * @param varToEntities
   * @param varToEvents
   */
  private static void populateConj(Map<Term, List<Term>> varToConj,
      Map<Term, List<Integer>> varToEntities,
      Map<Term, List<Integer>> varToEvents) {
    for (Term conjVar : varToConj.keySet()) {
      List<Term> terminalVars = new ArrayList<>();
      getTerminalVars(terminalVars, varToConj, conjVar);
      List<Integer> emptyList = new ArrayList<>();

      List<Integer> terminalEntitySources = new ArrayList<>();
      terminalVars.forEach(x -> terminalEntitySources.addAll(varToEntities
          .getOrDefault(x, emptyList)));
      varToEntities.put(conjVar, terminalEntitySources);

      List<Integer> terminalEventSources = new ArrayList<>();
      terminalVars.forEach(x -> terminalEventSources.addAll(varToEvents
          .getOrDefault(x, emptyList)));
      varToEvents.put(conjVar, terminalEventSources);
    }
  }

  private static void getTerminalVars(List<Term> terminalVars,
      Map<Term, List<Term>> varToConj, Term conjVar) {
    if (!varToConj.containsKey(conjVar)) {
      terminalVars.add(conjVar);
      return;
    }
    varToConj.get(conjVar).forEach(
        x -> getTerminalVars(terminalVars, varToConj, x));
  }

  /**
   * Makes predicates readable.
   * 
   * @param mainPredicates the main predicates that are to be cleaned, i.e., the
   *        predicates that start with "p_"
   * @param varToEvents a map containing a mapping between event lambda
   *        variables and their source word's index
   * @param varToEntities a map containing a mapping between event lambda
   *        variables and their source word's index
   * @param sentence the source sentence
   * @param lexicalizePredicates lexicalize predicates by appending the event,
   *        e.g., eat(1:e) ^ arg1(1:e , 1:x) becomes eat.arg1(1:e , 1:x)
   * @return the set of cleaned predicates
   */
  private static Set<String> createCleanPredicates(
      List<Literal> mainPredicates, Map<Term, List<Integer>> varToEvents,
      Map<Term, List<Integer>> varToEntities, Sentence sentence,
      boolean lexicalizePredicates) {
    Set<String> cleanedPredicates = new HashSet<>();
    for (Literal predicate : mainPredicates) {
      String basePredicate =
          ((LogicalConstant) predicate.getPredicate()).getBaseName();

      if (basePredicate.startsWith(PredicateKeys.EVENT_ENTITY_PREFIX)) {
        // (p_EVENT.ENTITY_arg1:b $0:<a,e> $1:<a,e>)
        // (p_EVENT.ENTITY_l-nmod.w-4-as:b $0:<a,e> $1:<a,e>)
        String cleanedPredicate =
            basePredicate.substring(PredicateKeys.EVENT_ENTITY_PREFIX.length());
        cleanedPredicate = getCleanedBasePredicate(cleanedPredicate);
        Term eventTerm = (Term) predicate.getArg(0);
        Term entityTerm = (Term) predicate.getArg(1);
        if (!varToEvents.containsKey(eventTerm)
            || !varToEntities.containsKey(entityTerm))
          continue;
        for (int eventIndex : varToEvents.get(eventTerm)) {
          String lexicalizedEvent =
              !lexicalizePredicates || isNamedEntity(sentence, eventIndex) ? ""
                  : sentence.getLemma(eventIndex) + ".";
          for (int entityIndex : varToEntities.get(entityTerm)) {
            cleanedPredicates.add(String.format("%s%s(%d:e , %s)",
                lexicalizedEvent, cleanedPredicate, eventIndex,
                getEntityVar(sentence, entityIndex)));
          }
        }
      } else if (basePredicate.startsWith(PredicateKeys.EVENT_EVENT_PREFIX)) {
        // (p_EVENT.EVENT_arg1:b $0:<a,e> $1:<a,e>)
        String cleanedPredicate =
            basePredicate.substring(PredicateKeys.EVENT_EVENT_PREFIX.length());
        cleanedPredicate = getCleanedBasePredicate(cleanedPredicate);
        Term eventTerm = (Term) predicate.getArg(0);
        Term argTerm = (Term) predicate.getArg(1);
        if (!varToEvents.containsKey(eventTerm)
            || !varToEvents.containsKey(argTerm))
          continue;
        for (int eventIndex : varToEvents.get(eventTerm)) {
          String lexicalizedEvent =
              !lexicalizePredicates || isNamedEntity(sentence, eventIndex) ? ""
                  : sentence.getLemma(eventIndex) + ".";
          for (int argIndex : varToEvents.get(argTerm)) {
            cleanedPredicates.add(String.format("%s%s(%d:e , %d:e)",
                lexicalizedEvent, cleanedPredicate, eventIndex, argIndex));
          }
        }
      } else if (basePredicate.startsWith(PredicateKeys.COUNT_PREFIX)) {
        // (p_COUNT:b $0:<a,e> $1:<a,e>)
        Term countTerm = (Term) predicate.getArg(0);
        Term resultTerm = (Term) predicate.getArg(1);
        if (!varToEntities.containsKey(countTerm)
            || !varToEntities.containsKey(resultTerm))
          continue;
        for (int countTermIndex : varToEntities.get(countTerm)) {
          for (int resultTermIndex : varToEntities.get(resultTerm)) {
            cleanedPredicates.add(String.format("COUNT(%d:x , %d:x)",
                countTermIndex, resultTermIndex));
          }
        }
      } else if (!lexicalizePredicates
          && basePredicate.startsWith(PredicateKeys.EVENT_PREFIX)) {
        // (p_EVENT_w-5-judge:u $0:<a,e>)
        String cleanedPredicate =
            basePredicate.substring(PredicateKeys.EVENT_PREFIX.length());
        cleanedPredicate = getCleanedBasePredicate(cleanedPredicate);

        Term eventTerm = (Term) predicate.getArg(0);
        if (!varToEvents.containsKey(eventTerm))
          continue;
        for (int eventIndex : varToEvents.get(eventTerm)) {
          if (!isNamedEntity(sentence, eventIndex)) {
            cleanedPredicates.add(String.format("%s(%d:e)", cleanedPredicate,
                eventIndex));
          }
        }
      } else if (basePredicate.startsWith(PredicateKeys.TYPE_PREFIX)) {
        // (p_TYPE_w-8-court:u $0:<a,e>)
        Matcher matcher = TYPE_ID_PATTERN.matcher(basePredicate);
        matcher.matches();
        int typeIsFromIndex = Integer.parseInt(matcher.group(1)) - 1;

        String cleanedPredicate =
            basePredicate.substring(PredicateKeys.TYPE_PREFIX.length());
        cleanedPredicate = getCleanedBasePredicate(cleanedPredicate);

        Term entityTerm = (Term) predicate.getArg(0);
        if (!varToEntities.containsKey(entityTerm))
          continue;
        for (int entityIndex : varToEntities.get(entityTerm)) {
          if (!isNamedEntity(sentence, entityIndex)
              || typeIsFromIndex != entityIndex) {
            cleanedPredicates.add(String.format("%s(%d:s , %s)",
                cleanedPredicate, typeIsFromIndex,
                getEntityVar(sentence, entityIndex)));
          }
        }
      } else if (basePredicate.startsWith(PredicateKeys.TYPEMOD_PREFIX)) {
        // (p_TYPEMOD_w-8-red:u $0:<a,e>)
        Matcher matcher = TYPEMOD_ID_PATTERN.matcher(basePredicate);
        matcher.matches();
        int typeIsFromIndex = Integer.parseInt(matcher.group(1)) - 1;

        String cleanedPredicate =
            basePredicate.substring(PredicateKeys.TYPEMOD_PREFIX.length());
        cleanedPredicate = getCleanedBasePredicate(cleanedPredicate);

        Term entityTerm = (Term) predicate.getArg(0);
        if (!varToEntities.containsKey(entityTerm))
          continue;
        for (int entityIndex : varToEntities.get(entityTerm)) {
          if (!isNamedEntity(sentence, entityIndex)
              || typeIsFromIndex != entityIndex) {
            cleanedPredicates.add(String.format("%s(%d:s , %s)",
                cleanedPredicate, typeIsFromIndex,
                getEntityVar(sentence, entityIndex)));
          }
        }
      } else if (basePredicate.startsWith(PredicateKeys.EVENTMOD_PREFIX)) {
        // (p_EVENTMOD_w-8-red:u $0:<a,e>)
        Matcher matcher = EVENTMOD_ID_PATTERN.matcher(basePredicate);
        matcher.matches();
        int typeIsFromIndex = Integer.parseInt(matcher.group(1)) - 1;

        String cleanedPredicate =
            basePredicate.substring(PredicateKeys.EVENTMOD_PREFIX.length());
        cleanedPredicate = getCleanedBasePredicate(cleanedPredicate);

        Term argTerm = (Term) predicate.getArg(0);
        if (!varToEvents.containsKey(argTerm))
          continue;
        for (int eventIndex : varToEvents.get(argTerm)) {
          cleanedPredicates.add(String.format("%s(%d:s , %d:e)",
              cleanedPredicate, typeIsFromIndex, eventIndex));
        }
      } else if (basePredicate.startsWith(PredicateKeys.TARGET_PREFIX)) {
        // (p_TARGET:u $0:<a,e>)
        Term entityTerm = (Term) predicate.getArg(0);
        if (!varToEntities.containsKey(entityTerm))
          continue;
        for (int entityIndex : varToEntities.get(entityTerm)) {
          if (!isNamedEntity(sentence, entityIndex))
            cleanedPredicates.add(String.format("%s(%s)",
                PredicateKeys.QUESTION_PREDICATE,
                getEntityVar(sentence, entityIndex)));
        }
      }
    }
    return cleanedPredicates;
  }

  /**
   * Convert entity variables that are named entities to readable format.
   * 
   * @param sentence the source sentence
   * @param entityIndex the source entity variable
   * @return if the entity variable is a named entity, returns
   *         index:named_entity, else returns index:x
   */
  private static String getEntityVar(Sentence sentence, int entityIndex) {
    if (!isNamedEntity(sentence, entityIndex)) {
      return String.format("%d:x", entityIndex);
    } else {
      return String.format("%d:m.%s", entityIndex,
          getEntityName(sentence, entityIndex));
    }
  }

  private static String getEntityName(Sentence sentence, int entityIndex) {
    JsonObject entityObj = sentence.getEntityAtWordIndex(entityIndex);
    if (entityObj == null || !entityObj.has(SentenceKeys.PHRASE))
      return sentence.getLemma(entityIndex);
    else
      return entityObj.get(SentenceKeys.PHRASE).getAsString().replace(" ", "_");
  }
  
  /**
   * Removes jargon from the base predicate, e.g. p_EVENT_w-eat becomes eat
   * 
   * @param basePredicate the base predicate to be cleaned
   * @return the cleaned base predicate
   */
  private static String getCleanedBasePredicate(String basePredicate) {
    Iterable<String> parts = Splitter.on(".").split(basePredicate);
    List<String> cleanedParts = new ArrayList<>();
    for (String part : parts) {
      part = part.replaceFirst("^l-", "");
      part = part.replaceFirst("^w-[0-9]+-", "");
      cleanedParts.add(part);
    }
    String cleanedPredicate = Joiner.on(".").join(cleanedParts);
    return cleanedPredicate;
  }

  /**
   * Checks if the word at index is a named entity in the input sentence.
   * 
   * @param sentence
   * @param index
   * @return
   */
  private static boolean isNamedEntity(Sentence sentence, int index) {
    return sentence.getEntityAtWordIndex(index) != null
        || PredicateKeys.NAMED_ENTITY_TAGS.contains(sentence.getWords()
            .get(index).getAsJsonObject().get(SentenceKeys.POS_KEY)
            .getAsString());
  }

  /**
   * Returns the main predicates, i.e., the predicates that start with p_.
   * 
   * @param parse
   * @param predicates
   */
  public static void getMainPredicates(LogicalExpression parse,
      List<Literal> predicates) {
    if (parse instanceof Lambda) {
      getMainPredicates(((Lambda) parse).getBody(), predicates);
    } else if (parse instanceof Literal) {
      if (((Literal) parse).getPredicate().toString().startsWith("p_")) {
        // System.out.println(parse);
        predicates.add((Literal) parse);
        ((Literal) parse).getArg(0).hashCode();
      } else {
        for (int i = 0; i < ((Literal) parse).numArgs(); i++) {
          getMainPredicates(((Literal) parse).getArg(i), predicates);
        }
      }
    }
  }

  /**
   * Creates a literal expression by appending the given predicates in
   * conjunction, i.e., (and:c expr1 expr2 .. )
   * 
   * @param predicates
   * @return
   */
  @SuppressWarnings("unused")
  private Literal createLiteralExpresison(List<LogicalExpression> predicates) {
    LogicalExpression[] predicatesList =
        new LogicalExpression[predicates.size()];
    LogicalExpression and = SimpleLogicalExpressionReader.read("and:c");
    return new Literal(and, predicates.toArray(predicatesList));
  }


  /**
   * Populates the list of mainPredicates, along with mapping of variables in
   * the lambda expression to potential sources from which they might have been
   * originated.
   * 
   * @param mainPredicates
   * @param varToEvents
   * @param varToEntities
   * @param sentence
   * @param parse
   */
  private static void process(List<Literal> mainPredicates,
      Map<Term, List<Integer>> varToEvents,
      Map<Term, List<Integer>> varToEntities, Map<Term, List<Term>> varToConj,
      List<Pair<Term, Term>> equalPairs, Sentence sentence,
      LogicalExpression parse) {
    if (parse instanceof Lambda) {
      process(mainPredicates, varToEvents, varToEntities, varToConj,
          equalPairs, sentence, ((Lambda) parse).getBody());
    } else if (parse instanceof Literal) {
      if (((LogicalConstant) ((Literal) parse).getPredicate()).getBaseName()
          .startsWith("p_")) {
        mainPredicates.add(((Literal) parse));
        processPredicate(((Literal) parse), varToEvents, varToEntities,
            varToConj, equalPairs);
      } else {
        for (int i = 0; i < ((Literal) parse).numArgs(); i++) {
          process(mainPredicates, varToEvents, varToEntities, varToConj,
              equalPairs, sentence, ((Literal) parse).getArg(i));
        }
      }
    }
  }


  /*-
   * 
   * 
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

  /**
   * Maps a variable to its potential source, e.g., if the predicate is
   * (p_TYPE_w-1-bush:u $0:<a,e>), then $0 is likely to originate from bush.
   * Similarly for events.
   * 
   * @param literal
   * @param varToEvents
   * @param varToEntities
   */
  private static void processPredicate(Literal literal,
      Map<Term, List<Integer>> varToEvents,
      Map<Term, List<Integer>> varToEntities, Map<Term, List<Term>> varToConj,
      List<Pair<Term, Term>> equalPairs) {
    String predicate = ((LogicalConstant) literal.getPredicate()).getBaseName();
    if (predicate.startsWith(PredicateKeys.EVENT_PREFIX)) {
      // (p_EVENT_w-2-nominate:u $0:<a,e>)
      Matcher matcher = EVENT_ID_PATTERN.matcher(predicate);
      matcher.matches();
      int eventId = Integer.parseInt(matcher.group(1));
      Term key = (Term) literal.getArg(0);
      varToEvents.putIfAbsent(key, new ArrayList<>());
      varToEvents.get(key).add(eventId - 1);
    } else if (predicate.startsWith(PredicateKeys.TYPE_PREFIX)) {
      Matcher matcher = TYPE_ID_PATTERN.matcher(predicate);
      matcher.matches();
      int typeId = Integer.parseInt(matcher.group(1));
      Term key = (Term) literal.getArg(0);
      varToEntities.putIfAbsent(key, new ArrayList<>());
      varToEntities.get(key).add(typeId - 1);
    } else if (predicate.startsWith(PredicateKeys.EQUAL_PREFIX)) {
      Term arg1 = (Term) literal.getArg(0);
      Term arg2 = (Term) literal.getArg(1);
      equalPairs.add(Pair.of(arg1, arg2));
    } else if (predicate.startsWith(PredicateKeys.CONJ_PREFIX)) {
      Term conj = (Term) literal.getArg(0);
      Term arg1 = (Term) literal.getArg(1);
      Term arg2 = (Term) literal.getArg(2);
      varToConj.putIfAbsent(conj, new ArrayList<>());
      varToConj.get(conj).add(arg1);
      varToConj.get(conj).add(arg2);
    }
  }

  /**
   * Maps entity variables to the likely source, e.g., the predicates
   * (p_TYPE_w-5-judge:u $0:<a,e>) and (p_TYPE_w-3-anderson:u $0:<a,e>) indicate
   * the 0 could have been originated either from 5 or 3, but since 3 is a named
   * entity, we make it the likely source.
   * 
   * @param varToEntities
   * @param sentence
   */
  private static void cleanVarToEntities(
      Map<Term, List<Integer>> varToEntities, Sentence sentence) {
    for (Term key : varToEntities.keySet()) {
      List<Integer> entityIds = new ArrayList<>();
      for (int wordId : varToEntities.get(key)) {
        JsonObject entity = sentence.getEntityAtWordIndex(wordId);
        if (entity != null) {
          int entityId =
              entity.has(SentenceKeys.ENTITY_INDEX) ? entity.get(
                  SentenceKeys.ENTITY_INDEX).getAsInt() : entity.get(
                  SentenceKeys.START).getAsInt();
          entityIds.add(entityId);
        } else if (isNamedEntity(sentence, wordId)) {
          entityIds.add(wordId);
        }
      }

      if (entityIds.size() > 1) {
        // Select the correct entity source.
        for (Integer entityId : entityIds) {
          if (isKBEntity(sentence, entityId)) {
            entityIds = new ArrayList<>();
            entityIds.add(entityId);
            break;
          }
        }

        // It is not clear what to do in cases when a term matches to two
        // entity sources. Current solution is to just pick the first one.
        if (entityIds.size() > 1) {
          Integer firstEntity = entityIds.get(0);
          entityIds = new ArrayList<>();
          entityIds.add(firstEntity);
        }
      }

      if (entityIds.size() == 0)
        entityIds.add(varToEntities.get(key).get(0));
      varToEntities.put(key, entityIds);
    }
  }

  private static boolean isKBEntity(Sentence sentence, Integer index) {
    return sentence.getEntityAtWordIndex(index) != null;
  }

  /**
   * Maps event variables to the likely source, e.g., the predicates
   * (p_EVENT_w-5-judge:u $0:<a,e>) and (p_EVENT_w-3-anderson:u $0:<a,e>)
   * indicate the event corresponding to 0 could have been originated either
   * from 5 or 3, but since 5 is not a named entity, we make it the likely
   * source.
   * 
   * @param varToEntities
   * @param sentence
   */
  private static void cleanVarToEvents(Map<Term, List<Integer>> varToEvents,
      Sentence sentence) {
    for (Term key : varToEvents.keySet()) {
      List<Integer> eventIds = new ArrayList<>();
      for (int wordId : varToEvents.get(key)) {
        if (!isNamedEntity(sentence, wordId)) {
          // It is not clear what to do in cases when a term matches to two
          // event sources. Current solution is to just pick the first one.
          eventIds.add(wordId);
          break;
        }
      }
      if (eventIds.size() == 0)
        eventIds.add(varToEvents.get(key).get(0));
      varToEvents.put(key, eventIds);
    }
  }
}
