package com.example.aitordas_entrega2;
import com.example.aitordas_entrega2.NetworkMonitorService;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.UserDictionary;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LugaresActivity extends AppCompatActivity {

    RecyclerView rvLugares;
    Button btnAñadirLugar;
    String servidor = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/agonzalo021/WEB/";
    LugarAdapter lugarAdapter;
    List<Lugar> lugarList = new ArrayList<>();
    private NetworkMonitorService servicioRed;
    private ServiceConnection conexionRed = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            servicioRed = ((NetworkMonitorService.MiBinder) service).obtenServicio();
            Log.d("LUGARES", "✅ Servicio de red enlazado");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            servicioRed = null;
            Log.d("LUGARES", "❌ Servicio de red desenlazado");
        }
    };
    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listado_lugares);

        rvLugares = findViewById(R.id.rvLugares);
        btnAñadirLugar = findViewById(R.id.btnAñadirLugar);
        Button btnVerGaleria = findViewById(R.id.btnVerGaleria); // 🔹 Añadido

        // Establecemos el layout manager para el RecyclerView
        rvLugares.setLayoutManager(new LinearLayoutManager(this));

        // Establecemos el adaptador
        lugarAdapter = new LugarAdapter(this, lugarList);
        rvLugares.setAdapter(lugarAdapter);

        btnAñadirLugar.setOnClickListener(v -> {
            // Aquí rediriges a la pantalla para añadir un nuevo lugar
            Intent intent = new Intent(LugaresActivity.this, SubirLugarActivity.class);
            startActivity(intent);
        });

        // 🔹 Acción del botón para ver las imágenes en la galería
        btnVerGaleria.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("image/*");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        // 👉 Solicitar permiso de notificaciones si estamos en Android 13 o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        101); // código de solicitud arbitrario
            }
        }

        // Llamar al servicio que obtiene los lugares desde la base de datos
        obtenerLugares();
        programarAlarma();
        forzarActualizacionWidget(this);
        // Enlazar el servicio de monitorización de red
        Intent intentServicio = new Intent(this, NetworkMonitorService.class);
        if (!isServiceRunning(this, NetworkMonitorService.class)) {
            ContextCompat.startForegroundService(this, new Intent(this, NetworkMonitorService.class));
        }

        bindService(intentServicio, conexionRed, Context.BIND_AUTO_CREATE);

    }


    private void forzarActualizacionWidget(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName widgetComponent = new ComponentName(context, SkyVisitWidget.class);
        int[] widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);

        if (widgetIds.length > 0) {
            Log.d("WIDGET", "🔁 Forzando actualización manual del widget");
            Intent intent = new Intent(context, SkyVisitWidget.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
            context.sendBroadcast(intent);
        }
    }

    private void obtenerLugares() {
        new Thread(() -> {
            try {
                Log.d("LUGAR_URL", "Iniciando la obtención de lugares...");
                URL url = new URL(servidor + "obtener_lugares.php");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                con.setRequestProperty("Accept", "application/json");
                con.setDoOutput(false);

                int code = con.getResponseCode();
                InputStream in = (code == 200) ? con.getInputStream() : con.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder resultado = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    resultado.append(line);
                }

                runOnUiThread(() -> {
                    try {
                        JSONArray lugares = new JSONArray(resultado.toString());

                        // 🔁 Limpiar lista actual antes de añadir nuevos lugares
                        lugarList.clear();

                        for (int i = 0; i < lugares.length(); i++) {
                            JSONObject lugar = lugares.getJSONObject(i);
                            String nombre = lugar.getString("nombre");
                            String urlFoto = lugar.getString("foto");
                            String urlCompleta = servidor + "fotos/" + urlFoto;

                            double latitud = lugar.getDouble("latitud");
                            double longitud = lugar.getDouble("longitud");

                            lugarList.add(new Lugar(nombre, urlCompleta, latitud, longitud));
                            Log.d("LUGAR_URL", "📍 Añadido: " + nombre);

                        }
                        SharedPreferences prefs = getSharedPreferences("skyvisit", MODE_PRIVATE);
                        prefs.edit().putInt("lugares_guardados", lugarList.size()).apply();
                        Log.d("WIDGET", "🧠 Total lugares guardados en prefs: " + lugarList.size());


                        // 🔄 Refrescar la vista
                        lugarAdapter.notifyDataSetChanged();

                    } catch (Exception e) {
                        Toast.makeText(LugaresActivity.this, "Error al procesar los lugares", Toast.LENGTH_LONG).show();
                        Log.e("LUGAR_URL", "Error procesando JSON: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(LugaresActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                Log.e("LUGAR_URL", "Error en conexión: " + e.getMessage());
            }
        }).start();
    }

    private void programarAlarma() {
        Log.d("ALARMA", "⏱ Verificando si ya existe una alarma de cercanía...");

        Intent intent = new Intent(this, LugaresAlarmReceiver.class);
        PendingIntent existingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);

        if (existingIntent != null) {
            Log.d("ALARMA", "⚠️ Ya hay una alarma de cercanía activa, no se vuelve a programar");
            return;
        }

        Log.d("ALARMA", "✅ Programando alarma de cercanía");

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        long tiempoInicio = System.currentTimeMillis() + 10_000;
        long intervalo = 10 * 1000;

        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                tiempoInicio,
                intervalo,
                pendingIntent);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso para notificaciones concedido", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permiso para notificaciones denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        obtenerLugares(); // 🔁 Refrescar lugares cada vez que se vuelve a esta pantalla
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conexionRed);
    }




}
