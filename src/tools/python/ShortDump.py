#!/usr/bin/python

file_in = "/Users/yevgen/Documents/data/master/dumps/elastic/travellerspoint0.json"
file_out = "/Users/yevgen/Documents/data/master/dumps/elastic/short.json"

json_data = open(file_in, "r")
out_file = open(file_out, "w")

counter = 10

for line in json_data:
    if counter == 0:
        break
    else:
        counter = counter - 1

    out_file.write(line)


out_file.close()
