package app.controller;

import app.twitter.TwitterHandler;
import app.util.Path;
import app.util.ViewUtil;
import crawler.twitter.Tweet;
import spark.Request;
import spark.Response;
import spark.Route;
import twitter4j.Status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Fran Lozano
 */
public class RumorCrawlerController {
    private static TwitterHandler th;

    public static void start (TwitterHandler handler) {
        th = handler;
    }

    public static Route servePage = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        Tweet t = th.getLastCrawledTweet();
        List<Status> tfTweets = null;
        List<Status> tfIdfTweets = null;
        Status crawledTweet = null;
        int nCrawled = 0;
        if(t != null) {
            crawledTweet = t.getStatus();
            tfTweets = th.getTfTweets(crawledTweet.getId());
            tfIdfTweets = th.getTfIdfTweets(crawledTweet.getId());
            nCrawled = th.countCrawled();
        }
        if(tfTweets == null) tfTweets = new ArrayList<Status>();
        if(tfIdfTweets == null) tfIdfTweets = new ArrayList<Status>();
        if(nCrawled < 0) nCrawled = 0;

        model.put ("crawledTweet",crawledTweet);
        model.put ("tfTweets",tfTweets);
        model.put ("tfIdfTweets",tfIdfTweets);
        model.put("nCrawled",nCrawled);

        return ViewUtil.render(request, model, Path.Template.RUMOR_CRAWLER);
    };
}
