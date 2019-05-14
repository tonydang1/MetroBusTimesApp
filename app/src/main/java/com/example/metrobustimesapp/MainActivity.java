package com.example.metrobustimesapp;

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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener {

    Button getLocationBtn;
    TextView locationText;
    LocationManager locationManager;
    NetworkInfo netInfo;
    ConnectivityManager connectMan;
    TextView jsonTxt;
    TextView textView;
    TextView editStop;
    String htmlText;
    DatabaseHandler dbHandler;
    String dbName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);
        dbName = getString(R.string.DBName);

        dbHandler = new DatabaseHandler(this, dbName, null,1);
        dbHandler.queryData("CREATE TABLE IF NOT EXISTS "+dbName+"(ID INTEGER, HASHTABLE TEXT)");

        double mch_lat = 36.996165;
        double mch_long =  -122.058873;
        double hagar_bus_lat = 36.996801;
        double hagar_bus_long = -122.055408;

        double lat1 = mch_lat;
        double lon1 = mch_long;
        double lat2 = hagar_bus_lat;
        double lon2 = hagar_bus_long;

        double d = Math.acos(Math.sin(lat1)*Math.sin(lat2)+Math.cos(lat1)*Math.cos(lat2)*Math.cos(lon1-lon2));

        double distance_km = 6371 * d;

        //Widget setup
        getLocationBtn = findViewById(R.id.getLocationBtn);
        locationText = findViewById(R.id.locationText);
        editStop = findViewById(R.id.enterBusStop);
        textView = findViewById(R.id.textView);
        jsonTxt = findViewById(R.id.jsonText);

        //Internet stuff
        connectMan = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        netInfo = connectMan.getActiveNetworkInfo();

        String string_d = Double.toString(distance_km);
        textView.setText("Distance in km: "+ string_d);

        display_stops();

        //get permission if not available
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
        }
        getLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getLocation();
            }
        });
    }

    //Display bus stops
    void display_stops(){

    }

    //Get location of user
    void getLocation() {
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 5, this);
        }
        catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    //When user moves do something
    @Override
    public void onLocationChanged(Location location) {

        locationText.setText("Latitude: " + location.getLatitude() + "\n Longitude: " + location.getLongitude());
        //locationText.setText("Latitude: " + mch_lat + "\n Longitude: " + mch_long);

        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            locationText.setText(locationText.getText() + "\n"+addresses.get(0).getAddressLine(0)+", "+
                    addresses.get(0).getAddressLine(1)+", "+addresses.get(0).getAddressLine(2));
        }catch(Exception e) {

        }

    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(MainActivity.this, "Please Enable GPS and Internet", Toast.LENGTH_SHORT).show();
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

    //Author: Anthony
    //Input: Called from getJsonButton
    //Output: Should transfer some kind of information to some database (WIP)
    //This is a test to see if you can get json files.
    //Right now it's hard coded to the 1616 bus stop ID. You can change it by changing the 15 that's
    //before wd to some other bus number like 16 or 20.
    protected void connectToMetro(View view){
        netInfo = connectMan.getActiveNetworkInfo();
        if(netInfo != null && netInfo.isConnected()){
            new OnlineMetroGetter().execute("https://www.scmtd.com/en/routes/schedule-by-stop/1616#tripDiv");
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
                return stopRow;


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
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (pd.isShowing()){
                pd.dismiss();
            }
            htmlText = result;
            jsonTxt.setText(result);
        }
    }
}
