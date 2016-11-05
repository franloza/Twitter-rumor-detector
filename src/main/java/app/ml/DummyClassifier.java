package app.ml;

import app.model.Tweet;

import java.util.Random;

/**
 * @author Fran Lozano
 */
public class DummyClassifier implements TweetClassifier{
    @Override
    public double getRumorScore(Tweet tweet) {
        return new Random().nextDouble();
    }
}
