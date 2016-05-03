package edu.cornell.cs.nlp.spf.mr.lambda;

import edu.cornell.cs.nlp.spf.mr.lambda.mapping.ScopeMapping;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.Simplify;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;

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
  public static String CONJUNCTION_PREDICATE = "and:<t*,t>";
  
  public static LogicalExpression read(String string) {
    TypeRepository typeRepository = LogicLanguageServices
        .getTypeRepository();
    ITypeComparator typeComparator = LogicLanguageServices
        .getTypeComparator();
    LogicalExpression exp =
        LogicalExpressionReader.INSTANCE.read(string, new ScopeMapping<>(), typeRepository,
            typeComparator);
    return Simplify.of(exp);
  }
}
