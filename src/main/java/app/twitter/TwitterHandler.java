package app.twitter;

import app.db.DataManager;
import app.db.TweetDAO;
import app.ml.DuplicateDetector;
import app.ml.KeywordExtractor;
import app.ml.KeywordExtractorAdapter;
import app.model.ClassifiedTweet;
import app.model.ScoredTweet;
import app.model.Tweet;
import crawler.main.TwitterCrawler;
import twitter4j.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
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
    private static long cacheUnclassified = 100; //Number of unclassified examples allowed before they're deleted

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

        //Export tweets to CSV for Classifier
        //classifiedToCSV("src/main/python/ml/",false);
        //classifiedToCSV("src/main/resources/data/docs/",true);
        //classifiedToCSV("src/main/resources/data/docs/",false);

        //Extract keywords
        extractKeywords();

    }

    public List<Status> getTweets() throws TwitterException {

        /*Authentication is done by means of twitter4j.properties file.
          The factory instance is re-useable and thread safe.*/
        Twitter twitter = new TwitterFactory().getInstance();
        List <String> tweetsText = new LinkedList<>();
        List<Status> tweets = new LinkedList<>();
        int usedResults = 0;
        Query query = buildQuery();
        this.currentQuery = query.getQuery();
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
                if (!tDao.checkDuplicate(tweet.getId(), text.hashCode())) {
                    //Saves the tweet in the database and adds it to the result list
                    if (!DuplicateDetector.isDuplicated(TweetFilter.filter(text),tweetsText)){
                        tweets.add(tweet);
                        tweetsText.add(TweetFilter.filter(tweet.getText()));
                        saveTweet(tweet);
                        usedResults++;
                        minId = tweet.getId();
                    }
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
        return query;
    }

    private void saveTweet(Status tweet) {
        tDao.insertTweet(tweet);
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

    /**
     * Function that uses Keyword Extractor component to get keywords from the tweets classified as rumors
     *
     */
    public void extractKeywords () {
        //Update list of rumor tweets
        rumorsToTXT("src/main/resources/data/docs/",true);

        KeywordExtractor ke = new KeywordExtractorAdapter();
        List<String> extractedKeywords = ke.getKeywords();
        List<String> keywords = tDao.getKeywordsList();

        for (String extracted: extractedKeywords ) {
            boolean exists = false;
            for (String current: keywords ) {
                if (current.contains(extracted)) exists = true;
            }
            if (!exists) {
                System.out.println("Extracted new keyword: {" + extracted + "}");
                DataManager.getInstance().getKeywords().add(extracted, 0.25);
                //Update database
                updateWeight(extracted, 0.25);
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
        long id = tDao.getLastCrawledTweetId("tf");
        if (id > 0) return new Tweet(crawler.twitter.getTweetByTweetID(id));
        else return null;
    }

    public List<Status> getTfTweets(long crawledId) {
        return tDao.getCrawledTweetsById("tf",crawledId);
    }

    public List<Status> getTfIdfTweets(long crawledId) {
        return tDao.getCrawledTweetsById("tfidf",crawledId);
    }

    public int countCrawled() {
        return tDao.countCrawled("tf") + tDao.countCrawled("idf");
    }

    private void classifiedToCSV(String path, boolean filtered) {
        System.out.println("Exporting classified tweets to CSV file in " + path);
        try {
            if (filtered) path += "tweets_filtered.csv"; else path += "tweets_unfiltered.csv";
            FileOutputStream out = new FileOutputStream(path);
            ClassifiedTweet.writeToCSVWithLabels(tDao.getClassifiedTweets(filtered),out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void classifiedToTXT(String path, boolean filtered) {
        System.out.println("Exporting classified tweets to TXT file in " + path);
        try {
            if (filtered) path += "tweets_filtered.txt"; else path += "tweets_unfiltered.txt";
            FileOutputStream out = new FileOutputStream(path);
            ClassifiedTweet.writeToFile(tDao.getClassifiedTweets(filtered),out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void rumorsToTXT(String path, boolean filtered) {
        System.out.println("Exporting rumors to TXT file in " + path);
        try {
            if (filtered) path += "rumors_filtered.txt"; else path += "rumors_unfiltered.txt";
            FileOutputStream out = new FileOutputStream(path);
            ClassifiedTweet.writeToFile(tDao.getClassifiedTweets(filtered,true),out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Tweet parseURL(String url) {
        URI uri;
        try {
            uri = new URI(url);
            String[] segments = uri.getPath().split("/");
            if(segments.length > 0) {
                String idStr = segments[segments.length-1];
                long id = Long.parseLong(idStr);
                Twitter twitter = new TwitterFactory().getInstance();
                twitter.showStatus(id);
                Status status = twitter.showStatus(id);
                if(status != null)return new Tweet(status);
                else return null;
            }
        } catch (URISyntaxException|NumberFormatException|TwitterException e) {
            System.err.println("The tweet couldn't be retrieved'");
            return null;
        }
       return null;
    }
}
