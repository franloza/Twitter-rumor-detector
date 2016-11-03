package app.twitter;

import app.db.CollectorDAO;
import app.db.DataManager;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Fran Lozano
 */
public class TweetCollector {

    //Number of threads used for collecting tweets
    private final int NUM_THREADS = 2;
    //Amount of tweets new collected before updating the model
    private final int REFRESH_RATE = 5000;
    //Query builder neural network
    private NeuralNet nn;

    public TweetCollector(NeuralNet nn) {
        this.nn = nn;
    }

    public void start() {
        /* Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream */
        BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(1000);

        /* Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
        List<String> terms = DataManager.getInstance().getCollectorDAO().getTerms();
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
                .name("Keyword Collector - ")
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue));

        Client hosebirdClient = builder.build();
        hosebirdClient.connect();
        //Execute threads
        for (int i = 0; i < NUM_THREADS; i++) {
            System.out.println("Starting collector thread number " + i);
            new ThreadCollector(msgQueue,hosebirdClient).start();
        }

    }

    public List<String> getTweets(int lastTweets) {
        CollectorDAO cDao = DataManager.getInstance().getCollectorDAO();
        return cDao.getTweets(lastTweets);
    }



    public int countCollected() {
        CollectorDAO cDao = DataManager.getInstance().getCollectorDAO();
        return cDao.countTweets();
    }

    private class ThreadCollector extends Thread {
        private BlockingQueue<String> msgQueue;
        private Client hosebirdClient;

        ThreadCollector(BlockingQueue<String> msgQueue, Client hosebirdClient) {
            this.msgQueue = msgQueue;
            this.hosebirdClient = hosebirdClient;
        }

        public void run(){
            retrieve(msgQueue, hosebirdClient);
        }
    }

    private void retrieve(BlockingQueue<String> msgQueue, Client hosebirdClient) {
        CollectorDAO cDao = DataManager.getInstance().getCollectorDAO();
        while (!hosebirdClient.isDone()) {
            String msg = null;
            try {
                msg = msgQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (msg != null) {
                try {
                    JSONObject jsonObject = new JSONObject(msg);
                    long id = jsonObject.getLong("id");
                    String text = jsonObject.getString("text");
                    text = TweetFilter.basicFilter(text);

                    //Insert if is not duplicated
                    if (!cDao.checkDuplicate(id, text.hashCode()))
                        cDao.insertTweet(id, text);
                    //System.out.println("Keyword Collector: " + id + " - " + text);
                    //Check if the model has to be rebuilt in a new thread
                    if (cDao.countTweets() % REFRESH_RATE == 0) {
                        new Thread() {
                            public void run() {
                                nn.createModel();
                            }
                        }.run();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        hosebirdClient.stop();
    }
}
