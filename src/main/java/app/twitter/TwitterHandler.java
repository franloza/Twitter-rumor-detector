package app.twitter;

import app.db.TweetDAO;
import twitter4j.*;

import javax.sql.DataSource;
import java.util.*;

/**
 * Class that handles all the interaction with the Twitter API
 * @author Fran Lozano
 */
public class TwitterHandler {

    private static int minRetweet = 0;
    private static int tweetsPerPage = 10;
    private TweetDAO tDao;

    // Dictionary ("user" : rumor_tweets) that stores the amount of tweets classified
    // as rumors for each specific user
    private static Map<String, Integer> users = new HashMap<>();
    public static Map<String, Integer> getUsers() {
        return users;
    }

    // Dictionary ("hashtag" : appearences) that stores the amount of times a hashtag has appeared
    private static Map<String, Integer> hashtags = new HashMap<>();
    public static Map<String, Integer> getHashtags() {
        return hashtags;
    }

    public TwitterHandler(DataSource ds) {
        this.tDao = new TweetDAO(ds);
    }


    public List<Status> getTweets() {

        /*Authentication is done by means of twitter4j.properties file.
          The factory instance is re-useable and thread safe.*/
        Twitter twitter = new TwitterFactory().getInstance();
        List<Status> tweets = new LinkedList<>();
        List<Status> newTweets = new LinkedList<>();
        try {
            //TODO: Define keyword list
            //Temporary keyword stack
            Stack<String> keywords = new Stack<>();
            keywords.add("\"9/11\" lang:en");
            keywords.add("#11s lang:en");
            keywords.add("#TwinTowers lang:en");
            keywords.add("#11sAttacks lang:en");
            keywords.add("#11-s lang:en");

            Query query = new Query(keywords.pop());
            query.setLocale("en");
            //query.setLang("en");
            query.resultType(Query.MIXED);
            query.setCount(tweetsPerPage);

            // TODO: Twitter async (http://twitter4j.org/en/code-examples.html#asyncAPI)

            //Filtering
            do {
                if(query == null) {
                    query = new Query(keywords.pop());
                }
                QueryResult result = twitter.search(query);
                if (result.getCount() > 0) {
                    for (Status tweet : result.getTweets()) {
                        //TODO: It's pulling (classic) reweets? Try to find original.
                        //Minimum number of retweets
                        if (tweet.getRetweetCount() >= minRetweet) {
                            //Check if the tweet is already and the database
                            if (tDao.checkID(tweet.getId())) {
                                //Add it if it's in the database but has not been classified
                                //if (!tDao.checkClassified(tweet.getId())) {
                                    ;//tweets.add(tweet);
                                    // They're classified after this method returns.
                                    // Uncommenting the statement results in the same tweets
                                    // repeated every two pages.
                                //}
                            }
                            else{
                                tweets.add(tweet);
                                newTweets.add(tweet);
                            }
                        }
                        //Return the tweets in the maximum number of tweets has been reached
                        if (tweets.size() == tweetsPerPage) break;
                    }
                    query = result.nextQuery();
                }
            }
            while (tweets.size() < tweetsPerPage);
        } catch (TwitterException e) {
            e.printStackTrace();
        }
        //Save new tweets to the database
        saveTweets(newTweets);
        return tweets;
    }

    private void saveTweets(List<Status> tweets) {
        for(Status tweet:tweets) {
            tDao.insertTweet(tweet);
        }
    }

    public boolean classifyTweet (long id, List<String> labels) {
        return tDao.classifyTweet(id,labels);
    }
}
