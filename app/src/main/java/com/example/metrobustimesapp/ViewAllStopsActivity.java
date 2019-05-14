package com.example.metrobustimesapp;

import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import static java.util.Calendar.HOUR_OF_DAY;

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
            for(String key: hash.keySet()){
                String busTimes = formBusTimeString(hash, key);
                list.add(new BusTimeGUI(id, busTimes, Integer.toString(id)));
            }
        }
    }

    //Input: HashMap, key
    //Output: String in the form of "time, next_time minutes" where time and next time are the next bus times
    //Called from ViewAllStopActivity.onCreate
    //Takes in the hash map and grabs the next two closest time.
    protected static String formBusTimeString(HashMap<String, List<String>> hash, String key){

        GregorianCalendar gcal = new GregorianCalendar();
        Calendar cal = GregorianCalendar.getInstance();
        int currHour = gcal.get(cal.HOUR_OF_DAY);
        int currMin = gcal.get(cal.MINUTE);
        
    }


}
