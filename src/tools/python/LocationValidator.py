from bintrees import AVLTree
import io
import chardet

class LocationValidator:


    def __init__(self):
        file_name = "/Users/yevgen/Documents/data/master/dumps/wikitravelorg_wiki_en-20110605-current.xml/wikitravelorg_wiki_en-20110605-titles.txt"

        r = chardet.detect(open(file_name).read())
        charenc = r['encoding']

        print "File encoding: " + charenc
        location_file = io.open(file_name, "r", encoding=charenc)

        self.tree = AVLTree()

        #load location names into tree
        for line in location_file:
            line = line.encode("utf-8", "ignore")
            line = line.strip()
            self.tree.__setitem__(line, line)

        print "Tree size " + str(self.tree.__len__())


    def isLocation(self, location):
        if self.tree.__contains__(location):
            return True
        return False


#Test
#valid = LocationValidator()

#print valid.isLocation("New York")
#print valid.isLocation("Gifhorn")
#print valid.isLocation("Abrakadabra")
