package app.ml;

import app.twitter.TweetFilter;
import app.util.Pair;
import info.debatty.java.stringsimilarity.interfaces.StringSimilarity;
import org.nd4j.linalg.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * @author Fran Lozano
 */
public class DuplicateDetector {

    //Threshold for considering a document as duplicated
    private static final double SIMILARITY_THRESHOLD = 0.7;
    private static final Similarity SIMILARITY = Similarity.NORMALIZED_LEVENSHTEIN;

    public static boolean isDuplicated (String document, List<String> documents) {
        for (String doc: documents) {
            if (SIMILARITY.get().similarity(document, doc) > SIMILARITY_THRESHOLD) return true;
        }
        return false;
    }

    public static void main(String[] args) {
        DuplicateDetector.getBenchmark();
    }

    private static void getBenchmark() {

        //Read test files
        List <String> noDuplicatesTemp = new LinkedList<>();
        List <Pair<String,String>> noDuplicates = new LinkedList<>();
        List <Pair<String,String>> duplicates = new LinkedList<>();
        try {
            File noDuplicatesFile = new ClassPathResource("data/docs/dd_noduplicate.txt").getFile();
            File duplicatesFile = new ClassPathResource("data/docs/dd_duplicate.txt").getFile();
            BufferedReader in = new BufferedReader(new FileReader(noDuplicatesFile));
            String line1, line2;
            while(((line1 = in.readLine()) != null))
            {
                noDuplicatesTemp.add(line1);
            }
            in.close();
            ListIterator<String> lit = noDuplicatesTemp.listIterator();
            while (lit.hasNext()) {
                String nd = lit.next();
                lit.remove();
                for(String ndd: noDuplicatesTemp) {
                    if(!ndd.trim().equals("")) {
                        noDuplicates.add(new Pair<>(nd, ndd));
                    }
                }
            }

            in = new BufferedReader(new FileReader(duplicatesFile));
            while(((line1 = in.readLine()) != null) && ((line2 = in.readLine()) != null))
            {
                duplicates.add(new Pair<>(line1, line2));
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        int nNoDuplicates = noDuplicates.size();
        int nDuplicates = duplicates.size();
        System.out.println("Number of non-duplicate pairs: " + nNoDuplicates);
        System.out.println("Number of duplicate pairs: " + nDuplicates);

        //Declare array of algorithms
        Similarity [] similarities = {Similarity.JACCARD,Similarity.COSINE,Similarity.JARO_WINKLER,Similarity.NORMALIZED_LEVENSHTEIN};
        for (int i = 0; i < similarities.length; i++) {
            Similarity sim = similarities[i];
            StringSimilarity similarityAlgorithm = sim.get();
            double maxFscore = 0.0 , scorePrecision = 0.0, scoreRecall = 0.0, optThreshold = 0.0;
            for (double threshold = 0.0; threshold < 1.0; threshold += 0.01) {
                int positives = 0;
                int truePositives = 0;
                //Iterate noDuplicates
                Iterator <Pair<String,String>> it = noDuplicates.iterator();
                while (it.hasNext()) {
                    Pair<String,String> pair = it.next();
                    double similarity = similarityAlgorithm.similarity(
                            TweetFilter.filter(pair.getLeft()),TweetFilter.filter(pair.getRight()));
                    if (similarity > threshold) positives++;
                }
                //Iterate Duplicates
                it = duplicates.iterator();
                while (it.hasNext()) {
                    Pair<String,String> pair = it.next();
                    double similarity = similarityAlgorithm.similarity(
                            TweetFilter.filter(pair.getLeft()),TweetFilter.filter(pair.getRight()));
                    if (similarity > threshold) {truePositives++; positives++;}
                }
                double precision =  (positives > 0)?(double) truePositives/positives:0.0;
                double recall = (nDuplicates > 0)?(double) truePositives/nDuplicates:0.0;
                double fScore = (precision + recall > 0)?(2* precision * recall) / (precision + recall):0.0;
                if(fScore >= maxFscore) {
                    maxFscore = fScore;
                    scorePrecision = precision;
                    scoreRecall = recall;
                    optThreshold = threshold;
                }
                //System.out.println(String.format("Threshold: %.2f - Precision: %.2f - Recall: %.2f - F1Score: %.2f",
                //threshold,precision,recall,fScore));
            }
            System.out.print(sim + ": ");
            System.out.println(String.format("Best F1-Score: %.2f - Opt. Threshold: %.2f - Precision: %.2f - Recall: %.2f",
                    maxFscore,optThreshold,scorePrecision,scoreRecall));
        }
    }
}
