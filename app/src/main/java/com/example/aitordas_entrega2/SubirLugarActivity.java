package com.example.aitordas_entrega2;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class SubirLugarActivity extends AppCompatActivity {

    EditText etNombreLugar;
    Button btnTomarFoto, btnObtenerUbicacion, btnSubirLugar;
    ImageView ivFoto;
    TextView tvUbicacion;

    Bitmap fotoBitmap;
    double latitud = 0.0, longitud = 0.0;

    FusedLocationProviderClient proveedorLocalizacion;

    static final int REQUEST_FOTO = 1;
    static final int REQUEST_PERMISOS = 3;
    static final int REQUEST_GALERIA = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subir_lugar);

        etNombreLugar = findViewById(R.id.etNombreLugar);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        btnTomarFoto.setOnClickListener(v -> mostrarDialogoFoto());
        btnObtenerUbicacion = findViewById(R.id.btnObtenerUbicacion);
        btnSubirLugar = findViewById(R.id.btnSubirLugar);
        ivFoto = findViewById(R.id.ivFoto);
        tvUbicacion = findViewById(R.id.tvUbicacion);

        proveedorLocalizacion = LocationServices.getFusedLocationProviderClient(this);

        btnObtenerUbicacion.setOnClickListener(v -> obtenerUbicacion());
        btnSubirLugar.setOnClickListener(v -> subirLugar());
    }

    private void abrirCamara() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISOS);
        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, REQUEST_FOTO);
            }
        }
    }



    private void obtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_PERMISOS);
            return;
        }

        proveedorLocalizacion.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                latitud = location.getLatitude();
                longitud = location.getLongitude();
                tvUbicacion.setText("Latitud: " + latitud + ", Longitud: " + longitud);

                // 👇 Este es el print por consola (Logcat)
                android.util.Log.d("UBICACION", "Latitud: " + latitud + " / Longitud: " + longitud);
            } else {
                tvUbicacion.setText("Ubicación desconocida");
                android.util.Log.w("UBICACION", "Location es null");
            }
        });
    }



    private void subirLugar() {
        String nombre = etNombreLugar.getText().toString().trim();

        if (nombre.isEmpty()) {
            Toast.makeText(this, "Introduce un nombre", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fotoBitmap == null) {
            Toast.makeText(this, "Toma una foto primero", Toast.LENGTH_SHORT).show();
            return;
        }

        if (latitud == 0.0 && longitud == 0.0) {
            Toast.makeText(this, "Obtén la ubicación primero", Toast.LENGTH_SHORT).show();
            return;
        }
        SharedPreferences prefs = getSharedPreferences("carspotter", MODE_PRIVATE);
        int usuarioId = prefs.getInt("usuario_id", -1);
        if (usuarioId == -1) {
            Toast.makeText(this, "⚠️ No se encontró el ID del usuario. Inicia sesión de nuevo.", Toast.LENGTH_LONG).show();
            return;
        }
        Log.d("SUBIR_DEBUG", "ID recuperado de prefs: " + usuarioId);


        android.util.Log.d("SUBIDA_DEBUG", "Nombre: " + nombre);
        android.util.Log.d("SUBIDA_DEBUG", "Latitud: " + latitud);
        android.util.Log.d("SUBIDA_DEBUG", "Longitud: " + longitud);
        android.util.Log.d("SUBIDA_DEBUG", "FotoBitmap es null? " + (fotoBitmap == null));
        android.util.Log.d("SUBIDA_DEBUG", "Usuario ID: " + usuarioId);

        new Thread(() -> {
            try {
                // Convertir Bitmap a archivo JPEG temporal
                File fotoFile = File.createTempFile("foto_", ".jpg", getCacheDir());
                FileOutputStream fos = new FileOutputStream(fotoFile);
                fotoBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.close();

                String boundary = "SkyVisitBoundary" + System.currentTimeMillis();
                URL url = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/agonzalo021/WEB/subir_lugar.php");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                con.setDoOutput(true);

                DataOutputStream request = new DataOutputStream(con.getOutputStream());

                // Campos de texto
                escribirCampo(request, "nombre", nombre, boundary);
                escribirCampo(request, "latitud", String.valueOf(latitud), boundary);
                escribirCampo(request, "longitud", String.valueOf(longitud), boundary);
                escribirCampo(request, "usuario_id", String.valueOf(usuarioId), boundary);

                // Campo de archivo
                request.writeBytes("--" + boundary + "\r\n");
                request.writeBytes("Content-Disposition: form-data; name=\"foto\"; filename=\"foto.jpg\"\r\n");
                request.writeBytes("Content-Type: image/jpeg\r\n\r\n");

                FileInputStream inputStream = new FileInputStream(fotoFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    request.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                request.writeBytes("\r\n");

                // Fin del multipart
                request.writeBytes("--" + boundary + "--\r\n");
                request.flush();
                request.close();

                // Leer respuesta
                int code = con.getResponseCode();
                InputStream in = (code == 200) ? con.getInputStream() : con.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder resultado = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) resultado.append(line);
                reader.close();

                runOnUiThread(() -> {
                    if (code == 200) {
                        Toast.makeText(this, "✅ Lugar subido correctamente", Toast.LENGTH_LONG).show();
                        etNombreLugar.setText("");
                        ivFoto.setImageResource(0);
                        tvUbicacion.setText("Latitud: -, Longitud: -");
                        latitud = 0.0;
                        longitud = 0.0;
                        fotoBitmap = null;
                    } else {
                        Toast.makeText(this, "❌ Error: " + resultado.toString(), Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "❌ Error al subir: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
    private void escribirCampo(DataOutputStream out, String nombre, String valor, String boundary) throws IOException {
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + nombre + "\"\r\n\r\n");
        out.writeBytes(valor + "\r\n");
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == REQUEST_FOTO) {
                fotoBitmap = (Bitmap) data.getExtras().get("data");
                ivFoto.setImageBitmap(fotoBitmap);
            } else if (requestCode == REQUEST_GALERIA) {
                Uri imagenUri = data.getData();
                try {
                    fotoBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imagenUri);
                    ivFoto.setImageBitmap(fotoBitmap);
                } catch (IOException e) {
                    Toast.makeText(this, "Error al cargar imagen", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISOS) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) || permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Permisos de ubicación concedidos", Toast.LENGTH_SHORT).show();
                        obtenerUbicacion();
                    } else {
                        Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
                    }
                }

                if (permissions[i].equals(Manifest.permission.CAMERA)) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Permiso de cámara concedido", Toast.LENGTH_SHORT).show();
                        abrirCamara(); // vuelve a intentarlo
                    } else {
                        Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void mostrarDialogoFoto() {
        String[] opciones = {"📷 Tomar foto con cámara", "🖼️ Elegir imagen de galería"};

        new android.app.AlertDialog.Builder(this)
                .setTitle("Seleccionar imagen")
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        abrirCamara();
                    } else if (which == 1) {
                        abrirGaleria();
                    }
                })
                .show();
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALERIA);
    }



}
