from stop_words import get_stop_words
from gensim import corpora, models, similarities
import logging
from nltk import PorterStemmer
from nltk.tokenize import RegexpTokenizer

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

class LsiSimilarity:

    stop_words = get_stop_words('en')

    def getLsiSimilarity(self, docs, vectors):

        tokenizer = RegexpTokenizer(r'\w+')
        #delete stopwords & tokenize
        docs = [[word for word in tokenizer.tokenize(doc.lower()) if word not in self.stop_words] for doc in docs]
        vectors = [[word for word in tokenizer.tokenize(doc.lower()) if word not in self.stop_words] for doc in vectors]

        all_tokens = sum(docs, [])

        print all_tokens
        print vectors

        #stemming

        print self.stop_words.__contains__(vectors)

        #stem words

        #transform into vecotra

        #compare


sim = LsiSimilarity()
sim.getLsiSimilarity(["a a blam, ba","b"],["a","you are Bla.","blabla"])
