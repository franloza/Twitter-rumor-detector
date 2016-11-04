package crawler.twitter;

import crawler.misc.Console;
import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.internal.http.HttpResponseCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jon Ayerdi on 05/10/2016.
 *
 * Provides convenience methods to search tweets and credentials management with twitter4j
 */
public class TweetNavigator {

    private Twitter twitter;
    private final TwitterCredential[] credentials;
    private TwitterCredential credential;
    private int credentialIndex;

    /**
     * Loads credentials and instantiates a TweetNavigator
     *
     * @param credentialsFile File containing the credentials for the twitter api
     * @throws Exception
     */
    public TweetNavigator(String credentialsFile) throws Exception{
        credentials = TwitterCredential.loadCredentialsFromFile(credentialsFile);
        if(credentials.length < 1)
            throw new Exception("No credentials loaded");
        credentialIndex = 0;
        credential = credentials[credentialIndex];
        buildConfiguration();
    }

    /**
     * Instantiates twitter4j.Twitter from the current configuration
     */
    public void buildConfiguration() {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(credential.getOAuthConsumerKey());
        builder.setOAuthConsumerSecret(credential.getOAuthConsumerSecret());
        builder.setDebugEnabled(false);
        Configuration configuration = builder.build();
        TwitterFactory factory = new TwitterFactory(configuration);
        twitter = factory.getInstance();
        //twitter.setOAuthConsumer(consumerKey, consumerSecret);
        AccessToken accessToken = new AccessToken(credential.getOAuthAccessToken(), credential.getOAuthAccessTokenSecret());
        twitter.setOAuthAccessToken(accessToken);
    }

    /**
     * Returns true if we should switch credentials in order to keep using the twitter API
     * (rate limit reached, invalid credentials...etc)
     *
     * @param code The received HTTPResponseCode
     * @return true if we should switch, false otherwise
     */
    public static boolean shouldSwitchCredentials(int code) {
        if(code == HttpResponseCode.TOO_MANY_REQUESTS || code == HttpResponseCode.UNAUTHORIZED
                ||code == HttpResponseCode.ENHANCE_YOUR_CLAIM)
            return true;
        else
            return false;
    }

    /**
     * Returns true if we should wait in order to keep using the twitter API
     * (server overload...etc)
     *
     * @param code The received HTTPResponseCode
     * @return true if we should wait, false otherwise
     */
    public static boolean shouldWait(int code) {
        if(code == HttpResponseCode.SERVICE_UNAVAILABLE || code == HttpResponseCode.GATEWAY_TIMEOUT)
            return true;
        else
            return false;
    }

    /**
     * Switches to the next available TwitterCredential, and waits if there are no credentials available
     *
     * @param secondsUntilReset Seconds the current TwitterCredential needs to wait in order to make
     *                          more requests (reported by twitter server)
     */
    public void switchCredentials(int secondsUntilReset) {
        //Save timestamp and how much we have to wait
        credential.setResetTimestamp(System.currentTimeMillis());
        credential.setSecondsUntilReset(secondsUntilReset);
        while(credential.remainingSeconds()>0) {
            //Switch to next credential
            credential = credentials[(++credentialIndex)%credentials.length];
            Console.out.println("[Twitter Crawler]: Switching to credential " + credentialIndex%credentials.length);
            if(credentialIndex%credentials.length == 0) {
                //When we get back to the first credential, wait until we can use it
                Console.out.println("[Twitter Crawler]: First credential : Sleeping for " + credential.remainingSeconds() + " seconds");
                try {
                    Thread.sleep(credential.remainingSeconds()*1200);
                } catch (Exception e) {}
            }
        }
        //Reload configuration with new credential
        buildConfiguration();
        Console.out.println("[Twitter Crawler]: Using credential " + credentialIndex%credentials.length);
    }

    /**
     * Fetch a tweet from its TweetID
     *
     * @param tweetID
     */
    public Status getTweetByTweetID(long tweetID) {
        boolean loop = true;
        while(loop) {
            loop = false;
            try {
                Status status = twitter.showStatus(tweetID);
                return status;
            } catch (TwitterException e) {
                if(shouldSwitchCredentials(e.getStatusCode())) {
                    RateLimitStatus rateLimitStatus = e.getRateLimitStatus();
                    if (rateLimitStatus != null)
                        switchCredentials(rateLimitStatus.getSecondsUntilReset());
                    else
                        switchCredentials(900);
                    loop = true;
                }
                else e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Returns the results reported by twitter.search(query), where query is formed
     * using searchTerms
     *
     * @param searchTerms Terms used to build the query
     * @param maxResults Maximum number of results returned
     * @return The results reported by twitter.search(query)
     */
    public List<Status> searchTweets(String searchTerms, int maxResults) {
        boolean loop = true;
        while(loop) {
            loop = false;
            try {
                Query query = new Query(searchTerms);
                query.setCount(maxResults);
                query.setLang("en");
                QueryResult result = twitter.search(query);
                return result.getTweets();
            } catch (TwitterException e) {
                if(shouldSwitchCredentials(e.getStatusCode())) {
                    RateLimitStatus rateLimitStatus = e.getRateLimitStatus();
                    if (rateLimitStatus != null)
                        switchCredentials(rateLimitStatus.getSecondsUntilReset());
                    else
                        switchCredentials(900);
                    loop = true;
                }
                else e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Gets the last tweets posted by tweet.getUser()
     *
     * @param tweet Base tweet from which to search for more tweets
     * @param minRetweets Minimum number of retweets of the results
     * @param maxResults Maximum number of tweets fetched in total
     * @return The last maxResults tweets posted by tweet.getUser()
     */
    public List<Status> getUserTimelineTweets(Status tweet, int minRetweets, int maxResults) {
        List<Status> userTimelineTweets = new ArrayList<Status>();
        try {
            for (int i = 1; i <= (maxResults / 100) + 1 && userTimelineTweets.size() < maxResults; i++) {
                try {
                    List<Status> result = twitter.getUserTimeline(tweet.getUser().getId(), new Paging(i, 100));
                    for(Status resultTweet : result)
                        if(resultTweet.getRetweetCount() >= minRetweets)
                            userTimelineTweets.add(resultTweet);
                }catch (TwitterException e) {
                    if(shouldSwitchCredentials(e.getStatusCode())) {
                        RateLimitStatus rateLimitStatus = e.getRateLimitStatus();
                        if (rateLimitStatus != null)
                            switchCredentials(rateLimitStatus.getSecondsUntilReset());
                        else
                            switchCredentials(900);
                        i--;
                    }
                    else if(shouldWait(e.getStatusCode())) {
                        try {
                            Thread.sleep(10000);
                        } catch (Exception e1) {}
                        i--;
                    }
                    else e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userTimelineTweets;
    }

    /**
     * Gets the list of tweets to which the base tweet replied
     *
     * @param tweet Base tweet from which to search for more tweets
     * @param maxResults Maximum number of tweets fetched in total
     * @return The upstream conversation of tweet
     */
    public List<Status> getInReplyToTweets(Status tweet, int maxResults) {
        List<Status> replyTweets = new ArrayList<Status>();
        Status currentTweet = tweet;
        int fetched = 0;
        boolean loop = true;
        while(loop) {
            loop = false;
            try {
                while (fetched < maxResults && currentTweet.getInReplyToStatusId() > 0) {
                    currentTweet = twitter.showStatus(currentTweet.getInReplyToStatusId());
                    replyTweets.add(currentTweet);
                    fetched++;
                }
            } catch (TwitterException e) {
                if(shouldSwitchCredentials(e.getStatusCode())) {
                    RateLimitStatus rateLimitStatus = e.getRateLimitStatus();
                    if (rateLimitStatus != null)
                        switchCredentials(rateLimitStatus.getSecondsUntilReset());
                    else
                        switchCredentials(900);
                    loop = true;
                }
                else if(shouldWait(e.getStatusCode())) {
                    try {
                        Thread.sleep(10000);
                    } catch (Exception e1) {}
                    loop = true;
                }
                else e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return  replyTweets;
    }

    /**
     * Gets the list of tweets that reply to the provided tweet
     *
     * @param tweet Base tweet from which to search for more tweets
     * @param minRetweets Minimum number of retweets of the results
     * @param maxResults Maximum number of tweets fetched in total
     * @return A list of tweets that replied to the provided tweet
     */
    public List<Status> getResponseTweets(Status tweet, int minRetweets, int maxResults) {
        List<Status> replyTweets = new ArrayList<Status>();
        try {
            //Search for tweets with @tweet.getUser() since tweet was posted
            Query query = new Query("@" + tweet.getUser().getScreenName() + " since_id:" + tweet.getId());
            query.setLang("en");
            query.setCount(100);
            QueryResult result = null;
            int count = 0;
            while (query != null && count < maxResults/100) {
                if (replyTweets.size() < maxResults) {
                    boolean loop = true;
                    while(loop) {
                        loop = false;
                        try {
                            result = twitter.search(query);
                        } catch (TwitterException e) {
                            if(shouldSwitchCredentials(e.getStatusCode())) {
                                RateLimitStatus rateLimitStatus = e.getRateLimitStatus();
                                if (rateLimitStatus != null)
                                    switchCredentials(rateLimitStatus.getSecondsUntilReset());
                                else
                                    switchCredentials(900);
                                loop = true;
                            }
                            else if(shouldWait(e.getStatusCode())) {
                                try {
                                    Thread.sleep(10000);
                                } catch (Exception e1) {}
                                loop = true;
                            }
                            else e.printStackTrace();
                        }
                    }
                }
                //Break if we can get more than maxResults in the next cycle
                else break;

                List<Status> resultTweets = result.getTweets();
                for (Status response : resultTweets)
                    //Make sure the response is indeed a reply to the original tweet
                    if (response.getRetweetCount() >= minRetweets && response.getInReplyToStatusId() == tweet.getId())
                        replyTweets.add(response);

                //Next page of results
                query = result.nextQuery();
                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  replyTweets;
    }

    /**
     * Gets the list of tweets that mention the provided tweet writer
     *
     * @param tweet Base tweet from which to search for more tweets
     * @param minRetweets Minimum number of retweets of the results
     * @param maxResults Maximum number of tweets fetched in total
     * @return A list of tweets that mention the provided tweet writer
     */
    public List<Status> getMentionTweets(Status tweet, int minRetweets, int maxResults) {
        List<Status> replyTweets = new ArrayList<Status>();
        try {
            //Search for tweets with @tweet.getUser() since tweet was posted
            Query query = new Query("@" + tweet.getUser().getScreenName());
            query.setLang("en");
            query.setCount(100);
            QueryResult result = null;
            int count = 0;
            while (query != null && count < maxResults/100) {
                if (replyTweets.size() < maxResults) {
                    boolean loop = true;
                    while(loop) {
                        loop = false;
                        try {
                            result = twitter.search(query);
                        } catch (TwitterException e) {
                            if(shouldSwitchCredentials(e.getStatusCode())) {
                                RateLimitStatus rateLimitStatus = e.getRateLimitStatus();
                                if (rateLimitStatus != null)
                                    switchCredentials(rateLimitStatus.getSecondsUntilReset());
                                else
                                    switchCredentials(900);
                                loop = true;
                            }
                            else if(shouldWait(e.getStatusCode())) {
                                try {
                                    Thread.sleep(10000);
                                } catch (Exception e1) {}
                                loop = true;
                            }
                            else e.printStackTrace();
                        }
                    }
                }
                //Break if we can get more than maxResults in the next cycle
                else break;

                List<Status> resultTweets = result.getTweets();
                for (Status response : resultTweets)
                    if(response.getRetweetCount() >= minRetweets)
                        replyTweets.add(response);

                //Next page of results
                query = result.nextQuery();
                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  replyTweets;
    }

}
