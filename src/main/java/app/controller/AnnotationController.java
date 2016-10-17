package app.controller;

import app.twitter.TwitterHandler;
import app.util.Path;
import app.util.ViewUtil;
import spark.Request;
import spark.Response;
import spark.Route;
import twitter4j.Status;
import twitter4j.TwitterException;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AnnotationController {

    private static TwitterHandler th;

    public static void start (TwitterHandler handler) {
        th = handler;
    }

    public static Route servePage = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        List <Status> tweets;
        try {
            do {
                tweets = th.getTweets();
            } while (tweets.size() == 0);
        } catch (TwitterException e) {
            model.put("error", e.getMessage());
            return ViewUtil.render(request, model, Path.Template.TWITTER_ERROR);
        }
        String query = th.getQuery();
        int nClassified = th.countClassified();
        int nRumor = th.countRumor();
        model.put ("tweets",tweets);
        if (query != null) model.put ("query",query);
        model.put("nClassified",nClassified);
        model.put("nRumors",nRumor);
        return ViewUtil.render(request, model, Path.Template.ANNOTATION);
      };

    public static Route processRequest = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        List <Status> tweets;

        // Hashtag regex
        Pattern pattern = Pattern.compile("#\\w*");
        long id;
        int i = 1;
        //Classify tweets
        String strId = request.queryParams("id_"+i);
        while  (strId != null && !strId.equals("")) {
            List <String> labels = new LinkedList<>();
            labels.add(request.queryParams("assertion_"+i));
            labels.add(request.queryParams("topic_"+i));
            labels.add(request.queryParams("rumor_"+i));
            try {
                id = Long.parseLong(strId);
                th.classifyTweet(id, labels);
                //Update the weights for the keyword(s)
                if (request.queryParams("rumor_" + (i)) != null) {
                    th.updateKeywordsWeight(id);
                }
            } catch (NumberFormatException e) {
                System.err.println(e.getMessage());
            }
            i++;
            strId = request.queryParams("id_"+i);
            }

        //Delete non classified if the limit was reached
        th.cleanUnclassified();

        //Render new page
        try {
            do {
                tweets = th.getTweets();
            } while(tweets.size() == 0);
        } catch (Exception e) {
            model.put("error",e.getMessage());
            return ViewUtil.render(request, model, Path.Template.TWITTER_ERROR);
        }
        String query = th.getQuery();
        int nClassified = th.countClassified();
        int nRumor = th.countRumor();
        model.put ("tweets",tweets);
        if (query != null) model.put ("query",query);
        model.put("nClassified",nClassified);
        model.put("nRumors",nRumor);
        return ViewUtil.render(request, model, Path.Template.ANNOTATION);
    };
}
