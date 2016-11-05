import os
from flask import Flask, request
from ml import hello
app = Flask(__name__)

@app.route('/')
def say_hi():
	return hello()

#@app.route('/login', methods=['GET', 'POST'])
@app.route('/user/<username>')
def show_user_profile(username):
	# show the user profile for that user
	return 'User %s' % username

if __name__ == "__main__":
	app.run()