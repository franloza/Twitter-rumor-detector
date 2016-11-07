package app.controller;

import app.ml.DecisionTreeClassifier;
import app.ml.DummyClassifier;
import app.ml.RandomForestClassifier;
import app.ml.TweetClassifier;
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
            TweetClassifier dummy = new DummyClassifier();
            TweetClassifier rf = new RandomForestClassifier();
            TweetClassifier dt = new DecisionTreeClassifier();
            model.put("dummyScore",Math.round(dummy.getRumorScore(tweet) * 100));
            model.put("rfScore",Math.round(rf.getRumorScore(tweet)));
            model.put("dtScore",Math.round(dt.getRumorScore(tweet)));
        }
        return ViewUtil.render(request, model, Path.Template.CLASSIFIER_POST);
    };
}
