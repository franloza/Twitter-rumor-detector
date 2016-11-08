package app.controller;

import app.ml.DecisionTreeClassifier;
import app.ml.NNClassifier;
import app.ml.RandomForestClassifier;
import app.ml.TweetClassifier;
import app.model.ScoredClassifiedTweet;
import app.model.Tweet;
import app.twitter.TwitterHandler;
import app.util.spark.Path;
import app.util.spark.ViewUtil;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Fran Lozano
 */
public class TweetClassifierController {

    private static TwitterHandler th;

    public static void start (TwitterHandler handler) {
        th = handler;
    }

    public static Route servePage = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        return ViewUtil.render(request, model, Path.Template.CLASSIFIER_GET);
    };

    public static Route processRequest = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        String url = request.queryParams("url");
        Tweet tweet = th.parseURL(url);
        if(tweet != null) {
            model.put("tweet", tweet.getStatus());
            NNClassifier nn = new NNClassifier();
            TweetClassifier rf = new RandomForestClassifier();
            TweetClassifier dt = new DecisionTreeClassifier();
            ScoredClassifiedTweet scoredTweet = nn.getNearestTweet(tweet);
            model.put("rfPred",rf.isRumor(tweet));
            model.put("dtPred",dt.isRumor(tweet));
            model.put("nnTweet", scoredTweet.getStatus());
            model.put("nnSimilarity",Math.round(scoredTweet.getScore()*100));
            model.put("nnPred",scoredTweet.getScore());
        }
        return ViewUtil.render(request, model, Path.Template.CLASSIFIER_POST);
    };
}
