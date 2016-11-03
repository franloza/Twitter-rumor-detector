package app.ml;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @author Fran Lozano
 */
public class KeywordExtractorAdapter implements KeywordExtractor {

    private int minCharacters;
    private int minFrecuency;
    private int maxTerms;
    private final float MIN_THRESHOLD = 1.0f;

    public KeywordExtractorAdapter(int minCharacters, int maxTerms,int minFrecuency) {
        this.minCharacters = minCharacters;
        this.maxTerms = maxTerms;
        this.minFrecuency = minFrecuency;
    }

    public KeywordExtractorAdapter() {
        //Default parameters
        this(4,3,5);
    }

    @Override
    public List<String> getKeywords() {
        HashMap<String, Float> map = readKeywords();
        List<String> keywords = new ArrayList<>();
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next().toString();
            float score = map.get(key);
            if(score > MIN_THRESHOLD) keywords.add(key);
        }
        return keywords;
    }

    private HashMap<String, Float> readKeywords() {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(String.format("python src\\main\\python\\keyword-extractor\\main.py %d %d %d",
                    this.minCharacters,this.maxTerms,this.minFrecuency));
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String output  = null;
        try {
            output = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject obj = new JSONObject(output);
        Iterator it = obj.keys();
        HashMap<String,Float> map = new HashMap<>();
        while (it.hasNext()) {
            String key = it.next().toString();
            float score = (float) obj.getDouble(key);
            map.put(key,score);
        }
        return map;
    }
}
