package deplambda.util;

import edu.stanford.nlp.trees.Tree;

public class TreePrinter {

	 /**
	   * Appends the printed form of a parse tree (as a bracketed String)
	   * to a {@code StringBuilder}.
	   * The implementation of this may be more efficient than for
	   * {@code toString()} on complex trees.
	   *
	   * @param sb The {@code StringBuilder} to which the tree will be appended
	   * @param printOnlyLabelValue If true, print only the value() of each node's label
	   * @return Returns the {@code StringBuilder} passed in with extra stuff in it
	   * vj: minor modifications to print indented, one entry per line
	   */
	  public static String toIndentedString(Tree tree) {
		  return toStringBuilder(tree, new StringBuilder(), false, "").toString();
	  }
	  static StringBuilder toStringBuilder(Tree tree, StringBuilder sb, 
			  boolean printOnlyLabelValue, String offset) {
	    if (tree.isLeaf()) {
	      if (tree.label() != null) sb.append(printOnlyLabelValue ? tree.label().value() : tree.label());
	      return sb;
	    } 
	    sb.append('(');
	    if (tree.label() != null) {
	    	if (printOnlyLabelValue) {
	    		if (tree.value() != null) sb.append(tree.label().value());
	    		// don't print a null, just nothing!
	    	} else {
	    		sb.append(tree.label());
	    	}
	    }
	    Tree[] kids = tree.children();
	    if (kids != null) {
	    	for (Tree kid : kids) {
	    		if (kid.isLeaf()) sb.append(' '); 
	    		else sb.append('\n').append(offset).append(' ');
	    		toStringBuilder(kid, sb, printOnlyLabelValue,offset + "  ");
	    	}
	    }
	    return sb.append(')');
	  }


}
