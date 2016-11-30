gq_plain_forest_to_conll:
	cat working/graphquestions.training.plain.forest.json \
		| java -cp bin:lib/* deplambda.others.ConvertGraphParserSentenceToConll \
		| sed -e "s/-lrb-/\(/g" \
		| sed -e "s/-rrb-/\)/g" \
		| sed -e "s/-RRB-/SYM/g" \
		| sed -e "s/-LRB-/SYM/g" \
		| python scripts/webquestions/make_conll_column_lowercase.py 1 \
		> working/graphquestions.training.conll
	cat working/graphquestions.testing.plain.forest.json \
		| java -cp bin:lib/* deplambda.others.ConvertGraphParserSentenceToConll \
		| sed -e "s/-lrb-/\(/g" \
		| sed -e "s/-rrb-/\)/g" \
		| sed -e "s/-RRB-/SYM/g" \
		| sed -e "s/-LRB-/SYM/g" \
		| python scripts/webquestions/make_conll_column_lowercase.py 1 \
		> working/graphquestions.testing.conll

gq_stanford_parse_conll:
	cat working/graphquestions.training.conll \
		| sed -e 's/_\t_\t_\t_$$/0\troot\t_\t_/g' \
		> working/graphquestions.training.conll.tmp
	java -cp .:lib/* edu.stanford.nlp.parser.nndep.DependencyParser \
		-model lib_data/ud-models-v1.3/en/neural-parser/en-lowercase-glove50.lower.nndep.model.txt.gz \
		-testFile working/graphquestions.training.conll.tmp \
		-outFile working/graphquestions.training.stanford.parsed.conll
	rm working/graphquestions.training.conll.tmp
	cat working/graphquestions.testing.conll \
		| sed -e 's/_\t_\t_\t_$$/0\troot\t_\t_/g' \
		> working/graphquestions.testing.conll.tmp
	java -cp .:lib/* edu.stanford.nlp.parser.nndep.DependencyParser \
		-model lib_data/ud-models-v1.3/en/neural-parser/en-lowercase-glove50.lower.nndep.model.txt.gz \
		-testFile working/graphquestions.testing.conll.tmp \
		-outFile working/graphquestions.testing.stanford.parsed.conll
	rm working/graphquestions.testing.conll.tmp

gq_merge_parsed_conll_with_forest:
	python scripts/webquestions/copy_column_from_to.py 2:2 \
		working/graphquestions.training.conll \
		working/graphquestions.training.stanford.parsed.conll \
		> working/graphquestions.training.stanford.parsed.tmp.conll
	java -cp bin:lib/* deplambda.others.MergeConllAndGraphParserFormats \
		working/graphquestions.training.stanford.parsed.tmp.conll \
		working/graphquestions.training.plain.forest.json \
		| java -cp bin:lib/* deplambda.others.NlpPipeline \
		annotators tokenize,ssplit \
		ssplit.newlineIsSentenceBreak always \
		tokenize.whitespace true \
		postprocess.removeMultipleRoots true \
		> working/graphquestions.training.stanford.forest.json 
	rm working/graphquestions.training.stanford.parsed.tmp.conll
	python scripts/webquestions/copy_column_from_to.py 2:2 \
		working/graphquestions.testing.conll \
		working/graphquestions.testing.stanford.parsed.conll \
		> working/graphquestions.testing.stanford.parsed.tmp.conll
	java -cp bin:lib/* deplambda.others.MergeConllAndGraphParserFormats \
		working/graphquestions.testing.stanford.parsed.tmp.conll \
		working/graphquestions.testing.plain.forest.json \
		| java -cp bin:lib/* deplambda.others.NlpPipeline \
		annotators tokenize,ssplit \
		ssplit.newlineIsSentenceBreak always \
		tokenize.whitespace true \
		postprocess.removeMultipleRoots true \
		> working/graphquestions.testing.stanford.forest.json 
	rm working/graphquestions.testing.stanford.parsed.tmp.conll

gq_deplambda_forest:
	cat working/graphquestions.training.stanford.forest.json \
		| java -cp bin:lib/* deplambda.cli.RunForestTransformer \
		-definedTypesFile lib_data/ud.types.txt \
		-treeTransformationsFile lib_data/ud-tree-transformation-rules.proto.txt \
		-relationPrioritiesFile lib_data/ud-relation-priorities.proto.txt \
		-lambdaAssignmentRulesFile lib_data/ud-lambda-assignment-rules.proto.txt \
		-nthreads 20  \
		| python scripts/dependency_semantic_parser/remove_spurious_predicates_from_forest.py \
		> working/graphquestions.training.stanford.deplambda.forest.json
	cat working/graphquestions.testing.stanford.forest.json \
		| java -cp bin:lib/* deplambda.cli.RunForestTransformer \
		-definedTypesFile lib_data/ud.types.txt \
		-treeTransformationsFile lib_data/ud-tree-transformation-rules.proto.txt \
		-relationPrioritiesFile lib_data/ud-relation-priorities.proto.txt \
		-lambdaAssignmentRulesFile lib_data/ud-lambda-assignment-rules.proto.txt \
		-nthreads 20  \
		| python scripts/dependency_semantic_parser/remove_spurious_predicates_from_forest.py \
		> working/graphquestions.testing.stanford.deplambda.forest.json

gq_deplambda_with_hyperexpand.32.stem:
	rm -rf ../working/gq_deplambda_with_hyperexpand.32.stem
	mkdir -p ../working/gq_deplambda_with_hyperexpand.32.stem
	java -Xms2048m -cp bin:lib/* in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/sempre2_schema_gp_format.txt \
	-relationTypesFile lib_data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 10 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 1 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-questionTypeGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag false \
	-useGoldRelations false \
	-allowMerging false \
	-handleEventEventEdges false \
	-useExpand false \
	-useHyperExpand true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight 0.0 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
    	-evaluateOnlyTheFirstBest false \
	-supervisedCorpus "working/graphquestions.training.stanford.deplambda.forest.json" \
	-contentWordPosTags "NOUN;VERB;ADJ;ADP;ADV;PRON" \
	-devFile working/graphquestions.testing.stanford.deplambda.forest.json \
	-logFile ../working/gq_deplambda_with_hyperexpand.32.stem/all.log.txt \
	> ../working/gq_deplambda_with_hyperexpand.32.stem/all.txt

