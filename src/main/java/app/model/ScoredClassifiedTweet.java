package app.model;

import twitter4j.Status;

/**
 * Created by User on 08/11/2016.
 */
public class ScoredClassifiedTweet extends ClassifiedTweet {

    private double score;

    public ScoredClassifiedTweet(Status status, double score, boolean assertion, boolean topic, boolean rumor) {
        super(status, assertion, topic, rumor);
        this.score = score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getScore() {
        return score;
    }

}
