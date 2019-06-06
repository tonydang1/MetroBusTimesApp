package com.example.metrobustimesapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import static android.support.v4.app.ActivityCompat.*;
import static android.support.v4.app.ActivityCompat.requestPermissions;

public class GPSHandler implements LocationListener {
    Context context;

    public GPSHandler(Context c){
        context = c;
    }

    public Location getLocation(){
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            Log.d("GPSHandler.getLocation", "GPS Permission not granted");
            Toast.makeText(context, "GPS Permission not granted", Toast.LENGTH_SHORT).show();
            String[] perm = {Manifest.permission.ACCESS_FINE_LOCATION};
            getLocation();
        }
        LocationManager lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(isGPSEnabled){
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 6000, 10, this);
            Location l = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            return l;
        }else{
            Log.d("GPSHandler.getLocation","Please enable GPS");
            Toast.makeText(context, "Please enable GPS", Toast.LENGTH_LONG).show();
        }
        return null;
    }


    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}