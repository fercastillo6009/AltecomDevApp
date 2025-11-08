package com.example.fondodepantalla.FragmentosAdministrador;

import androidx.annotation.Nullable; // 🌟 AÑADIR ESTA IMPORTACIÓN

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

    // 🌟 NUEVA INTERFAZ: Permite que el listener de Kotlin reciba 'null' en caso de error.
    public interface AsistenciaResultListener {
        void onResult(@Nullable DocumentSnapshot snapshot);
    }

    public AsistenciaManager(FirebaseFirestore db, String uid) {
        this.db = db;
        this.uid = uid;
    }

    // Registrar entrada (Sin cambios)
    public void registrarEntrada(Map<String, Object> data, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        String fecha = data.get("fecha").toString();
        // NOTA: Usas "asistencias" -> uid -> "asistencias". Es poco común pero se respeta tu estructura.
        db.collection("asistencias")
                .document(uid)
                .collection("asistencias")
                .document(fecha)
                .set(data)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    // 🌟 NUEVO MÉTODO: Reemplaza a 'registrarSalida'
    // Acepta un 'Map' para actualizar el documento, que es lo que el Kotlin envía.
    public void actualizarAsistencia(String fecha, Map<String, Object> data, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        DocumentReference ref = db.collection("asistencias")
                .document(uid)
                .collection("asistencias")
                .document(fecha);

        ref.update(data) // Actualiza usando el Map
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    // 🌟 MÉTODO ANTIGUO ELIMINADO
    // public void registrarSalida(...) { ... }

    // 🌟 MÉTODO MODIFICADO: Usa la nueva interfaz 'AsistenciaResultListener'
    public void verificarAsistenciaHoy(String fecha, AsistenciaResultListener listener) {
        DocumentReference ref = db.collection("asistencias")
                .document(uid)
                .collection("asistencias")
                .document(fecha);

        ref.get()
                .addOnSuccessListener(snapshot -> listener.onResult(snapshot)) // Devuelve el snapshot en éxito
                .addOnFailureListener(e -> listener.onResult(null)); // Devuelve null en fallo
    }


    // 🌟 MÉTODO CORREGIDO: Añade comprobación de 'null'
    public void actualizarResumenSemanal(String fecha) {
        Calendar cal = Calendar.getInstance();
        int semana = cal.get(Calendar.WEEK_OF_YEAR);
        int año = cal.get(Calendar.YEAR);
        String idSemana = año + "-W" + semana;

        DocumentReference refResumen = db.collection("asistencias")
                .document(uid)
                .collection("resumenSemanal")
                .document(idSemana);

        // Usa el 'verificarAsistenciaHoy' modificado
        verificarAsistenciaHoy(fecha, snapshot -> {

            // 🌟 COMPROBACIÓN DE NULOS: Evita 'NullPointerException' si el get() falla
            if (snapshot == null || !snapshot.exists()) {
                // No se pudo obtener el documento, no se puede actualizar el resumen
                return;
            }

            String estado = snapshot.getString("estado");

            db.runTransaction(transaction -> {
                DocumentSnapshot snap = transaction.get(refResumen);
                long asistencias = snap.exists() && snap.contains("asistencias") ? snap.getLong("asistencias") : 0;
                long retardos = snap.exists() && snap.contains("retardos") ? snap.getLong("retardos") : 0;

                Map<String, Object> nuevosDatos = new HashMap<>();

                // 🌟 Añadida comprobación para 'sitio_cliente'
                if ("retardo".equals(estado)) {
                    nuevosDatos.put("retardos", retardos + 1);
                    // No sumamos asistencia si ya existe el documento
                } else if ("puntual".equals(estado) || "sitio_cliente".equals(estado)) {
                    nuevosDatos.put("asistencias", asistencias + 1);
                }

                // Si el estado es 'retardo', solo actualizamos 'retardos'
                if ("retardo".equals(estado)) {
                    transaction.update(refResumen, "retardos", retardos + 1);
                }
                // Si es la primera asistencia (puntual o cliente), creamos/actualizamos ambos
                else if (("puntual".equals(estado) || "sitio_cliente".equals(estado)) && !snap.exists()) {
                    nuevosDatos.put("asistencias", asistencias + 1);
                    nuevosDatos.put("retardos", retardos); // Asegura que 'retardos' exista
                    transaction.set(refResumen, nuevosDatos);
                }

                // Esta lógica es compleja. Una versión más simple sería:
                /*
                Map<String, Object> datos = new HashMap<>();
                long asistencias = snap.exists() ? snap.getLong("asistencias") : 0;
                long retardos = snap.exists() ? snap.getLong("retardos") : 0;

                if ("retardo".equals(estado)) {
                    retardos++;
                }
                // Contamos 'sitio_cliente' como asistencia
                if ("puntual".equals(estado) || "sitio_cliente".equals(estado)) {
                    asistencias++;
                }
                datos.put("asistencias", asistencias);
                datos.put("retardos", retardos);

                // .set(datos, SetOptions.merge()) es más seguro
                transaction.set(refResumen, datos, com.google.firebase.firestore.SetOptions.merge());
                */

                return null;
            });
        });
    }

    // 🌟 MÉTODO ANTIGUO ELIMINADO
    // private String dataHora() { ... }
    // (Ya no es necesario, la hora se genera en Kotlin)
}