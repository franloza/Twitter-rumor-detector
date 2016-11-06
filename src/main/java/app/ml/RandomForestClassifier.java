package app.ml;

import app.model.Tweet;
import app.util.SendPost;

/**
 * @author guille
 */
public class RandomForestClassifier implements TweetClassifier {
	public double getRumorScore(Tweet tweet) {
		String url = "localhost:5000/rf";
		Integer rt = tweet.getStatus().getRetweetCount();
		String date = tweet.getStatus().getCreatedAt().toString();
		Integer fav = tweet.getStatus().getFavoriteCount();
		String text = tweet.getStatus().getText();

		return Float.parseFloat(SendPost.send(url, text, rt, fav, date));
	}
}
