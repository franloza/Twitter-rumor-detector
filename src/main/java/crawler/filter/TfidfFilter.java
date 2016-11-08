package crawler.filter;

import app.model.ScoredTweet;
import app.model.Tweet;

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

    public List<ScoredTweet> getScores(Tweet query, List<? extends Tweet> documents) {
        List<String> queryTerms;
        List<List<String>> documentTerms = new ArrayList<List<String>>();

        Map<String,Double> queryTF;
        List<Map<String,Double>> TF = new ArrayList<Map<String,Double>>();
        Map<String,Double> IDF;
        List<ScoredTweet> scoredTweets = new ArrayList<>();

        //Extract terms from query
        queryTerms = TermExtractor.extractTerms(query, stemmer);
        queryTF = TfIdf.tf(queryTerms);
        for(int i = 0 ; i < documents.size() ; i++) {
            //Extract terms from documents
            documentTerms.add(TermExtractor.extractTerms(documents.get(i),stemmer));
            //TF
            TF.add(TfIdf.tf(documentTerms.get(i)));
        }
        documentTerms.add(queryTerms);

        switch (mode) {
            //Calculate TF score
            case TF:
                for(int i = 0 ; i < documents.size() ; i++) {
                    Map<String,Double> tf = TF.get(i);
                    Tweet document = documents.get(i);
                    ScoredTweet scored = new ScoredTweet(document,query.getStatus().getId());
                    //Modules of document and query TF vectors for cosine normalization
                    double nDoc = 0.0;
                    double nQuery = 0.0;
                    if(tf.size() >= minTerms) {
                        for(String term : tf.keySet()) {
                            if(term.length() >= minTermSize) {
                                scored.score += tf.get(term) * queryTF.getOrDefault(term, 0.0);
                                nDoc += tf.get(term) * tf.get(term);
                                nQuery += queryTF.getOrDefault(term, 0.0) * queryTF.getOrDefault(term, 0.0);
                            }
                        }
                        //Divide by modules for cosine normalization
                        scored.score /= (Math.sqrt(nDoc) * Math.sqrt(nQuery));
                    }
                    scoredTweets.add(scored);
                }
                break;
            //Calculate TFIDF score
            case TFIDF:
                IDF = TfIdf.idf(documentTerms);
                Map<String,Double> queryTfidf = TfIdf.tfIdf(queryTF, IDF);
                for(int i = 0 ; i < documents.size() ; i++) {
                    Map<String,Double> tf = TF.get(i);
                    Map<String,Double> tfidf = TfIdf.tfIdf(tf, IDF);
                    Tweet document = documents.get(i);
                    ScoredTweet scored = new ScoredTweet(document,query.getStatus().getId());
                    if(tfidf.size() >= minTerms)
                        for(String term : tfidf.keySet())
                            if(term.length() >= minTermSize)
                                scored.score += tfidf.get(term) * queryTfidf.getOrDefault(term, 0.0);
                    scoredTweets.add(scored);
                }
                break;
        }
        return scoredTweets;
    }

}
