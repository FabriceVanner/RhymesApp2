package rhymesapp;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Fabrice Vanner on 05.10.2016.
 */
public class GuiUtils {
    private static GuiUtils guiUtilsSingleton;
    Context context;
    private String LOG_TAG = "RA - GuiUtils";
    static int colorArr[] = new int[]{Color.rgb(90, 144, 210), Color.GREEN, Color.RED, Color.WHITE, Color.YELLOW, Color.CYAN, Color.MAGENTA};
    RhymesBaseActivity rhymesBaseActivity;

    public static GuiUtils getInstance(Context context) {
        if (guiUtilsSingleton == null) {
            guiUtilsSingleton = new GuiUtils(context.getApplicationContext());
        }
        return guiUtilsSingleton;
    }

    public static GuiUtils getInstance(RhymesBaseActivity rhymesBaseActivity) {

        if (guiUtilsSingleton == null) {
            guiUtilsSingleton = new GuiUtils(rhymesBaseActivity);
        }
        return guiUtilsSingleton;
    }


    public void setRhymesBaseActivity(RhymesBaseActivity rhymesBaseActivity) {
        this.rhymesBaseActivity = rhymesBaseActivity;
    }

    private GuiUtils(RhymesBaseActivity rhymesBaseActivity) {
        this.rhymesBaseActivity = rhymesBaseActivity;
        this.context = rhymesBaseActivity.getApplicationContext();
    }

    private GuiUtils(Context context) {
        this.context = context;
    }

    /**
     * @param text
     * @param delimiter used to use a new color
     * @return
     */
    public Spannable colorizeText(String text, String delimiter) {
        Random random = new Random();
        int randmColorIndex = random.nextInt(colorArr.length);
        int oldRandomColorIndex = -1;
        Spannable spannable = new SpannableString(text);

        boolean breakOut = false;
        int index2 = text.indexOf(delimiter);
        int index1 = 0;
        if (index2 > 0) {
            while (true) {
                //  prevent choosing two times the same coleor:
                while(oldRandomColorIndex==randmColorIndex)randmColorIndex = random.nextInt(colorArr.length);
                spannable.setSpan(new ForegroundColorSpan(colorArr[randmColorIndex]), index1, index2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                oldRandomColorIndex = randmColorIndex;
                if (breakOut) break;
                index1 = index2 + delimiter.length() - 1;// text.indexOf("\n");
                index2 = text.indexOf(delimiter, index1+1);
                if (index2 == -1) {
                    if (text.length() > 1) {
                        index2 = text.length() - 1;
                        breakOut = true;
                    } else {
                        break;
                    }
                }
            }
        }

        return spannable;

    }

    /**
     * falls die voice textToSpeechEngine mehrere wörter als einen zusammenhängender ausdruck erkennt, nur den letzten nehmen
     * - ist das sinnvoll? manchmal gibt es im wiktionary auch zusammenhängende ausdrücke mit leerzeichen
     *
     * @param matches
     * @return
     */
    public ArrayList<String> prepareSpeechMatches(ArrayList<String> matches) {
        for (int i = 0; i < matches.size(); i++) {
            String match = matches.get(i);
            //
            if (match.contains(" ")) {
                String[] splitMatch = match.split(" ");
                String lastPart = splitMatch[splitMatch.length - 1];
                matches.set(i, lastPart);
            }
        }
        return matches;
    }

    public String concatStringListItems(ArrayList<String> matches, String delimiter) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < matches.size() - 1; i++) {
            String match = matches.get(i);
            str.append(match + delimiter);
        }
        str.append(matches.get(matches.size() - 1));
        return str.toString();
    }


    /**
     * TODO: still buggy of by one etc.
     *
     * @param textView
     * @param wordsStr rhymes string out of local db
     */
    public void setClickableWordsInTextView(TextView textView, String wordsStr, ArrayList<Integer> indexes) {
        SpannableString ss = new SpannableString(wordsStr);

        int nrOfWords = indexes.size();


        for (int i = 0; i <= nrOfWords; i++) {
            final int j = i;
            //final String word = wordsStr.substring();
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    String mess = "Clicked " + " ?? " + "View: " + textView.getId();
                    // Toast.makeText(RhymesBaseActivity.this, mess, Toast.LENGTH_SHORT).show();
                    Log.v(LOG_TAG, mess);
                    if (!(rhymesBaseActivity.rhymeResults.size() >= j)) {
                        mess = "setClickableWordsInTextView: Somehow the rhymeResult Array/Vector does not contain an entry with index of j = " + j;
                        Toast.makeText(rhymesBaseActivity, mess, Toast.LENGTH_SHORT).show();
                        Log.e(LOG_TAG, mess);
                        return;
                    }
                    rhymesBaseActivity.prepareAndSendViewText(rhymesBaseActivity.getOutputTextView(), rhymesBaseActivity.rhymeResults.get(j));
                }
            };
            // test comment: make internal method
            int startIndex;
            int endIndex;
            int delimiterLength = 2;
            if (i == 0) {
                startIndex = 0;
            } else {
                startIndex = indexes.get(i - 1) + delimiterLength;
            }

            if (i == indexes.size()) {
                endIndex = wordsStr.length();
            } else {
                endIndex = indexes.get(i);
            }
            try {
                ss.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); //ava.lang.IndexOutOfBoundsException: setSpan (-1 ... 8) ends beyond length 5
                ss.setSpan(new ForegroundColorSpan(Color.WHITE), startIndex, endIndex, 0); // 0 ricchtig?
            } catch (IndexOutOfBoundsException ex) {
                Log.e(LOG_TAG, "setClickableWordsInTextView: word =  < ?? > not found in wordsStr =  <" + wordsStr + ">  its startIndex(=indexOf(word)) is  =  " + startIndex + "( endIndex = " + endIndex + " )");
                throw ex;
            }
            //}
        }

        textView.setText(ss, TextView.BufferType.SPANNABLE);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public String prepareGroupedRhymesStr(String groupedRhymes) {
        String[] strArr = groupedRhymes.split("\n");
        for (String str : strArr) {
            ArrayList<Integer> commaIndexes = getDelimiterIndexes(str, ", ");

        }

        return groupedRhymes;
    }


    public String foldGroupedRhymes(String str) {

        return str;

    }

    public ArrayList<Integer> getDelimiterIndexes(String str, String delimiter) {
        ArrayList<Integer> indexes = new ArrayList<>();
        int index = str.indexOf(delimiter);
        while (index >= 0) {
            indexes.add(index);
            index = str.indexOf(delimiter, index + 1);
        }
        return indexes;
    }

}
