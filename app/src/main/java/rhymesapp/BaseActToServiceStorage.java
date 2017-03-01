package rhymesapp;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

/**
 * Created by Fabrice Vanner on 01.03.2017.
 */
public class BaseActToServiceStorage {
    private Handler timerHandler = new Handler();
    public int autoRandomSpeedinMS = 4000;
    public static Context context;
    private Runnable timerRunnable = new Runnable() {


        @Override
        public void run() {
            findRandomWordPair();
            timerHandler.postDelayed(this, autoRandomSpeedinMS);
        }
    };

    private void findRandomWordPair() {
        //  new AsyncRandomRhymesQuery().execute(0);
        Toast.makeText(context, "findRandomWordPair", Toast.LENGTH_SHORT).show();
    }

    public void startTimerHandler() {
        timerHandler.postDelayed(timerRunnable, 4000);
    }

    public void stopTimerHandler() {
        timerHandler.removeCallbacks(timerRunnable);
    }
    /**
     * Task used to run the rhyme-queries in background
     */
    /*
    private class AsyncRhymesQueryTask extends AsyncTask<AsynchRhymesQueryParamWrapper, Void, AsynchRhymesQueryParamWrapper> {
        @Override
        protected AsynchRhymesQueryParamWrapper doInBackground(AsynchRhymesQueryParamWrapper... query) {
        /   Log.d(LOG_TAG, "AsyncRhymesQueryTask doInBackground(): just run rhymes query with Nr.: " + query[0].nr + " and word " + query[0].word);
            query[0].rhymes = runRhymesQuery(query[0].word);
            return query[0];
        }


        @Override
        protected void onPostExecute(AsynchRhymesQueryParamWrapper result) {
            rhymeResults.add(result.rhymes);
            if (result.nr == 1) prepareAndSendColoredViewText(outputTextView, result.rhymes);
            Log.d(LOG_TAG, "AsyncRhymesQueryTask onPostExecute():  just added results of query " + result.nr + "( " + result.word + " ) to rhymeResults-Arraylist");
        }
    }
*/
/*
    private class AsyncRandomRhymesQuery extends AsyncTask<Integer, Void, rhymesapp.WordRhymesPair> {
        @Override
        protected rhymesapp.WordRhymesPair doInBackground(Integer... query) {
            // Log.d(LOG_TAG, "AsyncRhymesQueryTask doInBackground(): just run rhymes query with Nr.: "+query[0].nr + " and word "+query[0].word  );
            return Constatics.dataBaseHelper.getRandWordRhymesPair();

        }


        @Override
        protected void onPostExecute(rhymesapp.WordRhymesPair wordRhymesPair) {
            super.onPostExecute(wordRhymesPair);
            //    randomRhymesQuery = wordRhymesPair;
            showRandomWordRhymesPair(wordRhymesPair);
            if (enableTextToSpeech) {
                if (textToSpeechEngine == null) {
                    loadTextToSpeech();
                    onInit(TextToSpeech.SUCCESS);
                }
                speak(wordRhymesPair.getWord());
            }
        }

    }

*/
}
