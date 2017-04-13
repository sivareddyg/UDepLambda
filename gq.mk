dump_gq_%:
	java -cp lib/*:bin deplambda.others.NlpPipeline \
		annotators tokenize,ssplit \
		tokenize.whitespace true \
		ssplit.eolonly true \
		preprocess.capitalizeFirstWord true \
		preprocess.capitalizeEntities true \
		languageCode $* \
		posTagKey UD \
		nthreads 1 \
		< data/GraphQuestions/$*/$*-bilty-bist-graphquestions.train.deplambda.forest.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.ConvertGraphParserSentenceToConll \
		> working/$*-gq-train.conll.srl.tmp
	cut -f2 working/$*-gq-train.conll.srl.tmp \
		| sed -e 's/^/\t/g' \
		> working/$*-gq-train.conll.srl.input

	java -cp lib/*:bin deplambda.others.NlpPipeline \
		annotators tokenize,ssplit \
		tokenize.whitespace true \
		ssplit.eolonly true \
		preprocess.capitalizeFirstWord true \
		preprocess.capitalizeEntities true \
		languageCode $* \
		posTagKey UD \
		nthreads 1 \
		< data/GraphQuestions/$*/$*-bilty-bist-graphquestions.test.deplambda.forest.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.ConvertGraphParserSentenceToConll \
		> working/$*-gq-test.conll.srl.tmp
	cut -f2 working/$*-gq-test.conll.srl.tmp \
		| sed -e 's/^/\t/g' \
		> working/$*-gq-test.conll.srl.input

	java -cp lib/*:bin deplambda.others.NlpPipeline \
		annotators tokenize,ssplit \
		tokenize.whitespace true \
		ssplit.eolonly true \
		preprocess.capitalizeFirstWord true \
		preprocess.capitalizeEntities true \
		languageCode $* \
		posTagKey UD \
		nthreads 1 \
		< data/GraphQuestions/$*/$*-bilty-bist-graphquestions.dev.deplambda.forest.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.ConvertGraphParserSentenceToConll \
		> working/$*-gq-dev.conll.srl.tmp
	cut -f2 working/$*-gq-dev.conll.srl.tmp \
		| sed -e 's/^/\t/g' \
		> working/$*-gq-dev.conll.srl.input

correct-args-using-srl_%:
	python scripts/modify-arg1-arg2-srl.py \
		data/GraphQuestions/$*/$*-bilty-bist-graphquestions.train.deplambda.forest.json \
		working/$*-gq-train.conll.srl.output \
		> data/GraphQuestions/$*/$*-bilty-bist-srl-graphquestions.train.deplambda.forest.json
	python scripts/modify-arg1-arg2-srl.py \
		data/GraphQuestions/$*/$*-bilty-bist-graphquestions.dev.deplambda.forest.json \
		working/$*-gq-dev.conll.srl.output \
		> data/GraphQuestions/$*/$*-bilty-bist-srl-graphquestions.dev.deplambda.forest.json
	python scripts/modify-arg1-arg2-srl.py \
		data/GraphQuestions/$*/$*-bilty-bist-graphquestions.test.deplambda.forest.json \
		working/$*-gq-test.conll.srl.output \
		> data/GraphQuestions/$*/$*-bilty-bist-srl-graphquestions.test.deplambda.forest.json


process_gq:
	python scripts/graphquestions/convert_to_graph_parser_format.py data/freebase/mid_to_key.txt.gz \
		< data/GraphQuestions/original/freebase13/graphquestions.training.json \
		> working/graphquestions.training.mid.json
	python scripts/graphquestions/split_data.py working/graphquestions.training.mid.json 30
	mv working/graphquestions.training.mid.json.70 data/GraphQuestions/en/train.json
	mv working/graphquestions.training.mid.json.30 data/GraphQuestions/en/dev.json
	python scripts/graphquestions/convert_to_graph_parser_format.py data/freebase/mid_to_key.txt.gz \
		< data/GraphQuestions/original/freebase13/graphquestions.testing.json \
		> data/GraphQuestions/en/test.json

	python scripts/graphquestions/dump_sentences.py \
		< data/GraphQuestions/en/train.json \
		> data/GraphQuestions/en/train.txt
	python scripts/graphquestions/dump_sentences.py \
		< data/GraphQuestions/en/dev.json \
		> data/GraphQuestions/en/dev.txt
	python scripts/graphquestions/dump_sentences.py \
		< data/GraphQuestions/en/test.json \
		> data/GraphQuestions/en/test.txt

dump_to_database:
	cat data/GraphQuestions/en/train.json \
		data/GraphQuestions/en/dev.json \
		data/GraphQuestions/en/test.json \
	| python scripts/graphquestions/dump_to_database.py

dump_translations_%:
	mkdir -p data/GraphQuestions/$*
	python scripts/graphquestions/dump_from_database.py \
	working/$*-annotations.db \
	> data/GraphQuestions/$*/translations.txt
	python scripts/graphquestions/create_translations_in_json.py \
	data/GraphQuestions/$*/translations.txt \
	< data/GraphQuestions/en/train.json \
	> data/GraphQuestions/$*/train.json
	python scripts/graphquestions/create_translations_in_json.py \
	data/GraphQuestions/$*/translations.txt \
	< data/GraphQuestions/en/dev.json \
	> data/GraphQuestions/$*/dev.json
	python scripts/graphquestions/create_translations_in_json.py \
	data/GraphQuestions/$*/translations.txt \
	< data/GraphQuestions/en/test.json \
	> data/GraphQuestions/$*/test.json

annotate_entity_spans_%:
	cat data/GraphQuestions/$*/train.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.util.NlpPipeline \
		annotators tokenize,ssplit,pos,lemma \
		ssplit.newlineIsSentenceBreak always \
		languageCode $* \
		pos.model lib_data/ud-models-v1.3/$*/pos-tagger/utb-caseless-$*-bidirectional-glove-distsim-lower.tagger \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.scripts.NounPhraseAnnotator $*_ud \
		> working/$*-graphquestions.train.json
	cat data/GraphQuestions/$*/dev.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.util.NlpPipeline \
		annotators tokenize,ssplit,pos,lemma \
		ssplit.newlineIsSentenceBreak always \
		languageCode $* \
		pos.model lib_data/ud-models-v1.3/$*/pos-tagger/utb-caseless-$*-bidirectional-glove-distsim-lower.tagger \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.scripts.NounPhraseAnnotator $*_ud \
		> working/$*-graphquestions.dev.json
	cat data/GraphQuestions/$*/test.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.util.NlpPipeline \
		annotators tokenize,ssplit,pos,lemma \
		ssplit.newlineIsSentenceBreak always \
		languageCode $* \
		pos.model lib_data/ud-models-v1.3/$*/pos-tagger/utb-caseless-$*-bidirectional-glove-distsim-lower.tagger \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.scripts.NounPhraseAnnotator $*_ud \
		> working/$*-graphquestions.test.json

entity_annotate_%:
	# Entity Annotations
	cat working/$*-graphquestions.train.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.cli.RankMatchedEntitiesCli \
		--useKG true \
		--apiKey AIzaSyDj-4Sr5TmDuEA8UVOd_89PqK87GABeoFg \
		--langCode $* \
		> working/$*-graphquestions.train.ranked.json
	cat working/$*-graphquestions.dev.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.cli.RankMatchedEntitiesCli \
		--useKG true \
		--apiKey AIzaSyDj-4Sr5TmDuEA8UVOd_89PqK87GABeoFg \
		--langCode $* \
		> working/$*-graphquestions.dev.ranked.json
	cat working/$*-graphquestions.test.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.cli.RankMatchedEntitiesCli \
		--useKG true \
		--apiKey AIzaSyDj-4Sr5TmDuEA8UVOd_89PqK87GABeoFg \
		--langCode $* \
		> working/$*-graphquestions.test.ranked.json

copy_entity_annotations_%:
	cp -i working/$*-graphquestions.train.ranked.json \
		data/GraphQuestions/$*/train.kgapi.json
	cp -i working/$*-graphquestions.dev.ranked.json \
		data/GraphQuestions/$*/dev.kgapi.json
	cp -i working/$*-graphquestions.test.ranked.json \
		data/GraphQuestions/$*/test.kgapi.json

evaluate_entity_annotation_upperbound_%:
	cat data/GraphQuestions/$*/dev.kgapi.json \
		| python2.7 ../graph-parser/scripts/entity-annotation/get_entity_patterns.py

train_entity_annotator_%:
	mkdir -p data/entity-models
	java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.cli.RunTrainEntityScorer \
		-nthreads 20 \
		-iterations 100 \
		-hasId false \
		-useAPIScore true \
		-useNameOverlap true \
		-useEntityLength false \
		-useWordEntity false \
		-useWordBigramEntity false \
		-useWord false \
		-useWordBigram false \
		-useNextWord false \
		-usePrevWord false \
		-trainFile data/GraphQuestions/$*/train.kgapi.json \
		-devFile data/GraphQuestions/$*/dev.kgapi.json \
		-saveToFile data/entity-models/$*-graph-questions.ser

disambiguate_entities_%:
	cat data/GraphQuestions/$*/train.kgapi.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.cli.RunEntityDisambiguator \
		-loadModelFromFile data/entity-models/$*-graph-questions.ser \
		-endpoint localhost \
		-nthreads 20 \
		-nbestEntities 10 \
		-hasId false \
		-schema data/freebase/schema/sempre2_schema_gp_format.txt \
		> working/$*-graphquestions-train.disambiguated.json 
	cat data/GraphQuestions/$*/dev.kgapi.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.cli.RunEntityDisambiguator \
		-loadModelFromFile data/entity-models/$*-graph-questions.ser \
		-endpoint localhost \
		-nthreads 20 \
		-nbestEntities 10 \
		-hasId false \
		-schema data/freebase/schema/sempre2_schema_gp_format.txt \
		> working/$*-graphquestions-dev.disambiguated.json 
	cat data/GraphQuestions/$*/test.kgapi.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.cli.RunEntityDisambiguator \
		-loadModelFromFile data/entity-models/$*-graph-questions.ser \
		-endpoint localhost \
		-nthreads 20 \
		-nbestEntities 10 \
		-hasId false \
		-schema data/freebase/schema/sempre2_schema_gp_format.txt \
		> working/$*-graphquestions-test.disambiguated.json 

entity_annotation_results_%:
	python ../graph-parser/scripts/entity-annotation/evaluate_entity_annotation.py \
		< working/$*-graphquestions-test.disambiguated.json

entity_dismabiguated_to_plain_forest_en:
	cat working/en-graphquestions-train.disambiguated.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.util.CreateGraphParserForestFromEntityDisambiguatedSentences \
		preprocess.lowerCase true \
		annotators tokenize,ssplit,pos,lemma \
		tokenize.whitespace true \
		ssplit.eolonly true \
		languageCode en \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.util.NlpPipeline \
		annotators tokenize,ssplit,pos \
		tokenize.whitespace true \
		ssplit.eolonly true \
		languageCode en \
		posTagKey UD \
		pos.model lib_data/ud-models-v1.3/en/pos-tagger/utb-caseless-en-bidirectional-glove-distsim-lower.tagger \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.util.NlpPipeline \
		preprocess.addDateEntities true \
		preprocess.mergeEntityWords true \
		annotators tokenize,ssplit \
		tokenize.whitespace true \
		ssplit.eolonly true \
		languageCode en \
		posTagKey UD \
		postprocess.correctPosTags true \
		> working/en-graphquestions.train.plain.forest.json
	cat working/en-graphquestions-dev.disambiguated.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.util.CreateGraphParserForestFromEntityDisambiguatedSentences \
		preprocess.lowerCase true \
		annotators tokenize,ssplit,pos,lemma \
		tokenize.whitespace true \
		ssplit.eolonly true \
		languageCode en \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.util.NlpPipeline \
		annotators tokenize,ssplit,pos \
		tokenize.whitespace true \
		ssplit.eolonly true \
		languageCode en \
		posTagKey UD \
		pos.model lib_data/ud-models-v1.3/en/pos-tagger/utb-caseless-en-bidirectional-glove-distsim-lower.tagger \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.util.NlpPipeline \
		preprocess.addDateEntities true \
		preprocess.mergeEntityWords true \
		annotators tokenize,ssplit \
		tokenize.whitespace true \
		ssplit.eolonly true \
		languageCode en \
		posTagKey UD \
		postprocess.correctPosTags true \
		> working/en-graphquestions.dev.plain.forest.json
	cat working/en-graphquestions-test.disambiguated.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.util.CreateGraphParserForestFromEntityDisambiguatedSentences \
		preprocess.lowerCase true \
		annotators tokenize,ssplit,pos,lemma \
		tokenize.whitespace true \
		ssplit.eolonly true \
		languageCode en \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.util.NlpPipeline \
		annotators tokenize,ssplit,pos \
		tokenize.whitespace true \
		ssplit.eolonly true \
		languageCode en \
		posTagKey UD \
		pos.model lib_data/ud-models-v1.3/en/pos-tagger/utb-caseless-en-bidirectional-glove-distsim-lower.tagger \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.util.NlpPipeline \
		preprocess.addDateEntities true \
		preprocess.mergeEntityWords true \
		annotators tokenize,ssplit \
		tokenize.whitespace true \
		ssplit.eolonly true \
		languageCode en \
		posTagKey UD \
		postprocess.correctPosTags true \
		> working/en-graphquestions.test.plain.forest.json

entity_dismabiguated_to_plain_forest_%:
	cat working/$*-graphquestions-train.disambiguated.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.util.CreateGraphParserForestFromEntityDisambiguatedSentences \
		preprocess.lowerCase true \
		annotators tokenize,ssplit,pos \
		tokenize.whitespace true \
		ssplit.eolonly true \
		languageCode $* \
		posTagKey UD \
		pos.model lib_data/ud-models-v1.2/$*/pos-tagger/utb-caseless-$*-bidirectional-glove-distsim-lower.full.tagger \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.Stemmer $* 20 \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.util.NlpPipeline \
		preprocess.addDateEntities true \
		preprocess.mergeEntityWords true \
		annotators tokenize,ssplit \
		tokenize.whitespace true \
		ssplit.eolonly true \
		languageCode $* \
		posTagKey UD \
		postprocess.correctPosTags true \
		> working/$*-graphquestions.train.plain.forest.json
	cat working/$*-graphquestions-dev.disambiguated.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.util.CreateGraphParserForestFromEntityDisambiguatedSentences \
		preprocess.lowerCase true \
		annotators tokenize,ssplit,pos \
		tokenize.whitespace true \
		ssplit.eolonly true \
		languageCode $* \
		posTagKey UD \
		pos.model lib_data/ud-models-v1.2/$*/pos-tagger/utb-caseless-$*-bidirectional-glove-distsim-lower.full.tagger \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.Stemmer $* 20 \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.util.NlpPipeline \
		preprocess.addDateEntities true \
		preprocess.mergeEntityWords true \
		annotators tokenize,ssplit \
		tokenize.whitespace true \
		ssplit.eolonly true \
		languageCode $* \
		posTagKey UD \
		postprocess.correctPosTags true \
		> working/$*-graphquestions.dev.plain.forest.json
	cat working/$*-graphquestions-test.disambiguated.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.util.CreateGraphParserForestFromEntityDisambiguatedSentences \
		preprocess.lowerCase true \
		annotators tokenize,ssplit,pos \
		tokenize.whitespace true \
		ssplit.eolonly true \
		languageCode $* \
		posTagKey UD \
		pos.model lib_data/ud-models-v1.2/$*/pos-tagger/utb-caseless-$*-bidirectional-glove-distsim-lower.full.tagger \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.Stemmer $* 20 \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* in.sivareddy.graphparser.util.NlpPipeline \
		preprocess.addDateEntities true \
		preprocess.mergeEntityWords true \
		annotators tokenize,ssplit \
		tokenize.whitespace true \
		ssplit.eolonly true \
		languageCode $* \
		posTagKey UD \
		postprocess.correctPosTags true \
		> working/$*-graphquestions.test.plain.forest.json

easyccg_parse_%:
	cat working/en-graphquestions.$*.plain.forest.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.NlpPipeline \
		preprocess.capitalizeFirstWord true \
		annotators tokenize,ssplit,pos \
		tokenize.whitespace true \
		languageCode en \
		ssplit.eolonly true \
		nthreads 10 \
		postprocess.correctPosTags true \
		posTagKey PTB \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.NlpPipeline \
		annotators tokenize,ssplit \
		tokenize.whitespace true \
		ssplit.eolonly true \
		nthreads 10 \
		ccgParser easyccg \
		ccgParser.nbest 1 \
		ccgParser.modelFolder ../graph-parser/lib_data/easyccg_model_questions/ \
		ccgParser.parserArguments -s,-r,S[q],S[qem],S[wq] \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.NlpPipeline \
		annotators tokenize,ssplit \
		tokenize.whitespace true \
		ssplit.eolonly true \
		nthreads 10 \
		ccgParser easyccg \
		ccgParser.nbest 1 \
		ccgParser.modelFolder ../graph-parser/lib_data/easyccg_model/ \
		ccgParser.parserArguments -s,-r,S[q],S[qem],S[wq] \
		> working/en-graphquestions.$*.easyccg.json

gq_plain_forest_to_conll_%:
	cat working/$*-graphquestions.train.plain.forest.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.ConvertGraphParserSentenceToConll \
		| sed -e "s/-lrb-/\(/g" \
		| sed -e "s/-rrb-/\)/g" \
		| sed -e "s/-RRB-/SYM/g" \
		| sed -e "s/-LRB-/SYM/g" \
		| python scripts/webquestions/make_conll_column_lowercase.py 1 \
		> working/$*-graphquestions.train.conll
	cat working/$*-graphquestions.dev.plain.forest.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.ConvertGraphParserSentenceToConll \
		| sed -e "s/-lrb-/\(/g" \
		| sed -e "s/-rrb-/\)/g" \
		| sed -e "s/-RRB-/SYM/g" \
		| sed -e "s/-LRB-/SYM/g" \
		| python scripts/webquestions/make_conll_column_lowercase.py 1 \
		> working/$*-graphquestions.dev.conll
	cat working/$*-graphquestions.test.plain.forest.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.ConvertGraphParserSentenceToConll \
		| sed -e "s/-lrb-/\(/g" \
		| sed -e "s/-rrb-/\)/g" \
		| sed -e "s/-RRB-/SYM/g" \
		| sed -e "s/-LRB-/SYM/g" \
		| python scripts/webquestions/make_conll_column_lowercase.py 1 \
		> working/$*-graphquestions.test.conll

gq_stanford_parse_conll_%:
	cat working/$*-graphquestions.train.conll \
		| sed -e 's/_\t_\t_\t_$$/0\troot\t_\t_/g' \
		> working/$*-graphquestions.train.conll.tmp
	java -Dfile.encoding="UTF-8" -cp .:lib/* edu.stanford.nlp.parser.nndep.DependencyParser \
		-model lib_data/ud-models-v1.3/$*/neural-parser/$*-lowercase-glove50.lower.nndep.model.txt.gz \
		-testFile working/$*-graphquestions.train.conll.tmp \
		-outFile working/$*-stanford-graphquestions.train.parsed.conll
	rm working/$*-graphquestions.train.conll.tmp
	cat working/$*-graphquestions.dev.conll \
		| sed -e 's/_\t_\t_\t_$$/0\troot\t_\t_/g' \
		> working/$*-graphquestions.dev.conll.tmp
	java -Dfile.encoding="UTF-8" -cp .:lib/* edu.stanford.nlp.parser.nndep.DependencyParser \
		-model lib_data/ud-models-v1.3/$*/neural-parser/$*-lowercase-glove50.lower.nndep.model.txt.gz \
		-testFile working/$*-graphquestions.dev.conll.tmp \
		-outFile working/$*-stanford-graphquestions.dev.parsed.conll
	rm working/$*-graphquestions.dev.conll.tmp
	cat working/$*-graphquestions.test.conll \
		| sed -e 's/_\t_\t_\t_$$/0\troot\t_\t_/g' \
		> working/$*-graphquestions.test.conll.tmp
	java -Dfile.encoding="UTF-8" -cp .:lib/* edu.stanford.nlp.parser.nndep.DependencyParser \
		-model lib_data/ud-models-v1.3/$*/neural-parser/$*-lowercase-glove50.lower.nndep.model.txt.gz \
		-testFile working/$*-graphquestions.test.conll.tmp \
		-outFile working/$*-stanford-graphquestions.test.parsed.conll
	rm working/$*-graphquestions.test.conll.tmp

gq_stanford_bilty_parse_conll_%:
	cat working/$*-graphquestions.train.tagged.conll \
		| sed -e 's/_\t_\t_\t_$$/0\troot\t_\t_/g' \
		> working/$*-graphquestions.train.conll.tmp
	java -Dfile.encoding="UTF-8" -cp .:lib/* edu.stanford.nlp.parser.nndep.DependencyParser \
		-model lib_data/ud-models-v1.3/$*/neural-parser/$*-glove50.lower.nndep.model.txt.gz \
		-testFile working/$*-graphquestions.train.conll.tmp \
		-outFile working/$*-stanford-graphquestions.train.parsed.conll
	rm working/$*-graphquestions.train.conll.tmp
	cat working/$*-graphquestions.dev.tagged.conll \
		| sed -e 's/_\t_\t_\t_$$/0\troot\t_\t_/g' \
		> working/$*-graphquestions.dev.conll.tmp
	java -Dfile.encoding="UTF-8" -cp .:lib/* edu.stanford.nlp.parser.nndep.DependencyParser \
		-model lib_data/ud-models-v1.3/$*/neural-parser/$*-glove50.lower.nndep.model.txt.gz \
		-testFile working/$*-graphquestions.dev.conll.tmp \
		-outFile working/$*-stanford-graphquestions.dev.parsed.conll
	rm working/$*-graphquestions.dev.conll.tmp
	cat working/$*-graphquestions.test.tagged.conll \
		| sed -e 's/_\t_\t_\t_$$/0\troot\t_\t_/g' \
		> working/$*-graphquestions.test.conll.tmp
	java -Dfile.encoding="UTF-8" -cp .:lib/* edu.stanford.nlp.parser.nndep.DependencyParser \
		-model lib_data/ud-models-v1.3/$*/neural-parser/$*-glove50.lower.nndep.model.txt.gz \
		-testFile working/$*-graphquestions.test.conll.tmp \
		-outFile working/$*-stanford-graphquestions.test.parsed.conll
	rm working/$*-graphquestions.test.conll.tmp

gq_merge_parsed_conll_with_forest_%:
	$(eval LANG := $(shell echo $* | cut -d- -f1))
	python scripts/webquestions/copy_column_from_to.py 2:2 \
		working/$(LANG)-graphquestions.train.conll \
		working/$*-graphquestions.train.parsed.conll \
		> working/$*-graphquestions.train.parsed.tmp.conll
	java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.MergeConllAndGraphParserFormats \
		working/$*-graphquestions.train.parsed.tmp.conll \
		working/$(LANG)-graphquestions.train.plain.forest.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.NlpPipeline \
		annotators tokenize,ssplit \
		ssplit.newlineIsSentenceBreak always \
		tokenize.whitespace true \
		postprocess.removeMultipleRoots true \
		languageCode $(LANG) \
		> working/$*-graphquestions.train.forest.json 
	rm working/$*-graphquestions.train.parsed.tmp.conll
	python scripts/webquestions/copy_column_from_to.py 2:2 \
		working/$(LANG)-graphquestions.dev.conll \
		working/$*-graphquestions.dev.parsed.conll \
		> working/$*-graphquestions.dev.parsed.tmp.conll
	java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.MergeConllAndGraphParserFormats \
		working/$*-graphquestions.dev.parsed.tmp.conll \
		working/$(LANG)-graphquestions.dev.plain.forest.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.NlpPipeline \
		annotators tokenize,ssplit \
		ssplit.newlineIsSentenceBreak always \
		tokenize.whitespace true \
		postprocess.removeMultipleRoots true \
		languageCode $(LANG) \
		> working/$*-graphquestions.dev.forest.json 
	rm working/$*-graphquestions.dev.parsed.tmp.conll
	python scripts/webquestions/copy_column_from_to.py 2:2 \
		working/$(LANG)-graphquestions.test.conll \
		working/$*-graphquestions.test.parsed.conll \
		> working/$*-graphquestions.test.parsed.tmp.conll
	java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.MergeConllAndGraphParserFormats \
		working/$*-graphquestions.test.parsed.tmp.conll \
		working/$(LANG)-graphquestions.test.plain.forest.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.others.NlpPipeline \
		annotators tokenize,ssplit \
		ssplit.newlineIsSentenceBreak always \
		tokenize.whitespace true \
		postprocess.removeMultipleRoots true \
		languageCode $(LANG) \
		> working/$*-graphquestions.test.forest.json 
	rm working/$*-graphquestions.test.parsed.tmp.conll

gq_deplambda_forest_%:
	$(eval LANG := $(shell echo $* | cut -d- -f1))
	cat working/$*-graphquestions.test.forest.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.cli.RunForestTransformer \
		-definedTypesFile lib_data/ud.types.txt \
		-treeTransformationsFile lib_data/ud-enhancement-rules.proto \
		-relationPrioritiesFile lib_data/ud-obliqueness-hierarchy.proto \
		-lambdaAssignmentRulesFile lib_data/ud-substitution-rules.proto \
		-nthreads 20  \
		| python scripts/dependency_semantic_parser/remove_spurious_predicates_from_forest.py \
		> data/GraphQuestions/$(LANG)/$*-graphquestions.test.deplambda.forest.json
	cat working/$*-graphquestions.train.forest.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.cli.RunForestTransformer \
		-definedTypesFile lib_data/ud.types.txt \
		-treeTransformationsFile lib_data/ud-enhancement-rules.proto \
		-relationPrioritiesFile lib_data/ud-obliqueness-hierarchy.proto \
		-lambdaAssignmentRulesFile lib_data/ud-substitution-rules.proto \
		-nthreads 20  \
		| python scripts/dependency_semantic_parser/remove_spurious_predicates_from_forest.py \
		> data/GraphQuestions/$(LANG)/$*-graphquestions.train.deplambda.forest.json
	cat working/$*-graphquestions.dev.forest.json \
		| java -Dfile.encoding="UTF-8" -cp bin:lib/* deplambda.cli.RunForestTransformer \
		-definedTypesFile lib_data/ud.types.txt \
		-treeTransformationsFile lib_data/ud-enhancement-rules.proto \
		-relationPrioritiesFile lib_data/ud-obliqueness-hierarchy.proto \
		-lambdaAssignmentRulesFile lib_data/ud-substitution-rules.proto \
		-nthreads 20  \
		| python scripts/dependency_semantic_parser/remove_spurious_predicates_from_forest.py \
		> data/GraphQuestions/$(LANG)/$*-graphquestions.dev.deplambda.forest.json

gq_deplambda_with_hyperexpand.32.stem:
	rm -rf ../working/gq_deplambda_with_hyperexpand.32.stem
	mkdir -p ../working/gq_deplambda_with_hyperexpand.32.stem
	java -Dfile.encoding="UTF-8" -Xms2048m -cp bin:lib/* in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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

gq_deplambda_with_hyperexpand.33.stem:
	rm -rf ../working/gq_deplambda_with_hyperexpand.33.stem
	mkdir -p ../working/gq_deplambda_with_hyperexpand.33.stem
	java -Dfile.encoding="UTF-8" -Xms2048m -cp bin:lib/* in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-questionTypeGrelPartFlag false \
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
    	-evaluateOnlyTheFirstBest true \
	-supervisedCorpus "working/graphquestions.training.stanford.deplambda.forest.json" \
	-contentWordPosTags "NOUN;VERB;ADJ;ADP;ADV;PRON" \
	-devFile working/graphquestions.testing.stanford.deplambda.forest.json \
	-logFile ../working/gq_deplambda_with_hyperexpand.33.stem/all.log.txt \
	> ../working/gq_deplambda_with_hyperexpand.33.stem/all.txt

gq_deplambda_with_hyperexpand.34.stem:
	rm -rf ../working/gq_deplambda_with_hyperexpand.34.stem
	mkdir -p ../working/gq_deplambda_with_hyperexpand.34.stem
	java -Dfile.encoding="UTF-8" -Xms2048m -cp bin:lib/* in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-questionTypeGrelPartFlag false \
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
	-useAnswerTypeQuestionWordFlag true \
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
    	-evaluateOnlyTheFirstBest true \
	-supervisedCorpus "working/graphquestions.training.stanford.deplambda.forest.json" \
	-contentWordPosTags "NOUN;VERB;ADJ;ADP;ADV;PRON" \
	-devFile working/graphquestions.testing.stanford.deplambda.forest.json \
	-logFile ../working/gq_deplambda_with_hyperexpand.34.stem/all.log.txt \
	> ../working/gq_deplambda_with_hyperexpand.34.stem/all.txt


gq_deplambda_with_merge_with_expand.32.stem:
	rm -rf ../working/gq_deplambda_with_merge_with_expand.32.stem
	mkdir -p ../working/gq_deplambda_with_merge_with_expand.32.stem
	java -Dfile.encoding="UTF-8" -Xms2048m -cp bin:lib/* in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-handleEventEventEdges false \
	-allowMerging true \
	-useExpand true \
	-useHyperExpand false \
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
	-logFile ../working/gq_deplambda_with_merge_with_expand.32.stem/all.log.txt \
	> ../working/gq_deplambda_with_merge_with_expand.32.stem/all.txt

gq_deplambda_with_merge_with_expand.33.basic_%:
	$(eval LANG := $(shell echo $* | cut -d- -f1))
	rm -rf ../working/gq/$*/deplambda_with_merge_with_expand.33.basic
	mkdir -p ../working/gq/$*/deplambda_with_merge_with_expand.33.basic
	java -Dfile.encoding="UTF-8" -Xms2048m -cp bin:lib/* in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-questionTypeGrelPartFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-ngramStemMatchingFlag false \
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
	-handleEventEventEdges false \
	-allowMerging true \
	-useExpand true \
	-useHyperExpand false \
	-evaluateBeforeTraining true \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight 0.0 \
	-mergeEdgeWeight 2.0 \
	-stemFeaturesWeight 10 \
	-endpoint localhost \
    -evaluateOnlyTheFirstBest true \
	-contentWordPosTags "NOUN;VERB;ADJ;ADP;ADV;PRON" \
	-supervisedCorpus "data/GraphQuestions/$(LANG)/$*-graphquestions.train.deplambda.forest.json" \
	-devFile data/GraphQuestions/$(LANG)/$*-graphquestions.dev.deplambda.forest.json \
	-testFile data/GraphQuestions/$(LANG)/$*-graphquestions.test.deplambda.forest.json \
	-logFile ../working/gq/$*/deplambda_with_merge_with_expand.33.basic/all.log.txt \
	> ../working/gq/$*/deplambda_with_merge_with_expand.33.basic/all.txt

gq_deplambda_with_merge_with_expand.33.structonly_%:
	$(eval LANG := $(shell echo $* | cut -d- -f1))
	rm -rf ../working/gq/$*/deplambda_with_merge_with_expand.33.structonly
	mkdir -p ../working/gq/$*/deplambda_with_merge_with_expand.33.structonly
	java -Dfile.encoding="UTF-8" -Xms2048m -cp bin:lib/* in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/sempre2_schema_gp_format.txt \
	-relationTypesFile lib_data/dummy.txt \
	-mostFrequentTypesFile lib_data/dummy.txt \
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
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-questionTypeGrelPartFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-ngramStemMatchingFlag false \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations false \
	-handleEventEventEdges false \
	-allowMerging true \
	-useExpand true \
	-useHyperExpand false \
	-evaluateBeforeTraining true \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight 0.0 \
	-mergeEdgeWeight 2.0 \
	-stemFeaturesWeight 10 \
	-endpoint localhost \
    -evaluateOnlyTheFirstBest true \
	-contentWordPosTags "NOUN;VERB;ADJ;ADP;ADV;PRON" \
	-supervisedCorpus "data/GraphQuestions/$(LANG)/$*-graphquestions.train.deplambda.forest.json" \
	-devFile data/GraphQuestions/$(LANG)/$*-graphquestions.dev.deplambda.forest.json \
	-testFile data/GraphQuestions/$(LANG)/$*-graphquestions.test.deplambda.forest.json \
	-logFile ../working/gq/$*/deplambda_with_merge_with_expand.33.structonly/all.log.txt \
	> ../working/gq/$*/deplambda_with_merge_with_expand.33.structonly/all.txt

gq_deplambda_with_merge_with_expand.33.stem_%:
	$(eval LANG := $(shell echo $* | cut -d- -f1))
	rm -rf ../working/gq/$*/deplambda_with_merge_with_expand.33.stem
	mkdir -p ../working/gq/$*/deplambda_with_merge_with_expand.33.stem
	java -Dfile.encoding="UTF-8" -Xms2048m -cp bin:lib/* in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-ngramGrelPartFlag false \
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
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-ngramStemMatchingFlag true \
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
	-handleEventEventEdges false \
	-allowMerging true \
	-useExpand true \
	-useHyperExpand false \
	-evaluateBeforeTraining true \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight 0.0 \
	-mergeEdgeWeight 2.0 \
	-stemFeaturesWeight 10 \
	-endpoint localhost \
    -evaluateOnlyTheFirstBest true \
	-contentWordPosTags "NOUN;VERB;ADJ;ADP;ADV;PRON" \
	-supervisedCorpus "data/GraphQuestions/$(LANG)/$*-graphquestions.train.deplambda.forest.json" \
	-devFile data/GraphQuestions/$(LANG)/$*-graphquestions.dev.deplambda.forest.json \
	-testFile data/GraphQuestions/$(LANG)/$*-graphquestions.test.deplambda.forest.json \
	-logFile ../working/gq/$*/deplambda_with_merge_with_expand.33.stem/all.log.txt \
	> ../working/gq/$*/deplambda_with_merge_with_expand.33.stem/all.txt

gq_deplambda_with_merge_with_expand.33.embonly_%:
	$(eval LANG := $(shell echo $* | cut -d- -f1))
	rm -rf ../working/gq/$*/deplambda_with_merge_with_expand.33.embonly
	mkdir -p ../working/gq/$*/deplambda_with_merge_with_expand.33.embonly
	java -Dfile.encoding="UTF-8" -Xms2048m -cp bin:lib/* in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/sempre2_schema_gp_format.txt \
	-relationTypesFile lib_data/dummy.txt \
	-embeddingFile data/embeddings/twelve.table4.translation_invariance.window_3+size_40.normalized.de-en-es \
	-mostFrequentTypesFile lib_data/dummy.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 5 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 1 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-questionTypeGrelPartFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-ngramStemMatchingFlag true \
	-useEmbeddingSimilarityFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag false \
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag false \
	-useGoldRelations false \
	-handleEventEventEdges false \
	-allowMerging true \
	-useExpand true \
	-useHyperExpand false \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight 0.0 \
	-initialTypeWeight 0.0 \
	-initialWordWeight 0.0 \
	-mergeEdgeWeight 2.0 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
    -evaluateOnlyTheFirstBest true \
	-contentWordPosTags "NOUN;VERB" \
	-supervisedCorpus "data/GraphQuestions/$(LANG)/$*-graphquestions.train.deplambda.forest.json" \
	-devFile data/GraphQuestions/$(LANG)/$*-graphquestions.dev.deplambda.forest.json \
	-testFile data/GraphQuestions/$(LANG)/$*-graphquestions.test.deplambda.forest.json \
	-logFile ../working/gq/$*/deplambda_with_merge_with_expand.33.embonly/all.log.txt \
	> ../working/gq/$*/deplambda_with_merge_with_expand.33.embonly/all.txt

gq_dependency_with_hyperexpand.33.emb_%:
	$(eval LANG := $(shell echo $* | cut -d- -f1))
	rm -rf ../working/gq/$*/dependency_with_hyperexpand.33.emb
	mkdir -p ../working/gq/$*/dependency_with_hyperexpand.33.emb
	java -Dfile.encoding="UTF-8" -Xms2048m -cp bin:lib/* in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_question_graph \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/sempre2_schema_gp_format.txt \
	-relationTypesFile lib_data/dummy.txt \
	-embeddingFile data/embeddings/twelve.table4.translation_invariance.window_3+size_40.normalized.de-en-es \
	-mostFrequentTypesFile lib_data/dummy.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 5 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 1 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-questionTypeGrelPartFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-ngramStemMatchingFlag true \
	-useEmbeddingSimilarityFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag false \
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations false \
	-handleEventEventEdges false \
	-allowMerging false \
	-useExpand false \
	-useHyperExpand true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight 0.0 \
	-initialTypeWeight 0.0 \
	-initialWordWeight 0.0 \
	-mergeEdgeWeight 2.0 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
    -evaluateOnlyTheFirstBest true \
	-contentWordPosTags "NOUN;VERB" \
	-supervisedCorpus "data/GraphQuestions/$(LANG)/$*-graphquestions.train.deplambda.forest.json" \
	-devFile data/GraphQuestions/$(LANG)/$*-graphquestions.dev.deplambda.forest.json \
	-testFile data/GraphQuestions/$(LANG)/$*-graphquestions.test.deplambda.forest.json \
	-logFile ../working/gq/$*/dependency_with_hyperexpand.33.emb/all.log.txt \
	> ../working/gq/$*/dependency_with_hyperexpand.33.emb/all.txt

test_dependency_with_hyperexpand.33.emb_%:
	$(eval LANG := $(shell echo $* | cut -d- -f1))
	rm -rf ../working/gq/$*/test_dependency_with_hyperexpand.33.emb
	mkdir -p ../working/gq/$*/test_dependency_with_hyperexpand.33.emb
	java -Dfile.encoding="UTF-8" -Xms2048m -cp bin:lib/* in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_question_graph \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/sempre2_schema_gp_format.txt \
	-relationTypesFile lib_data/dummy.txt \
	-embeddingFile data/embeddings/twelve.table4.translation_invariance.window_3+size_40.normalized.de-en-es \
	-mostFrequentTypesFile lib_data/dummy.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 5 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 1 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-questionTypeGrelPartFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-ngramStemMatchingFlag true \
	-useEmbeddingSimilarityFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag false \
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations false \
	-handleEventEventEdges false \
	-allowMerging false \
	-useExpand false \
	-useHyperExpand true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight 0.0 \
	-initialTypeWeight 0.0 \
	-initialWordWeight 0.0 \
	-mergeEdgeWeight 2.0 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
    -evaluateOnlyTheFirstBest true \
	-contentWordPosTags "NOUN;VERB" \
	-supervisedCorpus "data/GraphQuestions/$(LANG)/$*-graphquestions.train.deplambda.forest.json;data/GraphQuestions/$(LANG)/$*-graphquestions.dev.deplambda.forest.json" \
	-devFile data/GraphQuestions/$(LANG)/$*-graphquestions.test.deplambda.forest.json \
	-logFile ../working/gq/$*/test_dependency_with_hyperexpand.33.emb/all.log.txt \
	> ../working/gq/$*/test_dependency_with_hyperexpand.33.emb/all.txt

gq_dependency_with_merge_without_expand.33.emb_%:
	$(eval LANG := $(shell echo $* | cut -d- -f1))
	rm -rf ../working/gq/$*/dependency_with_merge_without_expand.33.emb
	mkdir -p ../working/gq/$*/dependency_with_merge_without_expand.33.emb
	java -Dfile.encoding="UTF-8" -Xms2048m -cp bin:lib/* in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_question_graph \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/sempre2_schema_gp_format.txt \
	-relationTypesFile lib_data/dummy.txt \
	-embeddingFile data/embeddings/twelve.table4.translation_invariance.window_3+size_40.normalized.de-en-es \
	-mostFrequentTypesFile lib_data/dummy.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 5 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 1 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-questionTypeGrelPartFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-ngramStemMatchingFlag true \
	-useEmbeddingSimilarityFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag false \
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations false \
	-handleEventEventEdges false \
	-allowMerging true \
	-useExpand false \
	-useHyperExpand false \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight 0.0 \
	-initialTypeWeight 0.0 \
	-initialWordWeight 0.0 \
	-mergeEdgeWeight 2.0 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
    -evaluateOnlyTheFirstBest true \
	-contentWordPosTags "NOUN;VERB" \
	-supervisedCorpus "data/GraphQuestions/$(LANG)/$*-graphquestions.train.deplambda.forest.json" \
	-devFile data/GraphQuestions/$(LANG)/$*-graphquestions.dev.deplambda.forest.json \
	-testFile data/GraphQuestions/$(LANG)/$*-graphquestions.test.deplambda.forest.json \
	-logFile ../working/gq/$*/dependency_with_merge_without_expand.33.emb/all.log.txt \
	> ../working/gq/$*/dependency_with_merge_without_expand.33.emb/all.txt

gq_easyccg_with_merge_with_expand.33.emb:
	rm -rf ../working/gq/en/easyccg_with_merge_with_expand.33.emb
	mkdir -p ../working/gq/en/easyccg_with_merge_with_expand.33.emb
	java -Dfile.encoding="UTF-8" -Xms2048m -cp bin:lib/* in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey synPars \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/sempre2_schema_gp_format.txt \
	-relationTypesFile lib_data/dummy.txt \
	-embeddingFile data/embeddings/twelve.table4.translation_invariance.window_3+size_40.normalized.de-en-es \
	-mostFrequentTypesFile lib_data/dummy.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 5 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 1 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-questionTypeGrelPartFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-ngramStemMatchingFlag true \
	-useEmbeddingSimilarityFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag false \
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations false \
	-handleEventEventEdges false \
	-allowMerging true \
	-useExpand true \
	-useHyperExpand false \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight 0.0 \
	-initialTypeWeight 0.0 \
	-initialWordWeight 0.0 \
	-mergeEdgeWeight 2.0 \
	-stemFeaturesWeight 2.0 \
	-endpoint localhost \
    -evaluateOnlyTheFirstBest true \
	-contentWordPosTags "NN;NNS;VB;VBD;VBG;VBN;VBP;VBZ" \
	-supervisedCorpus "working/en-graphquestions.train.easyccg.json" \
	-devFile working/en-graphquestions.dev.easyccg.json \
	-testFile working/en-graphquestions.test.easyccg.json \
	-logFile ../working/gq/en/easyccg_with_merge_with_expand.33.emb/all.log.txt \
	> ../working/gq/en/easyccg_with_merge_with_expand.33.emb/all.txt

gq_deplambda_with_merge_with_expand.33.emb_%:
	$(eval LANG := $(shell echo $* | cut -d- -f1))
	rm -rf ../working/gq/$*/deplambda_with_merge_with_expand.33.emb
	mkdir -p ../working/gq/$*/deplambda_with_merge_with_expand.33.emb
	java -Dfile.encoding="UTF-8" -Xms2048m -cp bin:lib/* in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/sempre2_schema_gp_format.txt \
	-relationTypesFile lib_data/dummy.txt \
	-embeddingFile data/embeddings/twelve.table4.translation_invariance.window_3+size_40.normalized.de-en-es \
	-mostFrequentTypesFile lib_data/dummy.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 5 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 1 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-questionTypeGrelPartFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-ngramStemMatchingFlag true \
	-useEmbeddingSimilarityFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag false \
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations false \
	-handleEventEventEdges false \
	-allowMerging true \
	-useExpand true \
	-useHyperExpand false \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight 0.0 \
	-initialTypeWeight 0.0 \
	-initialWordWeight 0.0 \
	-mergeEdgeWeight 2.0 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
    -evaluateOnlyTheFirstBest true \
	-contentWordPosTags "NOUN;VERB" \
	-supervisedCorpus "data/GraphQuestions/$(LANG)/$*-graphquestions.train.deplambda.forest.json" \
	-devFile data/GraphQuestions/$(LANG)/$*-graphquestions.dev.deplambda.forest.json \
	-testFile data/GraphQuestions/$(LANG)/$*-graphquestions.test.deplambda.forest.json \
	-logFile ../working/gq/$*/deplambda_with_merge_with_expand.33.emb/all.log.txt \
	> ../working/gq/$*/deplambda_with_merge_with_expand.33.emb/all.txt

gq_test_deplambda_with_merge_with_expand.33.emb_%:
	$(eval LANG := $(shell echo $* | cut -d- -f1))
	rm -rf ../working/gq/$*/test_deplambda_with_merge_with_expand.33.emb
	mkdir -p ../working/gq/$*/test_deplambda_with_merge_with_expand.33.emb
	java -Dfile.encoding="UTF-8" -Xms2048m -cp bin:lib/* in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/sempre2_schema_gp_format.txt \
	-relationTypesFile lib_data/dummy.txt \
	-embeddingFile data/embeddings/twelve.table4.translation_invariance.window_3+size_40.normalized.de-en-es \
	-mostFrequentTypesFile lib_data/dummy.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 5 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 1 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-questionTypeGrelPartFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-ngramStemMatchingFlag true \
	-useEmbeddingSimilarityFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag false \
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations false \
	-handleEventEventEdges false \
	-allowMerging true \
	-useExpand true \
	-useHyperExpand false \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight 0.0 \
	-initialTypeWeight 0.0 \
	-initialWordWeight 0.0 \
	-mergeEdgeWeight 2.0 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
    -evaluateOnlyTheFirstBest true \
	-contentWordPosTags "NOUN;VERB" \
	-supervisedCorpus "data/GraphQuestions/$(LANG)/$*-graphquestions.train.deplambda.forest.json;data/GraphQuestions/$(LANG)/$*-graphquestions.dev.deplambda.forest.json" \
	-devFile data/GraphQuestions/$(LANG)/$*-graphquestions.test.deplambda.forest.json \
	-logFile ../working/gq/$*/test_deplambda_with_merge_with_expand.33.emb/all.log.txt \
	> ../working/gq/$*/test_deplambda_with_merge_with_expand.33.emb/all.txt


bow_supervised_without_merge_without_expand.stem_%:
	$(eval LANG := $(shell echo $* | cut -d- -f1))
	rm -rf ../working/gq/$*/bow_supervised_without_merge_without_expand.stem
	mkdir -p ../working/gq/$*/bow_supervised_without_merge_without_expand.stem
	java -Dfile.encoding="UTF-8" -Xms2048m -cp bin:lib/* in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey bow_question_graph \
	-schema data/freebase/schema/sempre2_schema_gp_format.txt \
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
	-ngramLength 1 \
	-useSchema true \
	-useKB true \
	-ngramGrelPartFlag true \
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
	-ngramStemMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag false \
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag false \
	-useGoldRelations false \
	-evaluateOnlyTheFirstBest true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-contentWordPosTags "NOUN;VERB;ADJ;ADP;ADV;PRON" \
	-supervisedCorpus "data/GraphQuestions/$(LANG)/$*-graphquestions.train.deplambda.forest.json" \
	-devFile data/GraphQuestions/$(LANG)/$*-graphquestions.dev.deplambda.forest.json \
	-testFile data/GraphQuestions/$(LANG)/$*-graphquestions.test.deplambda.forest.json \
	-logFile ../working/gq/$*/bow_supervised_without_merge_without_expand.stem/all.log.txt \
	> ../working/gq/$*/bow_supervised_without_merge_without_expand.stem/all.txt

bow_supervised_without_merge_without_expand.emb_%:
	$(eval LANG := $(shell echo $* | cut -d- -f1))
	rm -rf ../working/gq/$*/bow_supervised_without_merge_without_expand.emb
	mkdir -p ../working/gq/$*/bow_supervised_without_merge_without_expand.emb
	java -Dfile.encoding="UTF-8" -Xms2048m -cp bin:lib/* in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey bow_question_graph \
	-schema data/freebase/schema/sempre2_schema_gp_format.txt \
	-relationTypesFile lib_data/dummy.txt \
	-embeddingFile data/embeddings/twelve.table4.translation_invariance.window_3+size_40.normalized.de-en-es \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 5 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 1 \
	-useSchema true \
	-useKB true \
	-ngramGrelPartFlag false \
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
	-ngramStemMatchingFlag true \
	-useEmbeddingSimilarityFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag false \
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag false \
	-useGoldRelations false \
	-evaluateOnlyTheFirstBest true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-contentWordPosTags "NOUN;VERB" \
	-supervisedCorpus "data/GraphQuestions/$(LANG)/$*-graphquestions.train.deplambda.forest.json" \
	-devFile data/GraphQuestions/$(LANG)/$*-graphquestions.dev.deplambda.forest.json \
	-testFile data/GraphQuestions/$(LANG)/$*-graphquestions.test.deplambda.forest.json \
	-logFile ../working/gq/$*/bow_supervised_without_merge_without_expand.emb/all.log.txt \
	> ../working/gq/$*/bow_supervised_without_merge_without_expand.emb/all.txt

test_bow_supervised_without_merge_without_expand.emb_%:
	$(eval LANG := $(shell echo $* | cut -d- -f1))
	rm -rf ../working/gq/$*/test_bow_supervised_without_merge_without_expand.emb
	mkdir -p ../working/gq/$*/test_bow_supervised_without_merge_without_expand.emb
	java -Dfile.encoding="UTF-8" -Xms2048m -cp bin:lib/* in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey bow_question_graph \
	-schema data/freebase/schema/sempre2_schema_gp_format.txt \
	-relationTypesFile lib_data/dummy.txt \
	-embeddingFile data/embeddings/twelve.table4.translation_invariance.window_3+size_40.normalized.de-en-es \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 5 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 1 \
	-useSchema true \
	-useKB true \
	-ngramGrelPartFlag false \
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
	-ngramStemMatchingFlag true \
	-useEmbeddingSimilarityFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag false \
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag false \
	-useGoldRelations false \
	-evaluateOnlyTheFirstBest true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-contentWordPosTags "NOUN;VERB" \
	-supervisedCorpus "data/GraphQuestions/$(LANG)/$*-graphquestions.train.deplambda.forest.json;data/GraphQuestions/$(LANG)/$*-graphquestions.dev.deplambda.forest.json" \
	-devFile data/GraphQuestions/$(LANG)/$*-graphquestions.test.deplambda.forest.json \
	-logFile ../working/gq/$*/test_bow_supervised_without_merge_without_expand.emb/all.log.txt \
	> ../working/gq/$*/test_bow_supervised_without_merge_without_expand.emb/all.txt

