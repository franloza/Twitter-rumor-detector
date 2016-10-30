package app.ml;

import app.db.DataManager;
import app.db.TweetDAO;
import app.twitter.TweetFilter;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.CollectionSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Neural network of the annotation tool that helps to build the queries
 * @author Fran Lozano
 */
public class NeuralNet {

    private Word2Vec model;
    private final String modelPath = "src/main/resources/data/models/nn.model";
    private Object lock = new Object ();

    public NeuralNet() {
        File f = new File(modelPath);
        TweetDAO tDao = DataManager.getInstance().getTweetDao();

        if(f.exists() && !f.isDirectory()) {
                //Load model from binary file
                this.load();
        }else{
            //Create a new model
            HashSet<String> tweets;
            if(tDao.countCrawled("crawler") > 0){
                createModel();
            } else {
                //Load tweets from file
                trainModel("/data/rumorTweets.txt");
                //Save the model in a file
                this.save();
            }
        }
    }

    public synchronized void createModel () {
        List<String> tweets;
        //Load tweets from database
        tweets = this.loadTweets();
        trainModel(tweets);
        //Save the model in a file
        this.save();
    }

    public Collection<String> getWordsNearest(String keyword, int n) {
        Collection<String> words = new LinkedList<>();

        Collection<String> keywordColl = TweetFilter.tokenizeString(keyword);
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

    private List<String> loadTweets() {
        System.out.println("Loading & vectorizing crawled tweets...");
        TweetDAO tDao = DataManager.getInstance().getTweetDao();
        return tDao.getCrawledTweets();
    }

    private void trainModel(String path) {
        try {
            String absolutePath = new ClassPathResource(path).getFile().getAbsolutePath();
            SentenceIterator iter = new BasicLineIterator(absolutePath);
            trainModel(iter);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void trainModel(List<String> tweets) {
        SentenceIterator iter = new CollectionSentenceIterator(tweets);
        trainModel(iter);
    }

    private void trainModel(SentenceIterator iter) {
        //Set filtering function
        iter.setPreProcessor((SentencePreProcessor) sentence -> {
            String filtered = TweetFilter.filter(sentence);
            return filtered;
        });

        TokenizerFactory t = new DefaultTokenizerFactory();
        t.setTokenPreProcessor(new CommonPreprocessor());
        Word2Vec vec = new Word2Vec.Builder()
                .minWordFrequency(5)
                .iterations(200)
                .layerSize(100)
                .seed(42)
                .windowSize(5)
                .iterate(iter)
                .tokenizerFactory(t)
                .build();
        System.out.println("Fitting Word2Vec model....");
        vec.fit();
        System.out.println("Number of tweets deleted: " + TweetFilter.getFiltered());
        synchronized (lock) {
            this.model = vec;
        }
    }

    //Persistency functions
    private void save() {
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

    //Wrapper functions
    public List<String> getStopWords() {
        return model.getStopWords();
    }

    public double similarity (String word1, String word2) {
        return this.model.similarity(word1,word2);
    }

    public VocabCache<VocabWord> getVocabulary() {
        return model.getVocab();
    }
}
