# Compiles protos and creates source code.
compile_protos:
	protoc -I=protos --java_out=src protos/transformation-rules.proto

# Full commands of a language
create_data_%:
	make create_webquestions_$*
	make entity_annotate_webquestions_$*

# Run BoW experiments
run_bow_experiments_%:
	make extract_gold_graphs_bow_dev_$*
	make extract_gold_graphs_bow_$*
	make bow_supervised_without_merge_without_expand_$*

# Run dependency experiments
run_dependency_experiments_%:
	make extract_gold_graphs_dependency_dev_$*
	make extract_gold_graphs_dependency_$*
	make dependency_with_merge_without_expand_$*
	make test_dependency_with_merge_without_expand_$*
	make dependency_without_merge_without_expand_$*

# Data Preparation

# Parse WebQuestions
create_webquestions_en:
	cat data/webquestions/en/webquestions.examples.test.json \
		| python scripts/webquestions/convert_to_one_sentence_per_line.py \
		| python scripts/webquestions/add_gold_mid_using_gold_url.py data/freebase/mid_to_key.txt.gz \
		| java -cp lib/*: in.sivareddy.scripts.AddGoldRelationsToWebQuestionsData localhost data/freebase/schema/all_domains_schema.txt \
		> data/webquestions/en/webquestions.test.json
	cat data/webquestions/en/webquestions.examples.train.json \
		| python scripts/webquestions/convert_to_one_sentence_per_line.py \
		| python scripts/webquestions/extract_subset.py data/webquestions/webquestions_sentences.train.txt \
		| python scripts/webquestions/add_gold_mid_using_gold_url.py data/freebase/mid_to_key.txt.gz \
		| java -cp lib/*: in.sivareddy.scripts.AddGoldRelationsToWebQuestionsData localhost data/freebase/schema/all_domains_schema.txt \
		> data/webquestions/en/webquestions.train.json
	cat data/webquestions/en/webquestions.examples.train.json \
		| python scripts/webquestions/convert_to_one_sentence_per_line.py \
		| python scripts/webquestions/extract_subset.py data/webquestions/webquestions_sentences.dev.txt  \
		| python scripts/webquestions/add_gold_mid_using_gold_url.py data/freebase/mid_to_key.txt.gz \
		| java -cp lib/*: in.sivareddy.scripts.AddGoldRelationsToWebQuestionsData localhost data/freebase/schema/all_domains_schema.txt \
		> data/webquestions/en/webquestions.dev.json

create_webquestions_%:
	cat data/webquestions/en/webquestions.train.json \
		| python scripts/webquestions/merge_with_english.py \
		data/webquestions/$*/webquestions.examples.train.utterances_$* \
		data/webquestions/$*/webquestions.examples.train.utterances \
		> data/webquestions/$*/webquestions.train.json
	cat data/webquestions/en/webquestions.dev.json \
		| python scripts/webquestions/merge_with_english.py \
		data/webquestions/$*/webquestions.examples.train.utterances_$* \
		data/webquestions/$*/webquestions.examples.train.utterances \
		> data/webquestions/$*/webquestions.dev.json
	cat data/webquestions/en/webquestions.test.json \
		| python scripts/webquestions/merge_with_english.py \
		data/webquestions/$*/webquestions.examples.test.utterances_$* \
		data/webquestions/$*/webquestions.examples.test.utterances \
		> data/webquestions/$*/webquestions.test.json

entity_annotate_webquestions_%:
	cat data/webquestions/$*/webquestions.train.json \
		| java -cp bin:lib/* deplambda.others.NlpPipeline \
		annotators tokenize,ssplit,pos,lemma \
		ssplit.newlineIsSentenceBreak always \
		languageCode $* \
		pos.model lib_data/utb-models/$*/pos-tagger/utb-caseless-$*-bidirectional-glove-distsim-lower.full.tagger \
		| java -cp lib/*:bin in.sivareddy.scripts.NounPhraseAnnotator $*_ud \
		> working/$*-webquestions.train.json
	cat data/webquestions/$*/webquestions.dev.json \
		| java -cp bin:lib/* deplambda.others.NlpPipeline \
		annotators tokenize,ssplit,pos,lemma \
		ssplit.newlineIsSentenceBreak always \
		languageCode $* \
		pos.model lib_data/utb-models/$*/pos-tagger/utb-caseless-$*-bidirectional-glove-distsim-lower.full.tagger \
		| java -cp lib/*:bin in.sivareddy.scripts.NounPhraseAnnotator $*_ud \
		> working/$*-webquestions.dev.json
	cat data/webquestions/$*/webquestions.test.json \
		| java -cp bin:lib/* deplambda.others.NlpPipeline \
		annotators tokenize,ssplit,pos,lemma \
		ssplit.newlineIsSentenceBreak always \
		languageCode $* \
		pos.model lib_data/utb-models/$*/pos-tagger/utb-caseless-$*-bidirectional-glove-distsim-lower.full.tagger \
		| java -cp lib/*:bin in.sivareddy.scripts.NounPhraseAnnotator $*_ud \
		> working/$*-webquestions.test.json

	# Entity Annotations
	cat working/$*-webquestions.dev.json \
		| java -cp lib/*:bin in.sivareddy.graphparser.cli.RankMatchedEntitiesCli \
		--useKG false \
		--apiKey AIzaSyDj-4Sr5TmDuEA8UVOd_89PqK87GABeoFg \
		--langCode $* \
		> working/$*-webquestions.dev.ranked.json
	cat working/$*-webquestions.test.json \
		| java -cp lib/*:bin in.sivareddy.graphparser.cli.RankMatchedEntitiesCli \
		--useKG false \
		--apiKey AIzaSyDj-4Sr5TmDuEA8UVOd_89PqK87GABeoFg \
		--langCode $* \
		> working/$*-webquestions.test.ranked.json
	cat working/$*-webquestions.train.json \
		| java -cp lib/*:bin in.sivareddy.graphparser.cli.RankMatchedEntitiesCli \
		--useKG false \
		--apiKey AIzaSyDj-4Sr5TmDuEA8UVOd_89PqK87GABeoFg \
		--langCode $* \
		> working/$*-webquestions.train.ranked.json
	# if successful, take backup. Freebase API may stop working anytime.
	echo "Overwriting existing files: "
	cp -i working/$*-webquestions.train.ranked.json data/webquestions/$*/webquestions.train.ranked.json
	cp -i working/$*-webquestions.dev.ranked.json data/webquestions/$*/webquestions.dev.ranked.json
	cp -i working/$*-webquestions.test.ranked.json data/webquestions/$*/webquestions.test.ranked.json

evaluate_entity_annotation_upperbound_%:
	cat data/webquestions/$*/webquestions.dev.ranked.json \
		| python lib/graph-parser/scripts/entity-annotation/get_entity_patterns.py

train_entity_annotator_%:
	mkdir -p data/entity-models
	java -cp bin:lib/* deplambda.cli.RunTrainEntityScorer \
		-nthreads 20 \
		-iterations 100 \
		-trainFile data/webquestions/$*/webquestions.train.ranked.json \
		-devFile data/webquestions/$*/webquestions.dev.ranked.json \
		-testFile data/webquestions/$*/webquestions.test.ranked.json \
		-saveToFile data/entity-models/$*-webquestions.ser

disambiguate_entities_%:
	cat data/webquestions/$*/webquestions.dev.ranked.json \
		| java -cp bin:lib/* deplambda.cli.RunEntityDisambiguator \
		-loadModelFromFile data/entity-models/$*-webquestions.ser \
		-endpoint localhost \
		-nthreads 20 \
		-nbestEntities 10 \
		-schema data/freebase/schema/all_domains_schema.txt \
		> data/webquestions/$*/webquestions.dev.disambiguated.json
	cat data/webquestions/$*/webquestions.train.ranked.json \
		| java -cp bin:lib/* deplambda.cli.RunEntityDisambiguator \
		-loadModelFromFile data/entity-models/$*-webquestions.ser \
		-endpoint localhost \
		-nthreads 20 \
		-nbestEntities 10 \
		-schema data/freebase/schema/all_domains_schema.txt \
		> data/webquestions/$*/webquestions.train.disambiguated.json
	cat data/webquestions/$*/webquestions.test.ranked.json \
		| java -cp bin:lib/* deplambda.cli.RunEntityDisambiguator \
		-loadModelFromFile data/entity-models/$*-webquestions.ser \
		-endpoint localhost \
		-nthreads 20 \
		-nbestEntities 10 \
		-schema data/freebase/schema/all_domains_schema.txt \
		> data/webquestions/$*/webquestions.test.disambiguated.json

entity_disambiguation_results_%:
	cat data/webquestions/$*/webquestions.dev.disambiguated.json \
	    | python lib/graph-parser/scripts/entity-annotation/evaluate_entity_annotation.py 

entity_dismabiguated_to_graphparser_forest_ptb:
	cat data/webquestions/ptb/webquestions.dev.disambiguated.json \
        | java -cp lib/*:bin deplambda.util.CreateGraphParserForestFromEntityDisambiguatedSentences \
        annotators tokenize,ssplit,pos,lemma,depparse \
        tokenize.whitespace true \
        ssplit.newlineIsSentenceBreak always \
        languageCode en \
        pos.model lib_data/utb-models/en/pos-tagger/utb-en-bidirectional-glove-distsim-lower.full.tagger \
        depparse.model lib_data/utb-models/en/neural-parser/en-glove50.lower.nndep.model.txt.gz \
    > working/ptb-webquestions.dev.forest.json
	cat data/webquestions/ptb/webquestions.train.disambiguated.json \
        | java -cp lib/*:bin deplambda.util.CreateGraphParserForestFromEntityDisambiguatedSentences \
        annotators tokenize,ssplit,pos,lemma,depparse \
        tokenize.whitespace true \
        ssplit.newlineIsSentenceBreak always \
        languageCode en \
        pos.model lib_data/utb-models/en/pos-tagger/utb-en-bidirectional-glove-distsim-lower.full.tagger \
        depparse.model lib_data/utb-models/en/neural-parser/en-glove50.lower.nndep.model.txt.gz \
    > working/ptb-webquestions.train.forest.json
	cat data/webquestions/ptb/webquestions.test.disambiguated.json \
        | java -cp lib/*:bin deplambda.util.CreateGraphParserForestFromEntityDisambiguatedSentences \
        annotators tokenize,ssplit,pos,lemma,depparse \
        tokenize.whitespace true \
        ssplit.newlineIsSentenceBreak always \
        languageCode en \
        pos.model lib_data/utb-models/en/pos-tagger/utb-en-bidirectional-glove-distsim-lower.full.tagger \
        depparse.model lib_data/utb-models/en/neural-parser/en-glove50.lower.nndep.model.txt.gz \
    > working/ptb-webquestions.test.forest.json


entity_dismabiguated_to_graphparser_forest_%:
	cat data/webquestions/$*/webquestions.dev.disambiguated.json \
		| java -cp lib/*:bin deplambda.util.CreateGraphParserForestFromEntityDisambiguatedSentences \
		annotators tokenize,ssplit,pos,lemma,depparse \
		tokenize.whitespace true \
		ssplit.newlineIsSentenceBreak always \
		languageCode $* \
		pos.model lib_data/utb-models/$*/pos-tagger/utb-$*-bidirectional-glove-distsim-lower.full.tagger \
		depparse.model lib_data/utb-models/$*/neural-parser/$*-glove50.lower.nndep.model.txt.gz \
	> working/$*-webquestions.dev.forest.json
	cat data/webquestions/$*/webquestions.train.disambiguated.json \
		| java -cp lib/*:bin deplambda.util.CreateGraphParserForestFromEntityDisambiguatedSentences \
		annotators tokenize,ssplit,pos,lemma,depparse \
		tokenize.whitespace true \
		ssplit.newlineIsSentenceBreak always \
		languageCode $* \
		pos.model lib_data/utb-models/$*/pos-tagger/utb-$*-bidirectional-glove-distsim-lower.full.tagger \
		depparse.model lib_data/utb-models/$*/neural-parser/$*-glove50.lower.nndep.model.txt.gz \
	> working/$*-webquestions.train.forest.json
	cat data/webquestions/$*/webquestions.test.disambiguated.json \
		| java -cp lib/*:bin deplambda.util.CreateGraphParserForestFromEntityDisambiguatedSentences \
		annotators tokenize,ssplit,pos,lemma,depparse \
		tokenize.whitespace true \
		ssplit.newlineIsSentenceBreak always \
		languageCode $* \
		pos.model lib_data/utb-models/$*/pos-tagger/utb-$*-bidirectional-glove-distsim-lower.full.tagger \
		depparse.model lib_data/utb-models/$*/neural-parser/$*-glove50.lower.nndep.model.txt.gz \
	> working/$*-webquestions.test.forest.json

extract_gold_graphs_bow_dev_%:
	mkdir -p data/gold_graphs/
	cat working/$*-webquestions.dev.forest.json \
    | java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
        data/freebase/schema/all_domains_schema.txt localhost \
        bow_question_graph \
        data/gold_graphs/$*_bow_without_merge_without_expand.dev \
        lib_data/dummy.txt \
        false \
        false \
        > data/gold_graphs/$*_bow_without_merge_without_expand.dev.answers.txt
	cat working/$*-webquestions.dev.forest.json \
    | java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
        data/freebase/schema/all_domains_schema.txt localhost \
        bow_question_graph \
        data/gold_graphs/$*_bow_with_merge_without_expand.dev \
        lib_data/dummy.txt \
        true \
        false \
		> data/gold_graphs/$*_bow_with_merge_without_expand.dev.answers.txt

extract_gold_graphs_bow_%:
	mkdir -p data/gold_graphs/
	cat working/$*-webquestions.train.forest.json \
        working/$*-webquestions.dev.forest.json \
    | java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
        data/freebase/schema/all_domains_schema.txt localhost \
        bow_question_graph \
        data/gold_graphs/$*_bow_without_merge_without_expand.full \
        lib_data/dummy.txt \
        false \
        false \
        > data/gold_graphs/$*_bow_without_merge_without_expand.full.answers.txt
	cat working/$*-webquestions.train.forest.json \
        working/$*-webquestions.dev.forest.json \
    | java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
        data/freebase/schema/all_domains_schema.txt localhost \
        bow_question_graph \
        data/gold_graphs/$*_bow_with_merge_without_expand.full \
        lib_data/dummy.txt \
        true \
        false \
		> data/gold_graphs/$*_bow_with_merge_without_expand.full.answers.txt

extract_gold_graphs_dependency_dev_%:
	cat working/$*-webquestions.dev.forest.json \
		| java -cp lib/*:bin in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
        data/freebase/schema/all_domains_schema.txt localhost \
        dependency_question_graph \
        data/gold_graphs/$*_dependency_without_merge_without_expand.dev \
        lib_data/dummy.txt \
        false \
        false \
        > data/gold_graphs/$*_dependency_without_merge_without_expand.dev.answers.txt
	cat working/$*-webquestions.dev.forest.json \
		| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
        data/freebase/schema/all_domains_schema.txt localhost \
        dependency_question_graph \
        data/gold_graphs/$*_dependency_with_merge_without_expand.dev \
        lib_data/dummy.txt \
        true \
        false \
        > data/gold_graphs/$*_dependency_with_merge_without_expand.dev.answers.txt

extract_gold_graphs_dependency_%:
	cat working/$*-webquestions.train.forest.json \
        working/$*-webquestions.dev.forest.json \
    | java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
        data/freebase/schema/all_domains_schema.txt localhost \
        dependency_question_graph \
        data/gold_graphs/$*_dependency_without_merge_without_expand.full \
        lib_data/dummy.txt \
        false \
        false \
        > data/gold_graphs/$*_dependency_without_merge_without_expand.full.answers.txt
	cat working/$*-webquestions.train.forest.json \
        working/$*-webquestions.dev.forest.json \
    | java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
        data/freebase/schema/all_domains_schema.txt localhost \
        dependency_question_graph \
        data/gold_graphs/$*_dependency_with_merge_without_expand.full \
        lib_data/dummy.txt \
        true \
        false \
        > data/gold_graphs/$*_dependency_with_merge_without_expand.full.answers.txt

bow_supervised_without_merge_without_expand_%:
	rm -rf ../working/$*_bow_supervised_without_merge_without_expand
	mkdir -p ../working/$*_bow_supervised_without_merge_without_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
    -pointWiseF1Threshold 0.2 \
    -semanticParseKey dependency_lambda \
    -schema data/freebase/schema/all_domains_schema.txt \
    -relationTypesFile lib_data/dummy.txt \
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
    -ngramLength 2 \
    -useSchema true \
    -useKB true \
    -addBagOfWordsGraph true \
    -ngramGrelPartFlag true \
    -addOnlyBagOfWordsGraph true \
    -groundFreeVariables false \
    -groundEntityVariableEdges false \
    -groundEntityEntityEdges false \
    -useEmptyTypes false \
    -ignoreTypes false \
    -urelGrelFlag false \
    -urelPartGrelPartFlag false \
    -utypeGtypeFlag false \
    -gtypeGrelFlag false \
    -wordGrelPartFlag false \
    -wordGrelFlag false \
    -eventTypeGrelPartFlag false \
    -argGrelPartFlag false \
    -argGrelFlag false \
    -stemMatchingFlag false \
    -mediatorStemGrelPartMatchingFlag false \
    -argumentStemMatchingFlag false \
    -argumentStemGrelPartMatchingFlag false \
    -graphIsConnectedFlag false \
    -graphHasEdgeFlag true \
    -countNodesFlag false \
    -edgeNodeCountFlag false \
    -duplicateEdgesFlag true \
    -grelGrelFlag true \
    -useLexiconWeightsRel false \
    -useLexiconWeightsType false \
    -validQueryFlag true \
    -useGoldRelations true \
    -evaluateOnlyTheFirstBest true \
    -evaluateBeforeTraining false \
    -entityScoreFlag true \
    -entityWordOverlapFlag false \
    -initialEdgeWeight -0.5 \
    -initialTypeWeight -2.0 \
    -initialWordWeight -0.05 \
    -stemFeaturesWeight 0.05 \
    -endpoint localhost \
    -supervisedCorpus "working/$*-webquestions.train.forest.json" \
    -goldParsesFile data/gold_graphs/$*_bow_without_merge_without_expand.full.ser \
    -devFile working/$*-webquestions.dev.forest.json \
    -logFile ../working/$*_bow_supervised_without_merge_without_expand/all.log.txt \
    > ../working/$*_bow_supervised_without_merge_without_expand/all.txt

test_bow_supervised_without_merge_without_expand_%:
	rm -rf ../working/$*_test_bow_supervised_without_merge_without_expand
	mkdir -p ../working/$*_test_bow_supervised_without_merge_without_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
    -pointWiseF1Threshold 0.2 \
    -semanticParseKey dependency_lambda \
    -schema data/freebase/schema/all_domains_schema.txt \
    -relationTypesFile lib_data/dummy.txt \
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
    -ngramLength 2 \
    -useSchema true \
    -useKB true \
    -addBagOfWordsGraph true \
    -ngramGrelPartFlag true \
    -addOnlyBagOfWordsGraph true \
    -groundFreeVariables false \
    -groundEntityVariableEdges false \
    -groundEntityEntityEdges false \
    -useEmptyTypes false \
    -ignoreTypes false \
    -urelGrelFlag false \
    -urelPartGrelPartFlag false \
    -utypeGtypeFlag false \
    -gtypeGrelFlag false \
    -wordGrelPartFlag false \
    -wordGrelFlag false \
    -eventTypeGrelPartFlag false \
    -argGrelPartFlag false \
    -argGrelFlag false \
    -stemMatchingFlag false \
    -mediatorStemGrelPartMatchingFlag false \
    -argumentStemMatchingFlag false \
    -argumentStemGrelPartMatchingFlag false \
    -graphIsConnectedFlag false \
    -graphHasEdgeFlag true \
    -countNodesFlag false \
    -edgeNodeCountFlag false \
    -duplicateEdgesFlag true \
    -grelGrelFlag true \
    -useLexiconWeightsRel false \
    -useLexiconWeightsType false \
    -validQueryFlag true \
    -useGoldRelations true \
    -evaluateOnlyTheFirstBest true \
    -evaluateBeforeTraining false \
    -entityScoreFlag true \
    -entityWordOverlapFlag false \
    -initialEdgeWeight -0.5 \
    -initialTypeWeight -2.0 \
    -initialWordWeight -0.05 \
    -stemFeaturesWeight 0.05 \
    -endpoint localhost \
    -supervisedCorpus "working/$*-webquestions.train.forest.json;working/$*-webquestions.dev.forest.json" \
    -goldParsesFile data/gold_graphs/$*_test_bow_without_merge_without_expand.full.ser \
    -devFile working/$*-webquestions.dev.forest.json \
    -testFile working/$*-webquestions.test.forest.json \
    -logFile ../working/$*_test_bow_supervised_without_merge_without_expand/all.log.txt \
    > ../working/$*_test_bow_supervised_without_merge_without_expand/all.txt

dependency_without_merge_without_expand_%:
	rm -rf ../working/$*_dependency_without_merge_without_expand
	mkdir -p ../working/$*_dependency_without_merge_without_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_question_graph \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-useGoldRelations true \
	-evaluateOnlyTheFirstBest true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-allowMerging false \
	-handleEventEventEdges true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
    -supervisedCorpus "working/$*-webquestions.train.forest.json" \
	-goldParsesFile data/gold_graphs/$*_dependency_without_merge_without_expand.full.ser \
	-devFile "working/$*-webquestions.dev.forest.json"  \
	-logFile ../working/$*_dependency_without_merge_without_expand/all.log.txt \
	> ../working/$*_dependency_without_merge_without_expand/all.txt

dependency_with_merge_without_expand_%:
	rm -rf ../working/$*_dependency_with_merge_without_expand
	mkdir -p ../working/$*_dependency_with_merge_without_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_question_graph \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-useGoldRelations true \
	-evaluateOnlyTheFirstBest true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-allowMerging true \
	-handleEventEventEdges true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
    -supervisedCorpus "working/$*-webquestions.train.forest.json" \
	-goldParsesFile data/gold_graphs/$*_dependency_with_merge_without_expand.full.ser \
	-devFile "working/$*-webquestions.dev.forest.json"  \
	-logFile ../working/$*_dependency_with_merge_without_expand/all.log.txt \
	> ../working/$*_dependency_with_merge_without_expand/all.txt

test_dependency_with_merge_without_expand_%:
	rm -rf ../working/$*_test_dependency_with_merge_without_expand
	mkdir -p ../working/$*_test_dependency_with_merge_without_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_question_graph \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-useGoldRelations true \
	-evaluateOnlyTheFirstBest true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-allowMerging true \
	-handleEventEventEdges true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
    	-supervisedCorpus "working/$*-webquestions.train.forest.json;working/$*-webquestions.dev.forest.json" \
	-goldParsesFile data/gold_graphs/$*_dependency_with_merge_without_expand.full.ser \
	-devFile "working/$*-webquestions.dev.forest.json"  \
	-testFile "working/$*-webquestions.test.forest.json"  \
	-logFile ../working/$*_test_dependency_with_merge_without_expand/all.log.txt \
	> ../working/$*_test_dependency_with_merge_without_expand/all.txt

