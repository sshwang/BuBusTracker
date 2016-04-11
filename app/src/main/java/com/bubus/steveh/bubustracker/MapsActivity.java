package com.bubus.steveh.bubustracker;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.os.Handler;
import android.location.Location;
import android.util.Property;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.CameraUpdateFactory;


import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.lang.Runnable;


public class MapsActivity extends Activity implements GoogleMap.OnMarkerClickListener, GoogleMap.OnMyLocationChangeListener, GoogleMap.OnInfoWindowClickListener{
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private HashMap<Integer, Bus> busIDtoBus = new HashMap<Integer, Bus>();
    private HashMap<Integer, Marker> busIDtoMarker = new HashMap<Integer, Marker>();
    private int interval = 5000;
    private Handler mHandler;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 1;
    private LatLngInterpolator mLatLngInterpolator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mLatLngInterpolator = new LatLngInterpolator.Linear();

        verifyPermissions(this); // android 6.0+ permissions
        setUpMapIfNeeded(); // start setting up map

        mHandler = new Handler();
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            getBusInfo();
            mHandler.postDelayed(mStatusChecker, interval);
        }
    };

    private void getBusInfo() {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://www.bu.edu/bumobile/rpc/bus/livebus.json.php";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Integer n = 0; // response should be here in successful
                        parseBusInfo(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Integer n = 0; //debug
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
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

                    if (!busIDtoBus.containsKey(id)) { // if this is a new bus
                        busIDtoBus.put(id, b); // put into id to bus hashmap
                        if (!busIDtoMarker.containsKey(id)) { // ensure there is also no marker for that bus id
                            Marker m = mMap.addMarker(new MarkerOptions()
                                    .position(b.getLatLng())
                                    .icon(BitmapDescriptorFactory.fromResource(resId))
                                    .snippet(minToArrival)
                                    .title(nextStop));
                            busIDtoMarker.put(id, m); // create a marker, plot, and add it to the marker hashmap
                        }
                    }
                    else { // if this bus already exists
                        Marker m = busIDtoMarker.get(id);
                        m.setIcon(BitmapDescriptorFactory.fromResource(resId));
                        m.setSnippet(minToArrival);
                        m.setTitle(nextStop);
                        animateMarkerToICS(busIDtoMarker.get(id), b.getLatLng(),mLatLngInterpolator);// get the marker and animate it
                        busIDtoMarker.put(id, m);
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

    @Override
    public boolean onMarkerClick(final Marker marker) {

//        if (marker.equals(myMarker))
//        {
//            //handle click here
//        }
        return true;
    }

    @Override
    public void onResume() {
        mHandler.postDelayed(mStatusChecker, interval); // start handler
        super.onResume();
    }

    @Override
    public void onPause() {
        mHandler.removeCallbacks(mStatusChecker); // close handler
        super.onPause();
    }
    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                mMap.setOnMyLocationChangeListener(this);
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        addBUStops();
    }

    private void addBUStops() { // plot each BU stop

        Marker MylesStandish = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.349536,-71.094530))
                .title("Myles Standish")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_stop2)));
        Marker SilberWay = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.349453,-71.100748))
                .title("Silber Way")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_stop2)));
        Marker MarshPlaza = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.350181,-71.106085))
                .title("Marsh Plaza")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_stop2)));
        Marker CFA = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.351191,-71.114019))
                .title("CFA")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_stop2)));
        Marker Stuvi2 = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.351819,-71.118085))
                .title("Stuvi 2")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_stop2)));
        Marker AmorySt = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.350354,-71.113654))
                .title("Amory St")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_stop2)));
        Marker StMarysSt = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.349553,-71.106310))
                .title("St Mary's St")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_stop2)));
        Marker BlanfordSt = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.348802,-71.100170))
                .title("Blanford St")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_stop2)));
        Marker Kenmore = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.348585,-71.095490))
                .title("Kenmore")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_stop2)));
        Marker AlbanySt = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.335132,-71.070798))
                .title("Albany St")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_stop2)));
        Marker HuntingtonAve1 = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.342348,-71.084756))
                .title("Huntington Ave Outbound")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_stop2)));
        Marker HuntingtonAve2 = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.342336,-71.084150))
                .title("Huntington Ave Inbound")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_stop2)));
        Marker DanielsonHall = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.350876,-71.089718))
                .title("Danielson Hall")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_stop2)));

    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        Toast.makeText(getBaseContext(),
                "Info Window clicked@" + marker.getId(),
                Toast.LENGTH_SHORT).show();

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
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15)); // Jump to current location on app open

        }
        mMap.setOnMyLocationChangeListener(null); // Stop jumping to current location on location change
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
                    mMap.setMyLocationEnabled(true);

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
