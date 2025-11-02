package com.example.fondodepantalla;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Task {
    private String descripcion;

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    private String id;      // Este es el ID del documento en Firestore
    private String name;    // Nombre de la tarea
    private boolean taken;  // Estado de si fue tomada o no
    private String user;    // UID del usuario que tomó la tarea

    // Constructor vacío requerido por Firestore
    public Task() { }

    // Constructor completo opcional
    public Task(String name, boolean taken, String user) {
        this.name = name;
        this.taken = taken;
        this.user = user;
    }

    // Getter y Setter para el ID
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    // Getter y Setter para el nombre
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    // Getter y Setter para taken
    public boolean isTaken() {
        return taken;
    }
    public void setTaken(boolean taken) {
        this.taken = taken;
    }

    // Getter y Setter para user
    public String getUser() {
        return user;
    }
    public void setUser(String user) {
        this.user = user;
    }

    // Método opcional para imprimir la tarea en logs
    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", taken=" + taken +
                ", user='" + user + '\'' +
                '}';
    }
}
