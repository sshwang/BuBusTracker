package com.bubus.steveh.bubustracker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by steveh on 9/3/14.
 */
public class Bus {
    private String id;
    private String lng;
    private String lat;
    private String timestamp;
    private ArrayList<Stop> stops;
    private static Integer numBuses;
    private String BusType;
    private Boolean hasStops = false;
    private String minTimeToNextStop;

    public String getId() {
        return this.id;
    }
    public String getLng() {
        return this.lng;
    }
    public String getLat() {
        return this.lat;
    }
    public Integer getNumBuses() {
        return this.numBuses;
    }
    public String getTimestamp() {
        return this.timestamp;
    }
    public ArrayList<Stop> getStops() {
        return this.stops;
    }
    public Stop getNextStop() {
        return this.stops.get(0);
    }
    public String getBusType() {return  this.BusType; }
    public Boolean getHasStops() {return this.hasStops;}
    public String getMinTimeToNextStop() {return this.minTimeToNextStop;}

    public static Bus fromJsonObject(JSONObject jsonObject) {
        Bus b = new Bus();
        Stop s = new Stop();
        try {
            Integer iId = Integer.valueOf(jsonObject.getString("id"));
            b.id = jsonObject.getString("id");
            switch(iId){
                case 4009127:
                    b.BusType = "Big Bus";
                    break;
                case 4007492:
                    b.BusType = "Big Bus";
                    break;
                case 4007512:
                    b.BusType = "Big Bus";
                    break;
                case 4007516:
                    b.BusType = "Big Bus";
                    break;
                case 4007508:
                    b.BusType = "Big Bus";
                    break;
                case 4010503:
                    b.BusType = "Big Bus";
                    break;
                case 4008320:
                    b.BusType = "Big Bus";
                    break;
                case 4007568:
                    b.BusType = "Small Bus";
                    break;
                case 4007504:
                    b.BusType = "Small Bus";
                    break;
                case 4007496:
                    b.BusType = "Small Bus";
                    break;
                case 4007500:
                    b.BusType = "Small Bus";
                    break;

                default:
                    b.BusType = "Unknown Bus Type";
            }
            b.lng = jsonObject.getString("lng");
            b.lat = jsonObject.getString("lat");
            b.timestamp = jsonObject.getString("timestamp");
            if (jsonObject.has("arrival_estimates")) {
                JSONArray stops = jsonObject.getJSONArray("arrival_estimates");
                ArrayList myStopArray = s.fromJsonArray(stops);
                b.stops = myStopArray;
                b.hasStops = true;
            }
            else {
                b.hasStops = false;
            }


        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return b;
    }

    public static ArrayList<Bus> fromJsonArray(JSONArray jsonArray) {
        ArrayList<Bus> buses = new ArrayList<Bus>(jsonArray.length());
        Integer numBusesAdded = 0;
        for (int i=0; i<jsonArray.length(); i++) {
            JSONObject busJson = null;
            try {
                busJson = jsonArray.getJSONObject(i);
            } catch (Exception e)
            {
                e.printStackTrace();
                continue;
            }

            Bus bus = Bus.fromJsonObject(busJson);
            if (bus != null) {
                buses.add(bus);
                numBusesAdded++;
            }
        }
        numBuses = numBusesAdded;
        return buses;
    }


}