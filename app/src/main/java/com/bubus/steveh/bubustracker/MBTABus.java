package com.bubus.steveh.bubustracker;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Stack;

/**
 * Created by steveh on 9/13/14.
 */
public class MBTABus {
    private String trip_id;
    private String trip_name;
    private String trip_headsign;
    private String vehicle_id;
    private String lat;
    private String lng;
    private static Integer numMBTABuses;

    public String getTrip_id() {
        return this.trip_id;
    }
    public String getTrip_name() {
        return this.trip_name;
    }
    public String getTrip_headsign() {
        return this.trip_headsign;
    }
    public String getVehicle_id() {
        return this.vehicle_id;
    }
    public String getLat() {
        return this.lat;
    }
    public String getLng() {
        return this.lng;
    }
    public Integer getNumMBTABuses() {
        return this.numMBTABuses;
    }




    public static MBTABus fromJsonObject(JSONObject jsonObject) {
        MBTABus mbtaBus = new MBTABus();
        try {
            mbtaBus.trip_id = jsonObject.getString("trip_id");
            mbtaBus.trip_name = jsonObject.getString("trip_name");
            mbtaBus.trip_headsign = jsonObject.getString("trip_headsign");
            JSONObject vehicleDetails = jsonObject.getJSONObject("vehicle");
            mbtaBus.vehicle_id = vehicleDetails.getString("vehicle_id");
            mbtaBus.lat = vehicleDetails.getString("vehicle_lat");
            mbtaBus.lng = vehicleDetails.getString("vehicle_lon");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return mbtaBus;
    }

    public static ArrayList<MBTABus> fromJsonArray(JSONArray jsonArray) {
        ArrayList<MBTABus> MBTABuses = new ArrayList<MBTABus>(jsonArray.length());
        Integer numMBTABusesAdded = 0;
        for (int i =0; i<jsonArray.length(); i++) {
            JSONObject mbtaBusJson = null;
            try {
                mbtaBusJson = jsonArray.getJSONObject(i);
            }catch (Exception e)
            {
                e.printStackTrace();
                continue;
            }
            MBTABus mbtaBus = MBTABus.fromJsonObject(mbtaBusJson);
            if (mbtaBus != null) {
                MBTABuses.add(mbtaBus);
                numMBTABusesAdded++;
            }
        }
        numMBTABuses = numMBTABusesAdded;
        return MBTABuses;
    }



}

/*
"trip_id":"24534671",
"trip_name":"12:21 pm from Kenmore Station Busway to Watertown Yard",
"trip_headsign":"Watertown Yard via Brighton",
"vehicle":{
"vehicle_id":"y2281",
"vehicle_lat":"42.3501625061035",
"vehicle_lon":"-71.1698837280273",
"vehicle_timestamp":"1410627034"
 */