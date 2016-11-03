package app.controller;

import app.util.spark.Path;
import app.util.spark.ViewUtil;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.get;
import static spark.Spark.post;

/**
 * @author Fran Lozano
 */
public class Router {

    public static void setRoutes() {
        // Set up routes
        get(Path.Web.INDEX,          IndexController.servePage);
        get(Path.Web.ANNOTATION,     AnnotationController.servePage);
        get(Path.Web.TWEET_COLLECTOR, TweetCollectorController.servePage);
        get(Path.Web.RUMOR_CRAWLER,  RumorCrawlerController.servePage);
        post(Path.Web.ANNOTATION,    AnnotationController.processRequest);
        get(Path.Web.BENCHMARK, (request, response) -> ViewUtil.render(request, new HashMap<>(), Path.Template.BENCHMARK));
        get(Path.Web.BENCHMARK_QB, (request, response) -> {
            Map<String, Object> model = new HashMap<>();
            String result = BenchmarkController.getBenchmark(BenchmarkController.Benchmark.QUERY_BUILDER);
            result = result.replaceAll("(\r\n|\n)", "<br/>");
            result = result.replaceAll("(\t)","&nbsp &nbsp &nbsp &nbsp");
            model.put("result",result);
            return ViewUtil.render(request, model, Path.Template.BENCHMARK_QB);
        });
        get("*",                     ViewUtil.notFound);
    }
}
