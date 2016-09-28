package app.controller;

import app.twitter.TwitterHandler;
import app.util.Path;
import app.util.ViewUtil;
import spark.Request;
import spark.Response;
import spark.Route;
import twitter4j.Status;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AnnotationController {

    private static TwitterHandler th;

    public static void setDataSource (DataSource ds) {
        th = new TwitterHandler(ds);
    }

    public static Route servePage = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        List <Status> tweets = th.getTweets();
        model.put ("tweets",tweets);
        return ViewUtil.render(request, model, Path.Template.ANNOTATION);
      };

    public static Route processRequest = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        //Classify tweets
        for (int i = 1; i <= 10; i++) {
            List <String> labels = new LinkedList<>();
            labels.add(request.queryParams("assertion_"+i));
            labels.add(request.queryParams("topic_"+i));
            labels.add(request.queryParams("rumor_"+i));
            th.classifyTweet(Long.parseLong(request.queryParams("id_"+i)),labels);
        }
        //Render new page
        List <Status> tweets = th.getTweets();
        model.put ("tweets",tweets);
        return ViewUtil.render(request, model, Path.Template.ANNOTATION);
    };
}
