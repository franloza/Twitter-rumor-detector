package app.db;


import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * @author Fran Lozano
 */
public class CollectorDAO extends GenericDAO{

    private final String COLLECTOR_TABLE = "collector";
    private final String TERMS_TABLE = "collector_terms";

    public CollectorDAO(DataSource dataSource) {
        super(dataSource);
    }

    public List<String> getTweets(int lastTweets) {
        String sql = null;
        if (lastTweets > 0) {
            sql = "SELECT text FROM (" +
                    "SELECT * FROM " + COLLECTOR_TABLE +  " ORDER BY id DESC LIMIT " + lastTweets +
                    " ) sub " +
                    "ORDER BY id ASC";
        } else {
            sql = "SELECT text FROM " + COLLECTOR_TABLE;
        }
        return getList(sql);
    }

    public List<String> getTerms() {
        return getList("SELECT terms FROM " + TERMS_TABLE);
    }

    public boolean insertTweet(long id, String text) {
        String sql = "INSERT INTO " + COLLECTOR_TABLE + " (id,text,textHash) VALUES (?,?,?)";
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

    public boolean checkDuplicate(long id, int textHash) {
        return checkDuplicate(id,COLLECTOR_TABLE,textHash);
    }

    public List<String> getTweets() {
        return getTweets(0);
    }

    public int countTweets() {
        return count(COLLECTOR_TABLE);
    }
}
