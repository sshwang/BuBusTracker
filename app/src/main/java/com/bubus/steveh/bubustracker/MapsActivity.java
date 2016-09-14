package com.bubus.steveh.bubustracker;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.location.Location;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.util.Property;
import android.view.Menu;
import android.view.MenuInflater;

import com.google.common.collect.HashBiMap;

import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.services.Constants;
import com.mapbox.services.commons.ServicesException;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.directions.v4.DirectionsCriteria;
import com.mapbox.services.directions.v4.models.DirectionsResponse;
import com.mapbox.services.directions.v4.models.Waypoint;
import com.mapbox.services.directions.v4.MapboxDirections;
import com.mapbox.services.directions.v4.models.DirectionsRoute;


import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class MapsActivity extends Activity implements MapboxMap.OnMyLocationChangeListener, MapboxMap.OnInfoWindowClickListener, OnMapReadyCallback{
    private HashMap<Integer, Bus> busIDtoBus = new HashMap<Integer, Bus>();
    private HashMap<Integer, Marker> busIDtoMarker = new HashMap<Integer, Marker>();
    private HashBiMap<Integer, Marker> busIDandMarkerHashBiMap = HashBiMap.create();
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 1;
    private MapView mapView;
    private MapboxMap mbMap;
    private DirectionsRoute currentRoute;
    private static final String TAG = "MainActivity";
    private Intent intent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        intent = new Intent(this, AsyncTaskService.class);

        verifyPermissions(this); // android 6.0+ permissions
        //setUpMapIfNeeded(); // start setting up map

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        mbMap = mapboxMap;
        mbMap.setMyLocationEnabled(true);
        mbMap.setOnMyLocationChangeListener(this);
        mbMap.setOnInfoWindowClickListener(this);
        new DrawGeoJSON().execute();
    }

    private class DrawGeoJSON extends AsyncTask<Void, Void, List<LatLng>> {
        @Override
        protected List<LatLng> doInBackground(Void... voids) {

            ArrayList<LatLng> points = new ArrayList<>();

            try {
                // Load GeoJSON file
                InputStream inputStream = getAssets().open("buspath.geojson");
                BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
                StringBuilder sb = new StringBuilder();
                int cp;
                while ((cp = rd.read()) != -1) {
                    sb.append((char) cp);
                }

                inputStream.close();

                // Parse JSON
                JSONObject json = new JSONObject(sb.toString());
                JSONArray features = json.getJSONArray("features");
                JSONObject feature = features.getJSONObject(0);
                JSONObject geometry = feature.getJSONObject("geometry");
                if (geometry != null) {
                    String type = geometry.getString("type");

                    // Our GeoJSON only has one feature: a line string
                    if (!TextUtils.isEmpty(type) && type.equalsIgnoreCase("LineString")) {

                        // Get the Coordinates
                        JSONArray coords = geometry.getJSONArray("coordinates");
                        for (int lc = 0; lc < coords.length(); lc++) {
                            JSONArray coord = coords.getJSONArray(lc);
                            LatLng latLng = new LatLng(coord.getDouble(1), coord.getDouble(0));
                            points.add(latLng);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception Loading GeoJSON: " + e.toString());
            }

            return points;
        }

        @Override
        protected void onPostExecute(List<LatLng> points) {
            super.onPostExecute(points);

            if (points.size() > 0) {
                LatLng[] pointsArray = points.toArray(new LatLng[points.size()]);

                // Draw Points on MapView
                mbMap.addPolyline(new PolylineOptions()
                        .add(pointsArray)
                        .color(Color.parseColor("#3bb2d0"))
                        .width(2));
            }
        }
    }


    @Override
    public boolean onInfoWindowClick(Marker marker) {
        if (busIDandMarkerHashBiMap.containsValue(marker)) { // IF the marker is a bus
            Integer busId = busIDandMarkerHashBiMap.inverse().get(marker);
            Bus selectedBus = busIDtoBus.get(busId);

            ArrayList<Stop> myStops = selectedBus.getStops();
            if (myStops != null) {
                Iterator<Stop> it = myStops.iterator();
                String schedule = "";
                Date now = new Date();
                while (it.hasNext()) {
                    Stop currentStop = it.next();
                    schedule = schedule + getEAT(currentStop.getEstimatedArrivalDate()) + "    "+currentStop.getStopName() + "\n";
                }
                AlertDialog.Builder builder =
                        new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
                builder.setTitle(selectedBus.getBusType());
                builder.setMessage(schedule);
                builder.setPositiveButton("CLOSE", null);
                builder.show();
//                QustomDialogBuilder qustomDialogBuilder = new QustomDialogBuilder(this).
//                        setTitle(selectedBus.getBusType()).
//                        setTitleColor("#CC0000").
//                        setDividerColor("#CC0000").
//                        setMessage(schedule);
//
//                qustomDialogBuilder.show();

            }

        }
        else { // IF the marker is a stop

        }
        return true;
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //updateUI(intent);
//            String counter = intent.getStringExtra("counter");
            String time = intent.getStringExtra("time");
            ArrayList<Bus> newBuses =  intent.getExtras().getParcelableArrayList("newBuses");
            ArrayList<Bus> existingBuses =  intent.getExtras().getParcelableArrayList("existingBuses");
            animateExistingBus(existingBuses);
            Integer n = 0;
        }
    };

    public void plotNewBus(Bus bus){
        if (bus != null) {
                Icon icon = getIconForBus(bus.getGeneralHeading());
                Marker m = mbMap.addMarker(new MarkerOptions()
                        .position(bus.getLatLng())
                        .icon(icon)
                        .title(bus.getHasStops() ? bus.getNextStop().getStopName() + ": " + getETA(bus.getNextStop().getEstimatedArrivalDate()) : "No Schedule Available")
                        .snippet(bus.getHasStops() ? bus.getNextStop().getStopName() + ": " + getETA(bus.getNextStop().getEstimatedArrivalDate()) : "No Schedule Available"));
                busIDtoMarker.put(bus.getId(), m);
        }
    }

    public void animateExistingBus(ArrayList<Bus> buses){
        if (buses != null && buses.isEmpty() != true) {
            for (Bus b: buses) {
                if (busIDtoMarker.containsKey(b.getId())) {
                    Marker existingMarker = busIDtoMarker.get(b.getId());
                    Icon icon = getIconForBus(b.getGeneralHeading());
                    ValueAnimator markerAnimator = ObjectAnimator.ofObject(existingMarker, "position",
                            new LatLngEvaluator(), existingMarker.getPosition(), b.getLatLng());
                    markerAnimator.setDuration(5000);
                    markerAnimator.start();
                    existingMarker.setIcon(icon);
                    existingMarker.setTitle(b.getHasStops() ? b.getNextStop().getStopName() + ": " + getETA(b.getNextStop().getEstimatedArrivalDate()): "No Schedule Available");
                    existingMarker.setSnippet(b.getHasStops() ? b.getNextStop().getStopName() + ": " + getETA(b.getNextStop().getEstimatedArrivalDate())  : "No Schedule Available");
                    busIDtoMarker.put(b.getId(), existingMarker);
                }
                else {
                    plotNewBus(b);
                }

            }
        }
    }

    private static class LatLngEvaluator implements TypeEvaluator<LatLng> {

        private LatLng latLng = new LatLng();
        @Override
        public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
            latLng.setLatitude(startValue.getLatitude() +
                    ((endValue.getLatitude() - startValue.getLatitude()) * fraction));
            latLng.setLongitude(startValue.getLongitude() +
                    ((endValue.getLongitude() - startValue.getLongitude()) * fraction));
            return latLng;
        }
    }

    public Icon getIconForBus(Integer generalHeading) {
        String busIcon = "bus421" + generalHeading + "degrees";
        Integer resId = getResources().getIdentifier(busIcon, "drawable", getPackageName());
        Bitmap longBitMap = mapboxHackIcon(BitmapFactory.decodeResource(MapsActivity.this.getResources(), resId));
        IconFactory iconFactory = IconFactory.getInstance(MapsActivity.this);
        Icon icon = iconFactory.fromBitmap(longBitMap);
        return icon;
    }

    public static Bitmap mapboxHackIcon(Bitmap icon){
        Bitmap loc_img = Bitmap.createBitmap(icon.getWidth(), 2*icon.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas bitmapCanvas = new Canvas(loc_img);
        Bitmap tempBitmap = icon.copy(Bitmap.Config.ARGB_8888, false);
        bitmapCanvas.drawBitmap(tempBitmap, 0, 0, null);
        return loc_img;
    }

    @Override
    public void onResume() {
        super.onResume();
        startService(intent);
        registerReceiver(broadcastReceiver, new IntentFilter(AsyncTaskService.BROADCAST_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
        stopService(intent);
    }


    private void parseBusInfo(String rawJson) {
        ArrayList<Bus> busArray;
        try {
            JSONObject allData = new JSONObject(rawJson); // convert string to JSONObject
            Integer totalResultsAvailable = Integer.parseInt(allData.getString("totalResultsAvailable"));
            Integer isMissingResults = Integer.parseInt(allData.getString("isMissingResults"));
            if (totalResultsAvailable == 0) {
                Integer n = 0; //debug
            } else if (isMissingResults == 1) {
                Integer n = 0; // handle m
            } else {
                JSONObject resultSet = allData.getJSONObject("ResultSet");
                JSONArray buses = resultSet.getJSONArray("Result");
                Bus myBuses = new Bus();
                busArray = myBuses.fromJsonArray(buses); // parsing done. busArray is array of Bus objects

                for (Bus b: busArray) {
                    Integer id = b.getId();
                    Integer busGenHead = b.getGeneralHeading();
                    String busIcon = "bus421" + busGenHead + "degrees";
                    String minToArrival = "Unavailable";
                    String nextStop = "Unavailable";
                    if (b.getNextStop() != null) {
                        Date estimatedArrivalDate = b.getNextStop().getEstimatedArrivalDate();
                        minToArrival = getETA(estimatedArrivalDate);
                        nextStop = b.getNextStop().getStopName();
                    }

                    Integer resId = getResources().getIdentifier(busIcon, "drawable", getPackageName());
                    IconFactory iconFactory = IconFactory.getInstance(MapsActivity.this);

                    if (!busIDtoBus.containsKey(id)) { // if this is a new bus
                        busIDtoBus.put(id, b); // put into id to bus hashmap
                        if (!busIDandMarkerHashBiMap.containsKey(id)) { // ensure there is also no marker for that bus id
                            Drawable iconDrawable = ContextCompat.getDrawable(MapsActivity.this, resId);
                            Icon icon = iconFactory.fromDrawable(iconDrawable);
                            Marker m = mbMap.addMarker(new MarkerOptions()
                                    .position(b.getLatLng())
                                    .icon(icon)
                                    .snippet(minToArrival)
                                    .title(nextStop));
                            busIDandMarkerHashBiMap.put(id, m); // create a marker, plot, and add it to the marker hashmap
                        }
                    }
                    else { // if this bus already exists
                        Drawable iconDrawable = ContextCompat.getDrawable(MapsActivity.this, resId);
                        Icon icon = iconFactory.fromDrawable(iconDrawable);
                        Marker oldM = busIDandMarkerHashBiMap.get(id);
                        Marker m = mbMap.addMarker(new MarkerOptions()
                                .position(oldM.getPosition())
                                .icon(icon)
                                .snippet(minToArrival)
                                .title(nextStop));
                        oldM.remove();
                        busIDandMarkerHashBiMap.forcePut(id, m);
                        animateMarkerToICS(busIDandMarkerHashBiMap.get(id), b.getLatLng(),mLatLngInterpolator);// get the marker and animate it
                        busIDandMarkerHashBiMap.put(id, m);
                    }
                }
                for (Bus b: busArray) {

                }


            }
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main_actions, menu); // Leftover menu from older version
        return super.onCreateOptionsMenu(menu);
    }



    private void addBUStops() { // plot each BU stop
        IconFactory iconFactory = IconFactory.getInstance(MapsActivity.this);
        Drawable iconDrawable = ContextCompat.getDrawable(MapsActivity.this, R.drawable.bus_stop2);
        Icon icon = iconFactory.fromDrawable(iconDrawable);

        Marker MylesStandish = mbMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.349536,-71.094530))
                .title("Myles Standish")
                .icon(icon));
        Marker SilberWay = mbMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.349453,-71.100748))
                .title("Silber Way")
                .icon(icon));
        Marker MarshPlaza = mbMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.350181,-71.106085))
                .title("Marsh Plaza")
                .icon(icon));
        Marker CFA = mbMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.351191,-71.114019))
                .title("CFA")
                .icon(icon));
        Marker Stuvi2 = mbMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.351819,-71.118085))
                .title("Stuvi 2")
                .icon(icon));
        Marker AmorySt = mbMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.350354,-71.113654))
                .title("Amory St")
                .icon(icon));
        Marker StMarysSt = mbMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.349553,-71.106310))
                .title("St Mary's St")
                .icon(icon));
        Marker BlanfordSt = mbMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.348802,-71.100170))
                .title("Blanford St")
                .icon(icon));
        Marker Kenmore = mbMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.348585,-71.095490))
                .title("Kenmore")
                .icon(icon));
        Marker AlbanySt = mbMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.335132,-71.070798))
                .title("Albany St")
                .icon(icon));
        Marker HuntingtonAve1 = mbMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.342348,-71.084756))
                .title("Huntington Ave Outbound")
                .icon(icon));
        Marker HuntingtonAve2 = mbMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.342336,-71.084150))
                .title("Huntington Ave Inbound")
                .icon(icon));
        Marker DanielsonHall = mbMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.350876,-71.089718))
                .title("Danielson Hall")
                .icon(icon));

    }



    private String getETA(Date estimatedArrivalDate){ //Estimated time to arrival
        Date now = new Date();
        String minutes;
        long milliseconds = estimatedArrivalDate.getTime()-now.getTime();
        long min = milliseconds / (60 * 1000) % 60;
        if (min <1)
            minutes = "Arriving Now";
        else if (min ==1)
            minutes = Long.toString(min) + " minute";
        else
            minutes = Long.toString(min) + " minutes";
        return minutes;
    }

    private String getEAT(Date estimateArrivalDate) { //Estimated arrival time
        DateFormat dateFormat = new SimpleDateFormat("h:mm");
        String s = dateFormat.format(estimateArrivalDate);
        return s;
    }

    // Zoom to current location
    @Override
    public void onMyLocationChange(Location location) {
        if (location != null) {
            LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mbMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15)); // Jump to current location on app open

        }
        mbMap.setOnMyLocationChangeListener(null); // Stop jumping to current location on location change
    }

    private void createSnackbar(String text) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), text , Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    public void verifyPermissions(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                /// show explanation asynchronously
            }
            else {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) { // leftover switch from previous usage
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted
                    mbMap.setMyLocationEnabled(true);

                } else {
                    // permission denied. Handle error
                }
                return;
            }
        }
    }

    public void animateMarkerToICS(Marker marker, LatLng finalPosition, final LatLngInterpolator latLngInterpolator) {
        TypeEvaluator<LatLng> typeEvaluator = new TypeEvaluator<LatLng>() {
            @Override
            public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
                return latLngInterpolator.interpolate(fraction, startValue, endValue);
            }
        };
        Property<Marker, LatLng> property = Property.of(Marker.class, LatLng.class, "position");
        ObjectAnimator animator = ObjectAnimator.ofObject(marker, property, typeEvaluator, finalPosition);
        animator.setDuration(3000);
        animator.start();
    }
}
