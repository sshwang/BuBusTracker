package com.bubus.steveh.bubustracker;

import android.os.Parcel;
import android.os.Parcelable;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.annotations.Marker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by steveh on 9/3/14.
 */
public class Bus implements Parcelable {

    // 1) extend marker object to include ID and  2) bus object contains marker 3)
    private Integer id;
    private Integer generalHeading;
    private Double lng;
    private Double lat;
    private String timestamp;
    private ArrayList<Stop> stops;
    private static Integer numBuses;
    private String BusType;
    private Boolean hasStops = false;
    private Marker busMarker;
    private String route;

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
        if (stops != null && !stops.isEmpty()) {
            return this.stops.get(0);
        }
        else {
            return null;
        }
    }
    public String getRoute() {return this.route;}
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
            b.route = jsonObject.getString("route");
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
//        dest.writeIntArray(new int[]{this.id,this.generalHeading});
//        dest.writeDoubleArray(new double[]{this.lng, this.lat});
//        dest.writeString(this.timestamp);
//        dest.writeTypedList(stops);
        dest.writeInt(this.id);
        dest.writeInt(this.generalHeading);
        dest.writeDouble(this.lng);
        dest.writeDouble(this.lat);
        dest.writeString(this.timestamp);
        dest.writeString(this.route);
        dest.writeTypedList(stops);
        dest.writeByte((byte) (this.hasStops ? 1 : 0));
    }

    public Bus() {

    }

    public Bus(Parcel in) {
        this.id = in.readInt();
        this.generalHeading = in.readInt();
        this.lng = in.readDouble();
        this.lat = in.readDouble();
        this.timestamp = in.readString();
        this.route = in.readString();
        this.stops = new ArrayList<>();
        in.readTypedList(stops, Stop.CREATOR);
        this.hasStops = in.readByte() != 0;
    }


    public static final Parcelable.Creator<Bus> CREATOR = new Parcelable.Creator<Bus>() {
        public Bus createFromParcel(Parcel dest) {
            return new Bus(dest);
        }

        public Bus[] newArray(int size) {
            return new Bus[size];
        }
    };
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