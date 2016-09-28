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

public class AnnotationController {
    public static Route servePage = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        List <Status> tweets = TwitterHandler.getTweets();
        model.put ("tweets",tweets);
        return ViewUtil.render(request, model, Path.Template.ANNOTATION);
      };

    public static Route processRequest = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        List <Status> tweets = TwitterHandler.getTweets();
        model.put ("tweets",tweets);

        for (int i = 1; i <= 10; i++) {
            if (request.queryParams("rumor_" + i).equals("on")) {
                String user = tweets.get(i).getUser().getScreenName();
                Integer val = TwitterHandler.getUsers().getOrDefault(user, 0);
                TwitterHandler.getUsers().put(user, val);
            }
        }


        //Testing request
        for (int i = 1; i <= 10; i++) {
            System.out.println(
                    request.queryParams("assertion_" + i) + " - " +
                    request.queryParams("topic_" + i) + " - " +
                    request.queryParams("rumor_" + i)
            );
        }

        return ViewUtil.render(request, model, Path.Template.ANNOTATION);
    };
}
