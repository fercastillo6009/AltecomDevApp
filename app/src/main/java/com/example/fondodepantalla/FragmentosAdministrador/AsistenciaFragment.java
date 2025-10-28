package com.example.fondodepantalla.FragmentosAdministrador;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;

import com.example.fondodepantalla.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AsistenciaFragment extends Fragment {

    private LocationHelper locationHelper;
    private Button btnRegistrarEntrada, btnRegistrarSalida;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String uid, nombre, fechaHoy;
    private boolean entradaRegistrada = false;

    // CardView y TextViews
    private CardView cardView;
    private TextView tvUserEmail, tvFecha, tvHora, tvEstado;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_asistencia, container, false);

        // Inicializar LocationHelper y Firebase
        locationHelper = new LocationHelper(requireContext());
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        uid = auth.getCurrentUser().getUid();
        nombre = auth.getCurrentUser().getDisplayName() != null ?
                auth.getCurrentUser().getDisplayName() : "Empleado";

        fechaHoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Inicializar CardView y TextViews
        cardView = view.findViewById(R.id.cardView);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvFecha = view.findViewById(R.id.tvFecha);
        tvHora = view.findViewById(R.id.tvHora);
        tvEstado = view.findViewById(R.id.tvEstado);

        tvUserEmail.setText("Correo: " + (auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : "---"));
        tvFecha.setText("Fecha: " + fechaHoy);
        cardView.setVisibility(View.GONE); // oculto hasta cargar datos

        // Inicializar botones
        btnRegistrarEntrada = view.findViewById(R.id.btnRegistrarAsistencia);
        btnRegistrarSalida = view.findViewById(R.id.btnRegistrarSalida);

        // Revisar asistencia del día
        verificarAsistenciaHoy();

        // Listeners
        btnRegistrarEntrada.setOnClickListener(v -> checkLocationAndRegisterEntrada());
        btnRegistrarSalida.setOnClickListener(v -> registrarSalida());

        return view;
    }

    private void verificarAsistenciaHoy() {
        DocumentReference ref = db.collection("asistencias")
                .document(uid)
                .collection("asistencias")
                .document(fechaHoy);

        ref.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                entradaRegistrada = true;
                btnRegistrarEntrada.setVisibility(View.GONE);
                btnRegistrarSalida.setVisibility(View.VISIBLE);

                // Cargar datos al CardView
                actualizarCard(document);

                if (document.contains("horaSalida")) {
                    btnRegistrarSalida.setVisibility(View.GONE);
                }
            } else {
                btnRegistrarEntrada.setVisibility(View.VISIBLE);
                btnRegistrarSalida.setVisibility(View.GONE);
            }
        });
    }

    private void checkLocationAndRegisterEntrada() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        locationHelper.getCurrentLocation(location -> {
            if (location == null) {
                Toast.makeText(requireContext(), "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show();
            } else if (locationHelper.isInsideCompanyArea(location.getLatitude(), location.getLongitude())) {
                registrarEntrada(location);
            } else {
                Toast.makeText(requireContext(), "❌ Estás fuera del rango permitido", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registrarEntrada(Location location) {
        if (entradaRegistrada) {
            Toast.makeText(requireContext(), "Ya registraste tu entrada hoy", Toast.LENGTH_SHORT).show();
            return;
        }

        String horaActual = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        String estado = calcularEstadoEntrada(horaActual); // puntual o retardo

        Map<String, Object> data = new HashMap<>();
        data.put("nombre", nombre);
        data.put("fecha", fechaHoy);
        data.put("horaEntrada", horaActual);
        data.put("estado", estado);
        data.put("latitudEntrada", location.getLatitude());
        data.put("longitudEntrada", location.getLongitude());

        db.collection("asistencias")
                .document(uid)
                .collection("asistencias")
                .document(fechaHoy)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    entradaRegistrada = true;
                    btnRegistrarEntrada.setVisibility(View.GONE);
                    btnRegistrarSalida.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(), "✅ Entrada registrada (" + estado + ")", Toast.LENGTH_SHORT).show();

                    // Actualizar CardView
                    actualizarCard(data);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Error al registrar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void registrarSalida() {
        if (!entradaRegistrada) {
            Toast.makeText(requireContext(), "Primero registra tu entrada", Toast.LENGTH_SHORT).show();
            return;
        }

        locationHelper.getCurrentLocation(location -> {
            if (location == null) {
                Toast.makeText(requireContext(), "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show();
            } else if (locationHelper.isInsideCompanyArea(location.getLatitude(), location.getLongitude())) {
                String horaSalida = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

                DocumentReference ref = db.collection("asistencias")
                        .document(uid)
                        .collection("asistencias")
                        .document(fechaHoy);

                ref.update("horaSalida", horaSalida,
                                "latitudSalida", location.getLatitude(),
                                "longitudSalida", location.getLongitude())
                        .addOnSuccessListener(unused -> {
                            btnRegistrarSalida.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "🕕 Salida registrada", Toast.LENGTH_SHORT).show();

                            // Actualizar CardView con hora de salida
                            ref.get().addOnSuccessListener(document -> actualizarCard(document));

                            actualizarResumenSemanal(uid, fechaHoy);
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(requireContext(), "❌ Estás fuera del rango permitido", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String calcularEstadoEntrada(String hora) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date horaEntrada = sdf.parse(hora);
            Date horaInicio = sdf.parse("09:00");
            Date horaLimite = sdf.parse("09:15");

            if (horaEntrada != null && horaEntrada.after(horaLimite)) {
                return "retardo";
            } else {
                return "puntual";
            }
        } catch (Exception e) {
            return "puntual";
        }
    }

    private void actualizarResumenSemanal(String uid, String fecha) {
        Calendar cal = Calendar.getInstance();
        int semana = cal.get(Calendar.WEEK_OF_YEAR);
        int año = cal.get(Calendar.YEAR);
        String idSemana = año + "-W" + semana;

        DocumentReference refResumen = db.collection("asistencias")
                .document(uid)
                .collection("resumenSemanal")
                .document(idSemana);

        db.collection("asistencias").document(uid)
                .collection("asistencias").document(fecha)
                .get()
                .addOnSuccessListener(snapshot -> {
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

    // Método para actualizar CardView desde Firestore o Map
    private void actualizarCard(Object data) {
        String hora = "---";
        String estado = "No has registrado asistencia";

        if (data instanceof DocumentSnapshot) {
            DocumentSnapshot doc = (DocumentSnapshot) data;
            hora = doc.contains("horaEntrada") ? doc.getString("horaEntrada") : "---";
            if (doc.contains("horaSalida")) {
                hora += " - " + doc.getString("horaSalida");
            }
            estado = doc.contains("estado") ? doc.getString("estado") : estado;
        } else if (data instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) data;
            hora = map.containsKey("horaEntrada") ? map.get("horaEntrada").toString() : "---";
            estado = map.containsKey("estado") ? map.get("estado").toString() : estado;
        }

        tvHora.setText("Hora: " + hora);
        tvEstado.setText("Estado: " + estado);
        cardView.setVisibility(View.VISIBLE);
    }
}
