package com.example.metrobustimesapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.net.NetworkInfo;
import android.net.ConnectivityManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    int busID; //for debugging
    int index = 0;

    NetworkInfo netInfo;
    static DatabaseHandler dbHandler;
    Spinner stopSpinner;
    ArrayAdapter<String> spinnerAdapter;
    ConnectivityManager connectMan;
    GridView inRangeGrid;
    BusTimeListAdapter busTimeAdapter;
    String dbName;
    static String selectedBusStop; //bus stop selected from down down
    private String[] stopsWithinRange;
    ArrayList<BusTimeGUI> gridList;
    ArrayList<Bus> busList;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        init();
        connect();
        update();
    }

    //initiate stuff
    private void init(){
        dbName = getString(R.string.DBName);
        dbHandler = new DatabaseHandler(this, dbName, null,1);
        dbHandler.queryData("CREATE TABLE IF NOT EXISTS "+dbName+"(ID INTEGER, HASHTABLE TEXT)");

        //Widget setup
        stopSpinner = findViewById(R.id.nearbyStopsDropDown);
        inRangeGrid = findViewById(R.id.inRangeStopGrid);

        gridList = new ArrayList<>();
        busTimeAdapter = new BusTimeListAdapter(this, R.layout.layout_gridview, gridList);
        inRangeGrid.setAdapter(busTimeAdapter);

        busList = new ArrayList<>();
    }

    /*
    Input: Called from MainActivity.onCreate
    Output: Connects to the internet
    */
    private void connect(){
        //Internet stuff
        connectMan = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        netInfo = connectMan.getActiveNetworkInfo();
    }

    //Updates bus stops and spinner
    private void update(){
        gpsUpdate(); //Grabs the stops
        spinnerUpdate(); //Put the stops into spinner
    }

    /*
    Input: Called from MainActivity.onCreate
    Output: Sets up the adapter for scroll down
     */
    private void spinnerUpdate(){
        class SpinnerActivity extends Activity implements AdapterView.OnItemSelectedListener {

            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                // An item was selected. You can retrieve the selected item using
                //parent.getItemAtPosition(pos)
                selectedBusStop = parent.getItemAtPosition(pos).toString();
                gridUpdate();
            }

            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        }
        SpinnerActivity spinnerListener = new SpinnerActivity();
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, stopsWithinRange);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stopSpinner.setAdapter(spinnerAdapter);
        stopSpinner.setOnItemSelectedListener(spinnerListener);
    }

    private void gpsUpdate(){
        //GPS stuff
        GPSHandler g = new GPSHandler(getApplicationContext());
        Location l = g.getLocation();
        if(l != null){
            double lat = l.getLatitude();
            double lon = l.getLongitude();
            find_closest_bus(lat, lon);

            //test
//            double fresca_lat = 37.001028;
//            double fresca_lng = -122.057713;
//            find_closest_bus(fresca_lat, fresca_lng);
        } else {
            Log.d("gpsUpdate()", "Something wrong with GPS");
        }
    }

    private void gridUpdate(){
        //grabbing data from sqlite
        try {
            if (selectedBusStop.equals(null)) {
                selectedBusStop = stopSpinner.getSelectedItem().toString();
            }
        } catch (Exception e) {
            selectedBusStop = stopSpinner.getSelectedItem().toString();
        }
        String stopIDString = findStopID(selectedBusStop);
        busID = Integer.parseInt(stopIDString);
        populateGrid();
    }

    private void populateGrid(){
        String stopIDString = Integer.toString(busID);
        String sql = "SELECT * FROM "+dbName+" WHERE BUSID ="+stopIDString; //grabs everything
        Cursor cursor = dbHandler.getData(sql);
        gridList.clear();
        while(cursor.moveToNext()){
            int stopID = cursor.getInt(0);
            Log.d("populateGrid",cursor.getString(0));
            HashMap<String, List<String>> hash = MainActivity.stringToHash(cursor.getString(1));

            for(String key: hash.keySet()){ //going through every bus number
                Log.d(key, ": "+hash.get(key));
                String busTimes = ViewAllStopsActivity.formBusTimeString(hash, key);
                if(busTimes != "N/A")
                    gridList.add(new BusTimeGUI(stopID, busTimes, key));
            }
            busTimeAdapter.notifyDataSetChanged();
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

    private void find_closest_bus(double current_lat, double current_lng){
        BufferedReader reader;
        String[] line_split;

        // Parse R.raw.businfo and store into busList
        busList.clear();
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

        // DEBUG, prints busList, should be populated from R.raw.businfo
        for(Bus t : busList){
            Log.d("BusInfo", t.ID + " " + t.lat + " " + t.lon);
        }

        // closeBusses will contain the busses within 700 meters
        // closest_bus will have the closest id bus stop
        ArrayList<Bus> closeBusses = new ArrayList<>();

        Bus closest_bus = null;
        double min_d = 2000;
        for(Bus t : busList){
            double d = meterDistanceBetweenPoints(current_lat, current_lng, Double.parseDouble(t.lat), Double.parseDouble(t.lon));
            if(d <= 700){
                closeBusses.add(t);
                if(d < min_d){
                    min_d = d;
                    closest_bus = t;
                    Log.d("MIN", min_d + " " + t.ID);
                }
            }
        }

        if(closest_bus != null){
            Log.d("closest bus--", closest_bus.ID + " " + closest_bus.name);
            stopsWithinRange = new String[closeBusses.size()];
            // populate first index of spinner with closest
            stopsWithinRange[0] = closest_bus.name;
            // then populate with remaining stops within range
            int i = 1;
            for(Bus t : closeBusses){
                if(t.ID != closest_bus.ID) {
                    stopsWithinRange[i] = t.name;
                    i++;
                }
            }
        }//if
    }

    private double meterDistanceBetweenPoints(double lat_a, double lng_a, double lat_b, double lng_b) {
        Location selected_location=new Location("locationA");
        selected_location.setLatitude(lat_a);
        selected_location.setLongitude(lng_a);
        Location near_locations=new Location("locationB");
        near_locations.setLatitude(lat_b);
        near_locations.setLongitude(lng_b);

        return selected_location.distanceTo(near_locations);
    }

    // location lat lng
    // sne          36.999212, -122.060613
    // science hill 37.000069, -122.062129
    // tosca        36.980813, -122.060631
    // mch          36.996165, -122.058873
    // hagar_bus    36.996801, -122.055408
    // hagar_bus2   36.997611, -122.055053


    //Get all stops
    public void getAllStops(){
        for(int i=1000; i<3000; i++) {
            busID = i;
            connectToMetro();
        }
    }

    //Author: Anthony
    //Input: Hashtable
    //Output: Returns a string version of the hashtable
    protected String hashToString(HashMap<String, List<String>> hash){
        Gson gson = new Gson();
        String inputString= gson.toJson(hash);
        return inputString;
    }

    //Author: Anthony
    //Input: Jsonified hashtable
    //Output: Decoded hashtable
    //Decodes the json and turns it back into a hashtable
    protected static HashMap<String, List<String>> stringToHash(String stringHash){
        Log.d("stringToHash", stringHash);
        Gson gson = new Gson();
        Type type = new TypeToken<HashMap<String,ArrayList<String>>>() {}.getType();
        HashMap<String, List<String>> hash = gson.fromJson(stringHash, type);
        return hash;
    }

    protected void onClickSearch(View view){
        //getAllStops();
        Intent intent = new Intent(this, ViewAllStopsActivity.class);
        intent.putExtra("busList", (Serializable) busList);
        startActivity(intent);
//
//        busID = 1617;
//        connectToMetro();]
    }

    public void refresh(View view) {
        update();
    }

    //Input: Called from getJsonButton
    //Output: Should transfer some kind of information to some database (WIP)
    //This is a test to see if you can get json files.
    //Right now it's hard coded to the 1616 bus stop ID. You can change it by changing the 15 that's
    //before wd to some other bus number like 16 or 20.
    protected void connectToMetro(){
        netInfo = connectMan.getActiveNetworkInfo();

        String temp_busid = "";
        // TODO: check if busid is empty / valid
        // TODO: store all bus activity.
        if (netInfo != null && netInfo.isConnected()) {
            //temp_busid = Integer.toString(i);
            temp_busid = String.format("%04d", busID);
            new OnlineMetroGetter().execute("https://www.scmtd.com/en/routes/schedule-by-stop/" + temp_busid + "/2019-06-06#tripDiv", temp_busid);
        } else {
            Toast.makeText(MainActivity.this, "NO INTERNET CONNECTION", Toast.LENGTH_LONG).show();
        }
    }
    //Author: Anthony
    //Object for connecting to metro website
    private class OnlineMetroGetter extends AsyncTask<String, String, String> {
        private ProgressDialog pd;

        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("Please wait");
            pd.setCancelable(false);
            //pd.show();
        }

        @Override
        protected String doInBackground(String... params) {

            HttpURLConnection connection = null;
            BufferedReader reader = null;
            String stopRow;
            String backgroundStopID = params[1];
            Log.d("doInBackGround","============="+backgroundStopID+"===============");

            try {
                //connecting
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                //parsing info setup
                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();
                String line = "";

                //reading line
                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");
                }
                Document doc = Jsoup.parse(buffer.toString());
                Elements element = doc.select("tbody");
                stopRow = element.text();

                String out = parseElement(stopRow);
                out = backgroundStopID+" "+out;
                Log.d("doInBackground", "out is " + out);
                return out;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch(Exception e) {
                e.printStackTrace();
            } finally{
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (pd.isShowing()){
                pd.dismiss();
            }
            try {
                String[] arr = result.split("\\s+"); //arr[0] is id, arr[1] is result
                dbHandler.insertData(Integer.parseInt(arr[0]), arr[1]);
            } catch (Exception e){
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Input: HTML from Element returned by Jsoup.parse(url).select("tbody")
    // Output: Bus times separated by \n
    private String parseElement(String input){
        String delims = "[ ]+";
        String[] tokens = input.split(delims);
        String output = "";
        for(int i = 0; i < tokens.length; i++){
            if(tokens[i].equals("Santa")) continue;
            if(tokens[i].equals("Cruz")) continue;
            if(tokens[i].equals("Metro")) continue;
            if(tokens[i].equals("CenterTrip")) continue;
            if(tokens[i].equals("stops")) continue;
            if(tokens[i].equals("below")) continue;
            if(tokens[i].equals("below:")) continue;
            if(tokens[i].equals("Approximate")) continue;
            if(tokens[i].equals("Trip")) continue;
            if(tokens[i].equals("stop")) continue;
            if(tokens[i].equals("Arrives")) continue;
            if(tokens[i].equals("Delaware")) continue;
            if(tokens[i].equals("&")) continue;
            if(tokens[i].equals("Seymour")) continue;
            if(tokens[i].equals("Mission")) continue;
            if(tokens[i].equals("Trescony")) continue;
            if(tokens[i].equals("LibertyTrip")) continue;
            if(tokens[i].equals("TresconyTrip")) continue;
            if(tokens[i].equals("MissionTrip")) continue;
            if(tokens[i].equals("Science")) continue;
            if(tokens[i].equals("Hill")) continue;
            if(tokens[i].equals("Departs")) continue;
            if(tokens[i].equals("Bay")) continue;

            if(tokens[i].equals("times")){
                output += "\n";
                continue;
            }

            output += tokens[i];

            output += " ";
            //if(tokens[i].equals("1616")) output += "\n";
            //if(tokens[i].endsWith("Trip")) output += "\n";

        }
        // Removes Trip from Destination Time
        output = output.replace("Trip", "");
        output = output.replace("(UCSC) D ", "");
        output = output.replace("(UCSC) C ", "");
        output = output.replace("(UCSC) F ", "");
        output = output.replace("Watsonville Transit ", "");
        output = output.replace("Capitola Mall Lane 1 ", "");
        output = output.replace("Country Club ", "");
        output = output.replace("Mountain Store ", "");

        //Log.d("Parse", output);

        String tempString = populateDB(output);

        return tempString;
    }

    //Input: Bus times separated by \n
    //Output: Array of destination times for each bus
    private String populateDB(String input){
        String[] list = input.split(System.getProperty("line.separator"));

        // if length = 4, busID = list[0],                      schedTime = list[1], stopID = list[2], destTime = list[3]
        // if length = 6, busID = list[0] + list[1] + list[2],  schedTime = list[3], stopID = list[4], destTime = list[5]
        // if length = 7, busID = list[0] + list[1] + list[2], MWF bool = list[3]  schedTime = list[4], stopID = list[5], destTime = list[6]

        HashMap<String, List<String>> busses = new HashMap<>();

        List<String> Bus_16 = new ArrayList<>();
        List<String> Bus_10 = new ArrayList<>();
        List<String> Bus_20 = new ArrayList<>();
        List<String> Bus_20D = new ArrayList<>();
        List<String> Bus_22 = new ArrayList<>();

        List<String> Bus_15 = new ArrayList<>();
        List<String> Bus_19 = new ArrayList<>();

        String destTime = "";

        String[] list_to_array;
        for(int i = 0; i < list.length; i++){
            list_to_array = list[i].split(" ");
            if(list_to_array.length == 4){
                destTime = list_to_array[3];
            }else if(list_to_array.length == 6){
                destTime = list_to_array[5];
            }else if(list_to_array.length == 7){
                destTime = list_to_array[6];
            }
            switch(list_to_array[0]){
                case "16":
                    Bus_16.add(destTime);
                    break;
                case "10":
                    Bus_10.add(destTime);
                    break;
                case "20":
                    Bus_20.add(destTime);
                    break;
                case "20D":
                    Bus_20D.add(destTime);
                    break;
                case "22":
                    Bus_22.add(destTime);
                    break;
                case "15":
                    Bus_15.add(destTime);
                    break;
                case "19":
                    Bus_19.add(destTime);
                    break;
                default:
            }
        }//for

        busses.put("16", Bus_16);
        busses.put("10", Bus_10);
        busses.put("20", Bus_20);
        busses.put("20D", Bus_20D);
        busses.put("22", Bus_22);
        busses.put("15", Bus_15);
        busses.put("19", Bus_19);

        String s = hashToString(busses);
//        Log.d("Bus 10", Bus_10.toString());
//        Log.d("Bus 16", Bus_16.toString());
//        Log.d("Bus 20", Bus_20.toString());
//        Log.d("Bus 20D", Bus_20D.toString());
        return s;
    }
}
