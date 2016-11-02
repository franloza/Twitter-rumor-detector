package app.controller;

import app.util.spark.Path;
import app.util.spark.ViewUtil;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.HashMap;
import java.util.Map;

public class IndexController {
    public static Route servePage = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        return ViewUtil.render(request, model, Path.Template.INDEX);
      };
}
