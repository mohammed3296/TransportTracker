package com.mohammedabdullah3296.transporttracker;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class TrackerService extends Service {

    private static final String TAG = "TrackerService";


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        buildNotification();
        loginToFirebase();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void buildNotification() {

    //    Toast.makeText(this, "buildNotification", Toast.LENGTH_SHORT).show();
        String stop = "stop";
        registerReceiver(stopReceiver, new IntentFilter(stop));
        PendingIntent broadcastIntent = PendingIntent.getBroadcast(
                this, 0, new Intent(stop), PendingIntent.FLAG_UPDATE_CURRENT);
        // Create the persistent notification


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            //Toast.makeText(this, "O", Toast.LENGTH_SHORT).show();
            startMyOwnForeground(broadcastIntent);

        }
        else {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.notification_text))
                    .setOngoing(true)
                    .setContentIntent(broadcastIntent)
                    .setSmallIcon(R.drawable.placeholder);
            startForeground(1, builder.build());
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyOwnForeground(PendingIntent broadcastIntent) {
        String NOTIFICATION_CHANNEL_ID = "com.mohammedabdullah3296.transporttracker";
        String channelName = "My Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_text))
                .setOngoing(true)
                .setContentIntent(broadcastIntent)
                .setSmallIcon(R.drawable.placeholder)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }

    protected BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "received stop broadcast");
            // Stop the service when the notification is tapped
            unregisterReceiver(stopReceiver);
            stopSelf();
        }
    };

    private void loginToFirebase() {
        requestLocationUpdates(FirebaseAuth.getInstance().getCurrentUser().getUid());
    }

    private void requestLocationUpdates(String uId)   {

       // Toast.makeText(this, "requestLocationUpdates", Toast.LENGTH_SHORT).show();
        LocationRequest request = new LocationRequest();
        request.setInterval(10000);
        request.setFastestInterval(5000);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);


        double randomDouble = Math.random();
        randomDouble = randomDouble * 50 + 1;
        int randomInt = (int) randomDouble;

        final String path = getString(R.string.firebase_path)  + "/" + uId  ; // ;getString(R.string.transport_id);
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            // Request location updates and when an update is
            // received, store the location in Firebase
            client.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference myRef = database.getReference(path);
                    Location location = locationResult.getLastLocation();
                    if (location != null) {

                        Toast.makeText(TrackerService.this,     location.getLatitude() + " -- " + location.getLongitude(), Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "location: " + location.getLatitude() + " -- " + location.getLongitude());
                        myRef.setValue(location);
                    }
                }
            }, null);
        }
    }

}