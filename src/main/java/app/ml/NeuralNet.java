package app.ml;

import app.db.DataManager;
import app.db.TweetDAO;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.CollectionSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.io.ClassPathResource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Neural network of the annotation tool that helps to build the queries
 * @author Fran Lozano
 */
public class NeuralNet {

    private Word2Vec model;
    private final String modelPath = "src/main/resources/data/model.data";
    private Object lock = new Object ();

    public NeuralNet() {
        File f = new File(modelPath);
        TweetDAO tDao = DataManager.getInstance().getTweetDao();

        if(f.exists() && !f.isDirectory()) {
                this.load();
        }else{
            //Create a new model
            HashSet<String> tweets;
            if(tDao.countCrawled() > 0){
                createModel();
            } else {
                //Load tweets from file
                tweets = this.loadTweets("/data/rumorTweets.txt");
                trainModel(tweets);
                //Save the model in a file
                this.save();
            }
        }
    }

    public synchronized void createModel () {
        HashSet<String> tweets;
        //Load tweets from database
        tweets = this.loadTweets();
        trainModel(tweets);
        //Save the model in a file
        this.save();
    }

    public Collection<String> getWordsNearest(String keyword, int n) {
        Collection<String> words = new LinkedList<>();

        Collection<String> keywordColl = tokenizeString(keyword);
        synchronized (lock) {
            if (keywordColl.size() > 1) {
                INDArray vectors = model.getWordVectors(keywordColl);
                if (vectors.isVector()) {
                    words = model.wordsNearest(vectors, n);
                    //Remove duplicates
                    Iterator it = words.iterator();
                    while (it.hasNext()) {
                        String w = (String) it.next();
                        if (keyword.contains(w)) it.remove();
                    }
                }
            } else {
                words = model.wordsNearest(keyword, n);
            }
        }
        return words;
    }

    public VocabCache<VocabWord> getVocabulary() {
        return model.getVocab();
    }

    private HashSet<String> loadTweets(String filePath) {
        System.out.println("Loading & vectorizing initial tweets...");
        List<String> tokens = null;
        HashSet<String> tweets = new HashSet<>();
        try {
            String path = new ClassPathResource(filePath).getFile().getAbsolutePath();
            steamFile(tweets, path);
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return tweets;
    }

    private void steamFile(HashSet<String> tweets, String path) throws FileNotFoundException {
        List<String> tokens;SentenceIterator iter = new BasicLineIterator(path);
        String filteredTweet;

        //Tokenize the tweets
        TokenizerFactory t = new DefaultTokenizerFactory();
        t.setTokenPreProcessor(new CommonPreprocessor());
        while(iter.hasNext()) {
            tokens = t.create(iter.nextSentence()).getTokens();
            filteredTweet = cleanTerms(tokens);
            tweets.add(filteredTweet);
        }
    }

    private HashSet<String> loadTweets() {
        System.out.println("Loading & vectorizing crawled tweets...");
        TweetDAO tDao = DataManager.getInstance().getTweetDao();
        return steamCollection(tDao.getCrawledTweets());
    }

    private HashSet<String> steamCollection(Collection<String> collection) {

        List<String> tokens = null;
        HashSet<String> tweets = new HashSet<>();
        SentenceIterator iter = new CollectionSentenceIterator(collection);
        TokenizerFactory t = new DefaultTokenizerFactory();
        String filteredTweet;
        //Tokenize the tweets
        t.setTokenPreProcessor(new CommonPreprocessor());
        while(iter.hasNext()) {
            tokens = t.create(iter.nextSentence()).getTokens();
            filteredTweet = cleanTerms(tokens);
            tweets.add(filteredTweet);
        }
        return tweets;
    }

    private List<String> tokenizeString(String string) {

        ArrayList<String> collection = new ArrayList<String>();
        collection.add(string);
        List<String> tokens = null;
        SentenceIterator iter = new CollectionSentenceIterator(collection);
        TokenizerFactory t = new DefaultTokenizerFactory();
        String filteredTweet;
        //Tokenize the tweets
        t.setTokenPreProcessor(new CommonPreprocessor());
        tokens = t.create(iter.nextSentence()).getTokens();
        return Arrays.asList(cleanTerms(tokens).split("\\s+"));
    }

    public static String cleanTerms(List<String> tokens) {
        Iterator<String> i = tokens.iterator();
        String s;
        while (i.hasNext()) {
            s = i.next();
            if( //Empty word
                    s.trim().length() == 0 ||
                            //URLs, user mentions and other characters
                            Pattern.matches("(https?.*)|(@.*)|(—|\\.+|,+|…)",s) ) {
                {
                    i.remove();
                }
            } else {
                //Remove some characters
                s.replaceAll("[\\[\\]\"“”]","");
            }
        }
        return String.join(" ",tokens);
    }

    private void trainModel(HashSet<String> tweets) {
        SentenceIterator iter = new CollectionSentenceIterator(tweets);
        TokenizerFactory t = new DefaultTokenizerFactory();
        t.setTokenPreProcessor(new CommonPreprocessor());
        Word2Vec vec = new Word2Vec.Builder()
                .minWordFrequency(2)
                .iterations(200)
                .layerSize(100)
                .seed(42)
                .windowSize(5)
                .iterate(iter)
                .tokenizerFactory(t)
                .build();
        System.out.println("Fitting Word2Vec model....");
        vec.fit();
        synchronized (lock) {
            this.model = vec;
        }
    }

    public void save() {
        System.out.println(("Saving model...."));
        WordVectorSerializer.writeWord2Vec(model, modelPath);
    }

    private void load(){
        System.out.println(("Loading model...."));
        try {
            this.model = WordVectorSerializer.readWord2Vec(new File(modelPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
