package rhymesapp;

import android.webkit.CookieManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Fabrice Vanner on 05.01.2017.
 * <p>
 * intended to be used for finding assossiations
 */
public class HtmlParser {
    /**
     * collects the HTML content of the webpage received by opened HttpURLConnection to a url
     * @param connection
     * @return
     */
    public String getHTMLStringToScrape(HttpURLConnection connection) {
        StringBuilder sb=null;
        try {
            InputStream is = connection.getInputStream();
            BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);
            BufferedReader br = null;
            br = new BufferedReader(new InputStreamReader(inStream));

             sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            inStream.close();

        } catch (
                Exception e)

        {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private HttpsURLConnection urlConnection;
    private CookieManager cookieManager;

    public HttpsURLConnection getConnection(String url) throws MalformedURLException {
        URL request_url = new URL(url);
        try {
            //if (!isHttps()) {
            //    throw new ConnectException("you have to use SSL certifacated url!");
            //}
            urlConnection = (HttpsURLConnection) request_url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setReadTimeout(95 * 1000);
            urlConnection.setConnectTimeout(95 * 1000);
            urlConnection.setDoInput(true);
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("X-Environment", "android");

            /** Cookie Sets... */
            String cookie = cookieManager.getCookie(urlConnection.getURL().toString());
            cookieManager = CookieManager.getInstance();
            if (cookie != null)
                urlConnection.setRequestProperty("Cookie", cookie);

            List<String> cookieList = urlConnection.getHeaderFields().get("Set-Cookie");
            if (cookieList != null) {
                for (String cookieTemp : cookieList) {
                    cookieManager.setCookie(urlConnection.getURL().toString(), cookieTemp);
                }
            }
            /** Cookie Sets... */

            urlConnection.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    /** if it necessarry get url verfication */
                    //return HttpsURLConnection.getDefaultHostnameVerifier().verify("your_domain.com", session);
                    return true;
                }
            });
            urlConnection.setSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());


            urlConnection.connect();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return urlConnection;
    }

    /**
     * connects to URL and receives HTML String
     * to use on JAVA PC environmment
     * @return
     * @throws IOException
     */
    public List<String> parseWordAssociations() throws IOException {
        //Document doc = Jsoup.
        Document doc = Jsoup.connect("https://wordassociations.net/de/assoziationen-mit-dem-wort/Liebe").data("QUERYTYPEDEFAULT", "Java").userAgent("Mozilla").cookie("auth", "token").timeout(3000).post();
        return parseWordAssociations(doc);
    }

    /**
     * parses HTML String
     * to use on Android if Jsoup.connect can't be used because of missing SSL-Certificate
     * @param html
     * @return
     * @throws IOException
     */
    public List<String> parseWordAssociationsOnAndroid(String html) throws IOException {
        Document doc = Jsoup.parse(html);
        return parseWordAssociations(doc);
    }

    /**
     * parses WordAssociations, filters by Jsoup DOC received from Site https://wordassociations.net/de/assoziationen-mit-dem-wort/[WORD]
     * @param doc
     * @return StringList - might be Empty
     * @throws IOException
     */
    public static List<String> parseWordAssociations(Document doc) throws IOException {
        String textDelim = " ";
        List<String> allList = new ArrayList<String>();
        // Element wordscolumn = doc.select("div.wordscolumn").first();
        Elements elements = doc.getElementsByClass("wordscolumn");
        if(!elements.isEmpty()) {
            Element wordscolumn = elements.first();
            //Elements elements = wordscolumn.select("ul");


            Element nouns = wordscolumn.select("ul").first();
            if (nouns != null) {
                List<String> nounsList = new ArrayList<>(Arrays.asList(nouns.text().split(textDelim)));
                allList.addAll(nounsList);
            }

            Element adjects = null;
            try {
                adjects = wordscolumn.select("ul").get(2);
                if (adjects != null) {
                    List<String> adjList = new ArrayList<>((Arrays.asList(adjects.text().split(textDelim))));
                    allList.addAll(adjList);
                }
            } catch (IndexOutOfBoundsException e) {

            }

            Element verbs = null;
            try {
                verbs = wordscolumn.select("ul").get(3);
                if (verbs != null) {
                    List<String> verbsList = new ArrayList<>((Arrays.asList(verbs.text().split(textDelim))));
                    allList.addAll(verbsList);
                }
            } catch (IndexOutOfBoundsException e) {

            }


        }
        return allList;
    }


    public static void main(String[] args) throws IOException {

        //  parseWordAssociations();
    }

    /**
     * TODO:
     * http://tom.brondsted.dk/text2phoneme/ generates approximate Phonetic Version of German word by interpolating allready existing transcription
     * this method shall scrape them, when a word is missing in the phonetic library generated by the wiki
     */
    public static void textToPhoneme() {


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


        String responseString = "";
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
            ps.print("txt=" + word);
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
