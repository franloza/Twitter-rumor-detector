import rake
import json
import sys

def main(argv):
    if (len(argv) >= 3):
        minCharacters = int(argv[0])
        maxWords = int(argv[1])
        minFrequency = int(argv[2])
    elif (len(argv) == 0):
        #Default parameters
        minCharacters = 5
        maxWords = 3
        minFrequency = 5
    else:
        print ('Usage: main.py <minCharacters> <maxWords> <minFrequency>')
        sys.exit()
    rake_object = rake.Rake("src/main/python/keyword-extractor/SmartStoplist.txt", minCharacters,maxWords,minFrequency)
    sample_file = open("src/main/resources/data/docs/rumors_filtered.txt", 'r',encoding = "ISO-8859-1")
    text = sample_file.read()
    keywords = rake_object.run(text)
    print (json.dumps(dict(keywords)))

if __name__ == "__main__":
    main(sys.argv[1:])