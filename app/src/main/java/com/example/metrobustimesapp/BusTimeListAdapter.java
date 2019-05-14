package com.example.metrobustimesapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class BusTimeListAdapter extends BaseAdapter {

    private Context context;
    private int layout;
    private ArrayList<BusTimeGUI> timeList;

    public BusTimeListAdapter(Context context, int layout, ArrayList<BusTimeGUI> timeList) {
        this.context = context;
        this.layout = layout;
        this.timeList = timeList;
    }

    @Override
    public int getCount() {
        return timeList.size();
    }

    @Override
    public Object getItem(int position) {
        return timeList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private class ViewHolder{
        TextView busView, timeView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View row = convertView;
        ViewHolder holder = new ViewHolder();

        if(row == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(layout, null);

            holder.busView = row.findViewById(R.id.busNumber);
            holder.timeView = row.findViewById(R.id.nextBusTime);
            row.setTag(holder);
        } else {
            holder = (ViewHolder)row.getTag();
        }

        BusTimeGUI gui = timeList.get(position);
        holder.busView.setText(gui.getBusNumber());
        holder.timeView.setText(gui.getNextBus());

        return null;
    }
}
