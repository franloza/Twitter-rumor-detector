package app.twitter;

import app.db.DataManager;
import app.ml.NeuralNet;
import twitter4j.Query;

import java.util.Random;

/**
 * @author Fran Lozano
 * Class in charge of building the queries in Twitter.
 * Uses NLP to build queries based on its frecuency
 */
public class QueryBuilder {

    //Query builder neural network
    private NeuralNet nn;
    //Number of mean terms per query
    private final float meanTerms = 2;
    private final float deviationTerms = 2;
    public QueryBuilder() {
        this.nn = new NeuralNet();
    }

    public Query getQuery() {
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();
        String keyword;
        int queryTerms = (int) Math.abs(Math.round(rnd.nextGaussian()+1));
        keyword = DataManager.getInstance().getKeywords().next();
        sb.append(keyword);
        sb.append(" ");
        do {
            sb.append(String.join(" ", nn.getWordsNearest(keyword, queryTerms)));
        } while (sb.toString().length() < 1);
        return new Query(sb.toString());
    }

    public NeuralNet getNeuralNetwork () {
        return this.nn;
    }
}

