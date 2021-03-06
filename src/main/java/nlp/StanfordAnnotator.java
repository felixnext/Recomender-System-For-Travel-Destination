package nlp;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasOffset;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

import java.util.*;
import java.util.List;

/**
 * This class represents a wrapper for stanford nlp functions.
 */
public class StanfordAnnotator {

    private StanfordCoreNLP pipeline;
    //private static StanfordAnnotator instance;

    public StanfordAnnotator() {
        // creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse, sentiment, ner,  dcoref");
        props.put("ner.useSUTime", "false");
        pipeline = new StanfordCoreNLP(props);
    }

    /*
    public synchronized static StanfordAnnotator getInstance() {
        if (instance == null) instance = new StanfordAnnotator();
        return instance;
    }
      *

    /**
     * Takes a text and analyze the text. Detects conference between nouns and pronouns in whole text.
     * Extracts Sentiment sentences and pos tags.
     * @param text Text string, that should be annotated.
     * @return
     */
    public StanfordAnnotation annotateText(String text) {

        // create an empty Annotation just with the given text
        Annotation document = new Annotation(text);

        // run all Annotators on this text
        pipeline.annotate(document);

        // these are all the sentences in this document
        // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        Tree[] sentenceSentiment = new Tree[sentences.size()];

        String[] sentencesPos = new String[sentences.size()];

        String[] tokenizedSentences = new String[sentences.size()];

        for (int i = 0; i < sentences.size(); i++) {
            CoreMap sentence = sentences.get(i);

            StringBuilder sb = new StringBuilder();

            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // this is the text of the token
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                // this is the POS tag of the token
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                sb.append(word + "/" + pos + " ");
            }

            sentencesPos[i] = sb.toString();
            //System.out.println(sb.toString());
            tokenizedSentences[i] = sentence.toString();

            Tree sentimentTree = sentence.get(SentimentCoreAnnotations.AnnotatedTree.class);
            sentenceSentiment[i] = sentimentTree;

        }

        // Both sentence and token offsets start at 1!
        Map<Integer, CorefChain> graph =
                document.get(CorefCoreAnnotations.CorefChainAnnotation.class);

        return new StanfordAnnotation(sentenceSentiment, sentencesPos, graph, tokenizedSentences);
    }



    /**
     * Maps sentiment score to sentiment class
     * @param sentiment Sentiment score.
     * @return Sentiment class as a string.
     */
    public static String toCss(int sentiment) {
        switch (sentiment) {
            case 0:
                return "very negative";
            case 1:
                return "negative";
            case 2:
                return "neutral";
            case 3:
                return "positive";
            case 4:
                return "very positive";
            default:
                return "";
        }
    }
}
