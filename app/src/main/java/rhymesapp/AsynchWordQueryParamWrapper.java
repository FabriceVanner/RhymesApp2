package rhymesapp;

/**
 * Created by Fabrice Vanner on 03.10.2016.
 */
public class AsynchWordQueryParamWrapper {
    int nr;
    String word;
    String queryResult;
    RhymesService.QueryType queryType;

    public AsynchWordQueryParamWrapper(int nr, String word, RhymesService.QueryType queryType){
        this.nr = nr;
        this.word = word;
        this.queryType = queryType;
    }
}
