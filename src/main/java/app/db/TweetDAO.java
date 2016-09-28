package app.db;

import twitter4j.Status;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Iterator;
import java.util.List;


/**
 * Data Access Object in charge of tweet persistance
 * @author Fran Lozano
 */
public class TweetDAO {

    private DataSource ds;

    public TweetDAO(DataSource ds) {
        this.ds = ds;
    }

    public boolean insertTweet (Status s) {
        String sql = "INSERT INTO tweets (id,text) VALUES (?,?)";
        try(Connection con = ds.getConnection();
            PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setLong(1,s.getId());
            pst.setString(2,s.getText());
            pst.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

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
}
