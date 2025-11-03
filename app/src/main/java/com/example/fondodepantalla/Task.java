package com.example.fondodepantalla;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Task {
    private String descripcion;
    private String id;
    private String name;
    private boolean taken;
    private String user;
    private String uidAsignado;

    // TIPOS CORRECTOS SEGÚN FIRESTORE
    private Boolean confirmacionExito;  // Boolean en lugar de String
    private String firmaCliente;         // String está bien
    private Long progress;               // Long si Firestore lo guarda como número

    public Task() { }

    // Getters y setters
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isTaken() { return taken; }
    public void setTaken(boolean taken) { this.taken = taken; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public Boolean getConfirmacionExito() { return confirmacionExito; }
    public void setConfirmacionExito(Boolean confirmacionExito) { this.confirmacionExito = confirmacionExito; }

    public String getFirmaCliente() { return firmaCliente; }
    public void setFirmaCliente(String firmaCliente) { this.firmaCliente = firmaCliente; }

    public Long getProgress() { return progress; }
    public void setProgress(Long progress) { this.progress = progress; }
    public String getUidAsignado() { return uidAsignado; }
    public void setUidAsignado(String uidAsignado) { this.uidAsignado = uidAsignado; }
}
