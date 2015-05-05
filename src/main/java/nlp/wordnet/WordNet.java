package nlp.wordnet;

import edu.mit.jwi.IDictionary;
import edu.mit.jwi.RAMDictionary;
import edu.mit.jwi.data.ILoadPolicy;
import edu.mit.jwi.item.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Allows interaction with wordnet dataset.
 * Copied from https://github.com/AKSW/AutoSPARQL/blob/9830e99d897200f8bcd448aa543c8f4cde4f7b3c/commons/src/main/java/org/aksw/autosparql/commons/nlp/wordnet/WordNet.java
 */
public class WordNet {

    private IDictionary dict;

    private static WordNet wordnet;


    public static WordNet getInstance() {
        if(wordnet == null) wordnet =  new WordNet();
        return wordnet;
    }

    private WordNet() {
        try {
            File dictDirectory = WordNetUnpacker.getUnpackedWordNetDir();
            dict = new RAMDictionary(dictDirectory, ILoadPolicy.NO_LOAD);
            dict.open();
        } catch (IOException e) {
            throw new RuntimeException("couldn't open dictionary", e);
        }

    }

    /**
     * Returns a list with best synonyms.
     * @param pos Part of speech og given word.
     * @param s For this word synonyms will be searched.
     * @return A list with best synonyms.
     */
    public List<String> getBestSynonyms(POS pos, String s) {
        List<String> synonyms = new ArrayList<>();
        IIndexWord iw = dict.getIndexWord(s, pos);//dict.getMorphologicalProcessor().lookupBaseForm(pos, s)
        //			IndexWord iw = dict.getMorphologicalProcessor().lookupBaseForm(pos, s);
        try {
            if (iw != null) {
                IWordID wordID = iw.getWordIDs().get(0);
                IWord word = dict.getWord(wordID);
                ISynset synset = word.getSynset();
                // iterate over words associated with the syns
                for (IWord w : synset.getWords()) {
                    String c = w.getLemma();
                    if (!c.equalsIgnoreCase(s) && !c.contains(" ") && synonyms.size() < 4) {
                        synonyms.add(c.replace("_", " "));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return synonyms;
    }

    /**
     * Funktion returns a List of Hypo and Hypernyms of a given string
     * @param s Word for which you want to get Hypo and Hypersyms
     * @return List of Hypo and Hypernyms
     */
    public List<String> getRelatedNouns(String s) {
        List<String> result = new ArrayList<String>();
        IIndexWord iw = null;
        iw = dict.getIndexWord(s, POS.NOUN);

        if (iw != null) {

            IWordID wordID = iw.getWordIDs().get(0);
            IWord word = dict.getWord(wordID);
            ISynset synset = word.getSynset();

            List<ISynsetID> relatedSynsets1 = synset.getRelatedSynsets(Pointer.HYPERNYM);
            List<ISynsetID> relatedSynsets2 = synset.getRelatedSynsets(Pointer.HYPONYM);
            List<ISynsetID> relatedSynsets = new ArrayList<>(relatedSynsets1.size() + relatedSynsets2.size());
            relatedSynsets.addAll(relatedSynsets1);
            relatedSynsets.addAll(relatedSynsets2);
            //relatedSynsets.addAll(synset.getRelatedSynsets(Pointer.HYPONYM));

            List<IWord> words;
            for (ISynsetID sid : relatedSynsets) {
                words = dict.getSynset(sid).getWords();
                for (Iterator<IWord> i = words.iterator(); i.hasNext(); ) {
                    result.add(i.next().getLemma());
                }
            }
        }
        return result;
    }

}
