package crawler.filter;

import crawler.twitter.Tweet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Jon Ayerdi on 14/10/2016.
 */
public class TfidfFilter {

    public enum ScoringMode {
        TF,
        TFIDF
    }

    private ScoringMode mode;
    private Stemmer stemmer;
    private int minTerms;
    private int minTermSize;

    public TfidfFilter(ScoringMode mode, Stemmer stemmer, int minTerms, int minTermSize) {
        this.mode = mode;
        this.stemmer = stemmer;
        this.minTerms = minTerms;
        this.minTermSize = minTermSize;
    }

    public List<ScoredTweet> getScores(Tweet query, List<Tweet> documents) {
        List<String> queryTerms;
        List<List<String>> documentTerms = new ArrayList<List<String>>();

        List<Map<String,Double>> TF = new ArrayList<Map<String,Double>>();
        Map<String,Double> IDF;
        List<ScoredTweet> scoredTweets = new ArrayList<>();

        //Extract terms from query
        queryTerms = TermExtractor.extractTerms(query, stemmer);
        for(int i = 0 ; i < documents.size() ; i++) {
            //Extract terms from documents
            documentTerms.add(TermExtractor.extractTerms(documents.get(i)));
            //TF
            TF.add(TfIdf.tf(documentTerms.get(i)));
        }

        switch (mode) {
            //Calculate TF score
            case TF:
                for(int i = 0 ; i < documents.size() ; i++) {
                    Map<String,Double> tf = TF.get(i);
                    Tweet document = documents.get(i);
                    ScoredTweet scored = new ScoredTweet(document);
                    if(tf.size() >= minTerms)
                        for(String term : tf.keySet())
                            if(term.length() >= minTermSize && queryTerms.contains(term))
                                scored.score += tf.get(term);
                    scoredTweets.add(scored);
                }
                break;
            //Calculate TFIDF score
            case TFIDF:
                IDF = TfIdf.idf(documentTerms);
                for(int i = 0 ; i < documents.size() ; i++) {
                    Map<String,Double> tf = TF.get(i);
                    Map<String,Double> tfidf = TfIdf.tfIdf(tf, IDF);
                    Tweet document = documents.get(i);
                    ScoredTweet scored = new ScoredTweet(document);
                    if(tfidf.size() >= minTerms)
                        for(String term : tfidf.keySet())
                            if(term.length() >= minTermSize && queryTerms.contains(term))
                                scored.score += tfidf.get(term);
                    scoredTweets.add(scored);
                }
                break;
        }
        return scoredTweets;
    }

}
