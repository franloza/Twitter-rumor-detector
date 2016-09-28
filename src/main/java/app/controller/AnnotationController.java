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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

            if (request.queryParams("rumor_" + (i)) != null) {
                String user = tweets.get(i - 1).getUser().getScreenName();
                Integer val = TwitterHandler.getUsers().getOrDefault(user, 0);
                TwitterHandler.getUsers().put(user, val + 1);
                // TODO: Add to database on a users table or keep in memory?

                // Identify hashtags
                Matcher matcher = pattern.matcher(tweets.get(i - 1).getText());
                while (matcher.find()) {
                    System.out.println(matcher.group());
                    String hashtag = matcher.group();
                    val = TwitterHandler.getHashtags().getOrDefault(hashtag, 0);
                    TwitterHandler.getHashtags().put(hashtag, val+1);
                }
                // TODO: Add to database on a hashtags table or keep in memory?

                // TODO: Make POST params go from 0..9 ?
            }
        }
        //Render new page
        model.put ("tweets",tweets);
        return ViewUtil.render(request, model, Path.Template.ANNOTATION);
    };
}
