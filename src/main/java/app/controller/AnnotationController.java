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

public class AnnotationController {
    public static Route servePage = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        List <String> tweets = new LinkedList<> ();
        for (Status status :  TwitterHandler.getTweets()) {
            tweets.add("<b> @" + status.getUser().getScreenName() + " </b>  - " + status.getText());
        }
        model.put ("tweets",tweets);
        return ViewUtil.render(request, model, Path.Template.ANNOTATION);
      };
}
