package rhymesapp;

import java.io.IOException;

/**
 * Created by Fabrice Vanner on 02.12.2016.
 */
public class StorageLocationException extends IOException{
    public StorageLocationException(String str,Throwable throwable){
        super(str,throwable);
    }
}
