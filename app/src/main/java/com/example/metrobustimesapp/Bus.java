package com.example.metrobustimesapp;

public class Bus implements java.io.Serializable {
    String ID;
    String name;
    String lat;
    String lon;
    Bus(String ID, String name, String lat, String lon){
        this.ID = ID;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
    }
}
