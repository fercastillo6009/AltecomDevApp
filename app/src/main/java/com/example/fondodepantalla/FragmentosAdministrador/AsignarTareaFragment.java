package com.example.fondodepantalla.FragmentosAdministrador;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fondodepantalla.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AsignarTareaFragment extends Fragment {

    private AutoCompleteTextView dropdownUsuarios;
    private EditText editNombreTarea, editDescripcion;
    private Button btnAsignar;
    private FirebaseFirestore db;

    private ArrayList<String> listaUsuariosNombres = new ArrayList<>();
    private ArrayList<String> listaUsuariosUID = new ArrayList<>();
    private String uidSeleccionado = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_asignar_tarea, container, false);

        dropdownUsuarios = view.findViewById(R.id.dropdownUsuarios);
        editNombreTarea = view.findViewById(R.id.editNombreTarea);
        editDescripcion = view.findViewById(R.id.editDescripcion);
        btnAsignar = view.findViewById(R.id.btnAsignarTarea);

        db = FirebaseFirestore.getInstance();

        cargarUsuarios();
        btnAsignar.setOnClickListener(v -> asignarTarea());

        return view;
    }

    private void cargarUsuarios() {
        db.collection("usuarios").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String nombre = doc.getString("nombre");
                String uid = doc.getId();
                listaUsuariosNombres.add(nombre);
                listaUsuariosUID.add(uid);
            }

            if (getContext() == null) return;
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    getContext(),
                    android.R.layout.simple_list_item_1,
                    listaUsuariosNombres
            );
            dropdownUsuarios.setKeyListener(null);
            dropdownUsuarios.setAdapter(adapter);
            dropdownUsuarios.setOnClickListener(v -> dropdownUsuarios.showDropDown());
            dropdownUsuarios.setOnItemClickListener((parent, view, position, id) -> {
                uidSeleccionado = listaUsuariosUID.get(position);
            });


        }).addOnFailureListener(e -> {
            if (getContext() != null)
                Toast.makeText(getContext(), "Error al cargar usuarios", Toast.LENGTH_SHORT).show();
        });
    }
    private void asignarTarea() {
        String nombreTarea = editNombreTarea.getText().toString().trim();
        String descripcion = editDescripcion.getText().toString().trim();

        if (nombreTarea.isEmpty() || descripcion.isEmpty() || uidSeleccionado == null) {
            Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> tarea = new HashMap<>();
        tarea.put("name", nombreTarea);
        tarea.put("descripcion", descripcion);
        tarea.put("uidAsignado", uidSeleccionado);
        tarea.put("nombreEmpleado", dropdownUsuarios.getText().toString());
        tarea.put("fechaAsignacion", System.currentTimeMillis());

        db.collection("tareas").add(tarea)
                .addOnSuccessListener(docRef -> {
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Tarea asignada correctamente", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Error al asignar tarea", Toast.LENGTH_SHORT).show();
                });
    }
}
