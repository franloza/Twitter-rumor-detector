package app.db;

import app.util.RandomCollection;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Singleton class that stores the data structures that are stored in the cache and the database
 * It's a halfway between the database and the application.
 * @author Fran Lozano
 */
public class DataManager {

    private TweetDAO tweetDao;
    private CollectorDAO collectorDao;
    private RandomCollection<String> keywords;

    private static DataManager instance;

    private DataManager() {
        if(this.tweetDao == null || this.collectorDao == null) {
            DataSource dataSource = configureDatabase();
            try {
                //Test connection
                Connection conn = dataSource.getConnection();
                conn.close();
            } catch (SQLException e) {
                System.err.println("There was a problem with the database. Check if is running an instance");
                System.err.println("Terminating the program...");
                System.exit(0);
            }
            this.tweetDao = new TweetDAO(dataSource);
            this.collectorDao = new CollectorDAO(dataSource);
        }
        if(this.keywords == null) this.keywords = new RandomCollection<>(tweetDao.getKeywords());
    }

    public static DataManager getInstance() {
        return instance == null ? instance = new DataManager() : instance;
    }

    public TweetDAO getTweetDao() {
        return tweetDao;
    }

    public CollectorDAO getCollectorDAO() { return this.collectorDao;}

    public RandomCollection<String> getKeywords() {
        return keywords;
    }

    private DataSource configureDatabase () {
        //Configure connection to database
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (InstantiationException | ClassNotFoundException | IllegalAccessException e) {
           e.printStackTrace();
        }
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
