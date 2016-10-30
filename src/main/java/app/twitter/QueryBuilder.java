package app.twitter;

import app.db.DataManager;
import app.ml.NeuralNet;
import app.util.Pair;
import org.apache.commons.lang.text.StrBuilder;
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
        List<String> additionalWords;
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

    public String getBenchmark () {
        StrBuilder sb = new StrBuilder();
        String title = "QUERY BUILDER BENCHMARK";
        sb.appendln (title);
        for (int i = 0; i < title.length();i++) sb.append("=");
        sb.append("\n");

        //Print parameters
        sb.appendln("Parameters:");
        sb.appendln("\tMean terms per query: " + meanTerms);
        sb.appendln("\tStandard deviation for additional terms: " + deviationTerms);
        sb.appendln("\tSimilar words retrieved for each keyword: " + similarWords + "\n");

        //Print keyword information
        int count = 1;
        sb.appendln("Keyword information:");
        TreeSet<Pair<String, Double>> keywords = DataManager.getInstance().getKeywords().getElements();
        for (Pair<String,Double> keyword: keywords) {
            String keywordStr = keyword.getLeft();
            sb.appendln("\tKeyword #" + count + ": " + keywordStr);
            sb.appendln(String.format("\tWeight: %.2f", keyword.getRight()));
            List<String> additionalWords = nn.getWordsNearest(keywordStr, similarWords);
            if(additionalWords.size()>0) {
                sb.appendln("\tTop " + similarWords + " similar words to " + keywordStr);
                sb.appendln("\t\tWord - Similarity");
                sb.appendln("\t\t------------------");
                for (String word : additionalWords) {
                    Double similarity = nn.similarity(keywordStr, word);
                    if (!similarity.isNaN()) sb.appendln(String.format("\t\t%s - %.5f", word, similarity));
                }
            } else {
                sb.appendln("\tThere are not similar words to " + keywordStr);
            }
            sb.append("\n");
            count++;
        }

        //Get stop words
        List<String> stopWords = nn.getStopWords();
        if(stopWords.size() > 0) {
            sb.appendln("Stop words:");
            for(String word: stopWords) {
                sb.appendln("\t"+word);
            }
        }

        //Test queries
        int NUM_QUERIES = 1000;
        HashMap<String,Integer> map = new HashMap<>();
        for (int i = 0;i < NUM_QUERIES; i++) {
            String query = getQuery().getQuery();
            if(map.containsKey(query)) map.put(query,map.get(query)+1);
            else map.put(query,1);
        }

        //Make ranking
        Comparator<Pair<String,Integer>> comp = (o1, o2) -> {
            int comparison = o1.getRight().compareTo(o2.getRight());
            if (comparison == 0) comparison = o1.getLeft().compareTo(o2.getLeft());
            return comparison;
        };
        TreeSet<Pair<String,Integer>> ranking = new TreeSet<>(comp);
        for (String query: map.keySet()){
            ranking.add(new Pair<>(query, map.get(query)));
        }
        //Set maxiumum number in the ranking
        int RANKING_QUERIES = 20;
        //Print query
        sb.appendln("Top " + RANKING_QUERIES + " queries:");
        sb.appendln("\tQuery: Percentage of the total");
        sb.appendln("\t----------------------");
        Iterator<Pair<String, Integer>> iter = ranking.descendingIterator();
        for (int i = 0; i < RANKING_QUERIES; i++) {
            String query;
            float percentage;
            Pair<String,Integer> pair;
            if(iter.hasNext()) {
                pair = iter.next();
                query = pair.getLeft();
                percentage = ((float)pair.getRight() /NUM_QUERIES) * 100;
                sb.appendln(String.format("\t%s - %.2f%%",query,percentage));
            }

        }


        return sb.toString();
    }
}

