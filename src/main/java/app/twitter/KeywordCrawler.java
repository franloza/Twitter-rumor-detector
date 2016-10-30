package app.twitter;

import app.db.DataManager;
import app.db.TweetDAO;
import app.ml.NeuralNet;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Franxi
 */
public class KeywordCrawler {

    //Number of threads used for crawling
    private final int NUM_THREADS = 2;
    //Amount of tweets new collected before updating the model
    private final int REFRESH_RATE = 1000;
    //Query builder neural network
    private NeuralNet nn;

    public KeywordCrawler(NeuralNet nn) {
        this.nn = nn;
    }

    public void start() {
        /** Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream */
        BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);

        /** Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
        // Optional: set up some followings, and track terms
        List<String> terms = DataManager.getInstance().getTweetDao().getKeywordsList();
        hosebirdEndpoint.trackTerms(terms);

        Properties prop = new Properties();
        InputStream in = getClass().getClassLoader().getResourceAsStream("twitter4j.properties");
        try {
            prop.load(in);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Authentication hosebirdAuth = new OAuth1(prop.getProperty("oauth.consumerKey"),
                prop.getProperty("oauth.consumerSecret"),
                prop.getProperty("oauth.accessToken"),
                prop.getProperty("oauth.accessTokenSecret"));

        ClientBuilder builder = new ClientBuilder()
                .name("Keyword crawler - ")
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue));

        Client hosebirdClient = builder.build();
        hosebirdClient.connect();
        //Execute threads
        for (int i = 0; i < NUM_THREADS; i++) {
            System.out.println("Starting crawler thread number " + i);
            new ThreadCrawler(msgQueue,hosebirdClient).start();
        }

    }

    public List<String> getTweets(int lastTweets) {
        TweetDAO tDao = DataManager.getInstance().getTweetDao();
        return tDao.getCrawledTweets ("crawler",lastTweets);
    }



    public int countCrawled() {
        TweetDAO tDao = DataManager.getInstance().getTweetDao();
        return tDao.countCrawled("crawler");
    }

    private class ThreadCrawler extends Thread {
        private BlockingQueue<String> msgQueue;
        private Client hosebirdClient;

        public ThreadCrawler(BlockingQueue<String> msgQueue,Client hosebirdClient) {
            this.msgQueue = msgQueue;
            this.hosebirdClient = hosebirdClient;
        }

        public void run(){
            crawl(msgQueue, hosebirdClient);
        }
    }

    private void crawl(BlockingQueue<String> msgQueue, Client hosebirdClient) {
        TweetDAO tDao = DataManager.getInstance().getTweetDao();
        while (!hosebirdClient.isDone()) {
            String msg = null;
            try {
                msg = msgQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            JSONObject jsonObject = new JSONObject(msg);
            long id = jsonObject.getLong("id");
            String text = jsonObject.getString("text");
            text = TweetFilter.basicFilter(text);

            //Insert if is not duplicated
            if (!tDao.checkDuplicate(id,"crawler",text.hashCode()))
                tDao.insertCrawledTweet(id,text);
                //System.out.println("Keyword Crawler: " + id + " - " + text);
                //Check if the model has to be rebuilt in a new thread
                if (tDao.countCrawled("crawler") % REFRESH_RATE == 0) {
                    new Thread() {
                        public void run() {
                            nn.createModel();
                        }
                    }.run();
                }
        }
        hosebirdClient.stop();
    }
}
