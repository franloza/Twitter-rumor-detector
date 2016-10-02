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
import java.util.regex.Matcher;
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
        model.put ("tweets",tweets);
        model.put ("query",query);
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

            //Store hashtags classified as rumors
            if (request.queryParams("rumor_" + (i)) != null) {
                // Identify hashtags
                Matcher matcher = pattern.matcher(tweets.get(i - 1).getText());
                while (matcher.find()) {
                    System.out.println(matcher.group());
                    String hashtag = matcher.group();
                    //Save hashtags in database
                    th.saveHashtag(hashtag);
                }
                // TODO: Make POST params go from 0..9 ?
            }
        }
        //Render new page
        model.put ("tweets",tweets);
        return ViewUtil.render(request, model, Path.Template.ANNOTATION);
    };
}
