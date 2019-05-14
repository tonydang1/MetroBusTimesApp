package com.example.metrobustimesapp;

import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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

            for(String key: hash.keySet()){ //going through every bus number
                String busTimes = formBusTimeString(hash, key);
                list.add(new BusTimeGUI(id, busTimes, Integer.toString(id)));
            }
            adapter.notifyDataSetChanged();
        }
    }

    //Input: HashMap, key(should be bus number)
    //Output: String in the form of "time, next_time minutes" where time and next time are the next bus times
    //i.e. 20, 30 min
    //Called from ViewAllStopActivity.onCreate
    //Takes in the hash map and grabs the next two closest time.
    protected static String formBusTimeString(HashMap<String, List<String>> hash, String key){

        //time setup
        GregorianCalendar gcal = new GregorianCalendar();
        Calendar cal = GregorianCalendar.getInstance();
        int currHour = gcal.get(cal.HOUR_OF_DAY); //24 hours
        int currMin = (currHour*60)+gcal.get(cal.MINUTE);
        int busMin;
        String result="";

        //list setup
        List<String> busTimeList = hash.get(key);

        //loop through all bus times
        for(int index=0; index<busTimeList.size()-2; index++){
            //need to extract the hour and min
            busMin = getMinAndHour(busTimeList.get(index));
            if (busMin - currMin >= 0){
                result = (busMin-currMin) + ", " + (getMinAndHour(busTimeList.get(index+1))-currMin)+" min";
                return result;
            } else if(index == busTimeList.size()-2){ //for the last bus, will show last bus time and first bus time of next day
                busMin = getMinAndHour(busTimeList.get(busTimeList.size()-1))-currMin;
                if (busMin - currMin >= 0) {
                    result = (busMin - currMin) + ", " + (24*60-busMin+busTimeList.get(0)) + " min";
                    return result;
                }
            }
        }
        return result;
    }

    //Input: String i.e. "11:45pm"
    //Output: Minute, multiplies hour by 60 and adds it into minutes i.e. 95 (1:35am)
    protected static int getMinAndHour(String timeString){

        String[] timeArr;
        String hourString, minString, time;
        int hour, min;

        String amPM = timeString.substring(timeString.length()-2);
        time = timeString.replaceAll("[a-zA-Z]",""); //gets rid of AM/PM

        //splitting into hour and min
        timeArr = time.split(":");
        hour = Integer.parseInt(timeArr[0]);
        min = Integer.parseInt(timeArr[1]);

        //Need to add 12 to the hour
        if(amPM.matches("pm")){
            hour += 12;
        }

        min += hour*60;
        return min;
    }


}
