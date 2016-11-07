import pandas as pd
import numpy as np

# Pre-processing / feature extraction
import nltk
from datetime import datetime as dt
import string
from nltk.tokenize import TweetTokenizer
from nltk.corpus import stopwords
from nltk.stem.porter import PorterStemmer
from sklearn.feature_extraction.text import CountVectorizer

# Models
from sklearn.ensemble import RandomForestClassifier
from sklearn.tree import DecisionTreeClassifier
from sklearn.neighbors import KNeighborsClassifier

# Testing
from sklearn.model_selection import cross_val_score

import warnings
warnings.simplefilter('ignore')

tokenizer = TweetTokenizer()
stemmer = PorterStemmer()
stopws = set(stopwords.words('english'))
vectorizer = CountVectorizer(analyzer = 'word')

def clean_data(data):
	data.creationDate = pd.to_datetime(data.creationDate)
	data['creationDay'] = data.creationDate.apply(lambda x: x.weekday())
	data['creationMonth'] = data.creationDate.apply(lambda x: x.month)
	data.text = data.text.apply(tokenizer.tokenize)
	data.text = data.text.apply(lambda x: [w for w in x if w not in stopws and w.lower() not in string.punctuation])
	data.text = data.text.apply(lambda x: [stemmer.stem(w) for w in x])
	data['text_j'] = data.text.apply(lambda x: ' '.join(x))
	data['response'] = data.text_j.apply(lambda x: x.startswith(('@', ' @')))
	return data

tweets = pd.read_csv('tweets_unfiltered.csv', delimiter=';')
tweets = clean_data(tweets)

attributes = tweets[['retweetCount', 'favoriteCount', 'creationDay', 'creationMonth', 'response']]
attributes = pd.concat((attributes, pd.DataFrame(vectorizer.fit_transform(tweets.text_j).toarray())), axis=1)
classes = tweets.rumor

rf = RandomForestClassifier(n_estimators = 100)
dt = DecisionTreeClassifier()

# Train the classifiers
rf.fit(attributes, classes)
dt.fit(attributes, classes)

def predict_rf(text, rt, fav, date):
	data = pd.DataFrame([[text, date, rt, fav]], columns=['text','creationDate', 'retweetCount', 'favoriteCount'])
	data = clean_data(data)
	attributes = data[['retweetCount', 'favoriteCount', 'creationDay', 'creationMonth', 'response']]
	attributes = pd.concat((attributes, pd.DataFrame(vectorizer.transform(data.text_j).toarray())), axis=1)
	return str(rf.predict(attributes)[0])

def predict_dt(text, rt, fav, date):
	data = pd.DataFrame([[text, date, rt, fav]], columns=['text','creationDate', 'retweetCount', 'favoriteCount'])
	data = clean_data(data)
	attributes = data[['retweetCount', 'favoriteCount', 'creationDay', 'creationMonth', 'response']]
	attributes = pd.concat((attributes, pd.DataFrame(vectorizer.transform(data.text_j).toarray())), axis=1)
	return str(rf.predict(attributes)[0])


def test(X, y, f, k=10):
	"""
	Runs cross-validation for multiple models.

	:param X: input data
	:param y: input data
	:param f: scoring function
	:param k: number of folds
	:return: data frame with scoring mean and std for each model
	"""

	models = {
		'Random Forest': RandomForestClassifier(n_estimators = 100),
		'Decision Tree': DecisionTreeClassifier(),
		# 'k-NN': KNeighborsClassifier()
	}

	df = pd.DataFrame(columns=('mean', 'std'))
	for n, m in models.items():
		np.random.seed(121)
		fs = cross_val_score(m, X, y, scoring=f, cv=k)
		# print("%s\%s: %0.2f (+/- %0.2f)" % (n, f, fs.mean(), fs.std() * 2))
		df.loc[n] = fs.mean(), fs.std()
	return df

# res = test(attributes, classes, 'accuracy')
# print(res)