package com.example.fondodepantalla;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AsignarTareaActivity extends AppCompatActivity {

    private AutoCompleteTextView dropdownUsuarios;
    private EditText editNombreTarea, editDescripcion;
    private Button btnAsignar;
    private FirebaseFirestore db;

    private ArrayList<String> listaUsuariosNombres = new ArrayList<>();
    private ArrayList<String> listaUsuariosUID = new ArrayList<>();
    private String uidSeleccionado = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asignar_tarea);

        dropdownUsuarios = findViewById(R.id.dropdownUsuarios);
        editNombreTarea = findViewById(R.id.editNombreTarea);
        editDescripcion = findViewById(R.id.editDescripcion);
        btnAsignar = findViewById(R.id.btnAsignarTarea);

        db = FirebaseFirestore.getInstance();

        cargarUsuarios();

        btnAsignar.setOnClickListener(v -> asignarTarea());
    }

    private void cargarUsuarios() {
        db.collection("usuarios").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String nombre = doc.getString("nombre");
                String uid = doc.getId();
                listaUsuariosNombres.add(nombre);
                listaUsuariosUID.add(uid);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, listaUsuariosNombres);
            dropdownUsuarios.setAdapter(adapter);

            dropdownUsuarios.setOnItemClickListener((parent, view, position, id) -> {
                uidSeleccionado = listaUsuariosUID.get(position);
            });

        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error al cargar usuarios", Toast.LENGTH_SHORT).show();
        });
    }

    private void asignarTarea() {
        String nombreTarea = editNombreTarea.getText().toString().trim();
        String descripcion = editDescripcion.getText().toString().trim();

        if (nombreTarea.isEmpty() || descripcion.isEmpty() || uidSeleccionado == null) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> tarea = new HashMap<>();
        tarea.put("name", nombreTarea);
        tarea.put("descripcion", descripcion);
        tarea.put("uidAsignado", uidSeleccionado);
        tarea.put("nombreEmpleado", dropdownUsuarios.getText().toString());
        tarea.put("fechaAsignacion", System.currentTimeMillis());

        db.collection("tareas").add(tarea)
                .addOnSuccessListener(docRef -> Toast.makeText(this, "Tarea asignada correctamente", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error al asignar tarea", Toast.LENGTH_SHORT).show());
    }
}
