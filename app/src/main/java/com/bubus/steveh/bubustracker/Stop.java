package com.bubus.steveh.bubustracker;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

/**
 * Created by steveh on 9/5/14.
 */
public class Stop implements Comparable<Stop>,Parcelable{
    private String id;
    private String route;
    private String estimatedArrival;
    private String stopName;
    private static Integer numStops;
    private Date estimatedArrivalDate;
    private String estimatedTimeToArrival;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.route);
        dest.writeString(this.estimatedArrival);
        dest.writeString(this.stopName);
        dest.writeSerializable(this.estimatedArrivalDate);
    }

    public static final Parcelable.Creator<Stop> CREATOR = new Parcelable.Creator<Stop>() {
        public Stop createFromParcel(Parcel dest) {
            return new Stop(dest);
        }

        public Stop[] newArray(int size) {
            return new Stop[size];
        }
    };

    public Stop(Parcel in) {
        this.id = in.readString();
        this.route = in.readString();
        this.estimatedArrival = in.readString();
        this.stopName = in.readString();
        this.estimatedArrivalDate = (java.util.Date) in.readSerializable();
    }

    public Stop() {
    }

    public String getId() {
        return this.id;
    }
    public String getRoute() {
        return this.route;
    }
    public String getEstimatedArrival() {
        return this.estimatedArrival;
    }
    public String getStopName() {
        return this.stopName;
    }
    public Date getEstimatedArrivalDate() {
        return this.estimatedArrivalDate;
    }

    public static Stop fromJsonObject(JSONObject jsonObject) {
        Stop s = new Stop();
        try {
            Integer iId = Integer.valueOf(jsonObject.getString("stop_id"));
            s.id = jsonObject.getString("stop_id");
            s.route = jsonObject.getString("route_id");
            s.estimatedArrival = jsonObject.getString("arrival_at");
            s.estimatedArrivalDate = convertStringToDate(jsonObject.getString("arrival_at"));
            switch(iId){
                case 4160718:
                    s.stopName = "Huntington Ave Inbound";
                    break;
                case 4160722:
                    s.stopName = "Danielson Hall";
                    break;
                case 4160726:
                    s.stopName = "Myles Standish";
                    break;
                case 4160730:
                    s.stopName = "Silber Way";
                    break;
                case 4160734:
                    s.stopName = "Marsh Plaza";
                    break;
                case 4160738:
                    s.stopName = "CFA";
                    break;
                case 4160714:
                    s.stopName = "Stuvi 2";
                    break;
                case 4114006:
                    s.stopName = "Amory St";
                    break;
                case 4149154:
                    s.stopName = "St Mary's St";
                    break;
                case 4068466:
                    s.stopName = "Blanford St";
                    break;
                case 4068470:
                    s.stopName = "Kenmore";
                    break;
                case 4110206:
                    s.stopName = "Huntington Ave Outbound";
                    break;
                case 4068482:
                    s.stopName = "Albany St";
                    break;
                default:
                    s.stopName = "Unknown Stop";
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return s;
    }

    public static Date convertStringToDate(String input)  {
        Date d = new Date();
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'-05:00'");
            SimpleDateFormat formatter2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ");
            d = formatter2.parse(input);
            Integer i = 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return d;
        //printout shows: Thu Aug 15 17:00:48 EDT 2013
    }

    public static ArrayList<Stop> fromJsonArray(JSONArray jsonArray) {
        ArrayList<Stop> stops = new ArrayList<Stop>(jsonArray.length());
        Integer numStopsAdded = 0;
        for (int i=0; i<jsonArray.length(); i++) {
            JSONObject stopJson = null;
            try {
                stopJson = jsonArray.getJSONObject(i);
            } catch (Exception e)
            {
                e.printStackTrace();
                continue;
            }

            Stop stop = Stop.fromJsonObject(stopJson);
            if (stop != null) {
                stops.add(stop);
                numStopsAdded++;
            }
        }
        numStops = numStopsAdded;
        return stops;
    }
    @Override
    public int compareTo(Stop s) {
        return getEstimatedArrivalDate().compareTo(s.getEstimatedArrivalDate());
    }

}
