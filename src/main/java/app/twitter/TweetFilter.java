package app.twitter;

import app.util.LanguageDetectionManager;
import org.deeplearning4j.text.inputsanitation.InputHomogenization;
import org.deeplearning4j.text.sentenceiterator.CollectionSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

/**
 * @author Fran Lozano
 * Class that contains some text processing functions for filtering tweets
 */
public class TweetFilter {
    static private int filtered = 0;

    public static int getFiltered() {
        return filtered;
    }

    public static String basicFilter(String tweet) {
        //Remove URLs
        tweet = tweet.replaceAll("https?://\\S+\\s?", "");
        //Remove mentions
        tweet = tweet.replaceAll("RT +@[^ :]+:?", "");
        return tweet;
    }

    public static String filter(String tweet) {

        ArrayList<String> collection = new ArrayList<>();
        collection.add(tweet);
        List<String> tokens;
        SentenceIterator iter = new CollectionSentenceIterator(collection);
        TokenizerFactory t = new DefaultTokenizerFactory();

        List <String> filteredTokens;
        //Tokenize the tweets
        t.setTokenPreProcessor(new CommonPreprocessor());
        tokens = t.create(iter.nextSentence()).getTokens();

        //First filter (Clean terms)
        filteredTokens = cleanTerms(tokens);
        if (filteredTokens.size() == 0){
            filtered++;
            return "";
        }

        //Second filter (Remove tweets containing not accepted characters - Not english)
        String filteredTweet = String.join(" ", filteredTokens);
        InputHomogenization ih = new InputHomogenization(filteredTweet);
        String sanitized = ih.transform();
        if (!filteredTweet.equals(sanitized)){
            filtered++;
            return "";
        }

        //Third filter (Language detection)
        if (!LanguageDetectionManager.isEnglish(filteredTweet)) {
            filtered++;
            return "";
        }

        /*Other filters
         ---- Porter Stemmer ------
        System.out.println(String.join(" ", filteredTokens));
        ListIterator<String> it = filteredTokens.listIterator();
        while (it.hasNext()) {
            String token = it.next();
            PorterStemmer stemmer = new PorterStemmer();
            stemmer.setCurrent(token);
            stemmer.stem();
            it.set(stemmer.getCurrent());
        }
        System.out.println(String.join(" ", filteredTokens));

        ------ Spelling correction ------
        System.out.println(String.join(" ", filteredTokens));
        ListIterator<String> it = filteredTokens.listIterator();
        while (it.hasNext()) {
            String token = it.next();
            List<Word> suggestions = SpellCheckerManager.getSuggestions(token, 90);
            if (suggestions.size() > 0) {
                it.set(suggestions.get(0).getWord());
            }
        }
        System.out.println(String.join(" ", filteredTokens));*/


        return filteredTweet;
    }


    public static List<String> tokenizeString(String string) {

        ArrayList<String> collection = new ArrayList<String>();
        collection.add(string);
        List<String> tokens = null;

        SentenceIterator iter = new CollectionSentenceIterator(collection);
        TokenizerFactory t = new DefaultTokenizerFactory();
        String filteredTweet;
        //Tokenize the tweets
        t.setTokenPreProcessor(new CommonPreprocessor());
        tokens = t.create(iter.nextSentence()).getTokens();
        return tokens;
    }

    public static List<String> cleanTerms(List<String> tokens) {
        ListIterator<String> i = tokens.listIterator();
        String s;
        while (i.hasNext()) {
            s = i.next();
            if(
                //Empty word
                s.trim().length() == 0 ||
                //URLs, user mentions and other characters
                Pattern.matches("(https?.*)|(@.*)|(—|\\.+|,+|…)",s)
                ) {
                    i.remove();
            }
            //Remove incomplete words
            else if (s.contains("..") || s.contains("...") || s.contains("…")) {
                i.remove();
            }
            else {
                //Remove some characters
                String str = s.replaceAll("[\\[\\]\"“”‘’-]","");
                str = str.replaceAll("[\\t\\n\\r]","");
                str = str.replaceAll("&amp","");
                str = str.replaceAll("[$__#@]","");
                str = str.trim();
                if (!str.equals("")) i.set(str);
                else i.remove();
            }

        }
        return tokens;
    }

    public static String cleanTerm (String token) {
        String str = token;
        if(
            //Empty word
                token.trim().length() == 0 ||
                        //URLs, user mentions and other characters
                        Pattern.matches("(https?.*)|(@.*)|(—|\\.+|,+|…)",token)
                ) {
            return "";
        }
        //Remove incomplete words
        else if (token.contains("..") || token.contains("...") || token.contains("…")) {
            return "";
        }
        else {
            //Remove some characters
            str = token.replaceAll("[\\[\\]\"“”‘’-]","");
            str = str.replaceAll("[\\t\\n\\r]","");
            str = str.replaceAll("&amp","");
            str = str.replaceAll("[$__#@]","");
            str = str.trim();
        }
        return str;
    }
}


