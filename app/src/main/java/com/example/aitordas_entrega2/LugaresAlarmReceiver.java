package com.example.aitordas_entrega2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.aitordas_entrega2.Lugar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class LugaresAlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "cerca_lugares";
    private static final double DISTANCIA_MAX_METROS = 100;


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ALARMA", "⏰ Alarma activada");

        // Obtener ubicación actual
        FusedLocationHelper.getLastLocation(context, location -> {
            if (location == null) {
                Log.e("ALARMA", "❌ No se pudo obtener la ubicación");
                return;
            }

            Log.d("ALARMA", "📍 Ubicación actual: " + location.getLatitude() + ", " + location.getLongitude());

            // Consultar lugares desde el servidor
            new Thread(() -> {
                try {
                    Log.d("ALARMA", "🌐 Consultando lugares desde el servidor...");
                    URL url = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/agonzalo021/WEB/obtener_lugares.php");
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) result.append(line);

                    JSONArray lugares = new JSONArray(result.toString());
                    Log.d("ALARMA", "✅ " + lugares.length() + " lugares recibidos");

                    boolean hayCercano = false;

                    for (int i = 0; i < lugares.length(); i++) {
                        JSONObject obj = lugares.getJSONObject(i);
                        double lat = obj.getDouble("latitud");
                        double lon = obj.getDouble("longitud");
                        String nombre = obj.getString("nombre");

                        float[] resultados = new float[1];
                        Location.distanceBetween(
                                location.getLatitude(), location.getLongitude(),
                                lat, lon, resultados
                        );

                        Log.d("ALARMA", "📏 Distancia a " + nombre + ": " + resultados[0] + " m");

                        if (resultados[0] <= DISTANCIA_MAX_METROS) {
                            Log.d("ALARMA", "📢 Estás cerca de " + nombre);
                            lanzarNotificacion(context, nombre);
                            hayCercano = true;
                            break;
                        }
                    }

                    if (!hayCercano) {
                        Log.d("ALARMA", "🔕 No hay lugares a menos de 100 metros.");
                    }

                } catch (Exception e) {
                    Log.e("ALARMA", "❌ Error al consultar lugares: " + e.getMessage());
                }
            }).start();
        });
    }


    private void lanzarNotificacion(Context context, String nombreLugar) {
        crearCanalNotificaciones(context);

        // ✅ COMPROBAR PERMISO PARA NOTIFICACIONES
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            Log.e("NOTIF", "Permiso de notificación denegado. No se mostrará.");
            return; // 🚫 Salir sin mostrar notificación
        }

        Intent intent = new Intent(context, LugaresActivity.class);
        PendingIntent pendingIntent = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(intent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_location)
                .setContentTitle("¡Estás cerca de un lugar!")
                .setContentText("Estás a menos de 100m de: " + nombreLugar)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(1001, builder.build());
    }


    private void crearCanalNotificaciones(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Lugares cercanos";
            String description = "Notificaciones de lugares cercanos";
            int importance = NotificationManager.IMPORTANCE_HIGH; // 🔊 Esto permite sonido

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // ✅ Activar sonido
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build());

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}
