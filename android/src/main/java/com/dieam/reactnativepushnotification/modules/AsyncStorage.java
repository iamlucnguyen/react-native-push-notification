package com.dieam.reactnativepushnotification.modules;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.facebook.react.modules.storage.ReactDatabaseSupplier;

public class AsyncStorage {
    public Context context;

    public AsyncStorage (Context context) {
        this.context = context;
    }

    public String getAsyncStorage(String key) {
        String result = "";
        SQLiteDatabase readableDatabase = null;
        readableDatabase = ReactDatabaseSupplier.getInstance(this.context.getApplicationContext()).getReadableDatabase();
        Cursor catalystLocalStorage = readableDatabase.query("catalystLocalStorage", null, null, null, null, null, null);
        try {

            if (catalystLocalStorage.moveToFirst()) {

                do {
                    //Ex: key: 01082019_0800
                    //value: [{executionDate: 2019-08-01T08:00}]
                    String key1 = catalystLocalStorage.getString(0);
                    String json = catalystLocalStorage.getString(1);



                    if(key.equals(key1)) {
                        result = json;

                        break;
                    }



                } while (catalystLocalStorage.moveToNext());

            }

        } finally {
            if (catalystLocalStorage != null) {
                catalystLocalStorage.close();
            }

            if (readableDatabase != null) {
                readableDatabase.close();
            }
        }

        return result;
    }
}
