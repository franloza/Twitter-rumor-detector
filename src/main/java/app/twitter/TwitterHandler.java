package app.twitter;

import app.db.DataManager;
import app.db.TweetDAO;
import twitter4j.*;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Class that handles all the interaction with the Twitter API
 * @author Fran Lozano
 */
public class TwitterHandler {
    //Maximum number of past years in which the search will be done
    private static final int NUMBER_OF_YEARS =  6;
    private static int minRetweet = 0;
    private static int tweetsPerPage = 10;
    //Amount of keyword weight that is increased each tweet is classified as rumor
    private static double deltaWeight = 0.1;

    private TweetDAO tDao;
    private String currentQuery;

    private QueryBuilder queryBuilder;

    public TwitterHandler() {
        this.tDao = DataManager.getInstance().getTweetDao();
        this.queryBuilder = new QueryBuilder();
    }

    public List<Status> getTweets() throws TwitterException {

        /*Authentication is done by means of twitter4j.properties file.
          The factory instance is re-useable and thread safe.*/
        Twitter twitter = new TwitterFactory().getInstance();
        List<Status> tweets = new LinkedList<>();
        Query query = null;
        do {
            if (query == null) {
                query = buildQuery();
                this.currentQuery = query.getQuery();
            }
            QueryResult result;
            result = twitter.search(query);
            if (result.getCount() > 0) {
                for (Status tweet : result.getTweets()) {
                    //Minimum number of retweets
                    if (tweet.getRetweetCount() >= minRetweet) {
                        //Check if the tweet is duplicated
                        String text = tweet.getText();
                        //Remove URLs
                        text = tDao.cleanTweetText(text);
                        if (!tDao.checkDuplicate(tweet.getId(), "tweets", text.hashCode())) {
                            //Saves the tweet in the database and adds it to the result list
                            tweets.add(tweet);
                            saveTweet(tweet);
                        }
                    }
                    //Return the tweets in the maximum number of tweets has been reached
                    if (tweets.size() == tweetsPerPage) break;
                }
            }
            query = result.nextQuery();
        }
        while (tweets.size() < tweetsPerPage);
        return tweets;
    }


    private Query buildQuery() {
        Query query = queryBuilder.getQuery();
        this.currentQuery = query.getQuery();
        query.setLang("en");
        query.resultType(Query.MIXED);
        query.setCount(tweetsPerPage);
        long id = tDao.getMinId();
        if (id != 0) query.setMaxId(id);
        /*
        String year = getRandomYear();
        query.setSince(getSince(year));
        query.setUntil(getUntil(year));*/
        return query;
    }

    private void saveTweet(Status tweet) {
        tDao.insertTweet(tweet);
    }

    private void saveTweets(List<Status> tweets) {
        for(Status tweet:tweets) {
            tDao.insertTweet(tweet);
        }
    }

    private boolean updateWeight(String keyword,double deltaWeight) {
        return tDao.updateWeight(keyword,deltaWeight);
    }

    public boolean classifyTweet (long id, List<String> labels) {
        return tDao.setLabels(id,labels);
    }

    public String getQuery() {
        return this.currentQuery;
    }

    public void updateKeywordsWeight(long tweetId) {
        List<String> keywords = tDao.getKeywordsList();
        String tweet = tDao.getTweet(tweetId);
        for (String s: keywords ) {
            if(tweet.contains(s)) {
                System.out.println("Updating weight of keyword {" + s + "}");
                updateWeight(s,deltaWeight);
                DataManager.getInstance().getKeywords().update(s,deltaWeight);
            }
        }
    }

    public int countClassified() {
        return tDao.countClassified();
    }

    public int countRumor() {
        return tDao.countRumor();
    }

    public QueryBuilder getQueryBuilder() {
        return queryBuilder;
    }

    /*Not useful as Twitter API does not allow to retrieve tweets from more than 1 week"*/
    private String getRandomYear () {
        Random rnd = new Random();
        int year = Calendar.getInstance().get(Calendar.YEAR) - rnd.nextInt(NUMBER_OF_YEARS) -1;
        return String.valueOf(year);
    }

    private String getSince(String year) {
        return year + "-01-01";
    }

    private String getUntil(String year) {
        return year + "-12-31";
    }
}
