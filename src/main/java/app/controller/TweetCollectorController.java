package app.controller;

import app.twitter.TweetCollector;
import app.twitter.TwitterHandler;
import app.util.spark.Path;
import app.util.spark.ViewUtil;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Fran Lozano
 */
public class TweetCollectorController {
    private static TweetCollector kc;

    public static void start (TwitterHandler handler) {
        kc = new TweetCollector(handler.getQueryBuilder().getNeuralNetwork());
        kc.start();
    }

    public static Route servePage = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        List<String> tweets = kc.getTweets(10);
        int nClassified = kc.countCollected();
        if(tweets != null) model.put ("tweets",tweets);
        if(nClassified >= 0) model.put("nClassified",nClassified);
        return ViewUtil.render(request, model, Path.Template.KEYWORD_CRAWLER);
    };
}
