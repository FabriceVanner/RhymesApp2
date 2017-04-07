package rhymesapp;

import android.content.Context;
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
 * downloads a file to local app-space
 */
public class WebIO {
    public static boolean downloadFile(final String urlPath, String fileDirName, String fileName, DataBaseHelper dataBaseHelper)
    {
        //if (fileName=="") fileName =
        try
        {
            URL url = new URL(urlPath);
            URLConnection ucon = url.openConnection();
            final int length = ucon.getContentLength();
            ucon.setReadTimeout(5000);
            ucon.setConnectTimeout(10000);

            InputStream is = ucon.getInputStream();
            BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);

            File file = new File(context.getDir(fileDirName, Context.MODE_PRIVATE) + "/"+fileName);

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
                dataBaseHelper.rhymeService.broadcastCommandToBaseActivity("updateDownCopyProgress",String.valueOf(progress*progressFactor));
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

        return true;
    }

}
