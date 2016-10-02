package app.db;

import app.ml.NeuralNet;
import app.util.RandomCollection;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;

import javax.sql.DataSource;

/**
 * Singleton class that stores the data structures that are stored in the cache and the database
 * It's a halfway between the database and the application.
 * @author Fran Lozano
 */
public class DataManager {

    private TweetDAO tweetDao;
    private RandomCollection<String> keywords;
    private VocabCache<VocabWord> vocabulary;
    private NeuralNet neuralNet;

    private static DataManager instance;

    private DataManager() {
        this.neuralNet = new NeuralNet();
        this.vocabulary = neuralNet.getVocabulary();
        this.tweetDao = new TweetDAO(configureDatabase());
        this.keywords = new RandomCollection<>(tweetDao.getKeywords());
    }

    public static DataManager getInstance() {
        return instance == null ? instance = new DataManager() : instance;
    }

    public TweetDAO getTweetDao() {
        return tweetDao;
    }

    public RandomCollection<String> getKeywords() {
        return keywords;
    }

    public VocabCache<VocabWord> getVocabulary() {
        return vocabulary;
    }

    public NeuralNet getNeuralNet() {
        return neuralNet;
    }

    private DataSource configureDatabase () {
        //Configure connection to database
        ComboPooledDataSource cpds = new ComboPooledDataSource();
        cpds.setJdbcUrl("jdbc:mysql://localhost/trd");
        cpds.setUser("admin");
        cpds.setPassword("password");
        cpds.setAcquireRetryAttempts(1);
        cpds.setAcquireRetryDelay(1);
        cpds.setBreakAfterAcquireFailure(true);
        return cpds;
    }
}
