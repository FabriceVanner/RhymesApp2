package rhymesapp;

/**
 * Created by Fabrice Vanner on 22.12.2016.
 */
public class WordRhymesPair {
    String word;
    String rhymes;

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getRhymes() {
        return rhymes;
    }

    public void setRhymes(String rhymes) {
        this.rhymes = rhymes;
    }

    public WordRhymesPair(String word, String rhymes) {
        this.word = word;
        this.rhymes = rhymes;
    }
}
