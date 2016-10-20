package app.util;

import lombok.*;

/**
 * Class to handle all the paths of the application
 */

public class Path {

    // The @Getter methods are needed in order to access
    // the variables from Velocity Templates
    public static class Web {
        @Getter public static final String INDEX = "/";
        @Getter public static final String ANNOTATION = "/annotation/";
        @Getter public static final String KEYWORD_CRAWLER = "/keyword-crawler/";
        @Getter public static final String RUMOR_CRAWLER = "/rumor-crawler/";
    }

    public static class Template {
        public final static String INDEX = "/view/index.vm";
        public final static String ANNOTATION = "view/annotation.vm";
        public final static String KEYWORD_CRAWLER = "view/keywordCrawler.vm";
        public final static String RUMOR_CRAWLER = "view/rumorCrawler.vm";
        public final static String NOT_FOUND = "/view/notFound.vm";
        public final static String TWITTER_ERROR ="/view/twitterError.vm";
    }

}
