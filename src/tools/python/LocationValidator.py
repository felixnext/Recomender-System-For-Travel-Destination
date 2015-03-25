from bintrees import AVLTree
import io
import chardet
from sets import Set
import pickle

class LocationValidator:

    tree = AVLTree()

    def __init__(self):
        file_name = "/Users/yevgen/Documents/data/master/dumps/wikitravelorg_wiki_en-20110605-current.xml/wikitravelorg_wiki_en-20110605-titles.txt"

        r = chardet.detect(open(file_name).read())
        charenc = r['encoding']

        print "File 1 encoding: " + charenc
        location_file = io.open(file_name, "r", encoding=charenc)



        #load location names into tree
        for line in location_file:
            line = line.encode("utf-8", "ignore")
            line = line.strip()
            self.tree.__setitem__(line, line)

        print "Tree size " + str(self.tree.__len__())

        file2_name = "/Users/yevgen/Documents/data/master/virtualpoint/items.json_locations.xml"
        r = chardet.detect(open(file2_name).read())
        charenc = r['encoding']

        print "File 2 encoding: " + charenc
        location_file = io.open(file2_name, "r", encoding=charenc)

        #load location names into tree
        for line in location_file:
            line = line.encode("utf-8", "ignore")
            line = line.strip()
            self.tree.__setitem__(line, line)

        print "Tree size " + str(self.tree.__len__())

    def getAllLocations(self):
        return self.tree.__iter__()

    def isLocation(self, location):
        if self.tree.__contains__(location):
            return True
        return False








#Test
#valid = LocationValidator()
#for item in valid.getAllLocations:
#    print item

#print valid.isLocation("New York")
#print valid.isLocation("Gifhorn")
#print valid.isLocation("Abrakadabra")


#serialize locations

file_name = "/Users/yevgen/Documents/data/master/dumps/wikitravelorg_wiki_en-20110605-current.xml/wikitravelorg_wiki_en-20110605-titles.txt"

r = chardet.detect(open(file_name).read())
charenc = r['encoding']

print "File 1 encoding: " + charenc
location_file = io.open(file_name, "r", encoding=charenc)

locations = Set()
#load location names into tree
for line in location_file:
    line = line.encode("utf-8", "ignore")
    line = line.strip()
    locations.add(line)

file2_name = "/Users/yevgen/Documents/data/master/virtualpoint/items.json_locations.xml"
r = chardet.detect(open(file2_name).read())
charenc = r['encoding']

print "File 2 encoding: " + charenc
location_file = io.open(file2_name, "r", encoding=charenc)

#load location names into tree
for line in location_file:
    line = line.encode("utf-8", "ignore")
    line = line.strip()
    locations.add(line)

print len(locations)


pickle.dump( locations, open( "locations.p", "wb" ) )
