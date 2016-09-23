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
    }

    public static class Template {
        public final static String INDEX = "/view/index.vm";
        public static final String NOT_FOUND = "/view/notFound.vm";
    }

}
