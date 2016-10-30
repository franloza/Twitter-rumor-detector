package app.twitter;

import app.util.LanguageDetectionManager;
import org.deeplearning4j.text.inputsanitation.InputHomogenization;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

/**
 * @author Fran Lozano
 * Class that contains some text processing functions for filtering tweets
 */
public class TweetFilter {

    public static String basicFilter(String tweet) {
        //Remove URLs
        tweet = tweet.replaceAll("https?://\\S+\\s?", "");
        //Remove mentions
        tweet = tweet.replaceAll("RT +@[^ :]+:?", "");
        return tweet;
    }

    public static String filter(String tweet) {

        //Tokenize the tweets
        TokenizerFactory t = new DefaultTokenizerFactory();
        t.setTokenPreProcessor(new CommonPreprocessor());
        List<String>  tokens = t.create(tweet).getTokens();

        //First filter (Clean terms)
        List <String> filteredTokens = cleanTerms(tokens);
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

    public static String filterWord (String token) {

        token = new CommonPreprocessor().preProcess(token);
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
            String str = token.replaceAll("[\\[\\]\"“”‘’-]","");
            str = str.replaceAll("[\\t\\n\\r]","");
            str = str.replaceAll("&amp","");
            str = str.replaceAll("[$__#@]","");
            str = str.trim();
            return str;
        }
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

    static private int filtered = 0;

    public static int getFiltered() {
        return filtered;
    }

    public static void resetCounter() {
        filtered = 0;
    }
}


