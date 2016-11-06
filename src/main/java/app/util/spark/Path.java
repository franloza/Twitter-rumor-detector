package app.util.spark;

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
        @Getter public static final String TWEET_COLLECTOR= "/tweet-collector/";
        @Getter public static final String RUMOR_CRAWLER = "/rumor-crawler/";
        @Getter public static final String CLASSIFIER = "/tweet-classifier/";
        @Getter public static final String BENCHMARK = "/benchmark/";
        @Getter public static final String BENCHMARK_QB = "/benchmark/query-builder/";
        @Getter public static final String BENCHMARK_DD = "/benchmark/duplicate-detector/";
    }

    public static class Template {
        public final static String INDEX = "/view/index.vm";
        public final static String ANNOTATION = "view/annotation.vm";
        public final static String KEYWORD_CRAWLER = "view/tweetCollector.vm";
        public final static String RUMOR_CRAWLER = "view/rumorCrawler.vm";
        public final static String BENCHMARK= "view/benchmark.vm";
        public final static String CLASSIFIER_GET= "view/tweetClassifierGet.vm";
        public final static String CLASSIFIER_POST= "view/tweetClassifierPost.vm";
        public final static String BENCHMARK_QB= "view/benchmarks/queryBuilder.vm";
        public final static String BENCHMARK_DD= "view/benchmarks/duplicateDetector.vm";
        public final static String NOT_FOUND = "/view/notFound.vm";
        public final static String TWITTER_ERROR ="/view/twitterError.vm";
    }

}
