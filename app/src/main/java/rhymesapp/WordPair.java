package rhymesapp;

/**
 * Created by Fabrice Vanner on 22.12.2016.
 */
public class WordPair {
    String word;
    String resultWords;

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getResultWords() {
        return resultWords;
    }

    public void setResultWords(String resultWords) {
        this.resultWords = resultWords;
    }

    public WordPair(String word, String resultWords) {
        this.word = word;
        this.resultWords = resultWords;
    }
}
