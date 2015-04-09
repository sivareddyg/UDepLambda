package edu.uw.cs.lil.tiny.mr.lambda;

import java.util.HashMap;

import edu.uw.cs.lil.tiny.mr.lambda.visitor.Simplify;

/**
 * Similar to {@link LogicalExpressionReader} but the {@link #read(String)}
 * function returns neat logical expressions. The default
 * {@link LogicalExpressionReader#read(String)} expands variables with complex
 * types strangely.
 * 
 * @author Siva Reddy
 *
 */
public class SimpleLogicalExpressionReader {
  public static LogicalExpression read(String string) {
    LogicalExpression exp =
        LogicalExpressionReader.INSTANCE.read(string,
            new HashMap<String, LogicalExpression>(),
            LogicLanguageServices.getTypeRepository(),
            LogicLanguageServices.getTypeComparator());
    return Simplify.of(exp);
  }
}
