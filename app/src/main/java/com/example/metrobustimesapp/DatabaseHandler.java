package com.example.metrobustimesapp;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import android.widget.Toast;

public class DatabaseHandler extends SQLiteOpenHelper {
    Context ctx;
    static int  VERSION;
    String DB_NAME;

    //Database stores BUSID (INT), HASHTABLE(TEXT)
    public DatabaseHandler(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        ctx = context;
        VERSION = version;
        DB_NAME = name;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + DB_NAME+ "(BUSID INTEGER, HASHTABLE TEXT);");
        Toast.makeText(ctx, "TABLE IS CREATED", Toast.LENGTH_LONG).show();
    }

    //Input: SQL code
    //Output: Output should be logged into run
    public void queryData(String sql){
        SQLiteDatabase database = getWritableDatabase();
        database.execSQL(sql);
    }

    //Input: Bus stop ID, JSON string with hashtable
    //Output: adds it to the database
    public void insertData(int busID, String hashtable){
        SQLiteDatabase database = getWritableDatabase();
        String sql = "INSERT INTO " + DB_NAME + " VALUES (" + busID + ", ?)";
        SQLiteStatement statement = database.compileStatement(sql);
        statement.clearBindings();
        statement.bindString(1, hashtable);
        statement.executeInsert();
    }

    //Input: SQL code
    //Output: Returns the result of your query as a cursor
    public Cursor getData(String sql){
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.rawQuery(sql, null);
        return cursor;

    }

    //Input: ID of bus stop
    //Output: Deletes the data of that bus stop
    public int delete(String ID) {
        try {
            SQLiteDatabase db = getReadableDatabase();
            db.execSQL("DELETE FROM " + DB_NAME + " WHERE ID = \"" + ID + "\";");
            return 1;
        } catch (Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    //Input: None
    //Output: Clean database
    //Discards everything the database holds so its fresh
    public void clean(){
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("delete from " + DB_NAME);
    }

    //Got from class
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //DROP TABLE IF EXISTS NAME_TABLE;
        if (VERSION == oldVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + DB_NAME + ";");
            VERSION = newVersion;
            onCreate(db);
            Toast.makeText(ctx, "TABLE IS UPGRADED", Toast.LENGTH_LONG).show();
        }
    }
}
