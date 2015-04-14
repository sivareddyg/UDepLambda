package edu.uw.cs.lil.tiny.mr.language.type;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Preconditions;

import edu.uw.cs.lil.tiny.base.LispReader;

/**
 * A mutable {@link TypeRepository} which allows addition of new types.
 * 
 * @author Siva Reddy
 *
 */
public class MutableTypeRepository extends TypeRepository {

  private static String MACRO_TYPE_OPEN = "{";
  private static String MACRO_TYPE_CLOSE = "}";
  private final Map<String, Type> additional_types;
  public final static  Type NULL_TYPE = new TermType("null");
  public final static  Type BACKWARD_APPLICATION = new TermType("ba");
  public final static  Type FORWARD_APPLICATION = new TermType("fa");
  public final static  Type BIND_OPERATION = new TermType("bind");

  public MutableTypeRepository() {
    super();
    additional_types = new ConcurrentHashMap<>();
    additional_types.put(NULL_TYPE.getName(), NULL_TYPE);
    additional_types.put(BACKWARD_APPLICATION.getName(), BACKWARD_APPLICATION);
    additional_types.put(FORWARD_APPLICATION.getName(), FORWARD_APPLICATION);
    additional_types.put(BIND_OPERATION.getName(), BIND_OPERATION);
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
  public MutableTypeRepository(File typesFile) throws IOException {
    super();
    additional_types = new ConcurrentHashMap<>();
    additional_types.put(NULL_TYPE.getName(), NULL_TYPE);
    additional_types.put(BACKWARD_APPLICATION.getName(), BACKWARD_APPLICATION);
    additional_types.put(FORWARD_APPLICATION.getName(), FORWARD_APPLICATION);
    additional_types.put(BIND_OPERATION.getName(), BIND_OPERATION);

    BufferedReader br = new BufferedReader(new FileReader(typesFile));
    try {
      String line = br.readLine();
      while (line != null) {
        if (!line.startsWith("#") && !line.trim().equals(""))
          getTypeCreateIfNeeded(line.trim());

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

    if (label.startsWith(MACRO_TYPE_OPEN) && label.endsWith(MACRO_TYPE_CLOSE)) {
      // Define the macro. A macro is identified using {name type}.
      String[] splits =
          label.subSequence(1, label.length() - 1).toString().split("\\s", 2);

      Preconditions.checkArgument(splits.length == 2, "Wrong format: " + label);
      Type macroType = getTypeCreateIfNeeded(splits[1].trim());
      addAdditionalType(splits[0], macroType);
      return macroType;
    } else if (label.startsWith("(")) {
      final LispReader lispReader = new LispReader(new StringReader(label));

      // Label (the name of the type)
      final String current = lispReader.next();
      // The parent type
      final String parentTypeString = lispReader.next();
      final Type parentType = getType(parentTypeString);
      if (parentType instanceof TermType) {
        TermType currentType = new TermType(current, (TermType) parentType);
        additional_types.put(current, currentType);
        return currentType;
      } else {
        throw new IllegalArgumentException(String.format(
            "Parent (%s) of primitive type (%s) must be a primitive type",
            parentType, label));
      }
    }

    return super.getTypeCreateIfNeeded(label);
  };

  public void addAdditionalType(String name, Type type) {
    additional_types.put(name, type);
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
