import rake
import operator
rake_object = rake.Rake("SmartStoplist.txt", 5, 3, 5)
sample_file = open("../../resources/data/docs/rumors_filtered.txt", 'r',encoding = "ISO-8859-1")
text = sample_file.read()
keywords = rake_object.run(text)
print ("Keywords:", keywords)
