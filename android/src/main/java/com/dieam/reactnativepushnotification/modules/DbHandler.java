package com.dieam.reactnativepushnotification.modules;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.HashMap;


public class DbHandler extends SQLiteOpenHelper {
    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "inoutdb";
    private static final String TABLE_InOut = "inouttable";
    private static final String KEY_ID = "id";
    private static final String KEY_TIME = "time";
    private static final String KEY_DATE = "date";
    private static final String KEY_MAY_CHAM = "type";
    private static final String KEY_STATUS = "status";
    public DbHandler(Context context){
        super(context,DB_NAME, null, DB_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db){
        String CREATE_TABLE = "CREATE TABLE " + TABLE_InOut + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TIME + " TEXT,"
                + KEY_DATE + " TEXT,"
                + KEY_MAY_CHAM + " TEXT,"
                + KEY_STATUS + " TEXT" + ")";
        db.execSQL(CREATE_TABLE);

        CREATE_TABLE = "CREATE TABLE LastCheck (id INTEGER PRIMARY KEY AUTOINCREMENT, status TEXT, wifi TEXT)";

        db.execSQL(CREATE_TABLE);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        // Drop older table if exist
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_InOut);
        db.execSQL("DROP TABLE IF EXISTS LastCheck");
        // Create tables again
        onCreate(db);
    }

    void insertINOUT(String time, String status, String date, String type){
        //Get the Data Repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();
        //Create a new map of values, where column names are the keys
        ContentValues cValues = new ContentValues();
        cValues.put(KEY_TIME, time);
        cValues.put(KEY_STATUS, status);
        cValues.put(KEY_DATE, date);
        cValues.put(KEY_MAY_CHAM, type);
        long newRowId = db.insert(TABLE_InOut,null, cValues);
        db.close();
    }

    void insertLastCheck(String status, String wifi){
        //Get the Data Repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();
        //Create a new map of values, where column names are the keys
        ContentValues cValues = new ContentValues();
        cValues.put("status", status);
        cValues.put("wifi", wifi);
        long newRowId = db.insert("LastCheck",null, cValues);
        db.close();
    }

    public ArrayList<HashMap<String, String>> GetINOUT(){
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> inoutList = new ArrayList<>();
        String query = "SELECT * FROM "+ TABLE_InOut;
        Cursor cursor = db.rawQuery(query,null);
        while (cursor.moveToNext()){
            HashMap<String,String> inout = new HashMap<>();
            inout.put("time",cursor.getString(cursor.getColumnIndex(KEY_TIME)));
            inout.put("status",cursor.getString(cursor.getColumnIndex(KEY_STATUS)));
            inout.put("type",cursor.getString(cursor.getColumnIndex(KEY_MAY_CHAM)));
            inout.put("date",cursor.getString(cursor.getColumnIndex(KEY_DATE)));
            inout.put("id", cursor.getString(cursor.getColumnIndex(KEY_ID)));
            inoutList.add(inout);
        }
        return  inoutList;
    }

    public HashMap<String, String> getLastCheck(){
        SQLiteDatabase db = this.getWritableDatabase();
        HashMap<String, String> inout = new HashMap<>();
        String query = "SELECT * FROM LastCheck ORDER BY id DESC LIMIT 1";
        Cursor cursor = db.rawQuery(query,null);

        while (cursor.moveToNext()){
            inout.put("status",cursor.getString(cursor.getColumnIndex("status")));
            inout.put("wifi",cursor.getString(cursor.getColumnIndex("wifi")));
            inout.put("id",cursor.getString(cursor.getColumnIndex("id")));
        }

        return inout;
    }

    public void updateLastCheck(int id, String status, String wifi){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cVals = new ContentValues();
        cVals.put("status", status);
        cVals.put("wifi", wifi);
        db.update("LastCheck", cVals, KEY_ID+" = ?",new String[]{String.valueOf(id)});
    }

    public void delete(int id){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_InOut, KEY_ID+" = ?",new String[]{String.valueOf(id)});
        db.close();
    }
}
