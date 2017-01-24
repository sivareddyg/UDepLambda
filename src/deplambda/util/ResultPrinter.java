package deplambda.util;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import deplambda.others.NlpPipeline;
import deplambda.others.SentenceKeys;
import deplambda.util.LogicalExpressionSimpleIndenter.Printer;
import edu.cornell.cs.nlp.spf.mr.lambda.FlexibleTypeComparator;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.SimpleLogicalExpressionReader;
import edu.cornell.cs.nlp.spf.mr.language.type.MutableTypeRepository;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;


public class ResultPrinter {

	static String read(InputStream in) {
		String result = "";
		BufferedReader r = new BufferedReader(new InputStreamReader(in));
		try {
			String line;
			while (true) {
				line = r.readLine();
				if (line==null) break;
				result +=line +"\n";
			}
		} catch (Exception z) {

		}
		return result;
	}
	static String cleanString(String s) {
		if (s.startsWith("\"") && s.endsWith("\"")) {
			s =s.substring(1, s.length()-1);
		}
		return s;
	}
	static int numPerLine = 4;
	static void printJsonArray(JsonArray e) {
		System.out.print("[");
		int c = 0;
		for (int i=0; i < e.size(); i++) {
			if (e.get(i) instanceof JsonArray) {
				System.out.println();
				printJsonArray((JsonArray) e.get(i));
			} else
				System.out.print((c==0?" ":", ") + cleanString(""+e.get(i)));
			c++;
			if (c==numPerLine) {
				System.out.println(","); c=0;
			}
			
		}
		System.out.println("]");

	}
	public static void main(String[] args) {
		 if (args.length == 0 || args.length % 2 != 0) {
		      System.err
		          .println("Specify pipeline arguments, e.g., annotator, languageCode, preprocess.capitalize. See the NlpPipelineTest file.");
		      System.exit(0);
		    }

		    Map<String, String> options = new HashMap<>();
		    for (int i = 0; i < args.length; i += 2) {
		      options.put(args[i], args[i + 1]);
		    }

		 Printer printer = new LogicalExpressionSimpleIndenter.Printer("  ");
		 String defined_types = NlpPipeline.DEPLAMBDA_DEFINED_TYPES_FILE;
		 try {
		  TypeRepository types =
		            new MutableTypeRepository(options.get(defined_types));
		        System.err.println(String.format("%s=%s", defined_types,
		            options.get(defined_types)));

		        LogicLanguageServices.setInstance(new LogicLanguageServices.Builder(
		            types, new FlexibleTypeComparator())
		        		.closeOntology(false)
		        		.setNumeralTypeName("i")
		        	//	.setPrinter(new LogicalExpressionVjIndenter.Printer("  "))
		        		.build());
		 } catch (IOException z) {
			 z.printStackTrace();
		 }
		 String input = read(System.in);
		 JsonParser jsonParser = new JsonParser();
		 JsonObject jsonSentence =
				 jsonParser
				 .parse(input)
				 .getAsJsonObject();
		
		Sentence sentence = new Sentence(jsonSentence);
		 System.out.println("----\nSentence: " + cleanString(""+jsonSentence.get("sentence")));
		 System.out.println("\nDepTree:\n" + TreePrinter.toIndentedString(sentence.getRootNode()));
		 
		JsonPrimitive c = (JsonPrimitive) jsonSentence.get(SentenceKeys.DEPLAMBDA_EXPRESSION);
		String depLambda = cleanString(c.toString());
		
		JsonElement logic = jsonSentence.get(SentenceKeys.DEPENDENCY_LAMBDA);
		
		System.out.println("\nDepLambda Expr:\n " + printer.toString(SimpleLogicalExpressionReader.read(depLambda)));
		System.out.println("\nDepLambda simplified: ");
		printJsonArray((JsonArray) logic);
		
		
		

	}
}
