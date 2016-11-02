import app.controller.*;
import app.twitter.TwitterHandler;
import app.util.spark.Filters;
import twitter4j.TwitterException;

import static spark.Spark.*;
import static spark.debug.DebugScreen.enableDebugScreen;

/**
 * Entry point of the application
 * @author Fran Lozano
 */
public class Application {

        public static void main(String[] args) throws TwitterException {

            // Configure Spark
            staticFiles.location("/public");
            staticFiles.expireTime(600L);
            port(4567);
            enableDebugScreen();

            //Initialize controllers
            TwitterHandler th = new TwitterHandler();
            AnnotationController.start(th);
            KeywordCrawlerController.start(th);
            RumorCrawlerController.start(th);
            BenchmarkController.start(th.getQueryBuilder());

            // Set up before-filters (called before each get/post)
            before("*",                  Filters.addTrailingSlashes);
            before("*",                  Filters.handleLocaleChange);

            //Set up routes
            Router.setRoutes();
        }
}


