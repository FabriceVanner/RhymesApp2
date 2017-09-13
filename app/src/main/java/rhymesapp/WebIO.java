package rhymesapp;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import static rhymesapp.RhymesService.context;

/**
 * Created by Fabrice Vanner on 06/04/2017.
 * downloads a file to local app-space --> intended to be ussed for downloading the dictionary from the web after app has been installed from play-store
 */
public class WebIO {
    public static String LOG_TAG = "WebIO";
    private static RhymesService rhymesService;

    public static boolean downloadFile(final String urlPath, String fileDirName, String fileName, RhymesService rhymesService)
    {
        WebIO.rhymesService = rhymesService;
        //if (fileName=="") fileName =
        new AsyncHTTPDownloadTask().execute(urlPath,fileDirName,fileName);

        return true;
    }

    //PARALLEL THREADS:
    //new AsyncRhymesQuery().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,new AsynchRhymesQueryParamWrapper(i+1,words.get(i)));


    private static class AsyncHTTPDownloadTask extends AsyncTask<String, String, Boolean> {
        @Override
        protected Boolean doInBackground(String... strings) {
            try
            {
                URL url = new URL(strings[0]);
                URLConnection ucon = url.openConnection();
                final int length = ucon.getContentLength();
                ucon.setReadTimeout(5000);
                ucon.setConnectTimeout(10000);

                InputStream is = ucon.getInputStream();
                BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);

                File file = new File(context.getDir(strings[1], Context.MODE_PRIVATE) + "/"+strings[2]);

                if (file.exists())
                {
                    file.delete();
                }
                file.createNewFile();


                FileOutputStream outStream = new FileOutputStream(file);
                byte[] buff = new byte[5 * 1024];
                int progressFactor= 100/(length/(5*1024));
                int progress = 0;
                int len;
                while ((len = inStream.read(buff)) != -1)
                {

                    outStream.write(buff, 0, len);
                    progress++;
                    publishProgress(String.valueOf(progress*progressFactor));

                }

                outStream.flush();
                outStream.close();
                inStream.close();

            }
            catch (Exception e)
            {
                Toast.makeText(context, "Error Downloading:"+e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return false;
            }
//            Log.d(LOG_TAG, "AsyncHMLoaderTask doInBackground():  just loaded HM");
            return true;
        }



        @Override
        protected void onProgressUpdate(String... values) {
            rhymesService.broadcastCommandToBaseActivity("updateDownCopyProgress",values[0]);
        }
        @Override
        protected void onPostExecute(Boolean worked) {
            if (worked) {
                rhymesService.initDataProvider();
                Log.d(WebIO.LOG_TAG, "AsyncHMLoaderTask onPostExecute():  ...just finished downloading");

            }else{

            }
        }
    }



}
