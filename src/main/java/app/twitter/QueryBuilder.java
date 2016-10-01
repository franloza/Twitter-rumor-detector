package app.twitter;

import app.db.TweetDAO;
import twitter4j.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Fran Lozano
 * Class in charge of building the queries in Twitter.
 * Uses maps of terms and hashtags classified as rumors an change the weights depending
 * on the frequency
 */
public class QueryBuilder {

    private final float TERM_WEIGHT = 0.9f;
    private float hashtagWeight;
    private List<String> bagOfWords;
    private TweetDAO tDao;

    public QueryBuilder(TweetDAO tDao) {
        this.tDao = tDao;
    }

    private void generateInitialBag() {
        this.bagOfWords = new ArrayList<>();
        this.bagOfWords.add("september 11");
        this.bagOfWords.add("twin towers");
        this.bagOfWords.add("attacks");
        this.bagOfWords.add("9/11");
        this.bagOfWords.add("world trade center");
    }

    //TODO: Create an smart query builds that learns over time (Deep learning)
    /*This is a primitive function to start quering some interesting results.
    We need to build a smarter model to get queries based on relations between
    words that usually appear together in rumor tweets.
    See deeplearning4j API - Section NPL (Natural Language Processing)
    */
    public Query getQuery (){

        //Generate bag of words
        generateInitialBag();

        //First we calculate the length of the query between 1 and 3 terms
        Random rnd = new Random();
        int nTerms = rnd.nextInt(3)+1;
        StringBuilder sb = new StringBuilder();
        List<String> hashtags = tDao.getRumorHashtagsList();
        hashtagWeight = hashtags.size() == 0? 0: 1-TERM_WEIGHT;
        int index;
        for (int i = 0;i < nTerms;i++) {
            //Choose a hashtag
            if(rnd.nextFloat() < hashtagWeight) {
                index = rnd.nextInt(hashtags.size());
                sb.append(hashtags.get(index));
                sb.append(" ");
                hashtags.remove(index);
                //Update weight
                hashtagWeight = hashtags.size() == 0? 0: 1-TERM_WEIGHT;
            } else {
                //Choose a term
                index = rnd.nextInt(bagOfWords.size());
                sb.append(bagOfWords.get(index));
                bagOfWords.remove(index);
                sb.append(" ");
            }
        }
        System.out.println ("Query: " + sb.toString());
        return new Query (sb.toString());
    }
}
