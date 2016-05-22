package edu.cornell.cs.nlp.spf.mr.language.type;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Preconditions;

/**
 * A mutable {@link TypeRepository} which allows addition of new types and
 * define macros.
 * 
 * @author Siva Reddy
 *
 */
public class MutableTypeRepository extends TypeRepository {

  private static String MACRO_TYPE_OPEN = "{";
  private static String MACRO_TYPE_CLOSE = "}";
  private final Map<String, Type> additional_types;
  public final static Type NULL_TYPE = new TermType("null");
  public final static Type BACKWARD_APPLICATION = new TermType("ba");
  public final static Type FORWARD_APPLICATION = new TermType("fa");
  public final static Type BIND_OPERATION = new TermType("bind");
  public final static Type BIND_OPERATION_OLD = new TermType("bindold");

  public MutableTypeRepository() {
    super();
    additional_types = new ConcurrentHashMap<>();
    additional_types.put(NULL_TYPE.getName(), NULL_TYPE);
    additional_types.put(BACKWARD_APPLICATION.getName(), BACKWARD_APPLICATION);
    additional_types.put(FORWARD_APPLICATION.getName(), FORWARD_APPLICATION);
    additional_types.put(BIND_OPERATION.getName(), BIND_OPERATION);
    additional_types.put(BIND_OPERATION_OLD.getName(), BIND_OPERATION_OLD);
  }

  /**
   * Loads types from a file. Each line should have only one type defined.
   * Example types are shown below.
   * 
   * <pre>
   * {@code
   * #complex types
   * <e,t>
   * # Heirarchial types
   * (p e)
   * # Macros 
   * {s <p,t>}
   * {np <p,t>}
   * {vp <np,t>}
   * }
   * </pre>
   * 
   * @param typesFile
   * @throws IOException
   */
  public MutableTypeRepository(String typesFile) throws IOException {
    this();

    BufferedReader br = new BufferedReader(new FileReader(typesFile));
    try {
      String line = br.readLine();
      while (line != null) {
        line = line.trim();
        if (!line.startsWith("#") && !line.equals("")) {
          if (line.startsWith(MACRO_TYPE_OPEN)) { // Macro type
            line = line.replace(MACRO_TYPE_CLOSE, "");
            line = line.replace(MACRO_TYPE_OPEN, "");
            String[] parts = line.split(" ");
            Preconditions.checkArgument(parts.length == 2,
                "Wrong macro format: " + line);
            addMacroType(parts[0], parts[1]);
          } else if (line.startsWith("(")) { // Term type with parent
            line = line.replaceAll("[\\(\\)]", "");
            String[] parts = line.split(" ");
            Preconditions.checkArgument(parts.length == 2,
                "Wrong term type format: " + line);
            addTermType(parts[0], getTypeCreateIfNeeded(parts[1]));
          } else { // Term type
            addTermType(line.trim());
          }
        }
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }

  @Override
  public Type getType(String name) {
    if (additional_types != null && additional_types.containsKey(name))
      return additional_types.get(name);
    return super.getType(name);
  }

  @Override
  public Type getTypeCreateIfNeeded(String label) {
    if (additional_types != null && additional_types.containsKey(label))
      return additional_types.get(label);

    return super.getTypeCreateIfNeeded(label);
  };

  public Type addMacroType(String macroName, String macroTypeString) {
    Type macroType = getTypeCreateIfNeeded(macroTypeString);
    additional_types.put(macroName, macroType);
    return macroType;
  }

  public Type addTermType(String label) {
    Type newType = new TermType(label);
    additional_types.put(label, newType);
    return newType;
  }

  public Type addTermType(String label, Type parent) {
    Preconditions.checkArgument(parent instanceof TermType,
        "Parent should be of basic type");
    Type newType = new TermType(label, (TermType) parent);
    additional_types.put(label, newType);
    return newType;
  }

  @Override
  public String toString() {
    final StringBuilder ret = new StringBuilder();
    ret.append(super.toString());
    for (final Map.Entry<String, Type> entry : additional_types.entrySet()) {
      ret.append(entry.getKey());
      ret.append("\t::\t");
      ret.append(entry.getValue().toString());
      ret.append('\n');
    }
    return ret.toString();
  }

  /**
   * Recursively unfolds a type into combination of primitive types.
   * 
   * @param type the type to be unfolded.
   * @return unfolded string version.
   */
  public String unfoldType(Type type) {
    if (type == null)
      return null;

    if (type instanceof TermType) {
      if (((TermType) type).getParent() == null)
        return type.getName();
      else
        return unfoldType(((TermType) type).getParent());
    }

    if (type instanceof RecursiveComplexType) {
      return String.format("<%s*,%s>", unfoldType(getType(type.getDomain()
          .getName())), unfoldType(((RecursiveComplexType) type)
          .getFinalRange()));
    }

    if (type instanceof ComplexType) {
      return String.format("<%s,%s>", unfoldType(type.getDomain()),
          unfoldType(type.getRange()));
    }

    if (type instanceof ArrayType) {
      return String
          .format("%s[]", unfoldType(((ArrayType) type).getBaseType()));
    }
    return null;
  }
}
