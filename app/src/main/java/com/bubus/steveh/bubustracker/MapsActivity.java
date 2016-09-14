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


                }

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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main_actions, menu); // Leftover menu from older version
        return super.onCreateOptionsMenu(menu);
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
}
