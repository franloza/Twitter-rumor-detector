package app.ml;

import app.model.Tweet;
import app.util.SendPost;

/**
 * Created by guille on 11/5/16.
 */
public class DecisionTreeClassifier implements TweetClassifier {

	@Override
	public boolean isRumor (Tweet tweet) {
		String url = "http://localhost:5000/dt/";
		Integer rt = tweet.getStatus().getRetweetCount();
		String date = tweet.getStatus().getCreatedAt().toString();
		Integer fav = tweet.getStatus().getFavoriteCount();
		String text = tweet.getStatus().getText();
		String result = SendPost.send(url, text, rt, fav, date);
		System.out.println("Result: " + result);
		return result.equals("True");
	}

}
