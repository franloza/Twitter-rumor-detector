package app.ml;

import info.debatty.java.stringsimilarity.*;
import info.debatty.java.stringsimilarity.interfaces.StringSimilarity;

/**
 * @author Fran Lozano
 */
public enum Similarity {

    JACCARD("Jaccard similarity",new Jaccard()),
    COSINE ("Cosine similarity" , new Cosine()),
    JARO_WINKLER ("Jaro Winkler similarity", new JaroWinkler()),
    NORMALIZED_LEVENSHTEIN ("Normalized Levenshtein similarity: ", new NormalizedLevenshtein());

    private String name;
    private StringSimilarity type;

    Similarity(String name,StringSimilarity type) {
        this.name = name;
        this.type = type;
    }

    public String toString() {return name;}

    public StringSimilarity get() {
        return type;
    }

}
