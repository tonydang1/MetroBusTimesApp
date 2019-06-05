package com.example.metrobustimesapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    NetworkInfo netInfo;
    LocationManager locationManager;
    static DatabaseHandler dbHandler;
    ConnectivityManager connectMan;
    Button getLocationBtn;
    TextView locationText;
    TextView jsonTxt;
    TextView textView;
    TextView editStop;
    String htmlText;
    String dbName;
    String busIDString;
    int busID;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);
        dbName = getString(R.string.DBName);

        busID = 1616;
        busIDString = Integer.toString(busID);
        dbHandler = new DatabaseHandler(this, dbName, null,1);
        dbHandler.queryData("CREATE TABLE IF NOT EXISTS "+dbName+"(ID INTEGER, HASHTABLE TEXT)");

        double mch_lat = 36.996165;
        double mch_long =  -122.058873;
        double hagar_bus_lat = 36.996801;
        double hagar_bus_long = -122.055408;

        double d = Math.acos(Math.sin(mch_lat)*Math.sin(hagar_bus_lat)+Math.cos(mch_lat)*Math.cos(hagar_bus_lat)*Math.cos(mch_long - hagar_bus_long));

        double distance_km = 6371 * d;

        //Widget setup
        //getLocationBtn = findViewById(R.id.getLocationBtn);
        locationText = findViewById(R.id.locationText);
        editStop = findViewById(R.id.enterBusStop);
        textView = findViewById(R.id.textView);
        jsonTxt = findViewById(R.id.jsonText);

        //Internet stuff
        connectMan = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        netInfo = connectMan.getActiveNetworkInfo();

        String string_d = Double.toString(distance_km);
        textView.setText("Distance in km: "+ string_d);

        getLocationBtn = findViewById(R.id.button);
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},123);
        getLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GPSHandler g = new GPSHandler(getApplicationContext());
                Location l = g.getLocation();
                if(l != null){
                    double lat = l.getLatitude();
                    double lon = l.getLongitude();
                    Toast.makeText(getApplicationContext(), "LAT: "+ lat+ "\n LON: "+ lon, Toast.LENGTH_LONG).show();
                }
            }
        });


        // testing find_closest_bus
        double fresca_lat = 37.001028;
        double fresca_lng = -122.057713;
        find_closest_bus(fresca_lat, fresca_lng);

    }
    private void find_closest_bus(double current_lat, double current_lng){
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
            if(d <= 600){
                closeBusses.add(t);
                if(d < min_d){
                    min_d = d;
                    closest_bus = t;
                    Log.d("MIN", min_d + " " + t.ID);
                }
            }
        }

        for(Bus t : closeBusses){
            Log.d("close bus", t.ID + " " + t.name);
        }

        if(closest_bus != null){
            Log.d("closest bus--", closest_bus.ID + " " + closest_bus.name);
        }
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


    //Author: Anthony
    //Input: Called from pressing getAllStopsButton in activity_main
    //Output: Sends you to ShowAllStops (will change to search bar)
    public void getAllStops(View view) {
        Intent intent = new Intent(this, ViewAllStopsActivity.class);
        startActivity(intent);
    }

    public void getAllStops(){
        Intent intent = new Intent(this, ViewAllStopsActivity.class);
        startActivity(intent);
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
        Gson gson = new Gson();
        Type type = new TypeToken<HashMap<String,ArrayList<String>>>() {}.getType();
        HashMap<String, List<String>> hash = gson.fromJson(stringHash, type);
        return hash;
    }

    //Author: Anthony
    //Input: Called from getJsonButton
    //Output: Should transfer some kind of information to some database (WIP)
    //This is a test to see if you can get json files.
    //Right now it's hard coded to the 1616 bus stop ID. You can change it by changing the 15 that's
    //before wd to some other bus number like 16 or 20.
    protected void connectToMetro(View view){
        dbHandler.clean();
        netInfo = connectMan.getActiveNetworkInfo();

        String temp_busid = editStop.getText().toString();
        // TODO: check if busid is empty / valid

        if(temp_busid.matches("")){
            Toast.makeText(MainActivity.this, "MUST ENTER BUSID", Toast.LENGTH_LONG).show();
            return;
        }

        if(netInfo != null && netInfo.isConnected()){
            new OnlineMetroGetter().execute("https://www.scmtd.com/en/routes/schedule-by-stop/"+temp_busid+"/2019-05-17#tripDiv");
        } else {
            Toast.makeText(MainActivity.this, "NO INTERNET CONNECTION", Toast.LENGTH_LONG).show();
        }
    }

    //Author: Anthony
    //Object for connecting to metro website and grabbing json/xml file(hopefully).
    private class OnlineMetroGetter extends AsyncTask<String, String, String> {
        private ProgressDialog pd;

        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("Please wait");
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected String doInBackground(String... params) {

            HttpURLConnection connection = null;
            BufferedReader reader = null;
            String stopRow;

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
                return out;


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
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
            dbHandler.insertData(busID, result);
            htmlText = result;
            jsonTxt.setText(result);
            getAllStops();
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

        Log.d("Parse", output);

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
        Log.d("Bus 10", Bus_10.toString());
        Log.d("Bus 16", Bus_16.toString());
        Log.d("Bus 20", Bus_20.toString());
        Log.d("Bus 20D", Bus_20D.toString());
        return s;
    }
}
