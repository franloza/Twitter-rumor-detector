package app.twitter;

import twitter4j.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Class that handles all the interaction with the Twitter API
 * @author Fran Lozano
 */
public class TwitterHandler {

    private static int minRetweet = 50;
    private static int minTweets = 10;

    public static List<Status> getTweets() {

        /*Authentication is done by means of twitter4j.properties file.
          The factory instance is re-useable and thread safe.*/
        Twitter twitter = new TwitterFactory().getInstance();
        List<Status> tweets = new LinkedList<>();
        try {
            Query query = new Query("#11s");
            query.setLocale("en");
            query.resultType(Query.MIXED);
            query.setCount(minTweets);


            //Filtering
            do {
                QueryResult result = twitter.search(query);
                for (Status tweet : result.getTweets()) {
                    if (tweet.getRetweetCount() >= minRetweet) {
                        tweets.add(tweet);
                    }
                }
                query = result.nextQuery();
            } while (tweets.size() < minTweets);
        } catch (TwitterException e) {
            e.printStackTrace();
        }
        return tweets;
    }
}
