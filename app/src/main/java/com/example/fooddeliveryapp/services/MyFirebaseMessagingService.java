package com.example.fooddeliveryapp.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.activities.DeliveryBoyDashboardActivity;
import com.example.fooddeliveryapp.utils.Constants;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * MyFirebaseMessagingService
 *
 * Handles FCM push notifications for delivery boys:
 * - App in foreground: shows heads-up notification
 * - App in background / killed: FCM system tray notification auto-displayed
 *
 * Register in AndroidManifest.xml:
 *   <service android:name=".services.MyFirebaseMessagingService"
 *            android:exported="false">
 *       <intent-filter>
 *           <action android:name="com.google.firebase.MESSAGING_EVENT" />
 *       </intent-filter>
 *   </service>
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID   = "delivery_orders";
    private static final String CHANNEL_NAME = "Delivery Orders";
    private static final int    NOTIF_ID     = 1001;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "New Order 🛵";
        String body  = "You have a new delivery order!";

        // Extract notification payload
        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null)
                title = remoteMessage.getNotification().getTitle();
            if (remoteMessage.getNotification().getBody() != null)
                body  = remoteMessage.getNotification().getBody();
        }

        // Extract data payload
        String orderId = remoteMessage.getData().get("orderId");

        showNotification(title, body, orderId);
    }

    /**
     * Called when FCM generates a new registration token (e.g. after reinstall).
     * Save the new token to Firebase so admins can send future notifications.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        // Save updated token to Firebase for the current delivery boy
        com.example.fooddeliveryapp.utils.SessionManager sm =
                new com.example.fooddeliveryapp.utils.SessionManager(this);

        if (sm.isLoggedIn() && sm.getRole() != null
                && sm.getRole().equals(Constants.ROLE_DELIVERY_BOY)) {

            String uid = sm.getUid();
            if (uid != null && !uid.isEmpty()) {
                FirebaseDatabase.getInstance(Constants.FIREBASE_URL)
                        .getReference()
                        .child(Constants.NODE_DELIVERY_BOYS)
                        .child(uid)
                        .child(Constants.NODE_PROFILE)
                        .child("fcmToken")
                        .setValue(token);
            }
        }
    }

    private void showNotification(String title, String body, String orderId) {
        // Tapping the notification opens delivery dashboard
        Intent intent = new Intent(this, DeliveryBoyDashboardActivity.class);
        if (orderId != null && !orderId.isEmpty()) {
            intent.putExtra("orderId", orderId);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)   // add ic_notification drawable
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setSound(soundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Android 8+ requires a notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Delivery order assignments and updates");
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }

        manager.notify(NOTIF_ID, builder.build());
    }
}