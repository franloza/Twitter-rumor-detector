package crawler.twitter;

import com.opencsv.CSVReader;
import twitter4j.Status;

import java.io.FileReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jon Ayerdi on 11/10/2016.
 */
public class Tweet {

    private Status status;

    public Tweet(Status status) {
        this.status = status;
    }

    /**
     * Compares the text of this tweet and the provided tweet
     *
     * @param tweet Other tweet to compare
     * @return true if the texts are identical, false otherwise
     */
    public boolean isDuplicateOf(Tweet tweet) {
        return this.getTextHash() == tweet.getTextHash();
    }

    public String toString() {
        return "ID: " + status.getId() + "\nDate: " + status.getCreatedAt()
                + "\nUser: " + status.getUser().getScreenName() + "\nText: " + status.getText();
    }

    /**
     * Creates a List<Tweet> out of the statuses from the provided List
     *
     * @param statuses List of Status instances
     * @return A list of Tweet instances created from the provided statuses
     */
    public static List<Tweet> createList(List<Status> statuses) {
        List<Tweet> tweets = new ArrayList<Tweet>();
        try {
            for(Status status : statuses)
                tweets.add(new Tweet(status));
        } catch (Exception e) {}
        return tweets;
    }

    /**
     * Reads tweets from CSV and returns the List
     *
     * @param filename File from which to read the CSV
     * @return The list of tweets read from the CSV
     */
    public static List<Tweet> readFromCSV(String filename) {
        List<Tweet> tweets = new ArrayList<Tweet>();
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
                    tweets.add(tweet);
                } catch (Exception e1) {}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tweets;
    }

    /**
     * Writes a ; separated CSV with the provided tweets into the provided OutputStream
     *
     * @param tweets List of tweets to write to the CSV
     * @param out OutputStream in which to write the CSV
     */
    public static void writeToCSV(List<Tweet> tweets, OutputStream out) {
        PrintStream print = new PrintStream(out);
        print.println("id;userId;userName;text;creationDate;retweetCount;favoriteCount;textHash");
        for(Tweet tweet : tweets) {
            Status status = tweet.getStatus();
            print.println(status.getId()+";"+status.getUser().getId()+";"
                    +toCSVString(status.getUser().getScreenName())+";"+toCSVString(status.getText())
                    +";"+new Timestamp(status.getCreatedAt().getTime())+";"+status.getRetweetCount()
                    +";"+status.getFavoriteCount()+";"+status.getText().hashCode());
        }
    }

    /**
     * Transforms the provided String so that it can be written directly as a
     * CSV cell (neutralize existing " with "" and surround the String with ")
     *
     * @param s The string that must be formatted to be written in a CSV cell
     * @return The string formatted to be written in a CSV cell
     */
    public static String toCSVString(String s) {
        return "\"" + s.replaceAll("\"","\"\"") + "\"";
    }

    //Getters and Setters

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getTextHash() {
        return status.getText().hashCode();
    }

}
