package crawler.filter;


import java.util.*;

/**
 * Term frequency-Inverse document frequency
 *
 * Based on github.com/wpm/tfidf commit fcc87bea4a29a2e312fa483b2b0052509f270fc6
 * Modified by Jon Ayerdi on 15/10/2016.
 */
public class TfIdf {

    /**
     * Word count method used for term frequencies
     */
    public enum TfType {
        /**
         * Term frequency
         */
        NATURAL,
        /**
         * Log term frequency plus 1
         */
        LOGARITHM,
        /**
         * 1 if term is present, 0 if it is not
         */
        BOOLEAN
    }

    /**
     * Normalization of the tf-idf vector
     */
    public enum Normalization {
        /**
         * Do not normalize the vector
         */
        NONE,
        /**
         * Normalize by the vector elements added in quadrature
         */
        COSINE
    }

    /**
     * Term frequency for a single document
     *
     * @param document bag of terms
     * @param type     natural or logarithmic
     * @param <TERM>   term type
     * @return map of terms to their term frequencies
     */
    public static <TERM> Map<TERM, Double> tf(Collection<TERM> document, TfType type) {
        Map<TERM, Double> tf = new HashMap<>();
        for (TERM term : document) {
            tf.put(term, tf.getOrDefault(term, 0.0) + 1);
        }
        if (type != TfType.NATURAL) {
            for (TERM term : tf.keySet()) {
                switch (type) {
                    case LOGARITHM:
                        tf.put(term, 1 + Math.log(tf.get(term)));
                        break;
                    case BOOLEAN:
                        tf.put(term, tf.get(term) == 0.0 ? 0.0 : 1.0);
                        break;
                }
            }
        }
        return tf;
    }

    /**
     * Natural term frequency for a single document
     *
     * @param document bag of terms
     * @param <TERM>   term type
     * @return map of terms to their term frequencies
     */
    public static <TERM> Map<TERM, Double> tf(Collection<TERM> document) {
        return tf(document, TfType.LOGARITHM);
    }

    /**
     * Inverse document frequency for a set of documents
     *
     * @param documentVocabularies sets of terms which appear in the documents
     * @param smooth               smooth the counts by treating the document set as if it contained an additional
     *                             document with every term in the vocabulary
     * @param addOne               add one to idf values to prevent divide by zero errors in tf-idf
     * @param <TERM>               term type
     * @return map of terms to their inverse document frequency
     */
    public static <TERM> Map<TERM, Double> idf(List<List<TERM>> documentVocabularies,
                                               boolean smooth, boolean addOne) {
        Map<TERM, Integer> df = new HashMap<>();
        int d = smooth ? 1 : 0;
        int a = addOne ? 1 : 0;
        int n = d;
        for (Iterable<TERM> documentVocabulary : documentVocabularies) {
            n += 1;
            for (TERM term : documentVocabulary) {
                df.put(term, df.getOrDefault(term, d) + 1);
            }
        }
        Map<TERM, Double> idf = new HashMap<>();
        for (Map.Entry<TERM, Integer> e : df.entrySet()) {
            TERM term = e.getKey();
            double f = e.getValue();
            idf.put(term, Math.log(n / f) + a);
        }
        return idf;
    }

    /**
     * Smoothed, add-one inverse document frequency for a set of documents
     *
     * @param documentVocabularies sets of terms which appear in the documents
     * @param <TERM>               term type
     * @return map of terms to their inverse document frequency
     */
    public static <TERM> Map<TERM, Double> idf(List<List<TERM>> documentVocabularies) {
        return idf(documentVocabularies, true, true);
    }

    /**
     * tf-idf for a document
     *
     * @param tf            term frequencies of the document
     * @param idf           inverse document frequency for a set of documents
     * @param normalization none or cosine
     * @param <TERM>        term type
     * @return map of terms to their tf-idf values
     */
    public static <TERM> Map<TERM, Double> tfIdf(Map<TERM, Double> tf, Map<TERM, Double> idf,
                                                 Normalization normalization) {
        Map<TERM, Double> tfIdf = new HashMap<>();
        for (TERM term : tf.keySet()) {
            tfIdf.put(term, tf.get(term) * idf.get(term));
        }
        if (normalization == Normalization.COSINE) {
            double n = 0.0;
            for (double x : tfIdf.values()) {
                n += x * x;
            }
            n = Math.sqrt(n);

            for (TERM term : tfIdf.keySet()) {
                tfIdf.put(term, tfIdf.get(term) / n);
            }
        }
        return tfIdf;
    }

    /**
     * Unnormalized tf-idf for a document
     *
     * @param tf     term frequencies of the document
     * @param idf    inverse document frequency for a set of documents
     * @param <TERM> term type
     * @return map of terms to their tf-idf values
     */
    public static <TERM> Map<TERM, Double> tfIdf(Map<TERM, Double> tf, Map<TERM, Double> idf) {
        return tfIdf(tf, idf, Normalization.COSINE);
    }

}
