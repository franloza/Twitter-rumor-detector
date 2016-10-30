package app.db;

import app.twitter.ClassifiedTweet;
import app.twitter.TweetFilter;
import crawler.twitter.Tweet;
import crawler.twitter.TwitterStatus;
import crawler.twitter.TwitterUser;
import twitter4j.Status;
import twitter4j.User;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


/**
 * Data Access Object in charge of tweet persistence
 * @author Fran Lozano
 */
public class TweetDAO {

    private DataSource ds;

    public TweetDAO(DataSource ds) {
        this.ds = ds;
    }

    /**
     * Function that insert a new tweet in the database and set it as unclassified
     * @param s Tweet information
     * @return false if there was an error
     */
    public boolean insertTweet (Status s) {
        return insertTweet("tweets",s,-1);
    }

    public boolean insertCrawledTweetTf (long crawledId,Status s,double score) {
        return insertTweet("tweets_crawled_tf",crawledId,s,score);
    }

    public boolean insertCrawledTweetTfIdf (long crawledId,Status s, double score) {
        return insertTweet("tweets_crawled_tfidf",crawledId,s,score);
    }

    private boolean insertTweet (String tableName, long crawledId, Status s, double score) {
        String sql;

            sql = "INSERT INTO " + tableName + " (crawledId,id,userId,userName,text,retweetCount,creationDate,favoriteCount,textHash,score) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?)";

        try(Connection con = ds.getConnection();
            PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setLong(1,crawledId);
            pst.setLong(2,s.getId());
            pst.setLong(3,s.getUser().getId());
            pst.setString(4,s.getUser().getScreenName());
            String text =  TweetFilter.basicFilter(s.getText());
            pst.setString(5,text);
            pst.setInt(6,s.getRetweetCount());
            pst.setTimestamp(7,(new Timestamp(s.getCreatedAt().getTime())));
            pst.setInt(8,s.getFavoriteCount());
            pst.setInt(9,text.hashCode());
            pst.setDouble (10,score);
            pst.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    private boolean insertTweet (String tableName,Status s, double score) {
        boolean scored = false;
        String sql;
        if (score >= 0) scored = true;

        if (scored) {
            sql = "INSERT INTO " + tableName + " (id,userId,userName,text,retweetCount,creationDate,favoriteCount,textHash,score) " +
                    "VALUES (?,?,?,?,?,?,?,?,?)";
        } else {
            sql = "INSERT INTO " + tableName + " (id,userId,userName,text,retweetCount,creationDate,favoriteCount,textHash) " +
                    "VALUES (?,?,?,?,?,?,?,?)";
        }
        try(Connection con = ds.getConnection();
            PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setLong(1,s.getId());
            pst.setLong(2,s.getUser().getId());
            pst.setString(3,s.getUser().getScreenName());
            String text =  TweetFilter.basicFilter(s.getText());
            pst.setString(4,text);
            pst.setInt(5,s.getRetweetCount());
            pst.setTimestamp(6,(new Timestamp(s.getCreatedAt().getTime())));
            pst.setInt(7,s.getFavoriteCount());
            pst.setInt(8,text.hashCode());
            if(scored) pst.setDouble (9,score);
            pst.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    public boolean insertCrawledTweet(long id, String text) {
        String sql = "INSERT INTO crawler (id,text,textHash) VALUES (?,?,?)";
        try(Connection con = ds.getConnection();
            PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setLong(1,id);
            pst.setString(2,text);
            pst.setInt(3,text.hashCode());
            pst.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    /**
     * Function that set the labels for a certain tweet and set it as classified
     * @param id Tweet ID
     * @param labels List of labels to set to 1 in the database
     * @return false if there was an error
     */
    public boolean setLabels(long id, List<String> labels) {
        String sql = "UPDATE tweets SET classified=1,assertion=?,topic=?,rumor=? WHERE id=?";
        try(Connection con = ds.getConnection();
            PreparedStatement pst = con.prepareStatement(sql)) {
            Iterator it = labels.iterator();
            //Assertion label
            if (it.hasNext() && it.next() != null) {
                pst.setBoolean(1,true);
                //Topic label
                if (it.hasNext() && it.next() != null) {
                    pst.setBoolean(2,true);
                    //Rumor label
                    if (it.hasNext() && it.next() != null) {
                        pst.setBoolean(3,true);
                    }
                    else {
                        pst.setBoolean(3,false);
                    }
                }
                else {
                    pst.setBoolean(2,false);
                    pst.setNull(3, Types.BOOLEAN);
                }
            }
            else {
                pst.setBoolean(1,false);
                pst.setNull(2, Types.BOOLEAN);
                pst.setNull(3, Types.BOOLEAN);
            }
            pst.setLong(4,id);
            pst.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    /**
     * Function that checks if an id exists in the database
     * @param id
     * @return true if it exists
     */
    public boolean checkID(long id) {
        String sql = "SELECT COUNT(id) FROM tweets WHERE id=?";
        try(Connection con = ds.getConnection();
            PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setLong(1,id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                if (rs.getInt(1) == 0) return false;
                else return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    /**
     * Function that checks if an id exists in the database or the content belongs already to a tweet
     * @param id
     * @return true if it exists
     */
    public boolean checkDuplicate(long id, String tableName, int textHash) {
        String sql = "SELECT COUNT(id) FROM " + tableName + " WHERE id=? OR textHash=?";
        try(Connection con = ds.getConnection();
            PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setLong(1,id);
            pst.setInt(2,textHash);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                if (rs.getInt(1) > 0) return true;
                else return false;
            }
            return false;

        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    /**
     * Function that checks if an existing tweet is set as classified in the database
     * @param id
     * @return true if it has been classified
     */
    public boolean checkClassified(long id) {
        String sql = "SELECT classified FROM tweets WHERE id=?";
        try(Connection con = ds.getConnection();
            PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setLong(1,id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return (rs.getBoolean(1));
            }
            return false;

        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    public boolean updateWeight(String keyword, double deltaWeight) {
        String sql = "SELECT COUNT(keyword) FROM keywords WHERE keyword=?";
        try(Connection con = ds.getConnection();
            PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1,keyword);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                //The keyword is already in the data base
                if (rs.getInt(1) > 0 ) sql = "UPDATE keywords SET weight=weight + ? WHERE keyword=?";
                else sql = "INSERT INTO keywords VALUES(?,1 + deltaWeight)";
                PreparedStatement pst2 = con.prepareStatement(sql);
                pst2.setDouble(1,deltaWeight);
                pst2.setString(2,keyword);
                if (pst2.executeUpdate() < 1) return false;
            }
            else return false;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
        return true;
        }

    /**
     * Function that returns the amount of tweets classified as rumors
     * @return Number of tweets labeled as rumors.
     */
    public int countRumor () {
        return countTrues("rumor","tweets");
    }

    /**
     * Function that returns the amount of tweets classified
     * @return Number of tweets labeled as rumors.
     */
    public int countClassified () {
        return countTrues("classified","tweets");
    }

    private int countTrues(String columnName,String tableName){
        String sql = "SELECT COUNT(?) FROM " + tableName + " WHERE "+ columnName + "=1";
        return getCounts(sql,columnName);
        }

    private int count(String columnName,String tableName){
        String sql = "SELECT COUNT(?) FROM " + tableName;
        return getCounts(sql,columnName);
        }

        private int getCounts (String sql, String columnName) {
            try (Connection con = ds.getConnection();
                 PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, columnName);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1);
                } else return 0;
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                return -1;
            }
        }

    /**
     * Function that returns a dictionary ("user" : rumor_tweets) that stores the amount of tweets classified
     * as rumors for each user
     * @return Map of users containing at least one tweet classified as rumor.
     */
    public HashMap<String,Integer> getRumorUsers () {
        String sql = "SELECT user,count(rumor) FROM tweets WHERE rumor=1";
        return getMap(sql);
    }

    /**
     * Function that returns a dictionary ("keyword" : weight) that stores a keyword and a weight associated
     * to it
     * @return Map of keywords and weights
     */
    public HashMap<String,Double> getKeywords () {
        String sql = "SELECT keyword,weight FROM keywords";
        HashMap<String, Double> map = new HashMap<>();
        try (Connection con = ds.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                map.put(rs.getString(1), rs.getDouble(2));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return map;
    }

    /**
     * Generic method that returns a map with a string key and an integer value using a certain query
     * @return Map recovered from the database.
     */
    private HashMap<String,Integer> getMap (String sql) {
        HashMap<String, Integer> map = new HashMap<>();
        try (Connection con = ds.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                map.put(rs.getString(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return map;
    }

    /**
     * Generic method that returns a list of strings using a certain query
     * @return List recovered from the database.
     */
    private List<String> getList (String sql) {
        List<String> list = new ArrayList<>();
        try (Connection con = ds.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                list.add(rs.getString(1));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return list;
    }

    public List<String> getKeywordsList() {
        String sql = "SELECT keyword from keywords";
        return this.getList(sql);
    }

    public String getTweet(long tweetId) {
        String sql = "SELECT text FROM tweets WHERE id=?";
        try(Connection con = ds.getConnection();
            PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setLong(1,tweetId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
            return "";

        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return "";
        }
    }

    public int countCrawled(String tableName) {
        return count("id",tableName);
    }

    public List<String> getCrawledTweets() {
        return getCrawledTweets("crawler",0);
    }

    public List<String> getCrawledTweets(String tableName, int lastTweets) {
        String sql = null;
        if (lastTweets > 0) {
            sql = "SELECT text FROM (" +
                    "SELECT * FROM " + tableName +  " ORDER BY id DESC LIMIT " + lastTweets +
                    " ) sub " +
                    "ORDER BY id ASC";
        } else {
            sql = "SELECT text FROM " + tableName;
        }
        return getList(sql);
    }

    public long getMinId(String query) {
        String sql = "SELECT minTweetId FROM queries WHERE query=?";
        try(Connection con = ds.getConnection();
            PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1,query);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
               return rs.getLong(1);
            }
            else return 0;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return 0;
        }
    }

    public void deleteOldestUnclassified(long cacheUnclassified) {
        String sql = "DELETE FROM tweets WHERE id NOT IN (\n" +
                "  SELECT id FROM (" +
                "    SELECT id FROM tweets WHERE classified=0 ORDER BY id ASC LIMIT ?) sub) AND classified=0;";
        try(Connection con = ds.getConnection();
            PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setLong(1,cacheUnclassified);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public boolean updateQuery(String query, long minId, int count) {
        String sql = "SELECT COUNT(query) FROM queries WHERE query=?";
        try(Connection con = ds.getConnection();
            PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1,query);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                //The keyword is already in the data base
                PreparedStatement pst2;
                if (rs.getInt(1) > 0 ) {
                    sql = "UPDATE queries SET minTweetId=?, counter = counter + ? WHERE query=?";
                    pst2 = con.prepareStatement(sql);
                    pst2.setLong(1,minId);
                    pst2.setInt(2,count);
                    pst2.setString(3,query);
                }
                else {
                    sql = "INSERT INTO queries (minTweetId,query,counter) VALUES(?,?,?)";
                    pst2 = con.prepareStatement(sql);
                    pst2.setLong(1,minId);
                    pst2.setString(2,query);
                    pst2.setInt(3,count);
                }
                if (pst2.executeUpdate() < 1) return false;
            }
            else return false;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
        return true;
    }

    public boolean resetQueryTweetId (String query) {
        String sql = "UPDATE queries SET minTweetId=0 WHERE query=?";
        try(Connection con = ds.getConnection();
            PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1,query);
            return (pst.executeUpdate() >0);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    public long getLastCrawledTweetId(String tableName) {
        String sql = "SELECT DISTINCT crawledId FROM " + tableName + " WHERE crawledDate IN (SELECT MAX(crawledDate) FROM " + tableName+ ") LIMIT 1";
        try (Connection con = ds.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            } else return 0;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return -1;
        }

    }

    public List<Status> getCrawledTweetsById(String tableName, long crawledId) {
        if(crawledId > 0) {}
        String sql = (crawledId > 0)? "SELECT * FROM " + tableName + " WHERE crawledId = ?":"SELECT * FROM " + tableName;
        List<Status> tweets = new ArrayList<>();
        try(Connection con = ds.getConnection();
            PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setLong(1,crawledId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                long id = rs.getLong("id");
                User user = TwitterUser.create(rs.getLong("userId"), rs.getString("userName"));
                String text = rs.getString("text");
                Date createdAt = rs.getDate("creationDate");
                int retweets = rs.getInt("retweetCount");
                int favorites = rs.getInt("favoriteCount");
                Tweet tweet = new Tweet(TwitterStatus.create(id,user,text,createdAt,retweets,favorites));
                tweets.add(tweet.getStatus());
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return tweets;
    }

    public List<ClassifiedTweet> getClassifiedTweets(boolean filtered) {

        String sql = "SELECT * FROM tweets WHERE classified=1";
        List<ClassifiedTweet> tweets = new ArrayList<>();
        try(Connection con = ds.getConnection();
            PreparedStatement pst = con.prepareStatement(sql)) {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                long id = rs.getLong("id");
                User user = TwitterUser.create(rs.getLong("userId"), rs.getString("userName"));
                String text = (filtered)? TweetFilter.filter(rs.getString("text")):rs.getString("text");
                Date createdAt = rs.getDate("creationDate");
                int retweets = rs.getInt("retweetCount");
                int favorites = rs.getInt("favoriteCount");
                boolean assertion = rs.getBoolean("assertion");
                boolean topic = rs.getBoolean("topic");
                boolean rumor = rs.getBoolean("rumor");
                ClassifiedTweet tweet = new ClassifiedTweet(TwitterStatus.create(id,user,text,createdAt,retweets,favorites)
                        ,assertion,topic,rumor);
                tweets.add(tweet);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return tweets;
    }
}
