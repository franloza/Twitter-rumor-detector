package app.twitter;

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
 * @author Fran Lozano
 */
public class ClassifiedTweet extends crawler.twitter.Tweet {
    private boolean assertion;
    private boolean topic;
    private boolean rumor;
    public ClassifiedTweet(Status status,boolean assertion, boolean topic, boolean rumor) {
        super(status);
        this.assertion = assertion;
        this.topic = topic;
        this.rumor = rumor;
    }

    /**
     * Reads tweets from CSV and returns the List
     *
     * @param filename File from which to read the CSV
     * @return The list of tweets read from the CSV
     */
    public static List<ClassifiedTweet> readFromCSVWithLabels(String filename) {
        List<ClassifiedTweet> tweets = new ArrayList<ClassifiedTweet>();
        try {
            CSVReader reader = new CSVReader(new FileReader(filename),';');
            List<String[]> csvEntries = reader.readAll();
            for(String[] row : csvEntries) {
                try {
                    ClassifiedTweet tweet = new ClassifiedTweet(
                            TwitterStatus.create(Long.valueOf(row[0])
                                    , TwitterUser.create(Long.valueOf(row[1]),row[2]),row[3]
                                    ,new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.0").parse(row[4])
                                    ,Integer.valueOf(row[5]),Integer.valueOf(row[6]))
                                    ,Boolean.valueOf(row[7]),Boolean.valueOf(row[7]),Boolean.valueOf(row[7])
                    );
                    tweets.add(tweet);
                } catch (Exception e1) {}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tweets;
    }


    /**
     * Writes a ; separated CSV with the provided classified tweets into the provided OutputStream
     *
     * @param tweets List of tweets to write to the CSV
     * @param out OutputStream in which to write the CSV
     */
    public static void writeToCSVWithLabels(List<ClassifiedTweet> tweets, OutputStream out) {
        PrintStream print = new PrintStream(out);
        print.println("id;userId;userName;text;creationDate;retweetCount;favoriteCount;textHash;assertion;topic;rumor");
        for(ClassifiedTweet tweet : tweets) {
            Status status = tweet.getStatus();
            print.println(status.getId()+";"+status.getUser().getId()+";"
                    +Tweet.toCSVString(status.getUser().getScreenName())+";"+toCSVString(status.getText())
                    +";"+new Timestamp(status.getCreatedAt().getTime())+";"+status.getRetweetCount()
                    +";"+status.getFavoriteCount()+";"+status.getText().hashCode()
                    +";"+tweet.isAssertion()+";"+tweet.isTopic()+";"+tweet.isRumor());
        }
    }

    //Getters
    public boolean isAssertion() {
        return assertion;
    }

    public boolean isTopic() {
        return topic;
    }

    public boolean isRumor() {
        return rumor;
    }
}
