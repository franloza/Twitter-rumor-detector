package app.ml;

import app.db.DataManager;
import app.db.TweetDAO;
import app.twitter.TweetFilter;
import com.entopix.maui.filters.MauiFilter;
import com.entopix.maui.main.MauiModelBuilder;
import com.entopix.maui.main.MauiTopicExtractor;
import com.entopix.maui.util.MauiDocument;
import com.entopix.maui.util.MauiTopics;
import com.entopix.maui.util.Topic;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.CollectionSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.linalg.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;


/**
 * @author Mauro Allegretta
 */
public class KeywordExtractor {

    private static final String TWEET_PATH = "src/main/resources/data/docs/fao_train/";
    private static final String TEST_PATH = "src/main/resources/data/docs/fao_test/";
    private static final String MODEL_PATH = "src/main/resources/data/models/";

    public static void main(String[] args) {


        //Standfor TAGGER
        // Initialize the tagger
        String path;
        try {
            path = new ClassPathResource("data/taggers/english-left3words-distsim.tagger").getFile().getAbsolutePath();
            MaxentTagger tagger = new MaxentTagger(path);

            TweetDAO tDao = DataManager.getInstance().getTweetDao();
            List<String> tokens = null;
            HashSet<String> tweets = new HashSet<>();
            SentenceIterator iter = new CollectionSentenceIterator(tDao.getCrawledTweets());
            TokenizerFactory t = new DefaultTokenizerFactory();
            String filteredTweet;
            SentenceIterator iter2 = new BasicLineIterator("src/main/resources/data/rumorTweets.txt");

            //Writing in files
            //Tokenize the tweets
            t.setTokenPreProcessor(new CommonPreprocessor());
            int i = 0;
            PrintWriter writer = new PrintWriter(TWEET_PATH + "train.txt");
            while(iter.hasNext()) {
                tokens = t.create(iter.nextSentence()).getTokens();
                filteredTweet = String.join(" ",TweetFilter.cleanTerms(tokens));
                writer.println(filteredTweet);
                //String tagged = tagger.tagString(filteredTweet);
                //System.out.println(filteredTweet + " ---> " + tagged);
                i++;
            }
            writer.close();

            writer = new PrintWriter(TEST_PATH + "test.txt");
            while(iter2.hasNext()) {
                tokens = t.create(iter2.nextSentence()).getTokens();
                filteredTweet = String.join(" ",TweetFilter.cleanTerms(tokens));
                writer.println(filteredTweet);
                //String tagged = tagger.tagString(filteredTweet);
                //System.out.println(filteredTweet + " ---> " + tagged);
            }
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        //MAUI
        String modelname="keyword_extraction_model";
        File modelfile;
        try {
            modelfile = new File(modelname);
        } catch (Exception e){
            modelfile = null;
        }
        if (modelfile == null || !modelfile.exists()) {
            String[] trainOptions=new String[]{"-l",TWEET_PATH,"-m",MODEL_PATH + modelname,"-v","none","-o","2"};
            try {
                MauiModelBuilder mauiModelBuilder=new MauiModelBuilder();
                mauiModelBuilder.setBasicFeatures(true);
                mauiModelBuilder.setOptions(trainOptions);
                //System.out.println(mauiModelBuilder.getOptions());
                MauiFilter frankModel=mauiModelBuilder.buildModel();
                mauiModelBuilder.saveModel(frankModel);
            }
            catch (    Exception ex) {
                ex.printStackTrace();
            }
        }
        String[] testOptions=new String[]{"-l",TWEET_PATH,"-m",MODEL_PATH + modelname,"-v","none","-n","1"};
        try {
            MauiTopicExtractor mauiTopicExtractor=new MauiTopicExtractor();
            mauiTopicExtractor.setOptions(testOptions);
            mauiTopicExtractor.loadModel();
            List<MauiDocument> testFiles=mauiTopicExtractor.loadDocuments();
            List<MauiTopics> testTopics=mauiTopicExtractor.extractTopics(testFiles);
            for (    MauiTopics tts : testTopics) {
                System.out.println(tts.getFilePath());
                for ( Topic topic : tts.getTopics())       System.out.print(topic.getTitle() + ":" + topic.getProbability()+ ", ");
            }
        }
        catch (  Exception ex) {
            ex.printStackTrace();
        }
    }
}
