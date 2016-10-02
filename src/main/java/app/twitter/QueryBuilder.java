package app.twitter;

import app.db.DataManager;
import app.ml.NeuralNet;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;

import twitter4j.Query;

import java.util.Random;

/**
 * @author Fran Lozano
 * Class in charge of building the queries in Twitter.
 * Uses NLP to build queries based on its frecuency
 */
public class QueryBuilder {

    public static Query getQuery() {
        VocabCache<VocabWord> cache = DataManager.getInstance().getVocabulary();
        NeuralNet nn = DataManager.getInstance().getNeuralNet();
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();
        String keyword = null;
        int queryTerms = (int) Math.abs(Math.round(rnd.nextGaussian()+1));
        keyword = DataManager.getInstance().getKeywords().next();
        //keyword = cache.wordAtIndex(rnd.nextInt(cache.numWords()));
        sb.append(keyword + " ");
        if (queryTerms > 0) sb.append(String.join(" ", nn.getWordsNearest(keyword, queryTerms)));
        System.out.println("Query: " + sb.toString());
        return new Query(sb.toString());
    }
}

