package deplambda.others;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PredicateKeys {
  public static final String EVENT_ENTITY_PREFIX = "p_EVENT.ENTITY_";
  public static final String EVENT_EVENT_PREFIX = "p_EVENT.EVENT_";
  public static String EVENT_PREFIX = "p_EVENT_";
  public static String TYPE_PREFIX = "p_TYPE_";

  public static Set<String> NAMED_ENTITY_TAGS = new HashSet<>(
      Arrays.asList("PROPN"));

}
