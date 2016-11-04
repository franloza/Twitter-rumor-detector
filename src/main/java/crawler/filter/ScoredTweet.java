package crawler.filter;

import com.opencsv.CSVReader;
import crawler.twitter.Tweet;
import crawler.twitter.TwitterStatus;
import crawler.twitter.TwitterUser;
import twitter4j.Status;

import java.io.FileReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jon Ayerdi on 16/10/2016.
 */
public class ScoredTweet implements Comparable<ScoredTweet> {
    public Tweet tweet;
    public long crawledId;
    public double score;

    public ScoredTweet(Tweet tweet, long crawledId, double score) {
        this.tweet = tweet;
        this.crawledId = crawledId;
        this.score = score;
    }

    public ScoredTweet(Tweet tweet, long crawledId) {
        this(tweet, crawledId, 0.0);
    }

    /**
     * Writes a ; separated CSV with the provided tweets into the provided OutputStream
     *
     * @param tweets List of tweets to write to the CSV
     * @param out OutputStream in which to write the CSV
     */
    public static void writeToCSV(List<ScoredTweet> tweets, OutputStream out) {
        PrintStream print = new PrintStream(out);
        for(ScoredTweet tweet : tweets) {
            Status status = tweet.tweet.getStatus();
            print.println(status.getId()+";"+status.getUser().getId()+";"
                    +Tweet.toCSVString(status.getUser().getScreenName())+";"+Tweet.toCSVString(status.getText())
                    +";"+new Timestamp(status.getCreatedAt().getTime())+";"+status.getRetweetCount()
                    +";"+status.getFavoriteCount()+";"+status.getText().hashCode()
                    +";"+tweet.crawledId + ";" + tweet.score);
        }
    }

    /**
     * Reads tweets from CSV and returns the List
     *
     * @param filename File from which to read the CSV
     * @return The list of tweets read from the CSV
     */
    public static List<ScoredTweet> readFromCSV(String filename) {
        List<ScoredTweet> tweets = new ArrayList<ScoredTweet>();
        try {
            CSVReader reader = new CSVReader(new FileReader(filename),';');
            List<String[]> csvEntries = reader.readAll();
            for(String[] row : csvEntries) {
                try {
                    Tweet tweet = new Tweet(
                            TwitterStatus.create(Long.valueOf(row[0])
                                    , TwitterUser.create(Long.valueOf(row[1]),row[2]),row[3]
                                    ,new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.0").parse(row[4])
                                    ,Integer.valueOf(row[5]),Integer.valueOf(row[6]))
                    );
                    tweets.add(new ScoredTweet(tweet,Long.valueOf(row[7]),Double.valueOf(row[8])));
                } catch (Exception e1) {}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tweets;
    }

    @Override
    public int compareTo(ScoredTweet o) {
        return this.score > o.score ? 1 : (this.score < o.score ? -1 : 0);
    }
}
