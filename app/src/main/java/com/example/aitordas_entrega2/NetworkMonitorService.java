package com.example.aitordas_entrega2;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class NetworkMonitorService extends Service {

    private final IBinder elBinder = new MiBinder();
    private NetworkReceiver receiver;
    private static final String CHANNEL_ID = "canal_red";
    private static final int NOTIF_ID = 2024;

    public class MiBinder extends Binder {
        public NetworkMonitorService obtenServicio() {
            return NetworkMonitorService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return elBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        crearCanalNotificaciones();
        lanzarNotificacionForeground();

        // Registrar receptor de cambios de red
        receiver = new NetworkReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(receiver, filter);

        Log.d("NETWORK_SERVICE", "Servicio creado y receptor registrado");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        Log.d("NETWORK_SERVICE", "Servicio destruido y receptor anulado");
    }

    private void lanzarNotificacionForeground() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SkyVisit: Monitorizando red")
                .setContentText("Este servicio detecta si pierdes conexión a Internet")
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setOngoing(true)
                .build();

        startForeground(NOTIF_ID, notification);
    }

    private void crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Canal Red";
            String description = "Avisos de pérdida de conexión";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean conectado = hayInternet(context);
            Log.d("NETWORK_RECEIVER", "¿Conectado?: " + conectado);

            if (!conectado) {
                lanzarNotificacionDesconexion(context);
            }

        }

        private boolean hayInternet(Context context) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                Network network = cm.getActiveNetwork();
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                return capabilities != null &&
                        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
            }
            return false;
        }

        private void lanzarNotificacionDesconexion(Context context) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setContentTitle("SkyVisit")
                    .setContentText("¡Sin conexión a Internet!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                            == PackageManager.PERMISSION_GRANTED) {

                NotificationManagerCompat.from(context).notify(999, builder.build());

            } else {
                Log.w("NOTIF", "❌ No se tiene permiso para mostrar notificaciones");
            }

        }


    }


}
