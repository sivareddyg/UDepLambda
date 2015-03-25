package deplambda.parser;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.google.protobuf.TextFormat;

import deplambda.protos.TransformationRulesProto.RuleGroups;

public class TreeTransformation {
  // Transformation rules that modify the tree.
  private RuleGroups rules;

  public TreeTransformation(String ruleFile) throws FileNotFoundException,
      IOException {
    RuleGroups.Builder rulesBuilder = RuleGroups.newBuilder();
    TextFormat.merge(new FileReader(ruleFile), rulesBuilder);
    rules = rulesBuilder.build();
  }

  public static void main(String[] args) throws FileNotFoundException,
      IOException {
    TreeTransformation parser =
        new TreeTransformation("lib_data/deplambda-lambda-assignment-rules.txt");
  }
}
