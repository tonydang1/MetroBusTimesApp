package com.example.metrobustimesapp;

public class BusTimeGUI {
    private int ID;
    private String nextBus, busNumber;

    //nextBus is the times of the next bus i.e. "15, 20 minutes"
    //busNumber is the number of the bus i.e. "16"
    public BusTimeGUI(int id, String nextBus, String busNumber) {
        ID = id;
        this.nextBus = nextBus;
        this.busNumber = busNumber;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getNextBus() {
        return nextBus;
    }

    public void setNextBus(String nextBus) {
        this.nextBus = nextBus;
    }

    public String getBusNumber() {
        return busNumber;
    }

    public void setBusNumber(String busNumber) {
        this.busNumber = busNumber;
    }
}
