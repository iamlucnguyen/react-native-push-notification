package com.dieam.reactnativepushnotification.modules;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dieam.reactnativepushnotification.modules.RNPushNotification.LOG_TAG;

public class RNPushNotificationPublisher extends BroadcastReceiver {
    final static String NOTIFICATION_ID = "notificationId";

    @Override
    public void onReceive(final Context context, Intent intent) {
        int id = intent.getIntExtra(NOTIFICATION_ID, 0);
        long currentTime = System.currentTimeMillis();
        final Bundle bundle = intent.getExtras();
        Bundle stockBundle = bundle;

        Log.i(LOG_TAG, "NotificationPublisher: Prepare To Publish: " + id + ", Now Time: " + currentTime);

        //Check storage luc logout, neu ok moi goi
        Application applicationContext = (Application) context.getApplicationContext();
        RNPushNotificationHelper pushNotificationHelper = new RNPushNotificationHelper(applicationContext);

        Log.v(LOG_TAG, "onMessageReceived: " + bundle);

        bundle.putDouble("fireDate", new Date().getTime() + 20000);

        AsyncStorage as = new AsyncStorage(context);
        String mobileAuthorize = as.getAsyncStorage("MobileAuthorize");
        String portalToken = as.getAsyncStorage("PortalToken");
        String mayChamCong = as.getAsyncStorage("MAY_CHAM_CONG_WIFI");
        String baseURL = "";

        try {
            JSONObject obj = new JSONObject(mobileAuthorize);
            baseURL = obj.getString("PORTAL_API_URL");

        } catch (Throwable t) {
            Log.e("My App", "Could not parse malformed JSON");
        }

        if(portalToken.length() == 0)
            return;

        pushNotificationHelper.cancelAllScheduledNotifications();
        pushNotificationHelper.sendNotificationScheduled(bundle);

        if(mayChamCong.length() == 0)
            return;

        DbHandler dbHandler = new DbHandler(context);

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        String lastCheck = "";
        String lastWifi = "";
        int idLastCheck = -1;
        HashMap<String, String> lastCheckData = dbHandler.getLastCheck();


        if(!lastCheckData.isEmpty()) {
            lastCheck = lastCheckData.get("status");
            idLastCheck = Integer.parseInt(lastCheckData.get("id"));
            lastWifi = lastCheckData.get("wifi");
        }

        if (activeNetwork != null) {
            // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wifiManager.getConnectionInfo();
                String ssid  = info.getSSID().replace("\"", "");
                List<String> ssidList = new ArrayList<>();
                String[] wifis = mayChamCong.split("\\|");

                for(String item: wifis)
                    ssidList.add(item);

                if(ssidList.contains(ssid)) {
                    if(lastCheck.equals("OUT") || lastCheck.equals("")) {
                        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

                        dbHandler.insertINOUT(time, "IN", date, ssid);
                        Bundle notiBundle = getBundleNotificationInOut(stockBundle, "IN", time);
                        handleLocalNotification(context, notiBundle);

                        if(lastCheck.equals(""))
                            dbHandler.insertLastCheck("IN", ssid);
                        else if(idLastCheck != -1)
                            dbHandler.updateLastCheck(idLastCheck, "IN", ssid);
                    }
                }

                doPost(context, baseURL, portalToken);

            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {

                if(lastCheck.equals("IN")) {
                    String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                    String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

                    dbHandler.insertINOUT(time, "OUT", date, lastWifi);
                    Bundle notiBundle = getBundleNotificationInOut(stockBundle, "OUT", time);
                    handleLocalNotification(context, notiBundle);

                    if(idLastCheck != -1)
                        dbHandler.updateLastCheck(idLastCheck, "OUT", lastWifi);
                }

                doPost(context, baseURL, portalToken);
            }
        } else {
            if(lastCheck.equals("IN")) {
                String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

                dbHandler.insertINOUT(time, "OUT", date, lastWifi);
                Bundle notiBundle = getBundleNotificationInOut(stockBundle, "OUT", time);
                handleLocalNotification(context, notiBundle);

                if(idLastCheck != -1)
                    dbHandler.updateLastCheck(idLastCheck, "OUT", lastWifi);
            }
        }
    }

    private Bundle getBundleNotificationInOut(Bundle bundle, String status, String time) {
        String currentStatus = status.equals("IN") ? "VÀO" : "RA";
        bundle.putString("title","Chấm công Wifi");
        bundle.putString("message","Bạn đã chấm công " + currentStatus + " lúc " + time);
        SecureRandom randomNumberGenerator = new SecureRandom();
        bundle.putString("id", String.valueOf(randomNumberGenerator.nextInt()));

        return bundle;
    }

    private void doPost(Context context, String url, final String token) {
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        JSONObject jsonObject = new JSONObject();
        final DbHandler dbHandler = new DbHandler(context);

        String baseURL = url + "api/DiemDanh/DoDiemDanh";

        final ArrayList<HashMap<String, String>> dataList = dbHandler.GetINOUT();

        for (HashMap<String, String> item : dataList) {
            try {
                jsonObject.put("DATA_ID", "");
                jsonObject.put("NHANVIEN_ID", "");
                jsonObject.put("MA_CHAMCONG", "");
                jsonObject.put("NGAY", item.get("date"));
                jsonObject.put("GIO", item.get("time"));
                jsonObject.put("IN_OUT", item.get("status"));
                jsonObject.put("MAY_CHAMCONG", item.get("type"));
                jsonObject.put("ID_TOCHUC", "");
                jsonObject.put("NGAY_INSERT", null);
                jsonObject.put("Latitude", null);
                jsonObject.put("Longitude", null);
                jsonObject.put("VI_TRI", null);
                jsonObject.put("HINH_ANH", null);

                final int id = Integer.valueOf(item.get("id"));

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, baseURL, jsonObject, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if(response.getBoolean("Data")) {
                                Log.i("SSIDne", String.valueOf(id));
                                dbHandler.delete(id);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                }) {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        final Map<String, String> headers = new HashMap<>();
                        headers.put("Content-Type", "application/json");
                        headers.put("Portal-Token", token);
                        headers.put("Language", "vi-VN");
                        return headers;
                    }
                };

                requestQueue.add(jsonObjectRequest);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleLocalNotification(Context context, Bundle bundle) {

        // If notification ID is not provided by the user for push notification, generate one at random
        if (bundle.getString("id") == null) {
            SecureRandom randomNumberGenerator = new SecureRandom();
            bundle.putString("id", String.valueOf(randomNumberGenerator.nextInt()));
        }

        Application applicationContext = (Application) context.getApplicationContext();
        RNPushNotificationHelper pushNotificationHelper = new RNPushNotificationHelper(applicationContext);
        
        Log.v(LOG_TAG, "sendNotification: " + bundle);

        pushNotificationHelper.sendToNotificationCentre(bundle);
    }
}