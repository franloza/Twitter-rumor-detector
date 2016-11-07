import os
from flask import Flask, request
from ml import predict_rf, predict_dt
app = Flask(__name__)

@app.route('/dt/', methods=['POST'])
def predict_tweet_dt():
	text = request.form['text']
	rt = request.form['rt']
	fav = request.form['fav']
	date = request.form['date']

	return predict_dt(text, rt, fav, date)

@app.route('/rf/', methods=['POST'])
def predict_tweet_rf():
	text = request.form['text']
	rt = request.form['rt']
	fav = request.form['fav']
	date = request.form['date']

	return predict_rf(text, rt, fav, date)

if __name__ == "__main__":
	app.run()
