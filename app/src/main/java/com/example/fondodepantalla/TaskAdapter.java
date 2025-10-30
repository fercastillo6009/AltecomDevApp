package com.example.fondodepantalla;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    private List<Task> tasks;
    private FirebaseFirestore db;
    private OnTaskClickListener listener;

    public TaskAdapter(List<Task> tasks, FirebaseFirestore db, OnTaskClickListener listener) {
        this.tasks = tasks;
        this.db = db;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.tvName.setText(task.getName());
        holder.switchTaken.setOnCheckedChangeListener(null);
        holder.switchTaken.setChecked(task.isTaken());

        holder.switchTaken.setOnClickListener(v -> {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            if (holder.switchTaken.isChecked()) {
                if (!task.isTaken()) {
                    db.collection("tareas").document(task.getId())
                            .update("taken", true, "user", uid)
                            .addOnFailureListener(e -> {
                                holder.switchTaken.setChecked(false);
                                Toast.makeText(holder.itemView.getContext(), "Error al tomar tarea", Toast.LENGTH_SHORT).show();
                            });
                    task.setTaken(true);
                    task.setUser(uid);
                } else {
                    holder.switchTaken.setChecked(true);
                    Toast.makeText(holder.itemView.getContext(), "Tarea ya tomada", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (task.getUser() != null && task.getUser().equals(uid)) {
                    db.collection("tareas").document(task.getId())
                            .update("taken", false, "user", "")
                            .addOnFailureListener(e -> {
                                holder.switchTaken.setChecked(true);
                                Toast.makeText(holder.itemView.getContext(), "Error al liberar tarea", Toast.LENGTH_SHORT).show();
                            });
                    task.setTaken(false);
                    task.setUser("");
                } else {
                    holder.switchTaken.setChecked(true);
                    Toast.makeText(holder.itemView.getContext(), "No puedes liberar esta tarea", Toast.LENGTH_SHORT).show();
                }
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTaskClick(task);
        });
    }

    @Override
    public int getItemCount() { return tasks.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        Switch switchTaken;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTaskName);
            switchTaken = itemView.findViewById(R.id.switchTaken);
        }
    }
}
