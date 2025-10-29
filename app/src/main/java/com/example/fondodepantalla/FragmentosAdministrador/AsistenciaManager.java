package com.example.fondodepantalla.FragmentosAdministrador;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AsistenciaManager {

    private final FirebaseFirestore db;
    private final String uid;

    public AsistenciaManager(FirebaseFirestore db, String uid) {
        this.db = db;
        this.uid = uid;
    }

    // Registrar entrada
    public void registrarEntrada(Map<String, Object> data, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        String fecha = data.get("fecha").toString();
        db.collection("asistencias")
                .document(uid)
                .collection("asistencias")
                .document(fecha)
                .set(data)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    // Registrar salida
    public void registrarSalida(String fecha, double lat, double lon, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        DocumentReference ref = db.collection("asistencias")
                .document(uid)
                .collection("asistencias")
                .document(fecha);

        ref.update("horaSalida", dataHora(),
                        "latitudSalida", lat,
                        "longitudSalida", lon)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    // Verificar asistencia del día
    public void verificarAsistenciaHoy(String fecha, OnSuccessListener<DocumentSnapshot> onSuccess) {
        DocumentReference ref = db.collection("asistencias")
                .document(uid)
                .collection("asistencias")
                .document(fecha);
        ref.get().addOnSuccessListener(onSuccess);
    }

    // Actualizar resumen semanal
    public void actualizarResumenSemanal(String fecha) {
        Calendar cal = Calendar.getInstance();
        int semana = cal.get(Calendar.WEEK_OF_YEAR);
        int año = cal.get(Calendar.YEAR);
        String idSemana = año + "-W" + semana;

        DocumentReference refResumen = db.collection("asistencias")
                .document(uid)
                .collection("resumenSemanal")
                .document(idSemana);

        verificarAsistenciaHoy(fecha, snapshot -> {
            String estado = snapshot.getString("estado");
            db.runTransaction(transaction -> {
                DocumentSnapshot snap = transaction.get(refResumen);
                long asistencias = snap.exists() && snap.contains("asistencias") ? snap.getLong("asistencias") : 0;
                long retardos = snap.exists() && snap.contains("retardos") ? snap.getLong("retardos") : 0;

                Map<String, Object> nuevosDatos = new HashMap<>();
                if ("retardo".equals(estado)) {
                    nuevosDatos.put("retardos", retardos + 1);
                    nuevosDatos.put("asistencias", asistencias);
                } else {
                    nuevosDatos.put("asistencias", asistencias + 1);
                    nuevosDatos.put("retardos", retardos);
                }
                transaction.set(refResumen, nuevosDatos);
                return null;
            });
        });
    }

    private String dataHora() {
        return new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date());
    }
}
