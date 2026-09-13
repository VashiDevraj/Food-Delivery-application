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
import com.example.fooddeliveryapp.activities.AdminOrdersActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FoodDeliveryFCMService extends FirebaseMessagingService {

    private static final String CHANNEL_ORDERS   = "order_notifications";
    private static final String CHANNEL_GENERAL  = "general_notifications";
    private static final int    NOTIF_ID_ORDER   = 1001;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        String title = "New Order 🛒";
        String body  = "A new order has been placed!";

        if (message.getNotification() != null) {
            if (message.getNotification().getTitle() != null)
                title = message.getNotification().getTitle();
            if (message.getNotification().getBody() != null)
                body  = message.getNotification().getBody();
        }

        // Also read data payload for richer content
        if (message.getData().containsKey("orderId")) {
            String orderId = message.getData().get("orderId");
            body = "Order #" + (orderId != null ? orderId.substring(0, 8) : "")
                    + " just arrived!";
        }

        sendOrderNotification(title, body);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Save token to Firebase under admin node for server-side targeting
        saveTokenToFirebase(token);
    }

    // ── Send notification ──────────────────────────────────────────────────

    public static void sendOrderNotification(Context context, String title, String body) {

        Intent intent = new Intent(context, AdminOrdersActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ORDERS)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ORDERS,
                    "Order Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void sendOrderNotification(String title, String body) {
        sendOrderNotification(this, title, body);
    }

    // ── Save FCM token ─────────────────────────────────────────────────────

    private void saveTokenToFirebase(String token) {
        FirebaseDatabase.getInstance().getReference()
                .child("adminFcmTokens")
                .child("token")
                .setValue(token);
    }
}