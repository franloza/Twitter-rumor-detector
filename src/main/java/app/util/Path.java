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
    }

    public static class Template {
        public final static String INDEX = "/view/index.vm";
        public final static String ANNOTATION = "view/annotation.vm";
        public static final String NOT_FOUND = "/view/notFound.vm";
    }

}
