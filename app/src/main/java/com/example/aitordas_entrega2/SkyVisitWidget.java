package com.example.aitordas_entrega2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.widget.RemoteViews;
import android.util.Log;
import android.widget.Toast;

public class SkyVisitWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d("WIDGET", "🔄 Actualizando widget...");

        SharedPreferences prefs = context.getSharedPreferences("skyvisit", Context.MODE_PRIVATE);
        int totalLugares = prefs.getInt("lugares_guardados", 0);
        int totalImagenes = contarImagenesEnGaleria(context);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_skyvisit);

            // ✅ Cambiar IDs a los correctos según el nuevo layout
            views.setTextViewText(R.id.tvWidgetTextoLugares, "🌍 Lugares: " + totalLugares);
            views.setTextViewText(R.id.tvWidgetTextoImagenes, "🖼️ Imágenes: " + totalImagenes);

            // Clic para abrir LugaresActivity (lo puedes asociar al layout entero o a uno de los TextView)
            Intent intent = new Intent(context, LugaresActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.tvWidgetTextoLugares, pendingIntent);
            views.setOnClickPendingIntent(R.id.tvWidgetTextoImagenes, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
        asegurarAlarmaActiva(context);

    }

    private void asegurarAlarmaActiva(Context context) {
        Log.d("WIDGET", "🛠 Verificando si la alarma del widget está activa");

        Intent intent = new Intent(context, SkyVisitWidgetUpdateReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 1234, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);

        if (pendingIntent == null) {
            Log.d("WIDGET", "⚠️ No había alarma activa. Reprogramando.");
            PendingIntent nuevoIntent = PendingIntent.getBroadcast(
                    context, 1234, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.setInexactRepeating(
                    AlarmManager.RTC,
                    System.currentTimeMillis() + 5000,
                    30 * 1000,
                    nuevoIntent
            );
        } else {
            Log.d("WIDGET", "✅ La alarma del widget ya estaba activa");
        }
    }

    private int contarImagenesEnGaleria(Context context) {
        String selection = android.provider.MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
        String[] selectionArgs = new String[]{"Pictures/SkyVisit%"};

        Cursor cursor = context.getContentResolver().query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null,
                selection,
                selectionArgs,
                null
        );

        int total = 0;
        if (cursor != null) {
            total = cursor.getCount();
            cursor.close();
        }

        Log.d("WIDGET", "🖼️ Imágenes encontradas en galería: " + total);
        return total;
    }

    @Override
    public void onEnabled(Context context) {
        Log.d("WIDGET", "✅ Widget habilitado: creando alarma de actualización");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);


        Intent intent = new Intent(context, SkyVisitWidgetUpdateReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 1234, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent); // <-- antes de setInexactRepeating

        alarmManager.setInexactRepeating(
                AlarmManager.RTC,
                System.currentTimeMillis() + 5000,
                30 * 1000,
                pendingIntent
        );
    }


    @Override
    public void onDisabled(Context context) {
        Log.d("WIDGET", "🛑 Widget deshabilitado: cancelando alarma");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SkyVisitWidgetUpdateReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 1234, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        alarmManager.cancel(pendingIntent);
    }


}
