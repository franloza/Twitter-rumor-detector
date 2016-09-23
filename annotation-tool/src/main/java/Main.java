import static spark.Spark.*;
/**
 * Entry point of the application
 * @author Fran Lozano
 */
public class Main {
        public static void main(String[] args) {
            get("/hello", (req, res) -> "Hello World");
        }
}
