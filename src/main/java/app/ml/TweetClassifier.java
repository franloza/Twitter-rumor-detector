package app.ml;

import app.model.Tweet;

/**
 * @author Fran Lozano
 */
public interface TweetClassifier {
    public boolean isRumor(Tweet tweet);
}
