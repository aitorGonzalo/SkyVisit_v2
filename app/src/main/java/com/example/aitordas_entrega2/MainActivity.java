package com.example.aitordas_entrega2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    EditText etUsuario, etPassword;
    TextView tvResultado;
    String servidor = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/agonzalo021/WEB/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etUsuario = findViewById(R.id.etUsuario);
        etPassword = findViewById(R.id.etPassword);
        tvResultado = findViewById(R.id.tvResultado);

        findViewById(R.id.btnRegistrar).setOnClickListener(v -> ejecutarServicio("registro.php"));
        findViewById(R.id.btnLogin).setOnClickListener(v -> ejecutarServicio("login.php"));
    }

    void ejecutarServicio(String script) {
        new Thread(() -> {
            try {
                URL url = new URL(servidor + script);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                con.setRequestProperty("Accept", "application/json");
                con.setDoOutput(true);

                String usuario = etUsuario.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                JSONObject json = new JSONObject();
                json.put("usuario", usuario);
                json.put("password", password);

                OutputStream os = con.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(json.toString());
                writer.flush();
                writer.close();
                os.close();

                int code = con.getResponseCode();
                InputStream in = (code == 200) ? con.getInputStream() : con.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder resultado = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) resultado.append(line);

                runOnUiThread(() -> {
                    // Filtrado en el Logcat para depuración
                    android.util.Log.d("LOGIN_RESPONSE", resultado.toString()); // Muestra la respuesta del servidor

                    try {
                        JSONObject respuesta = new JSONObject(resultado.toString());

                        if (respuesta.has("resultado") && respuesta.getString("resultado").equals("ok")) {
                            String mensaje = script.equals("registro.php") ?
                                    "✅ Registro completado correctamente" :
                                    "✅ Inicio de sesión correcto";

                            if (respuesta.has("id")) {
                                int usuarioId = respuesta.getInt("id");
                                Log.d("LOGIN_DEBUG", "ID guardado en prefs: " + usuarioId);

                                SharedPreferences prefs = getSharedPreferences("carspotter", MODE_PRIVATE);
                                prefs.edit().putInt("usuario_id", usuarioId).apply();
                            }
                            Toast.makeText(MainActivity.this, mensaje, Toast.LENGTH_LONG).show();
                            if (script.equals("login.php")) {
                                // Redirigir a la actividad de Lugares si es login
                                Intent intent = new Intent(MainActivity.this, LugaresActivity.class);
                                startActivity(intent);
                            }
                        } else if (respuesta.has("error")) {
                            Toast.makeText(MainActivity.this, "❌ " + respuesta.getString("error"), Toast.LENGTH_LONG).show();
                        }

                    } catch (Exception ex) {
                        // Si ocurre un error en la respuesta JSON, lo muestra en Logcat
                        android.util.Log.e("LOGIN_ERROR", "Error al procesar la respuesta: " + ex.getMessage());
                        Toast.makeText(MainActivity.this, "Error al procesar la respuesta", Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                // Si ocurre un error en la conexión o procesamiento, lo muestra en Logcat
                android.util.Log.e("LOGIN_ERROR", "Error: " + e.getMessage());
                runOnUiThread(() -> tvResultado.setText("Error: " + e.getMessage()));
            }
        }).start();
    }
}
