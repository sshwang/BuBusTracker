package com.bubus.steveh.bubustracker;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by steveh on 9/3/14.
 */
public class Bus {

    // 1) extend marker object to include ID and  2) bus object contains marker 3)
    private Integer id;
    private Double lng;
    private Double lat;
    private String timestamp;
    private ArrayList<Stop> stops;
    private static Integer numBuses;
    private String BusType;
    private Boolean hasStops = false;
    private Marker busMarker;
    private Integer generalHeading;

    public Integer getId() {
        return id;
    }
    public Integer getGeneralHeading() { return this.generalHeading; }
    public Integer getNumBuses() {
        return this.numBuses;
    }
    public Marker getBusMarker() {
        return busMarker;
    }
    public LatLng getLatLng() {
        return new LatLng(lat,lng);
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

    public static Bus fromJsonObject(JSONObject jsonObject) {
        Bus b = new Bus();
        Stop s = new Stop();
        try {
            Integer iId = Integer.valueOf(jsonObject.getString("id"));
            b.id = jsonObject.getInt("id");
            b.lng = jsonObject.getDouble("lng");
            b.lat = jsonObject.getDouble("lat");
            b.timestamp = jsonObject.getString("timestamp");
            b.generalHeading = jsonObject.getInt("general_heading");
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
//switch(iId){
//        case 4009127:
//        b.BusType = "Big Bus";
//        break;
//        case 4007492:
//        b.BusType = "Big Bus";
//        break;
//        case 4007512:
//        b.BusType = "Big Bus";
//        break;
//        case 4007516:
//        b.BusType = "Big Bus";
//        break;
//        case 4007508:
//        b.BusType = "Big Bus";
//        break;
//        case 4010503:
//        b.BusType = "Big Bus";
//        break;
//        case 4008320:
//        b.BusType = "Big Bus";
//        break;
//        case 4007568:
//        b.BusType = "Small Bus";
//        break;
//        case 4007504:
//        b.BusType = "Small Bus";
//        break;
//        case 4007496:
//        b.BusType = "Small Bus";
//        break;
//        case 4007500:
//        b.BusType = "Small Bus";
//        break;
//
//default:
//        b.BusType = "Unknown Bus Type";
//        }