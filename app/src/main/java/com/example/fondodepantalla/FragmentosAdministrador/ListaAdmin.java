package com.example.fondodepantalla.FragmentosAdministrador;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.fondodepantalla.R;
import com.example.fondodepantalla.Task;
import com.example.fondodepantalla.TaskAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ListaAdmin extends Fragment {

    private RecyclerView recyclerTasks;
    private TaskAdapter adapter;
    private List<Task> taskList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public ListaAdmin() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_admin, container, false);

        recyclerTasks = view.findViewById(R.id.recyclerTasks);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        adapter = new TaskAdapter(taskList, db, task -> {
            Toast.makeText(requireContext(), "Click: " + task.getId(), Toast.LENGTH_SHORT).show();

            Bundle args = new Bundle();
            args.putString("taskId", task.getId());

            RegistrarAdmin registrarAdmin = new RegistrarAdmin();
            registrarAdmin.setArguments(args);

            ((androidx.appcompat.app.AppCompatActivity) requireActivity())
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_containerA, registrarAdmin)
                    .addToBackStack(null)
                    .commit();
        });

        recyclerTasks.setAdapter(adapter);

        String uidActual = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (uidActual != null) {
            db.collection("tareas")
                    .whereEqualTo("uidAsignado", uidActual) // <-- Filtra por el UID
                    .addSnapshotListener((value, error) -> {
                        if (error != null) return;
                        taskList.clear();
                        if (value != null) {
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                Task t = doc.toObject(Task.class);
                                if (t != null) {
                                    t.setId(doc.getId());
                                    Boolean completada = doc.getBoolean("confirmacionExito");
                                    if (completada == null || !completada) {
                                        taskList.add(t);
                                    }
                                }
                            }
                            adapter.notifyDataSetChanged();
                        }
                    });
        } else {
            Toast.makeText(requireContext(), "Error: no se encontró UID del usuario", Toast.LENGTH_SHORT).show();
        }

        return view;
    }
}
