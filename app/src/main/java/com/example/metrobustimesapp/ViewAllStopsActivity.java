package com.example.metrobustimesapp;

import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import static java.util.Calendar.HOUR_OF_DAY;

public class ViewAllStopsActivity extends AppCompatActivity {

    GridView gridView;
    BusTimeListAdapter gridAdapter;
    ArrayList<BusTimeGUI> list;
    BusTimeListAdapter adapter = null;
    String dbName;
    EditText searchBar;
    ImageButton searchButton;
    String stopName;
    ArrayList<Bus> busList;

    private String[] stopList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_all_stops);

        Intent intent = getIntent();
        Bundle extra = intent.getExtras();
        busList = (ArrayList<Bus>) extra.getSerializable("busList");
        searchBar = findViewById(R.id.autoCompleteTextView);
        searchButton=findViewById(R.id.viewAllButton);
        gridView = findViewById(R.id.gridView);
        populate_stopList();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, stopList);
        AutoCompleteTextView textView = findViewById(R.id.autoCompleteTextView);
        textView.setAdapter(adapter);
    }

    protected void updateGrid(){
        dbName = getString(R.string.DBName);
        //gridView = findViewById(R.id.allStopGridView);
        list = new ArrayList<>();
        gridAdapter = new BusTimeListAdapter(this, R.layout.layout_gridview,list);
        gridView.setAdapter(gridAdapter);
        populateGrid();
    }

    //When user presses search button, take what they have on edit text and
    //put it on the gridview
    protected void beginSearch(View view){
        stopName = searchBar.getText().toString();
        updateGrid();
    }

    protected void populateGrid(){
        //grabbing data from sqlite
        String stopIDString = findStopID(stopName);
        String sql = "SELECT * FROM "+dbName+" WHERE BUSID ="+stopIDString; //grabs everything
        Cursor cursor = MainActivity.dbHandler.getData(sql);
        list.clear();
        while(cursor.moveToNext()){
            int stopID = cursor.getInt(0);
            HashMap<String, List<String>> hash = MainActivity.stringToHash(cursor.getString(1));

            for(String key: hash.keySet()){ //going through every bus number
                Log.d(key, ": "+hash.get(key));
                String busTimes = formBusTimeString(hash, key);
                if(!busTimes.equals("N/A"))
                    list.add(new BusTimeGUI(stopID, busTimes, key));
            }
            gridAdapter.notifyDataSetChanged();
        }
    }

    private String findStopID(String stopName){
        for(Bus bus: busList){
            if (stopName.equals(bus.name)) {
                return bus.ID;
            }
        }
        return null;
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
        System.out.println("Current time: "+currHour+":"+currMin);
        String result="";

        //list setup
        List<String> busTimeList = hash.get(key);

        //loop through all bus times
        for(int index=0; index<busTimeList.size()-1; index++){
            //need to extract the hour and min
            busMin = getMinAndHour(busTimeList.get(index));

            System.out.println(busMin+"-"+currMin+"="+(busMin-currMin));

            if (busMin - currMin >= 0){ //time till next bus in minutes
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
        return "N/A";
    }

    //Input: String i.e. "11:45pm"
    //Output: Minute, multiplies hour by 60 and adds it into minutes i.e. 95 (1:35am)
    protected static int getMinAndHour(String timeString){

        String[] timeArr;
        String time;
        int hour, min;

        String amPM = timeString.substring(timeString.length()-2);
        time = timeString.replaceAll("[a-zA-Z]",""); //gets rid of AM/PM

        //splitting into hour and min
        timeArr = time.split(":");
        hour = Integer.parseInt(timeArr[0]);
        min = Integer.parseInt(timeArr[1]);

        //Need to add 12 to the hour
        if(amPM.matches("pm")){
            if(hour != 12)
                hour += 12;
        } else if(hour == 12) { //for 12am
            hour += 12;
        }
        System.out.print(hour+":"+min+amPM+": ");

        min += hour*60;
        return min;
    }

    public void searchClicked(View view) {
        AutoCompleteTextView editText = findViewById(R.id.autoCompleteTextView);
        if(Arrays.asList(stopList).contains(editText.getText().toString())){
            // return to main
            // still need to send to main editText.getText().toString()
            Log.d("searchClicked", "Selected " + editText.getText().toString());
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }else{
            Toast.makeText(ViewAllStopsActivity.this, "Must enter valid bus stop", Toast.LENGTH_LONG).show();
        }
    }

    private void populate_stopList(){
        BufferedReader reader;
        String[] line_split;

        // Parse R.raw.businfo and store into busList
        ArrayList<Bus> busList = new ArrayList<>();
        try{
            final InputStream file = getResources().openRawResource(R.raw.businfo);
            reader = new BufferedReader(new InputStreamReader(file));
            String line = reader.readLine();
            while(line != null){
                line_split = line.split("\\|");
                busList.add(new Bus(line_split[0], line_split[1], line_split[2], line_split[3]));
                line = reader.readLine();
            }
        } catch(IOException ioe){
            ioe.printStackTrace();
        }
        stopList = new String[busList.size()];
        int j = 0;
        for(Bus t : busList){
            stopList[j] = t.name;
            j++;
        }
    }
}
