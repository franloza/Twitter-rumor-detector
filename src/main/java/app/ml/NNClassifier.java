package app.ml;

import app.db.DataManager;
import app.db.TweetDAO;
import app.model.ClassifiedTweet;
import app.model.ScoredClassifiedTweet;
import app.model.ScoredTweet;
import app.model.Tweet;
import crawler.filter.PorterStemmer;
import crawler.filter.TfidfFilter;

import java.util.Comparator;
import java.util.List;

/**
 * Created by User on 08/11/2016.
 */
public class NNClassifier implements TweetClassifier{

    public static final int MIN_TERMS_TFIDF = 5;
    public static final int MIN_TERM_SIZE_TFIDF = 2;

    private TweetDAO tDao;
    private TfidfFilter scorer;

    public NNClassifier() {
        tDao = DataManager.getInstance().getTweetDao();
        scorer = new TfidfFilter(TfidfFilter.ScoringMode.TFIDF, new PorterStemmer(), MIN_TERMS_TFIDF, MIN_TERM_SIZE_TFIDF);
    }

    public ScoredClassifiedTweet getNearestTweet(Tweet tweet) {
        List<ClassifiedTweet> collection = tDao.getClassifiedTweets(false);
        List<ScoredTweet> scoredTweets = scorer.getScores(tweet, collection);
        scoredTweets.sort(new Comparator<ScoredTweet>() {
            public int compare(ScoredTweet o1, ScoredTweet o2) {
                return o1.score < o2.score ? 1 : (o1.score > o2.score ? -1 : 0);
            }
        });
        for(ScoredTweet current : scoredTweets) {
            ClassifiedTweet classified = ((ClassifiedTweet)(current.tweet));
            return new ScoredClassifiedTweet(classified.getStatus(),current.score
                    ,classified.isAssertion(),classified.isTopic(),classified.isRumor());
        }
        return null;
    }

    @Override
    public boolean isRumor(Tweet tweet) {
        ScoredClassifiedTweet classifiedTweet = getNearestTweet(tweet);
        if(classifiedTweet != null) return classifiedTweet.isRumor();
        else return false;
    }
}
