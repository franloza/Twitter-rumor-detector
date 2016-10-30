package app.twitter;

import app.db.DataManager;
import app.db.TweetDAO;
import crawler.filter.ScoredTweet;
import crawler.main.TwitterCrawler;
import crawler.twitter.Tweet;
import twitter4j.*;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Class that handles all the interaction with the Twitter API
 * @author Fran Lozano
 */
public class TwitterHandler {
    //Maximum number of past years in which the search will be done
    private static final int NUMBER_OF_YEARS =  6;
    private static int minRetweet = 0;
    private static int tweetsPerPage = 20;
    //Amount of keyword weight that is increased each tweet is classified as rumor
    private static double deltaWeight = 0.01;
    private static long cacheUnclassified = 100; //Number of unclassified examples allowed before it's deleted

    private TweetDAO tDao;
    private String currentQuery;

    private QueryBuilder queryBuilder;

    private Thread crawlerThread;
    private TwitterCrawler crawler;
    private BlockingQueue<Long> tweetsToCrawl;

    public TwitterHandler() {
        this.tDao = DataManager.getInstance().getTweetDao();
        this.queryBuilder = new QueryBuilder();

        //Crawler
        try {
            this.crawler = new TwitterCrawler(TwitterCrawler.CREDENTIALS_FILE);
        } catch (Exception e) {
            System.err.println("");
            System.exit(0);
        }
        tweetsToCrawl = new ArrayBlockingQueue<>(100);
        crawlerThread = new Thread() {
            public void run() {
                startCrawlingAsync();
            }
        };
        crawlerThread.start();
        //PONER ESTE CODIGO EN TWITTER HANDLER
        //SALVAR TWEETS A CSV
        /*
        try {
            FileOutputStream out = new FileOutputStream("tweets_unfiltered.csv");
            ClassifiedTweet.writeToCSVWithLabels(tDao.getClassifiedTweets(false),out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }*/

    }

    public List<Status> getTweets() throws TwitterException {

        /*Authentication is done by means of twitter4j.properties file.
          The factory instance is re-useable and thread safe.*/
        Twitter twitter = new TwitterFactory().getInstance();
        List<Status> tweets = new LinkedList<>();
        Query query = null;
        int usedResults = 0;
        if (query == null) {
            query = buildQuery();
            this.currentQuery = query.getQuery();
        }
        QueryResult result;
        long minId = 0;
        result = twitter.search(query);
        for (Status tweet : result.getTweets()) {
            //Get original tweet
            while (tweet.getRetweetedStatus() != null) {
                tweet = tweet.getRetweetedStatus();
            }
            //Minimum number of retweets
            if (tweet.getRetweetCount() >= minRetweet) {
                //Check if the tweet is duplicated
                String text = TweetFilter.basicFilter(tweet.getText());
                if (!tDao.checkDuplicate(tweet.getId(), "tweets", text.hashCode())) {
                    //Saves the tweet in the database and adds it to the result list
                    tweets.add(tweet);
                    saveTweet(tweet);
                    usedResults++;
                    minId = tweet.getId();
                }
            }
            //Return the tweets in the maximum number of tweets has been reached
            if (tweets.size() == tweetsPerPage) break;
        }
        tDao.updateQuery(currentQuery,minId,tweets.size());

        System.out.println("Query: "+ currentQuery + " - Number of found results: " + result.getTweets().size() +
        "- Number of used results: " + usedResults);
        return tweets;
    }


    private Query buildQuery() {
        Query query = queryBuilder.getQuery();
        this.currentQuery = query.getQuery();
        query.setLang("en");
        query.resultType(Query.MIXED);
        query.setCount(tweetsPerPage*5);
        long id = tDao.getMinId(currentQuery);
        if (id != 0) query.setMaxId(id-1);
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
        int count = 0;
        for(String label : labels)
            if(label != null)
                count++;
        if(count==3)
            tweetsToCrawl.offer(id);
        return tDao.setLabels(id,labels);
    }

    public void startCrawlingAsync() {
        while(true) {
            try {
                //Fetch tweetID
                long tweetID = tweetsToCrawl.take();
                //Fetch the tweet
                Tweet query = new Tweet(crawler.twitter.getTweetByTweetID(tweetID));
                //Crawl tweets
                List<Tweet> crawled = crawler.crawl(query);
                //TFIDF
                List<ScoredTweet> scoredTFIDF = crawler.getBestTweetsTFIDF(query, crawled);
                //TF
                List<ScoredTweet> scoredTF = crawler.getBestTweetsTF(query, crawled);
                //Save to DB
                for(ScoredTweet scoredTweet : scoredTFIDF)
                    tDao.insertCrawledTweetTfIdf(tweetID,scoredTweet.tweet.getStatus(),scoredTweet.score);
                for(ScoredTweet scoredTweet : scoredTF)
                    tDao.insertCrawledTweetTf(tweetID,scoredTweet.tweet.getStatus(),scoredTweet.score);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

    public void cleanUnclassified() {
        tDao.deleteOldestUnclassified(cacheUnclassified);
    }

    public TwitterCrawler getTwitterCrawler() {
        return crawler;
    }

    public Tweet getLastCrawledTweet() {
        long id = tDao.getLastCrawledTweetId("tweets_crawled_tfidf");
        if (id > 0) return new Tweet(crawler.twitter.getTweetByTweetID(id));
        else return null;
    }

    public List<Status> getTfTweets(long crawledId) {
        return tDao.getCrawledTweetsById("tweets_crawled_tf",crawledId);
    }

    public List<Status> getTfIdfTweets(long crawledId) {
        return tDao.getCrawledTweetsById("tweets_crawled_tfidf",crawledId);
    }

    public int countCrawled() {
        return tDao.countCrawled("tweets_crawled_tf") + tDao.countCrawled("tweets_crawled_tfidf");
    }
}
