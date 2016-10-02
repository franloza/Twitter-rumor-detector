package app.twitter;

import app.db.DataManager;
import app.db.TweetDAO;
import twitter4j.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Class that handles all the interaction with the Twitter API
 * @author Fran Lozano
 */
public class TwitterHandler {

    private static int minRetweet = 0;
    private static int tweetsPerPage = 10;

    private TweetDAO tDao;
    private String currentQuery;

    public TwitterHandler() {
        this.tDao = DataManager.getInstance().getTweetDao();;
    }

    public List<Status> getTweets() {

        /*Authentication is done by means of twitter4j.properties file.
          The factory instance is re-useable and thread safe.*/
        Twitter twitter = new TwitterFactory().getInstance();
        List<Status> tweets = new LinkedList<>();
        try {
            Query query = QueryBuilder.getQuery();
            this.currentQuery = query.getQuery();
            query.setLang("en");
            query.resultType(Query.MIXED);
            query.setCount(tweetsPerPage);

            // TODO: Twitter async (http://twitter4j.org/en/code-examples.html#asyncAPI)

            //Filtering
            do {
                if(query == null) {
                    query = QueryBuilder.getQuery();
                    this.currentQuery = query.getQuery();
                }
                QueryResult result = twitter.search(query);
                if (result.getCount() > 0) {
                    for (Status tweet : result.getTweets()) {
                        //TODO: It's pulling (classic) reweets? Try to find original.
                        //Minimum number of retweets
                        if (tweet.getRetweetCount() >= minRetweet) {
                            //Check if the tweet is already and the database
                            if (!tDao.checkID(tweet.getId())) {
                                tweets.add(tweet);
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
        saveTweets(tweets);
        return tweets;
    }

    private void saveTweets(List<Status> tweets) {
        for(Status tweet:tweets) {
            tDao.insertTweet(tweet);
        }
    }

    public boolean saveHashtag(String hashtag) {
        return tDao.saveHashtag(hashtag);
    }

    public boolean classifyTweet (long id, List<String> labels) {
        return tDao.classifyTweet(id,labels);
    }

    public String getQuery() {
        return this.currentQuery;
    }
}
