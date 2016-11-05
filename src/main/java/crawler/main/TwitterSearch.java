package crawler.main;

import app.model.Tweet;
import crawler.twitter.TweetNavigator;

import java.util.List;
import java.util.Scanner;

/**
 * Created by Jon Ayerdi on 12/10/2016.
 */
public class TwitterSearch {

    private TweetNavigator twitter;

    public static void main(String[] args) {
        TwitterSearch search = new TwitterSearch();
        search.start();
    }

    public void start() {
        try {
            twitter = new TweetNavigator(TwitterCrawler.CREDENTIALS_FILE);
            Scanner in = new Scanner(System.in);
            System.out.print("Search: ");
            List<Tweet> result = Tweet.createList(twitter.searchTweets(in.nextLine(),30));
            for(Tweet tweet : result) {
                System.out.println("------------------------------------------------------------------");
                System.out.println(tweet);
                System.out.println("------------------------------------------------------------------");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
