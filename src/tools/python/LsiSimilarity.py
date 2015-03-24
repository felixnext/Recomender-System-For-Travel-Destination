from stop_words import get_stop_words
from gensim import corpora, models, similarities
import logging
from nltk import PorterStemmer
from nltk.tokenize import RegexpTokenizer

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

class LsiSimilarity:

    stop_words = get_stop_words('en')

    def getLsiSimilarity(self, docs1, docs2):

        tokenizer = RegexpTokenizer(r'\w+')
        #delete stopwords & tokenize
        docs1 = [[PorterStemmer().stem_word(word) for word in tokenizer.tokenize(doc.lower())
        if word not in self.stop_words] for doc in docs1]

        docs2 = [[PorterStemmer().stem_word(word) for word in tokenizer.tokenize(doc.lower())
        if word not in self.stop_words] for doc in docs2]

        all_tokens = sum(docs1, [])

        #transform into vecotra
        #creates dictionary on doc1
        dictionary = corpora.Dictionary([all_tokens])

        #create corpus
        corpus = [dictionary.doc2bow(text) for text in docs1]
        print corpus

        #transform corpora into lsi space
        lsi = models.LsiModel(corpus, id2word=dictionary, num_topics=2)

        #convert doc2 to vectors in doc1 space
        docs2_vectors = [dictionary.doc2bow(doc) for doc in docs2]
        docs2_lsi = [lsi[vec_bow] for vec_bow in docs2_vectors]
        #print docs2_vectors

        # transform corpus to LSI space and index it
        index = similarities.MatrixSimilarity(lsi[corpus])

        #claculate similarities
        sims = [index[vec_lsi]  for vec_lsi in  docs2_lsi]

        max_similarities = [max(sim) for sim in sims]

        return max_similarities


sim = LsiSimilarity()
documents = ["Human machine interface for lab abc computer applications",
              "A survey of user opinion of computer system response time",
              "The EPS user interface management system",
              "System and human system engineering testing of EPS",
              "Relation of user perceived response time to error measurement",
              "The generation of random binary unordered trees",
              "The intersection graph of paths in trees",
              "Graph minors IV Widths of trees and well quasi ordering",
              "Graph minors A survey"]

print sim.getLsiSimilarity(documents,["computer auto controling","Human computer interaction"])
