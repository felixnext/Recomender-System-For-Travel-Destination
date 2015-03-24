import wikipedia

ny = wikipedia.suggest("New York (City)")

page = wikipedia.WikipediaPage("New York (City)")

print page.coordinates
