package com.example.aitordas_entrega2;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.io.OutputStream;
import java.net.URL;
import java.util.List;

public class LugarAdapter extends RecyclerView.Adapter<LugarAdapter.LugarViewHolder> {

    private Context context;
    private List<Lugar> lugarList;
    private SharedPreferences prefs;

    public LugarAdapter(Context context, List<Lugar> lugarList) {
        this.context = context;
        this.lugarList = lugarList;
        this.prefs = context.getSharedPreferences("galeria_lugares", Context.MODE_PRIVATE);
    }

    @Override
    public LugarViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.item_lugar, parent, false);
        return new LugarViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(LugarViewHolder holder, int position) {
        Lugar lugar = lugarList.get(position);

        holder.nombreLugar.setText(lugar.getNombre());
        holder.latitud.setText("Latitud: " + lugar.getLatitud());
        holder.longitud.setText("Longitud: " + lugar.getLongitud());
        Picasso.get().load(lugar.getUrlFoto()).into(holder.imagenLugar);

        boolean estaGuardado = prefs.getBoolean(lugar.getNombre(), false);
        holder.btnGuardar.setVisibility(estaGuardado ? View.GONE : View.VISIBLE);
        holder.btnEliminar.setVisibility(estaGuardado ? View.VISIBLE : View.GONE);

        holder.btnGuardar.setOnClickListener(v -> {
            guardarEnGaleria(lugar);
            prefs.edit().putBoolean(lugar.getNombre(), true).apply();
            holder.btnGuardar.setVisibility(View.GONE);
            holder.btnEliminar.setVisibility(View.VISIBLE);
        });

        holder.btnEliminar.setOnClickListener(v -> {
            eliminarDeGaleria(lugar.getNombre());
            prefs.edit().remove(lugar.getNombre()).apply();
            holder.btnGuardar.setVisibility(View.VISIBLE);
            holder.btnEliminar.setVisibility(View.GONE);
        });
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MapaLugarActivity.class);
            intent.putExtra("nombre", lugar.getNombre());
            intent.putExtra("latitud", lugar.getLatitud());
            intent.putExtra("longitud", lugar.getLongitud());
            intent.putExtra("urlFoto", lugar.getUrlFoto());
            context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return lugarList.size();
    }

    public static class LugarViewHolder extends RecyclerView.ViewHolder {
        TextView nombreLugar, latitud, longitud;
        ImageView imagenLugar;
        Button btnGuardar, btnEliminar;

        public LugarViewHolder(View itemView) {
            super(itemView);
            nombreLugar = itemView.findViewById(R.id.tvNombreLugar);
            latitud = itemView.findViewById(R.id.tvLatitud);
            longitud = itemView.findViewById(R.id.tvLongitud);
            imagenLugar = itemView.findViewById(R.id.ivLugar);
            btnGuardar = itemView.findViewById(R.id.btnGuardarGaleria);
            btnEliminar = itemView.findViewById(R.id.btnEliminarGaleria);
        }
    }

    private void guardarEnGaleria(Lugar lugar) {
        new Thread(() -> {
            try {
                URL url = new URL(lugar.getUrlFoto());
                Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                String nombreArchivo = lugar.getNombre() + "_" + System.currentTimeMillis() + ".jpg";

                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, nombreArchivo);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SkyVisit");

                Uri uri = context.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                );

                if (uri != null) {
                    OutputStream fos = context.getContentResolver().openOutputStream(uri);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();
                    Log.d("GALERIA", "✅ Guardado en galería: " + nombreArchivo);
                }
            } catch (Exception e) {
                Log.e("GALERIA", "❌ Error al guardar: " + e.getMessage());
            }
        }).start();
    }

    private void eliminarDeGaleria(String nombreLugar) {
        String selection = MediaStore.Images.Media.DISPLAY_NAME + " LIKE ?";
        String[] selectionArgs = { nombreLugar + "%" };

        int deleted = context.getContentResolver().delete(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                selection,
                selectionArgs
        );
        Log.d("GALERIA", "🗑️ Eliminadas " + deleted + " imágenes de " + nombreLugar);
    }
}
