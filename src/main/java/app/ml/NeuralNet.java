package app.ml;

import app.db.DataManager;
import app.db.TweetDAO;
import app.twitter.TweetFilter;
import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.CollectionSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.io.ClassPathResource;
import org.nd4j.linalg.ops.transforms.Transforms;

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

    public List<String> getWordsNearest(String keyword, int n) {
        Collection<String> retrievedWords;
        List<String> nearestWords = new LinkedList<>();
        Tokenizer tokenizer = new DefaultTokenizerFactory().create(keyword);

        tokenizer.setTokenPreProcessor(TweetFilter::filterWord);
        List<String> keywordColl = tokenizer.getTokens();
        keywordColl.removeAll(Arrays.asList(null,""));

        synchronized (lock) {
            //More than one terms are valid
            if (keywordColl.size() > 0) {
                //INDArray vectors = model.getWordVectors(keywordColl);
                    retrievedWords = model.wordsNearest(keywordColl,new ArrayList<>(), n + keywordColl.size());
                    Iterator<String> it = retrievedWords.iterator();
                    //Retrieve similar words
                    String word;
                    while (it.hasNext()) {
                        word = it.next();
                        //Save words except the keywords themselves
                        if(!keywordColl.contains(word)) nearestWords.add(word);
                    }
            }
        }
        return nearestWords;
    }

    public double similarity (List<String> labels1, List<String> labels2) {

        if (labels1 == null || labels2 == null) {
            return Double.NaN;
        }

        WeightLookupTable<VocabWord> lookupTable = model.getLookupTable();
        //Mean first labels
        INDArray words1 = Nd4j.create(labels1.size(), lookupTable.layerSize());
        int row = 0;
        for (String s : labels1) {
            words1.putRow(row++, lookupTable.vector(s));
        }
        INDArray mean1 = words1.isMatrix() ? words1.mean(0) : words1;
        //Mean second labels
        INDArray words2 = Nd4j.create(labels2.size(), lookupTable.layerSize());
        row = 0;
        for (String s : labels2) {
            words2.putRow(row++, lookupTable.vector(s));
        }
        INDArray mean2 = words2.isMatrix() ? words2.mean(0) : words2;

        if (mean1 == null || mean2 == null) {
            return Double.NaN;
        }

        Collections.sort(labels1);
        Collections.sort(labels2);
        if (labels1.equals(labels2)) return 1.0;

        return Transforms.cosineSim(mean1, mean2);

    }

    public double similarity (String label1, String label2) {
        DefaultTokenizerFactory factory = new DefaultTokenizerFactory();
        Tokenizer tokenizer = factory.create(label1);
        tokenizer.setTokenPreProcessor(TweetFilter::filterWord);
        List<String> labels1 = tokenizer.getTokens();

        tokenizer = factory.create(label2);
        tokenizer.setTokenPreProcessor(TweetFilter::filterWord);
        List<String> labels2 = tokenizer.getTokens();
        labels1.removeAll(Arrays.asList(null,""));
        labels2.removeAll(Arrays.asList(null,""));

        if (labels1.size() == 0 || labels2.size() == 0) return Double.NaN;
        else if (labels1.size() == 1 && labels2.size() == 1) return model.similarity(labels1.get(0),labels2.get(0));
        else return similarity(labels1,labels2);
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
        iter.setPreProcessor((SentencePreProcessor) TweetFilter::filter);

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
    public VocabCache<VocabWord> getVocabulary() {
        return model.getVocab();
    }
}
