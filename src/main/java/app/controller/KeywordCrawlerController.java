package app.controller;

import app.twitter.KeywordCrawler;
import app.twitter.TwitterHandler;
import app.util.Path;
import app.util.ViewUtil;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Fran Lozano
 */
public class KeywordCrawlerController {
    private static KeywordCrawler kc;

    public static void start (TwitterHandler handler) {
        kc = new KeywordCrawler(handler.getQueryBuilder().getNeuralNetwork());
        kc.start();
    }

    public static Route servePage = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        List<String> tweets = kc.getTweets(10);
        int nClassified = kc.countCrawled();
        if(tweets != null) model.put ("tweets",tweets);
        if(nClassified >= 0) model.put("nClassified",nClassified);
        return ViewUtil.render(request, model, Path.Template.KEYWORD_CRAWLER);
    };
}
