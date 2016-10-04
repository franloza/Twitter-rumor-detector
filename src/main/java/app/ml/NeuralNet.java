package app.ml;

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
//TODO: Update neural network with each classified tweet and save in in a file
public class NeuralNet {

    private Word2Vec model;
    private final String modelPath = "src/main/resources/data/model.data";
    public NeuralNet() {
        File f = new File(modelPath);
        if(f.exists() && !f.isDirectory()) {
                this.load();
        }else{
            //Create a new model using some initial tweets
            List<String> tweets = this.loadInitialTweets("/data/rumorTweets.txt");
            this.model = trainModel(tweets);
            //Save the model in a file
            this.save();
        }
    }

    public Collection<String> getWordsNearest(String keyword, int n) {
        Collection<String> words = new LinkedList<>();
        Collection<String> keywordColl = Arrays.asList(keyword.split("\\s+"));
        if (keywordColl.size() > 1) {
            INDArray vectors = model.getWordVectors(keywordColl);
            if(vectors.isVector()) {
                words = model.wordsNearest(vectors, n);
                //Remove duplicates
                Iterator it = words.iterator();
                while (it.hasNext()) {
                    String w = (String) it.next();
                    if (keyword.contains(w)) it.remove();
                }
            }
        } else {
            words = model.wordsNearest(keyword,n);
        }
        return words;
    }

    public VocabCache<VocabWord> getVocabulary() {
        return model.getVocab();
    }

    private List<String> loadInitialTweets(String filePath) {
        System.out.println("Loading & vectorizing initial tweets...");
        List<String> tokens = null;
        List<String> tweets = new LinkedList<>();
        try {
            String path = new ClassPathResource(filePath).getFile().getAbsolutePath();
            SentenceIterator iter = new BasicLineIterator(path);
            String filteredTweet;

            //Tokenize the tweets
            TokenizerFactory t = new DefaultTokenizerFactory();
            t.setTokenPreProcessor(new CommonPreprocessor());
            while(iter.hasNext()) {
                tokens = t.create(iter.nextSentence()).getTokens();
                filteredTweet = cleanTerms(tokens);
                tweets.add(filteredTweet);
            }
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return tweets;
    }

    private static String cleanTerms(List<String> tokens) {
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

    private static Word2Vec trainModel(List<String> tweets) {
        System.out.println("Building model....");
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
        return vec;
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
