

file1 = open("/Users/yevgen/Documents/data/master/dumps/elastic/wikipedia1.json", "r")
file2 = open("/Users/yevgen/Documents/data/master/dumps/elastic/travellerspoint0.json", "r")
file3 = open("/Users/yevgen/Documents/data/master/dumps/elastic/wikipedia0.json", "r")
file4 = open("/Users/yevgen/Documents/data/master/dumps/elastic/wikitravel0.json", "r")
path = "/Users/yevgen/Documents/data/master/dumps/elastic/target/"


doc_id = 0
file_number = 0
index ="wikitravel"

def recreateFile(f):

    out = open(path + "file" + str(file_number) + ".json", "w")

    global doc_id
    global file_number
    file_number = file_number + 1

    while True:
        line1 = f.readline()
        line2 = f.readline()

        if not line2:
            break

        out.write("""{"create": { "_index": "%s", "_type": "traveldata", "_id" : "%s" }}\n""" % (index, doc_id))
        out.write(line2)
        doc_id = doc_id + 1


recreateFile(file1)
recreateFile(file2)
recreateFile(file3)
recreateFile(file4)
