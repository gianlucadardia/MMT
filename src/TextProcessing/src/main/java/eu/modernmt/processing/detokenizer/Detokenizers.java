package eu.modernmt.processing.detokenizer;

import eu.modernmt.processing.detokenizer.moses.MosesDetokenizer;

import java.util.Locale;

/**
 * Created by davide on 27/01/16.
 */
public class Detokenizers {

    public static Detokenizer forLanguage(Locale language) {
        String languageTag = language.toLanguageTag().substring(0, 2);
        return new MosesDetokenizer(languageTag);
    }

}