package app.controller;

import app.twitter.TwitterHandler;
import app.util.*;
import spark.*;
import twitter4j.Status;

import java.util.*;

public class IndexController {
    public static Route serveIndexPage = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        List <String> tweets = new LinkedList<> ();
        for (Status status :  TwitterHandler.getTweets()) {
            tweets.add("<b> @" + status.getUser().getScreenName() + " </b> :" + status.getText());
        }
        model.put ("tweets",tweets);
        return ViewUtil.render(request, model, Path.Template.INDEX);
      };
}
