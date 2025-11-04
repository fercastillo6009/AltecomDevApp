package com.example.fondodepantalla.FragmentosAdministrador;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.fondodepantalla.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class PerfilAdmin extends Fragment {

    private MaterialButton btnAgregar, btnQuitar;
    private boolean isAgregar = true; // bandera para saber qué operación hacer
    private FirebaseFirestore db;

    public PerfilAdmin() {
        // Required empty public constructor
    }

    public static PerfilAdmin newInstance(String param1, String param2) {
        PerfilAdmin fragment = new PerfilAdmin();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil_admin, container, false);

        btnAgregar = view.findViewById(R.id.btnAgregar);
        btnQuitar = view.findViewById(R.id.btnQuitar);

        btnAgregar.setOnClickListener(v -> {
            isAgregar = true;
            abrirScanner();
        });

        btnQuitar.setOnClickListener(v -> {
            isAgregar = false;
            abrirScanner();
        });

        return view;
    }

    // Lanzar scanner ZXing
    private void abrirScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Escanea un código de barras");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        barcodeLauncher.launch(options);
    }

    // Callback cuando se escanea
    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    actualizarInventario(result.getContents(), isAgregar);
                }
            });

    // Actualizar Firestore
    private void actualizarInventario(String codigo, boolean sumar) {
        DocumentReference docRef = db.collection("inventario").document(codigo);

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot snapshot = transaction.get(docRef);
            if (snapshot.exists()) {
                long cantidad = snapshot.getLong("cantidad") != null ? snapshot.getLong("cantidad") : 0;
                if (sumar) {
                    transaction.update(docRef, "cantidad", FieldValue.increment(1));
                } else {
                    if (cantidad > 0) {
                        transaction.update(docRef, "cantidad", FieldValue.increment(-1));
                    } else {
                        throw new RuntimeException("No hay stock disponible");
                    }
                }
            } else {
                if (sumar) {
                    transaction.set(docRef, new Producto("Producto sin nombre", 1));
                } else {
                    throw new RuntimeException("El producto no existe en inventario");
                }
            }
            return null;
        }).addOnSuccessListener(aVoid ->
                Toast.makeText(getContext(), "Inventario actualizado", Toast.LENGTH_SHORT).show()
        ).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    // Clase modelo para guardar productos
    public static class Producto {
        private String nombre;
        private int cantidad;

        public Producto() {}

        public Producto(String nombre, int cantidad) {
            this.nombre = nombre;
            this.cantidad = cantidad;
        }

        public String getNombre() { return nombre; }
        public int getCantidad() { return cantidad; }
    }
}
