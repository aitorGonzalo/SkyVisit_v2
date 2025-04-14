package com.example.aitordas_entrega2;

public class Lugar {

    private String nombre;
    private String urlFoto;
    private double latitud;
    private double longitud;

    public Lugar(String nombre, String urlFoto, double latitud, double longitud) {
        this.nombre = nombre;
        this.urlFoto = urlFoto;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUrlFoto() {
        return urlFoto;
    }

    public double getLatitud() {
        return latitud;
    }

    public double getLongitud() {
        return longitud;
    }
}
