package com.bubus.steveh.bubustracker;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;


/**
 * Created by steveh on 8/4/16.
 */

public class AsyncTaskService extends Service {

    private static final String TAG = "AsyncTaskService";
    public static final String BROADCAST_ACTION = "com.bubus.steveh.bubustracker";
    private final Handler handler = new Handler();
    private ArrayList<Bus> busArray;
    Intent intent;
    int counter = 0;

    public AsyncTaskService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        intent = new Intent(BROADCAST_ACTION);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        handler.removeCallbacks(sendUpdatesToUI);
        handler.postDelayed(sendUpdatesToUI, 1000); // 1 second

    }

    private Runnable sendUpdatesToUI = new Runnable() {
        public void run() {
            getBusInfo();
            handler.postDelayed(this, 5000); // 5 seconds
        }
    };


    @Override
    public void onDestroy() {
        handler.removeCallbacks(sendUpdatesToUI);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class ParseBusInfo extends AsyncTask<String, Void, ArrayList<Bus>> {
        private ArrayList<Bus> existingBuses;
        private String test;


        public void setExistingBuses(ArrayList<Bus> buses) {
            existingBuses = buses;
        }

        public void setTestString(String testString) {
            test = testString;
        }

        protected ArrayList<Bus> doInBackground(String... rawJson) {
            ArrayList<Bus> busArray = new ArrayList<Bus>();
            try {
                JSONObject allData = new JSONObject(rawJson[0]); // convert string to JSONObject
                Integer totalResultsAvailable = Integer.parseInt(allData.getString("totalResultsAvailable"));
                Integer isMissingResults = Integer.parseInt(allData.getString("isMissingResults"));
                if (totalResultsAvailable == 0) {
                    Integer n = 0; //debug
                } else if (isMissingResults == 1) {
                    Integer n = 0;
                } else {
                    JSONObject resultSet = allData.getJSONObject("ResultSet");
                    JSONArray buses = resultSet.getJSONArray("Result");
                    Bus currentBuses = new Bus();
                    busArray = currentBuses.fromJsonArray(buses); // parsing done. busArray is array of Bus objects
                    return busArray;
                }
            } catch (JSONException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
            if (existingBuses != null && existingBuses.size() == 0) {
                return busArray;
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<Bus> newBuses) {
            this.existingBuses = newBuses;
            intent.putExtra("time", "from AsyncTaskService");
            intent.putExtra("busArray",newBuses );
            sendBroadcast(intent);
        }
    }


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
                        ParseBusInfo logic = new ParseBusInfo();
                        logic.setExistingBuses(busArray);
                        logic.setTestString("HIIIIII");
                        logic.execute(response);
                        //new ParseBusInfo().execute(response);
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


}