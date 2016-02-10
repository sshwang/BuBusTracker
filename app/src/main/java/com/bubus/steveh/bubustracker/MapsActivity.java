package com.bubus.steveh.bubustracker;

import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.graphics.Point;
import android.location.Location;
import android.app.AlertDialog;
import android.view.View;
import android.view.Menu;
import android.view.MenuInflater;
//import android.app.ActionBar;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Color;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.DialogInterface;


import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Iterator;
import java.text.SimpleDateFormat;
import java.text.DateFormat;


import android.widget.Toast;

import java.lang.Runnable;


public class MapsActivity extends ActionBarActivity implements GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener{
    public static final String PREFS_NAME = "MyPrefsFile";
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private MenuItem refresh;
    private Marker myMarker;
    private boolean autoRefresh = false;
    private Handler handler = new Handler();
    private boolean mbtaData = false;
    private JSONObject oldJSON;
    private TimerTask mTimerTask;
    private Timer mTimer;
    private boolean turnOff = false;
    private HashMap<Bus, Marker> markerHashMap = new HashMap<Bus, Marker>();
    private HashMap<Marker,Bus> busHashMap = new HashMap<Marker, Bus>();
    private HashMap<Marker,MBTABus> mbtaBusHashMap = new HashMap<Marker, MBTABus>();
    private HashMap<MBTABus, Marker> mbtaIDHashMap = new HashMap<MBTABus, Marker>();


    private ArrayList<Bus> currentBuses = new ArrayList<Bus>();


    final Context context = this;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        autoRefresh = settings.getBoolean("autoRefresh", false);
        mbtaData = settings.getBoolean("mbtaData", false);
        mTimer = new Timer();
        setContentView(R.layout.activity_maps);
        //ActionBar bar = getActionBar();
        ActionBar bar = getSupportActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CC0000")));
        setUpMapIfNeeded();
        refreshMapRunnable.run();
        startMap();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main_actions, menu);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        menu.findItem(R.id.action_MBTAData).setChecked(mbtaData);
        menu.findItem(R.id.action_autorefresh).setChecked(autoRefresh);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        autoRefresh = settings.getBoolean("autoRefresh", false);
        mbtaData = settings.getBoolean("mbtaData", false);
        this.mTimer= new Timer();
        this.turnOff = false;
        setUpMapIfNeeded();
        startMap();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //mTimerTask.cancel();
        this.turnOff = true;
        handler.removeCallbacks(refreshMapRunnable);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("autoRefresh", autoRefresh);
        editor.putBoolean("mbtaData", mbtaData);
        editor.commit();

    }


    @Override
    public void onStop(){
        super.onStop();
        //mTimerTask.cancel();
        this.turnOff = true;
        handler.removeCallbacks(refreshMapRunnable);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("autoRefresh", autoRefresh);
        editor.putBoolean("mbtaData", mbtaData);
        editor.commit();

        finish();
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        this.turnOff = true;
        handler.removeCallbacks(refreshMapRunnable);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("autoRefresh", false);
        editor.putBoolean("mbtaData", false);
        editor.commit();
        //mTimerTask.cancel();
        //finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Take appropriate action for each action item click
        switch (item.getItemId()) {
            case R.id.action_refresh:
                // refresh
                refreshMapRunnable.run();
                return true;
            case R.id.action_autorefresh:
                if (!item.isChecked()) {
                    item.setChecked(true);
                    autoRefresh = true;
                    startMap();
                    return true;
                }
                else{
                    item.setChecked(false);
                    autoRefresh = false;
                    handler.removeCallbacks(refreshMapRunnable);
                    return true;
                }
            case R.id.action_MBTAData:
                if (!item.isChecked()) {
                    item.setChecked(true);
                    mbtaData = true;
                    refreshMapRunnable.run();
                    startMap();
                    return true;
                }
                else{
                    item.setChecked(false);
                    mbtaData = false;
                    for (Marker key : mbtaBusHashMap.keySet()) {
                        key.remove();
                    }
                    refreshMapRunnable.run();
                        //iterate through each bus now called key
                    startMap();
                    return true;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean onMarkerClick(final Marker marker) {

        if (marker.equals(myMarker))
        {
            //handle click here
        }
        return true;
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
        String result = "";
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }


    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void startMap() {
            refreshMapRunnable.run();

    }

    private Runnable refreshMapRunnable =  new Runnable() {
        @Override
        public void run() {
            try {
                String result = "";
                RefreshBUMapAsync RefreshBUMapAsync = new RefreshBUMapAsync();
                markerHashMap = RefreshBUMapAsync.execute(markerHashMap).get();
                if (mbtaData == true) {
                    RefreshMBTAMapAsync RefreshMBTAMapAsync = new RefreshMBTAMapAsync();
                    mbtaIDHashMap = RefreshMBTAMapAsync.execute(mbtaIDHashMap).get();
                }



            } catch (Exception e) {
                String msg = (e.getMessage() == null) ? "No data!" : e.getMessage();
            }

            if (autoRefresh == true) {
                handler.postDelayed(this, 7000);
                //Toast.makeText(getApplicationContext(), "auotrefresh=true", Toast.LENGTH_SHORT).show();

            } else {
                handler.removeCallbacks(refreshMapRunnable);
                //Toast.makeText(getApplicationContext(), "auotrefresh=false", Toast.LENGTH_SHORT).show();
            }
        }

    };


    private void setUpMap() {
        LatLng BOSTONU = new LatLng(42.350630, -71.094332);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BOSTONU, 13));
        addBUStops();
        addMBTAStops();
        mMap.setMyLocationEnabled(true);
        Location myLocation = mMap.getMyLocation();
        mMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {

            @Override
            public void onInfoWindowClick(Marker marker) {
                if (mbtaIDHashMap.containsValue(marker)) {

                }

                if (markerHashMap.containsValue(marker)) {
                    if (busHashMap.get(marker).getHasStops()) {
                        ArrayList<Stop> myStops = busHashMap.get(marker).getStops();
                        Iterator<Stop> it = myStops.iterator();
                        String schedule = "";
                        Date now = new Date();
                        while (it.hasNext()) {
                            Stop currentStop = it.next();
                            schedule = schedule + getEAT(currentStop.getEstimatedArrivalDate()) + "    "+currentStop.getStopName() + "\n";
                        }
                        QustomDialogBuilder qustomDialogBuilder = new QustomDialogBuilder(context).
                                setTitle(busHashMap.get(marker).getBusType()).
                                setTitleColor("#CC0000").
                                setDividerColor("#CC0000").
                                setMessage(schedule);

                                qustomDialogBuilder.show();
                        //alertDialogBuilder.setMessage(schedule);

                    } else {
                        //alertDialogBuilder.setMessage("No Schedule Available");
                        /*QustomDialogBuilder qustomDialogBuilder = new QustomDialogBuilder(context).
                                setTitle(busHashMap.get(marker).getBusType()).
                                setTitleColor("#CC0000").
                                setDividerColor("#CC0000").
                                setMessage("No Schedule Available");

                        qustomDialogBuilder.show();*/
                        return;
                    }

                }
                else if (mbtaIDHashMap.containsValue(marker)){
                    QustomDialogBuilder qustomDialogBuilder = new QustomDialogBuilder(context).
                            setTitle(mbtaBusHashMap.get(marker).getTrip_name()).
                            setTitleColor("#CC0000").
                            setDividerColor("#CC0000").
                            setMessage("Data for MBTA Buses coming soon");

                    qustomDialogBuilder.show();
                }
                else {
                    try {
                        String schedule = "";
                        ArrayList<Date> scheduledTimes = new ArrayList<Date>();
                        for (Map.Entry<Bus, Marker> entry: markerHashMap.entrySet()) {
                            Bus key = entry.getKey();
                            //iterate through each bus now called key
                            if (key.getHasStops()) {
                                ArrayList<Stop> keyStops = key.getStops();
                                Iterator<Stop> it = keyStops.iterator();
                                Date now = new Date();
                                while (it.hasNext()) { //iterate through the bus stops to find where stopname matches marker title
                                    Stop currentStop = it.next();
                                    String markerTitle = marker.getTitle();
                                    String currentStopName = currentStop.getStopName();
                                    if (currentStopName.equals(markerTitle))
                                    {
                                        //schedule = schedule + getEAT(currentStop.getEstimatedArrivalDate()) + "\n";
                                        Date currentStopDate = currentStop.getEstimatedArrivalDate();
                                        scheduledTimes.add(currentStopDate);
                                    }
                                }
                            }
                        }
                        if (scheduledTimes.isEmpty()) {
                            schedule = "No Schedule Available";
                        }
                        else {
                            Collections.sort(scheduledTimes);
                            Iterator<Date> it = scheduledTimes.iterator();
                            while (it.hasNext()) {
                                Date currentDate = it.next();
                                schedule = schedule + getEAT(currentDate) + "\n";
                            }
                        }
                        //alertDialogBuilder.setMessage("No Schedule Available");
                        QustomDialogBuilder qustomDialogBuilder = new QustomDialogBuilder(context).
                                setTitle(marker.getTitle()).
                                setTitleColor("#CC0000").
                                setDividerColor("#CC0000").
                                setMessage(schedule);

                        qustomDialogBuilder.show();
                        scheduledTimes.clear();
                    }
                    catch (Exception e){
                        e.getMessage();
                    }
                }

            }

        });
    }
    private void addMBTAStops() {
        //Marker CommonwealthAveStMarysSt = mMap.addMarker(new MarkerOptions()
          //      .position(new LatLng(42.349789,-71.106392))
            //    .title("Commonwealth Ave @ St Marys St")
              //  .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_iconmonstr_stop_4_icon_256)));
    }
    private void addBUStops() {

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

        //mMap.clear();


    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        Toast.makeText(getBaseContext(),
                "Info Window clicked@" + marker.getId(),
                Toast.LENGTH_SHORT).show();

    }

    private String getETA(Date now, Date estimatedArrivalDate){
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
    private String getEAT(Date estimateArrivalDate) {
        DateFormat dateFormat = new SimpleDateFormat("h:mm");
        String s = dateFormat.format(estimateArrivalDate);
        return s;
    }

    public class RefreshBUMapAsync extends AsyncTask<HashMap<Bus, Marker>, String, HashMap<Bus, Marker>> {
        private JSONObject oldJSON;
        private HashMap<Bus, Marker> newMarkerHashMap = new HashMap<Bus, Marker>();
        private HashMap<Bus, Marker> oldMarkerHashMap = new HashMap<Bus, Marker>();
        private ArrayList<Bus> myBusArray;

        protected void onPreExecute() {
            // Runs on the UI thread before doInBackground
            // Good for toggling visibility of a progress indicator
            super.onPreExecute();
        }

        private String convertInputStreamToString(InputStream inputStream) throws IOException {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            String result = "";
            while ((line = bufferedReader.readLine()) != null)
                result += line;
            inputStream.close();
            return result;

        }

        protected HashMap<Bus, Marker> doInBackground(HashMap<Bus, Marker>... params) {
            // Some long-running task like downloading an image.

            oldMarkerHashMap = params[0];
            InputStream inputStream = null;
            String result = "";
            String url = "http://www.bu.edu/bumobile/rpc/bus/livebus.json.php";
            try {
                HttpClient client = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(url);
                HttpResponse httpResponse = client.execute(httpGet);
                inputStream = httpResponse.getEntity().getContent();
                if (inputStream != null)
                    result = convertInputStreamToString(inputStream);
                else {
                    result = "Did not work!";
                    //DO NOTHING
                }
                JSONObject jsonObject = new JSONObject(result);
                if (jsonObject.equals(oldJSON)) {
                    result = oldJSON.toString();
                    //DO NOTHING
                } else if (jsonObject.length() == 0) {
                    result = oldJSON.toString();
                    //DO NOTHING
                } else {
                    oldJSON = jsonObject;
                    String totalResultsAvailable = jsonObject.getString("totalResultsAvailable");
                    String isMissingResults = jsonObject.getString("isMissingResults");
                    //{"title":"BU Bus Positions","service":"Fall Weekday","ResultSet":{"Result":[]},"totalResultsAvailable":0,"isMissingResults":1}
                    if (totalResultsAvailable == "0") {
                        result = oldJSON.toString();
                    } else if (isMissingResults == "1") {
                        result = oldJSON.toString();
                    } else {
                        JSONObject resultSet = jsonObject.getJSONObject("ResultSet");
                        JSONArray buses = resultSet.getJSONArray("Result");
                        result = buses.toString();
                    }


                }
                JSONArray buses = new JSONArray(result);
                Bus myBuses = new Bus();
                myBusArray = myBuses.fromJsonArray(buses);


            } catch (Exception e) {
                String msg = (e.getMessage() == null) ? "No data!" : e.getMessage();
                Log.i("Cannot get data", msg);
            }
            return newMarkerHashMap;
        }

        @Override
        protected void onPostExecute(HashMap<Bus, Marker> result) {
            // This method is executed in the UIThread
            // with access to the result of the long running task
            //imageView.setImageBitmap(result);
            // Hide the progress bar
            if (myBusArray !=null && !myBusArray.isEmpty()) {
                for (Object o : myBusArray) {
                    Boolean match = false;
                    Bus b = (Bus) o;
                    Float lat = 0.0f;
                    Float lng = 0.0f;
                    if (b.getLat().isEmpty()) {
                        continue;
                    } else {
                        lat = Float.parseFloat(b.getLat());
                        lng = Float.parseFloat(b.getLng());
                    }

                    LatLng latlng = new LatLng(lat, lng);
                    String busType = b.getBusType();
                    String busId = b.getId();
                    String busGenHead = b.getGeneralHeading();
                    String busIcon = "bus421" + busGenHead + "degrees";
                    int resId = getResources().getIdentifier(busIcon, "drawable", getPackageName());
                    for (Map.Entry<Bus, Marker> entry : oldMarkerHashMap.entrySet()) {
                        Bus key = entry.getKey();
                        //iterate through oldMarkerHashMap to find bus with same id
                        String keyId = key.getId();
                        if (keyId == busId) {
                            match = true;
                            break;
                        }
                    }
                    if (match == true) { ///EXISTING MARKER
                        Marker existingMarker = oldMarkerHashMap.get(b);
                        if (b.getHasStops()) {
                            Stop nextStop = b.getNextStop();
                            Date estimatedArrivalDate = nextStop.getEstimatedArrivalDate();
                            Date now = new Date();
                            String minToArrival = getETA(now, estimatedArrivalDate);
                            //existingMarker.setPosition(newLatLng);
                            //animateMarker(existingMarker, latlng, false);


                            existingMarker.remove();
                            Marker m = mMap.addMarker(new MarkerOptions()
                                    .position(latlng)
                                    .snippet(minToArrival)
                                    .title("Next Stop: " + nextStop.getStopName())
                                    .icon(BitmapDescriptorFactory.fromResource(resId)));
                            newMarkerHashMap.put(b, m);
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
                            busHashMap.remove(existingMarker);
                            busHashMap.put(m, b);

                        } else {

                            Marker m = mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(lat, lng))
                                    .title("No Schedule Available")
                                    .snippet(busType)
                                    .icon(BitmapDescriptorFactory.fromResource(resId)));
                            //mMap.addMarker(m);
                            //animateMarker(existingMarker, latlng, false);

                            existingMarker.remove();
                            newMarkerHashMap.put(b, m);
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
                            busHashMap.remove(existingMarker);
                            busHashMap.put(m, b);
                        }
                    } else {
                        if (b.getHasStops()) { //has schedule
                            Stop nextStop = b.getNextStop();
                            Date estimatedArrivalDate = nextStop.getEstimatedArrivalDate();
                            Date now = new Date();
                            String minToArrival = getETA(now, estimatedArrivalDate);
                            Marker m = mMap.addMarker(new MarkerOptions()
                                    .position(latlng)
                                    .snippet(minToArrival)
                                    .title("Next Stop: " + nextStop.getStopName())
                                    .icon(BitmapDescriptorFactory.fromResource(resId)));
                            newMarkerHashMap.put(b, m);
                            busHashMap.put(m, b);
                        } else {
                            Marker m = mMap.addMarker(new MarkerOptions()
                                    .position(latlng)
                                    .title("No Schedule Available")
                                    .snippet(busType)
                                    .icon(BitmapDescriptorFactory.fromResource(resId)));
                            newMarkerHashMap.put(b, m);
                            busHashMap.put(m, b);
                        }
                    }
                    match = false;

                }
            }
            for (Map.Entry<Bus, Marker> entry: oldMarkerHashMap.entrySet()) {
                //iterate through oldMarkerHashMap to find bus with same id
                Bus key = entry.getKey();
                oldMarkerHashMap.get(key).remove();
            }
            super.onPostExecute(newMarkerHashMap);
        }
    }

    public class RefreshMBTAMapAsync extends AsyncTask<HashMap<MBTABus, Marker>, String, HashMap<MBTABus, Marker>> {
        private JSONObject oldJSON;
        private JSONObject oldJSON57A;
        private HashMap<MBTABus, Marker> newMarkerHashMap = new HashMap<MBTABus, Marker>();
        private HashMap<MBTABus, Marker> oldMarkerHashMap = new HashMap<MBTABus, Marker>();
        private ArrayList<MBTABus> myMBTABusArray;

        protected void onPreExecute() {
            // Runs on the UI thread before doInBackground
            // Good for toggling visibility of a progress indicator
            super.onPreExecute();
        }

        private String convertInputStreamToString(InputStream inputStream) throws IOException {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            String result = "";
            while ((line = bufferedReader.readLine()) != null)
                result += line;
            inputStream.close();
            return result;

        }



        protected HashMap<MBTABus, Marker> doInBackground(HashMap<MBTABus, Marker>... params) {
            // Some long-running task like downloading an image.
            oldMarkerHashMap = params[0];
            InputStream inputStream = null;
            String result = "";
            String url = "http://realtime.mbta.com/developer/api/v2/vehiclesbyroute?api_key=iWPz9v7U3kORKZtdOjRsmw&route=57&format=json";
            String url57A = "http://realtime.mbta.com/developer/api/v2/vehiclesbyroute?api_key=iWPz9v7U3kORKZtdOjRsmw&route=57&format=json";
            try {
                HttpClient client = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(url);
                HttpResponse httpResponse = client.execute(httpGet);
                inputStream = httpResponse.getEntity().getContent();
                if (inputStream != null)
                    result = convertInputStreamToString(inputStream);
                else {
                    result = "Did not work!";
                    //DO NOTHING
                }
                JSONObject jsonObject = new JSONObject(result);
                if (jsonObject.equals(oldJSON)) {
                    result = oldJSON.toString();
                    //DO NOTHING
                } else if (jsonObject.length() == 0) {
                    result = oldJSON.toString();
                    //DO NOTHING
                } else {
                    oldJSON = jsonObject;
                    //JSONObject direction = jsonObject.getJSONObject("direction");
                    JSONArray direction = jsonObject.getJSONArray("direction");
                    result = direction.toString();
                }
                JSONArray mbtaDirectionsArray = new JSONArray(result);
                JSONObject mbtaOutboundArray = mbtaDirectionsArray.getJSONObject(0);
                JSONArray mbtaOutboundBusArray = mbtaOutboundArray.getJSONArray("trip");
                MBTABus myBuses = new MBTABus();
                myMBTABusArray = myBuses.fromJsonArray(mbtaOutboundBusArray);
                if (mbtaDirectionsArray.length()>0) {
                    JSONObject mbtaInboundArray = mbtaDirectionsArray.getJSONObject(1);
                    JSONArray mbtaInboundBusArray = mbtaInboundArray.getJSONArray("trip");
                    myMBTABusArray.addAll(myBuses.fromJsonArray(mbtaInboundBusArray));
                }


                ///START 57A PARSING
                httpGet = new HttpGet(url57A);
                httpResponse = client.execute(httpGet);
                inputStream = null;
                inputStream = httpResponse.getEntity().getContent();
                if (inputStream != null)
                    result = convertInputStreamToString(inputStream);
                else {
                    result = "Did not work!";
                    //DO NOTHING
                }
                JSONObject jsonObject57A = new JSONObject(result);
                if (jsonObject57A.equals(oldJSON57A)) {
                    result = oldJSON57A.toString();
                    //DO NOTHING
                } else if (jsonObject.length() == 0) {
                    result = oldJSON57A.toString();
                    //DO NOTHING
                } else {
                    oldJSON57A = jsonObject;
                    //JSONObject direction = jsonObject.getJSONObject("direction");
                    JSONArray direction = jsonObject57A.getJSONArray("direction");
                    result = direction.toString();
                }
                JSONArray mbtaDirectionsArray57A = new JSONArray(result);
                JSONObject mbtaOutboundArray57A = mbtaDirectionsArray57A.getJSONObject(0);
                JSONArray mbtaOutboundBusArray57A = mbtaOutboundArray57A.getJSONArray("trip");
                myMBTABusArray.addAll(myBuses.fromJsonArray(mbtaOutboundBusArray57A));

                if (mbtaDirectionsArray57A.length()>0) {
                    JSONObject mbtaInboundArray57A = mbtaDirectionsArray57A.getJSONObject(1);
                    JSONArray mbtaInboundBusArray57A = mbtaInboundArray57A.getJSONArray("trip");
                    myMBTABusArray.addAll(myBuses.fromJsonArray(mbtaInboundBusArray57A));
                }

            } catch (Exception e) {
                String msg = (e.getMessage() == null) ? "No data!" : e.getMessage();
                Log.i("Cannot get data", msg);
            }
            return newMarkerHashMap;
        }

        @Override
        protected void onPostExecute(HashMap<MBTABus, Marker> result) {
            // This method is executed in the UIThread
            // with access to the result of the long running task
            //imageView.setImageBitmap(result);
            // Hide the progress bar
            for (Object o : myMBTABusArray) {
                Boolean match = false;
                MBTABus b = (MBTABus) o;
                Float lat = Float.parseFloat(b.getLat());
                Float lng = Float.parseFloat(b.getLng());
                LatLng latlng = new LatLng(lat, lng);
                //String busType = b.getBusType();
                String busId = b.getVehicle_id();
                //for (Map.Entry<String, Integer> entry : map.entrySet())
                for (Map.Entry<MBTABus, Marker> entry: oldMarkerHashMap.entrySet()) {
                    MBTABus key = entry.getKey();
                    String keyId = key.getVehicle_id();
                    if (keyId == busId) {
                        match = true;
                        break;
                    }
                }
                if (match == true) { ///EXISTING MARKER
                    Marker existingMarker = oldMarkerHashMap.get(b);
                    existingMarker.remove();
                    Marker m = mMap.addMarker(new MarkerOptions()
                            .position(latlng)
                            .title(b.getTrip_headsign())
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_iconmonstr_location_icon_256_green)));
//                    animateMarker(existingMarker, latlng, false);

                    newMarkerHashMap.put(b, m);
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
                    mbtaBusHashMap.remove(existingMarker);
                    mbtaBusHashMap.put(m, b);
                } else { //NEW MARKER
                    Marker m = mMap.addMarker(new MarkerOptions()
                            .position(latlng)
                            .title(b.getTrip_headsign())
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_iconmonstr_location_icon_256_green)));
                    newMarkerHashMap.put(b, m);
                    mbtaBusHashMap.put(m, b);
                }

                match = false;

            }
            for (Map.Entry<MBTABus, Marker> entry: oldMarkerHashMap.entrySet())  {
                MBTABus key = entry.getKey();
                //iterate through oldMarkerHashMap to find bus with same id
                oldMarkerHashMap.get(key).remove();
            }
            super.onPostExecute(newMarkerHashMap);
        }
    }

    public void animateMarker(final Marker marker, final LatLng toPosition,
                              final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = mMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }

}
