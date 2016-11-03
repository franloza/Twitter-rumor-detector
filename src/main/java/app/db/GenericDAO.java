package app.db;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Fran Lozano
 */
public class GenericDAO {

    protected DataSource ds;

    protected GenericDAO(DataSource ds) {
        this.ds = ds;
    }

    /**
     * Generic method that returns a list of strings using a certain query
     * @return List recovered from the database.
     */
    protected List<String> getList (String sql) {
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

    /**
     * Generic method that returns a map with a string key and an integer value using a certain query
     * @return Map recovered from the database.
     */
    protected HashMap<String,Integer> getMap (String sql) {
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
     * Function that checks if an id exists in the database or the content belongs already to a tweet
     * @param id
     * @return true if it exists
     */
    protected boolean checkDuplicate(long id, String tableName, int textHash) {
        String sql = "SELECT COUNT(id) FROM " + tableName + " WHERE id=? OR textHash=?";
        try(Connection con = ds.getConnection();
            PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setLong(1, id);
            pst.setInt(2, textHash);
            ResultSet rs = pst.executeQuery();
            return rs.next() && rs.getInt(1) > 0;

        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    protected int count(String columnName,String tableName){
        String sql = "SELECT COUNT(?) FROM " + tableName;
        return getCounts(sql,columnName);
    }

    protected int count(String tableName){
        String sql = "SELECT COUNT(?) FROM " + tableName;
        return getCounts(sql,"*");
    }

    protected int getCounts(String sql, String columnName) {
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

}
