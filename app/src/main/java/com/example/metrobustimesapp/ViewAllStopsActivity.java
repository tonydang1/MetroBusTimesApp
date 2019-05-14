package com.example.metrobustimesapp;

import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

public class ViewAllStopsActivity extends AppCompatActivity {

    GridView gridView;
    ArrayList<BusTimeGUI> list;
    BusTimeListAdapter adapter = null;
    String dbName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_all_stops);

        dbName = getString(R.string.DBName);
        gridView = findViewById(R.id.allStopGridView);
        list = new ArrayList<>();
        adapter = new BusTimeListAdapter(this, R.layout.layout_gridview,list);
        gridView.setAdapter(adapter);

        //grabbing data from sqlite
        String sql = "SELECT * FROM "+dbName;
        Cursor cursor = MainActivity.dbHandler.getData(sql);
        list.clear();
        while(cursor.moveToNext()){
            int id = cursor.getInt(0);
            HashMap<String, List<String>> hash = MainActivity.stringToHash(cursor.getString(1));
            
            list.add(new BusTimeGUI(id, hash, Integer.toString(id)));
        }
    }


}
