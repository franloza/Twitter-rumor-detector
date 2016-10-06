package app.controller;

import app.twitter.TwitterHandler;
import app.util.Path;
import app.util.ViewUtil;
import spark.Request;
import spark.Response;
import spark.Route;
import twitter4j.Status;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AnnotationController {

    private static TwitterHandler th;

    public static void start () {
        th = new TwitterHandler();
    }

    public static Route servePage = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        List <Status> tweets = th.getTweets();
        String query = th.getQuery();
        int nClassified = th.countClassified();
        int nRumor = th.countRumor();
        if(tweets != null) model.put ("tweets",tweets);
        if (query != null) model.put ("query",query);
        model.put("nClassified",nClassified);
        model.put("nRumors",nRumor);

        return ViewUtil.render(request, model, Path.Template.ANNOTATION);
      };

    public static Route processRequest = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        List <Status> tweets = th.getTweets();
        // Hashtag regex
        Pattern pattern = Pattern.compile("#\\w*");

        //Classify tweets
        for (int i = 1; i <= 10; i++) {
            List <String> labels = new LinkedList<>();
            labels.add(request.queryParams("assertion_"+i));
            labels.add(request.queryParams("topic_"+i));
            labels.add(request.queryParams("rumor_"+i));
            th.classifyTweet(Long.parseLong(request.queryParams("id_"+i)),labels);

            //Update the weights for the keyword(s)
            if (request.queryParams("rumor_" + (i)) != null) {
                th.updateKeywordsWeight(Long.parseLong(request.queryParams("id_"+i)));
                }
            }
        //Render new page
        String query = th.getQuery();
        int nClassified = th.countClassified();
        int nRumor = th.countRumor();
        if(tweets != null) model.put ("tweets",tweets);
        if (query != null) model.put ("query",query);
        model.put("nClassified",nClassified);
        model.put("nRumors",nRumor);
        return ViewUtil.render(request, model, Path.Template.ANNOTATION);
    };
}
