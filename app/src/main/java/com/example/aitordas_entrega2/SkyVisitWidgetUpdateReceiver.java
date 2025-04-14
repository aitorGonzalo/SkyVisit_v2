package com.example.aitordas_entrega2;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SkyVisitWidgetUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("WIDGET", "⏰ Recibida alarma para actualizar el widget");
        // Forzar actualización del widget desde el sistema
        Intent updateIntent = new Intent(context, SkyVisitWidget.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        // Obtener todos los widgets activos
        int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                new ComponentName(context, SkyVisitWidget.class));
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

        // Lanzar la actualización
        context.sendBroadcast(updateIntent);
    }
}
