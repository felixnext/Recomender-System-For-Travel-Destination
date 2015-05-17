import wikipedia
from wikipedia.exceptions import DisambiguationError
import pickle
import copy

#from LsiSimilarity import LsiSimilarity


class WikipediaDownloader:

    #lsi_similarity = LsiSimilarity()

    def downloadArticle(self, texts, title):
        pages = []
        #download article
        try:
            page = wikipedia.WikipediaPage(title)
            pages.append(page)
        except DisambiguationError as de:
            page = self.disambiguateArticles(texts,de.options, title)
            pages = page
        except Exception as e:
            try:
                print "ERROR 1: "  +  str(e).encode("utf-8")
            except:
                pass
            return ""

        return pages

    #if exception than download all suggestion
    #calculate return with max similarity
    def disambiguateArticles(self, texts, titles, page_title):

        pages = []

        for title in titles:
            try:
                pages.append(wikipedia.WikipediaPage(title) )

            except Exception as e:
                print "ERROR 2: "  +  str(e)

        pages_content = [page.content for page in pages]

        #calculate similarity?
        #sim = self.lsi_similarity.getLsiSimilarity(texts, pages_content)


        #title suggestion occurrences in article
        #new_titles = [ title.replace(page_title, "")
        #.replace("(", "").replace(")", "") for title in titles]
        #occurrences = [[text.count(title) for text in texts] for title in new_titles]

        return pages




wikid = WikipediaDownloader()
#print wikid.downloadArticle("bla", "ABC Islands")

out = open("wikipedia_articles.xml", "wb")

out.write("<pages>\n")


locations =  pickle.load(open("./data/locations.p","rb"))


while len(locations) > 1:
    location = locations.pop()

    #proof location
    if "User" in location or "Image" in location or "Talk" in location or
     "Wikitravel" in location or "Category" in location:
        continue

    out.write("  <page request_title=\"" + location + "\">\n")

    wiki_pages = wikid.downloadArticle("bla", location)

    print wiki_pages
    if wiki_pages == "":
        out.write("EMPTY")
    else:
        for wiki_page in wiki_pages:
            out.write("    <title>" + wiki_page.title.encode("utf-8") + "</title>\n")
            try:
                out.write("    <coordinates lat=\"" + str(wiki_page.coordinates[0]) + "\" lon=\""  + str(wiki_page.coordinates[1]) + "\"/>\n")
            except:
                out.write("    <coordinates lat=\"" + "none" + "\" lon=\""  + "none" + "\"/>\n")
            out.write("    <content>" + wiki_page.content.encode("utf-8") + "</content>\n")

    out.write("  </page>\n")

out.write("</pages>\n")

#save coordinate of location
