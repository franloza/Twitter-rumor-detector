package app.twitter;

import twitter4j.*;

import java.util.List;

/**
 * Class that handles all the interaction with the Twitter API
 * @author Fran Lozano
 */
public class TwitterHandler {
    public static List<Status> getTweets() {

        /*Authentication is done by means of twitter4j.properties file.
          The factory instance is re-useable and thread safe.*/
        Twitter twitter = new TwitterFactory().getInstance();
        List<Status> tweets = null;
        try {
            Query query = new Query("#11s");
            query.setLocale("en");
            query.resultType(Query.MIXED);
            query.setCount(10);
            QueryResult result = twitter.search(query);
            tweets = result.getTweets();
        } catch (TwitterException e) {
            e.printStackTrace();
        }
        return tweets;
    }
}
