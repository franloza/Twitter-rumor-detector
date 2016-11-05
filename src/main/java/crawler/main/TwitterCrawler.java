package crawler.main;

import crawler.filter.PorterStemmer;
import app.model.ScoredTweet;
import crawler.filter.TfidfFilter;
import crawler.misc.Console;
import app.model.Tweet;
import crawler.twitter.TweetNavigator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

/**
 * Created by Jon Ayerdi on 11/10/2016.
 */
public class TwitterCrawler {

    //Crawler
    public static final String CREDENTIALS_FILE = "credentials.ini";
    public static final int MIN_RETWEETS = 1;
    public static final int FETCH_TWEETS = 10000;

    //TFIDF
    public static final double MIN_SCORE_TFIDF = 0.25;
    public static final int MIN_TERMS_TFIDF = 5;
    public static final int MIN_TERM_SIZE_TFIDF = 2;

    //TF
    public static final double MIN_SCORE_TF = 0.25;
    public static final int MIN_TERMS_TF = 5;
    public static final int MIN_TERM_SIZE_TF = 2;

    public TweetNavigator twitter;

    public TwitterCrawler(String credentialsFile) throws Exception {
        twitter = new TweetNavigator(credentialsFile);
    }

    public static void main(String[] args) throws Exception {
        Console.captureOutput();
        //Load credentials file
        TwitterCrawler crawler = new TwitterCrawler(CREDENTIALS_FILE);
        crawler.start4();
    }

    public void start() {
        try {
            Scanner in = new Scanner(System.in);
            Console.out.print("TweetID: ");
            long tweetID = Long.valueOf(in.nextLine());
            List<Tweet> result = crawl(tweetID);
            FileOutputStream out = new FileOutputStream(tweetID + ".csv");
            Tweet.writeToCSV(result,out);
            out.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void start2() {
        try {
            FileOutputStream out = new FileOutputStream("794148256037109760_Take2.csv");
            List<ScoredTweet> filtered = getBestTweetsTFIDF(new Tweet(twitter.getTweetByTweetID(794148256037109760L))
                    ,Tweet.readFromCSV("794148256037109760_TFIDF.csv"));
            ScoredTweet.writeToCSV(filtered, out);
            out.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void start3() {
        try {
            //Read TweetID from stdin
            Scanner in = new Scanner(System.in);
            Console.out.print("TweetID: ");
            long tweetID = Long.valueOf(in.nextLine());
            //Fetch the tweet
            Tweet query = new Tweet(twitter.getTweetByTweetID(tweetID));
            //Crawl tweets
            List<Tweet> crawled = crawl(query);
            //TFIDF
            List<ScoredTweet> scoredTFIDF = getBestTweetsTFIDF(query, crawled);
            FileOutputStream out = new FileOutputStream(tweetID + "_TFIDF.csv");
            ScoredTweet.writeToCSV(scoredTFIDF,out);
            out.close();
            //TF
            List<ScoredTweet> scoredTF = getBestTweetsTF(query, crawled);
            out = new FileOutputStream(tweetID + "_TF.csv");
            ScoredTweet.writeToCSV(scoredTF,out);
            out.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void start4() {
        try {
            Scanner in = new Scanner(new FileInputStream("Rumors.txt"));
            if(!new File("Rumors_TFIDF.csv").exists()) {
                PrintStream print = new PrintStream(new FileOutputStream("Rumors_TFIDF.csv"));
                print.println("id;userId;userName;text;creationDate;retweetCount;favoriteCount;textHash;crawledId;score");
                print.close();
            }
            if(!new File("Rumors_TF.csv").exists()) {
                PrintStream print = new PrintStream(new FileOutputStream("Rumors_TF.csv"));
                print.println("id;userId;userName;text;creationDate;retweetCount;favoriteCount;textHash;crawledId;score");
                print.close();
            }
            while(in.hasNext()) {
                try {
                    long tweetID = Long.valueOf(in.nextLine());
                    //Fetch the tweet
                    Tweet query = new Tweet(twitter.getTweetByTweetID(tweetID));
                    //Crawl tweets
                    List<Tweet> crawled = crawl(query);
                    //TFIDF
                    List<ScoredTweet> scoredTFIDF = getBestTweetsTFIDF(query, crawled);
                    FileOutputStream out = new FileOutputStream("Rumors_TFIDF.csv",true);
                    ScoredTweet.writeToCSV(scoredTFIDF,out);
                    out.close();
                    //TF
                    List<ScoredTweet> scoredTF = getBestTweetsTF(query, crawled);
                    out = new FileOutputStream("Rumors_TF.csv",true);
                    ScoredTweet.writeToCSV(scoredTF,out);
                    out.close();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            in.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Extracts tweets related to the provided tweet
     *
     * @param tweetID ID of the tweet to crawl
     * @return A list with the extracted tweets
     */
    public List<Tweet> crawl(long tweetID) {
        return crawl(new Tweet(twitter.getTweetByTweetID(tweetID)));
    }

    /**
     * Extracts tweets related to the provided tweet
     *
     * @param tweet The tweet to crawl
     * @return A list with the extracted tweets
     */
    public List<Tweet> crawl(Tweet tweet) {
        List<Tweet> resultList;
        int lastResultSize = 0;
        //Do not repeat users
        Set<String> crawledUsers = new HashSet<String>();
        //Do not repeat mentions
        Set<String> crawledMentions = new HashSet<String>();
        //We use Maps with textHashes as keys so that there are no duplicates
        Map<Integer,Tweet> toCrawlResponse = new HashMap<Integer,Tweet>();
        Map<Integer,Tweet> toCrawlUser = new HashMap<Integer,Tweet>();
        Map<Integer,Tweet> result = new HashMap<Integer,Tweet>(FETCH_TWEETS);
        //Start crawling from the initial tweet
        toCrawlResponse.put(tweet.getTextHash(),tweet);
        toCrawlUser.put(tweet.getTextHash(),tweet);
        Console.out.println("[Crawler]: Crawling TweetID " + tweet.getStatus().getId() + " started");
        while(result.size() + toCrawlResponse.size() + toCrawlUser.size() - 1 < FETCH_TWEETS) {
            Console.out.println("[Crawler]: Total result size: " + lastResultSize);
            //Fetch tweets from the same user
            Console.out.println("[Crawler]: User timeline tweets: " + toCrawlUser.size());
            for (Integer key : toCrawlUser.keySet()) {
                //Early exit
                if(result.size() + toCrawlResponse.size() + toCrawlUser.size() - 1 >= FETCH_TWEETS) break;
                Tweet crawl = toCrawlUser.get(key);
                List<Tweet> fetchList = Tweet.createList(twitter.getUserTimelineTweets(crawl.getStatus(), MIN_RETWEETS, 100));
                for(Tweet fetchTweet : fetchList)
                    toCrawlResponse.put(fetchTweet.getTextHash(),fetchTweet);
            }
            //Add already crawled tweets to result
            result.putAll(toCrawlUser);
            toCrawlUser.clear();
            //Fetch in-reply-to and response tweets
            Console.out.println("[Crawler]: Response tweets: " + toCrawlResponse.size());
            for (Integer key : toCrawlResponse.keySet()) {
                //Early exit
                if(result.size() + toCrawlResponse.size() + toCrawlUser.size() - 1 >= FETCH_TWEETS) break;
                Tweet crawl = toCrawlResponse.get(key);
                List<Tweet> fetchList = new ArrayList<>();//Tweet.createList(twitter.getResponseTweets(crawl.getStatus(), MIN_RETWEETS, 100));
                if(!crawledMentions.contains(crawl.getStatus().getUser().getScreenName())) {
                    fetchList = Tweet.createList(twitter.getMentionTweets(crawl.getStatus(), MIN_RETWEETS, 100));
                    crawledMentions.add(crawl.getStatus().getUser().getScreenName());
                }
                //fetchList.addAll(Tweet.createList(twitter.getInReplyToTweets(crawl.getStatus(), 100)));
                for(Tweet fetchTweet : fetchList)
                    if(!crawledUsers.contains(fetchTweet.getStatus().getUser().getScreenName())) {
                        toCrawlUser.put(fetchTweet.getTextHash(),fetchTweet);
                        crawledUsers.add(fetchTweet.getStatus().getUser().getScreenName());
                    }
            }
            //Add already crawled tweets to result
            result.putAll(toCrawlResponse);
            toCrawlResponse.clear();
            //If we have not fetched more tweets, we terminate
            if(result.size() > lastResultSize)
                lastResultSize = result.size();
            else
                break;
        }
        //Add already crawled tweets to result
        result.putAll(toCrawlUser);
        result.putAll(toCrawlResponse);
        //Remove original tweet
        result.remove(tweet.getTextHash());
        //Filter tweets
        resultList = Arrays.asList(result.values().toArray(new Tweet[0]));
        Console.out.println("[Crawler]: Crawling TweetID " + tweet.getStatus().getId() + " finished");
        return resultList;
    }

    /**
     * Returns the tweets that best match the query with their corresponding score
     *
     * @param query The query tweet
     * @param tweets The documents to score
     * @param mode Scoring mode
     * @param minScore Minimum score needed for a document to show in result
     * @param minTerms Minimum number of terms a document needs in order to show in result
     * @param minTermSize Minimum number of characters a term must have to count in the score
     * @return The tweets that best match the query with their corresponding score
     */
    public List<ScoredTweet> getBestTweets(Tweet query, List<Tweet> tweets, TfidfFilter.ScoringMode mode
            , double minScore, int minTerms, int minTermSize) {
        TfidfFilter filter = new TfidfFilter(mode
                , new PorterStemmer(), minTerms, minTermSize);
        List<ScoredTweet> scored = filter.getScores(query, tweets);
        List<ScoredTweet> filtered = new ArrayList<>();
        for(ScoredTweet scoredTweet : scored)
            if(scoredTweet.score >= minScore)
                filtered.add(scoredTweet);
        return filtered;
    }

    /**
     * Returns the tweets that best match the query with their corresponding TFIDF score
     *
     * @param query The query tweet
     * @param tweets The documents to score
     * @return The tweets that best match the query with their corresponding TFIDF score
     */
    public List<ScoredTweet> getBestTweetsTFIDF(Tweet query, List<Tweet> tweets) {
        return getBestTweets(query, tweets, TfidfFilter.ScoringMode.TFIDF
                , MIN_SCORE_TFIDF, MIN_TERMS_TFIDF, MIN_TERM_SIZE_TFIDF);
    }

    /**
     * Returns the tweets that best match the query with their corresponding TF score
     *
     * @param query The query tweet
     * @param tweets The documents to score
     * @return The tweets that best match the query with their corresponding TF score
     */
    public List<ScoredTweet> getBestTweetsTF(Tweet query, List<Tweet> tweets) {
        return getBestTweets(query, tweets, TfidfFilter.ScoringMode.TF
                , MIN_SCORE_TF, MIN_TERMS_TF, MIN_TERM_SIZE_TF);
    }

}
