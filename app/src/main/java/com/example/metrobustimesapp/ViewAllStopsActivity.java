package com.example.metrobustimesapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class ViewAllStopsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_all_stops);
    }

    //Written by Anthony
    //Input: Called by pressing the viewAllStopsGetStopsButton button
    //Output:
    //Grabs the data from metro bus servers. Need Internet to work.
    public void getStopsFromWeb(View view) {

    }
}
