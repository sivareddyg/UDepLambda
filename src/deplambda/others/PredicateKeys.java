package deplambda.others;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PredicateKeys {
  public static final String EVENT_ENTITY_PREFIX = "p_EVENT.ENTITY_";
  public static final String EVENT_EVENT_PREFIX = "p_EVENT.EVENT_";
  public static final String COUNT_PREFIX = "p_COUNT";
  public static final String TARGET_PREFIX = "p_TARGET";
  public static final String EVENT_PREFIX = "p_EVENT_";
  public static final String TYPE_PREFIX = "p_TYPE_";
  public static final String TYPEMOD_PREFIX = "p_TYPEMOD_";
  public static final String EVENTMOD_PREFIX = "p_EVENTMOD_";
  public static final String CONJ_PREFIX = "p_CONJ";
  public static final String EQUAL_PREFIX = "p_EQUAL";
  
  public static final String QUESTION_PREDICATE = "QUESTION";

  public static Set<String> NAMED_ENTITY_TAGS = new HashSet<>(
      Arrays.asList("PROPN"));

}
