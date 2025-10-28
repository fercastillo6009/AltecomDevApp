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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ListaAdmin extends Fragment {

    private RecyclerView recyclerTasks;
    private TaskAdapter adapter;
    private List<Task> taskList = new ArrayList<>();
    private FirebaseFirestore db;

    public ListaAdmin() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_admin, container, false);

        recyclerTasks = view.findViewById(R.id.recyclerTasks);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();

        adapter = new TaskAdapter(taskList, db, task -> {
            // DEBUG: verifica que el click se dispare
            Toast.makeText(requireContext(), "Click: " + task.getId(), Toast.LENGTH_SHORT).show();

            Bundle args = new Bundle();
            args.putString("taskId", task.getId());

            RegistrarAdmin registrarAdmin = new RegistrarAdmin();
            registrarAdmin.setArguments(args);

            // Usa el FragmentManager del Activity (más seguro)
            ((androidx.appcompat.app.AppCompatActivity) requireActivity())
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_containerA, registrarAdmin)
                    .addToBackStack(null)
                    .commit();
        });

        recyclerTasks.setAdapter(adapter);

        db.collection("tareas")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    taskList.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Task t = doc.toObject(Task.class);
                            if (t != null) t.setId(doc.getId());
                            taskList.add(t);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });

        return view;
    }
}
