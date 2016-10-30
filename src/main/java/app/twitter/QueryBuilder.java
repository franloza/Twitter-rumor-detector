package app.twitter;

import app.db.DataManager;
import app.ml.NeuralNet;
import app.util.Pair;
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
    //Standard deviation of terms per query
    private final float deviationTerms = 2;
    //Similar words retrieved for each term
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
        //Print benchmark
        this.printBenchmark();
        return new Query(sb.toString().trim());
    }

    public NeuralNet getNeuralNetwork () {
        return this.nn;
    }

    public void printBenchmark () {
        String title = "QUERY BUILDER BENCHMARK";
        System.out.println (title);
        for (int i = 0; i < title.length();i++) System.out.print("=");
        System.out.println();

        //Print parameters
        System.out.println("Parameters:");
        System.out.println("\tMean terms per query: " + meanTerms);
        System.out.println("\tStandard deviation for additional terms: " + deviationTerms);
        System.out.println("\tSimilar words retrieved for each keyword: " + similarWords + "\n");

        //Print keyword information
        int count = 1;
        System.out.println("Keyword information:");
        TreeSet<Pair<String, Double>> keywords = DataManager.getInstance().getKeywords().getElements();
        for (Pair<String,Double> keyword: keywords) {
            String keywordStr = keyword.getLeft();
            System.out.println("\tKeyword #" + count + ": " + keywordStr);
            System.out.println("\tWeight: " + keyword.getRight());
            List<String> additionalWords = (List<String>) nn.getWordsNearest(keywordStr, similarWords);
            System.out.println("\tTop " + similarWords + " similar words to " + keywordStr);
            System.out.println("\t\tWord - Similarity");
            System.out.println("\t\t=================");
            for (String word: additionalWords) {
                System.out.println("\t\t"+word + " - " + nn.similarity(keywordStr,word));
            }
            System.out.println();
        }

        //Get stop words
        List<String> stopWords = nn.getStopWords();
        if(stopWords.size() > 0) {
            System.out.println("Stop words:");
            for(String word: stopWords) {
                System.out.println("\t"+word);
            }
        }
    }
}

