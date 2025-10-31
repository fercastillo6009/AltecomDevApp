package com.example.fondodepantalla.FragmentosAdministrador;

public class ResumenItem {
    private String nombre;
    private int asistencias;
    private int retardos;
    private int faltas;

    public ResumenItem(String nombre, int asistencias, int retardos, int faltas) {
        this.nombre = nombre;
        this.asistencias = asistencias;
        this.retardos = retardos;
        this.faltas = faltas;
    }

    public String getNombre() { return nombre; }
    public int getAsistencias() { return asistencias; }
    public int getRetardos() { return retardos; }
    public int getFaltas() { return faltas; }
}