package com.example.aitordas_entrega2;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MapaLugarActivity extends AppCompatActivity {

    private MapView map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configuración de osmdroid
        Configuration.getInstance().load(getApplicationContext(),
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        setContentView(R.layout.activity_mapa_lugar);

        // Obtener datos del intent
        String nombre = getIntent().getStringExtra("nombre");
        double lat = getIntent().getDoubleExtra("latitud", 0.0);
        double lon = getIntent().getDoubleExtra("longitud", 0.0);
        String urlFoto = getIntent().getStringExtra("urlFoto");

        // Configurar mapa
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        IMapController mapController = map.getController();
        mapController.setZoom(15.0);
        GeoPoint punto = new GeoPoint(lat, lon);
        mapController.setCenter(punto);

        // Añadir marcador
        Marker marker = new Marker(map);
        marker.setPosition(punto);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(nombre);
        marker.setOnMarkerClickListener((m, v) -> {
            Toast.makeText(this, nombre, Toast.LENGTH_SHORT).show();
            // Aquí podrías abrir un diálogo con la imagen del lugar si quieres
            return true;
        });
        map.getOverlays().add(marker);
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }
}
