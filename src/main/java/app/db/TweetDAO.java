package app.db;

import twitter4j.Status;

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
        String sql = "INSERT INTO tweets (id,user,text) VALUES (?,?,?)";
        try(Connection con = ds.getConnection();
            PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setLong(1,s.getId());
            pst.setString(2,s.getUser().getName());
            pst.setString(3,s.getText());
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
    public boolean classifyTweet (long id, List<String> labels) {
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

    /**
     * Function that save a hashtag in the database
     * @param hashtag The hashtag to be saved
     * @return false if an error ocurred
     */
    public boolean saveHashtag(String hashtag) {
        String sql = "SELECT COUNT(hashtag) FROM hashtags WHERE hashtag=?";
        try(Connection con = ds.getConnection();
            PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1,hashtag);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                //The hashtag is already in the data base
                if (rs.getInt(1) > 0 ) sql = "UPDATE hashtags SET count=count+1 WHERE hashtag=?";
                else sql = "INSERT INTO hashtags VALUES(?,1)";
                PreparedStatement pst2 = con.prepareStatement(sql);
                pst2.setString(1,hashtag);
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
     * Function that returns the amount of tweets classified as rumors by certain user
     * @param user Name of Twitter account of the user
     * @return Number of tweets labeled as rumors.
     */
    public int countRumorTweets (String user) {
        String sql = "SELECT COUNT(id) FROM tweets WHERE rumor=1 and user=?";
        try(Connection con = ds.getConnection();
        PreparedStatement pst = con.prepareStatement(sql)) {
        pst.setString(1,user);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
           return rs.getInt(1);
        }
        else return 0;
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
     * Function that returns a dictionary ("hashtag" : appearences) that stores the amount of times a hashtag
     * has apppear in a tweet classified as rumor.
     * @return Map of hashtags that appear in rumor tweets.
     */
    public HashMap<String,Integer> getRumorHashtags () {
        String sql = "SELECT hashtag,count FROM hashtags";
        return getMap(sql);
    }

    /**
     * Function that returns a list of hashtags that
     * has appear in a tweet classified as rumor.
     * @return Map of hashtags that appear in rumor tweets.
     */
    public List<String> getRumorHashtagsList () {
        String sql = "SELECT hashtag FROM hashtags";
        return getList(sql);
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
}
