package newDevelopments;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by Fabrice Vanner on 05.01.2017.
 */
public class HtmlParser {

    public static void main(String[]args) throws IOException {
        /*
        Document doc = Jsoup.connect("https://wordassociations.net/de/assoziationen-mit-dem-wort/Liebe").data("query", "Java").userAgent("Mozilla").cookie("auth", "token").timeout(3000).post();

       // Element wordscolumn = doc.select("div.wordscolumn").first();
        Element wordscolumn = doc.getElementsByClass("wordscolumn").first();
        Element nouns = wordscolumn.select("ul").first();
        Element adject = wordscolumn.select("ul").get(2);
        Element verbs = wordscolumn.select("ul").get(3);
        System.out.println(nouns.text());
        */

        //Host=tom.brondsted.dk
        //User-Agent=Mozilla/5.0 (Windows NT 10.0; WOW64; rv:50.0) Gecko/20100101 Firefox/50.0
        //Accept=text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
   //     Accept-Language=de-AT,en-US;q=0.7,en;q=0.3
//Accept-Encoding=gzip, deflate
//Cookie=PHPSESSID=5cdritf77b4u4dmq3iacra197em2uh4l
//DNT=1


        //Referer=http://tom.brondsted.dk/text2phoneme/
        //Accept=text/css,*/*;q=0.1




    //    Host=tom.brondsted.dk
     //   User-Agent=Mozilla/5.0 (Windows NT 10.0; WOW64; rv:50.0) Gecko/20100101 Firefox/50.0
      //  Accept=text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
//Accept-Language=de-AT,en-US;q=0.7,en;q=0.3
//Accept-Encoding=gzip, deflate
//Referer=http://tom.brondsted.dk/text2phoneme/
//Cookie=PHPSESSID=5cdritf77b4u4dmq3iacra197em2uh4l
//DNT=1
//Connection=keep-alive
//Upgrade-Insecure-Requests=1
//Content-Type=application/x-www-form-urlencoded
//Content-Length=38
//POSTDATA=txt=Hello&language=danish&alphabet=IPA

        /*


        HttpURLConnection myURLConnection = (HttpURLConnection)myURL.openConnection();
String userCredentials = "username:password";
String basicAuth = "Basic " + new String(new Base64().encode(userCredentials.getBytes()));
myURLConnection.setRequestProperty ("Authorization", basicAuth);
myURLConnection.setRequestMethod("POST");
myURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
myURLConnection.setRequestProperty("Content-Length", "" + postData.getBytes().length);
myURLConnection.setRequestProperty("Content-Language", "en-US");
myURLConnection.setUseCaches(false);
myURLConnection.setDoInput(true);
myURLConnection.setDoOutput(true);



         */


        String responseString="";
        try {
            // open a connection to the site
            URL url = new URL("http://tom.brondsted.dk/text2phoneme/transcribeit.php");
            URLConnection con = url.openConnection();
            // activate the output
            con.setDoOutput(true);
            PrintStream ps = new PrintStream(con.getOutputStream());
            // send your parameters to your site
            //txt=Ikea&language=danish&alphabet=IPA
            String word = "Maus";
            ps.print("txt="+word);
            ps.print("&language=german");
            ps.print("&alphabet=IPA");

            // we have to get the input stream in order to actually send the request
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            //print result
            responseString = response.toString();


            // close the print stream
            ps.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Document doc = Jsoup.parse(responseString);
        Element ipaElement = doc.select("div#maintext").first().select("p.indent").get(1);
        System.out.println(ipaElement.ownText());

    }

}
