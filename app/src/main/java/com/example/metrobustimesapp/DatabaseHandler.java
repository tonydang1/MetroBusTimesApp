package com.example.metrobustimesapp;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.widget.Toast;

public class DatabaseHandler extends SQLiteOpenHelper {
    Context ctx;
    static int  VERSION;
    String DB_NAME;


    public DatabaseHandler(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        ctx = context;
        VERSION = version;
        DB_NAME = name;
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE ImageDB(Id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, image BLOG);");
        Toast.makeText(ctx, "TABLE IS CREATED", Toast.LENGTH_LONG).show();
    }

    //From youtube
    public void queryData(String sql){
        SQLiteDatabase database = getWritableDatabase();
        database.execSQL(sql);
    }

    public void insertData(String title, byte[] image){
        SQLiteDatabase database = getWritableDatabase();
        String sql = "INSERT INTO ImageDB VALUES (NULL, ?, ?)";

        SQLiteStatement statement = database.compileStatement(sql);
        statement.clearBindings();

        statement.bindString(1, title);
        statement.bindBlob(2, image);

        statement.executeInsert();
    }

    public Cursor getData(String sql){
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.rawQuery(sql, null);
        return cursor;

    }

    public int delete(String ID, String name) {
        try {
            SQLiteDatabase db = getReadableDatabase();
            db.execSQL("DELETE FROM ImageDB WHERE Id = \"" + ID + "\" OR name = \"" + name + "\";");
            return 1;
        } catch (Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    public void clean(){
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("delete from ImageDB");
        db.delete("SQLITE_SEQUENCE", "NAME = ?", new String[]{"ImageDB"});
    }

    //Got from class
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //DROP TABLE IF EXISTS NAME_TABLE;
        if (VERSION == oldVersion) {
            db.execSQL("DROP TABLE IF EXISTS ImageDB;");
            VERSION = newVersion;
            onCreate(db);
            Toast.makeText(ctx, "TABLE IS UPGRADED", Toast.LENGTH_LONG).show();
        }
    }
}
