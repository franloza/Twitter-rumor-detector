package app.controller;

import app.twitter.TwitterHandler;
import app.util.Path;
import app.util.ViewUtil;
import spark.Request;
import spark.Response;
import spark.Route;
import twitter4j.Status;

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
        Status crawledTweet = th.getLastCrawledTweet().getStatus();
        if(crawledTweet != null) {
            model.put ("crawledTweet",crawledTweet);
            List<Status> tfTweets = th.getTfTweets(crawledTweet.getId());
            List<Status> tfIdfTweets = th.getTfIdfTweets(crawledTweet.getId());
            int nCrawled = th.countCrawled();
            if(tfTweets != null) model.put ("tfTweets",tfTweets);
            if(tfIdfTweets != null) model.put ("tfIdfTweets",tfIdfTweets);
            if(nCrawled >= 0) model.put("nCrawled",nCrawled);
        }
        return ViewUtil.render(request, model, Path.Template.RUMOR_CRAWLER);
    };
}
