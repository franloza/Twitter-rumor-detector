package app.twitter;

import twitter4j.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class that handles all the interaction with the Twitter API
 * @author Fran Lozano
 */
public class TwitterHandler {

    private static int minRetweet = 50;
    private static int minTweets = 10;
    private static Map<String, Integer> users = new HashMap<>();

    public static Map<String, Integer> getUsers() {
        return users;
    }

    public static List<Status> getTweets() {

        /*Authentication is done by means of twitter4j.properties file.
          The factory instance is re-useable and thread safe.*/
        Twitter twitter = new TwitterFactory().getInstance();
        List<Status> tweets = new LinkedList<>();

        try {
            Query query = new Query("\"9/11\" lang:en");
            query.setLocale("en");
            //query.setLang("en");
            query.resultType(Query.MIXED);
            query.setCount(minTweets);

            // TODO: Twitter async (http://twitter4j.org/en/code-examples.html#asyncAPI)
            // TODO: Skip repeated results

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
