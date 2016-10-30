package app.controller;

import app.twitter.QueryBuilder;

/**
 * @author Fran Lozano
 */
public class BenchmarkController {
    private static QueryBuilder qb;
    public enum Benchmark {QUERY_BUILDER}

    public static void start(QueryBuilder queryBuilder ) {
        qb = queryBuilder;
    }

    public static String getBenchmark (Benchmark benchmark) {
        String result = "";
        if (benchmark == Benchmark.QUERY_BUILDER) {
            if (qb != null)result = qb.getBenchmark();
        }
        return result;
    }
}
