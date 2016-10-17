package app.twitter;

import app.db.DataManager;
import app.ml.NeuralNet;
import twitter4j.Query;

import java.util.*;

/**
 * @author Fran Lozano
 * Class in charge of building the queries in Twitter.
 * Uses NLP to build queries based on its frecuency
 */
public class QueryBuilder {

    //Query builder neural network
    private NeuralNet nn;
    //Number of mean terms per query
    private final float meanTerms = 4;
    private final float deviationTerms = 2;
    private final int similarWords = 5;
    public QueryBuilder() {
        this.nn = new NeuralNet();
    }

    public Query getQuery() {
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();
        String keyword;
        List<String> additionalWords = new ArrayList<String>();
        int queryTerms = (int) Math.abs(Math.round(rnd.nextGaussian()*meanTerms+deviationTerms));
        keyword = DataManager.getInstance().getKeywords().next();
        sb.append(keyword);
        int keywordSize = keyword.split("\\s+").length;
        //Query expansion
        if (keywordSize < queryTerms) {
            additionalWords = (List<String>) nn.getWordsNearest(keyword, similarWords);
            Collections.shuffle(additionalWords);
            Iterator it = additionalWords.iterator();
            for (int i = 0; i < queryTerms-keywordSize;i++) {
                if (it.hasNext())
                    sb.append(" " + it.next());
            }
        }
        return new Query(sb.toString().trim());
    }

    public NeuralNet getNeuralNetwork () {
        return this.nn;
    }
}

