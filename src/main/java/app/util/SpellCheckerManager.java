package app.util;

/**
 * @author James Goodwill
 */
import com.swabunga.spell.engine.SpellDictionaryHashMap;
import com.swabunga.spell.event.SpellChecker;
import org.nd4j.linalg.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SpellCheckerManager {

    protected static SpellDictionaryHashMap dictionary = null;
    protected static SpellChecker spellChecker = null;

    static {

        try {

            dictionary =
                    new SpellDictionaryHashMap(new
                            File(new ClassPathResource("/data/dictionary/english.0").getFile().getAbsolutePath()));
        }
        catch (IOException e) {

            e.printStackTrace();
        }
        spellChecker = new SpellChecker(dictionary);
    }

    public static List getSuggestions(String word,
                                      int threshold) {

        return spellChecker.getSuggestions(word, threshold);
    }
}
