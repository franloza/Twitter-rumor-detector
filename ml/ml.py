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
from sklearn.svm import NuSVC, LinearSVC

# Testing
from sklearn.model_selection import cross_val_score

import warnings
warnings.simplefilter('ignore')

tokenizer = TweetTokenizer()
stemmer = PorterStemmer()
stopws = set(stopwords.words('english'))
vectorizer = CountVectorizer(analyzer = 'word')

tweets = pd.read_csv('tweets_unfiltered.csv', delimiter=';')
tweets.creationDate = pd.to_datetime(tweets.creationDate)
tweets['creationDay'] = tweets.creationDate.apply(lambda x: x.weekday())
tweets['creationMonth'] = tweets.creationDate.apply(lambda x: x.month)
tweets.text = tweets.text.apply(tokenizer.tokenize)
tweets.text = tweets.text.apply(lambda x: [w for w in x if w not in stopws and w.lower() not in string.punctuation])
tweets.text = tweets.text.apply(lambda x: [stemmer.stem(w) for w in x])
tweets['text_j'] = tweets.text.apply(lambda x: ' '.join(x))
tweets['response'] = tweets.text_j.apply(lambda x: x.startswith(('@', ' @')))

attributes = tweets[['retweetCount', 'favoriteCount', 'creationDay', 'creationMonth', 'response']]
attributes = pd.concat((attributes, pd.DataFrame(vectorizer.fit_transform(tweets.text_j).toarray())), axis=1)

classes = tweets.rumor
# classes = tweets[['topic', 'assertion', 'rumor']]




rf = RandomForestClassifier(n_estimators = 100)
dt = DecisionTreeClassifier()

rf.fit(attributes, classes)
dt.fit(attributes, classes)

def predict_rf(text, rt, fav, date):

	# rf.predict()
	return "Hi"

def predict_dt(text, rt, fav, date):
	return "Hi"




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
		# print("%s\F1: %0.2f (+/- %0.2f)" % (n, fs.mean(), fs.std() * 2))
		df.loc[n] = fs.mean(), fs.std()
	return df

# res = test(attributes, classes, 'f1')