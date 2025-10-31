package com.example.fondodepantalla.FragmentosAdministrador;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fondodepantalla.R;
import java.util.List;

public class ResumenAdapter extends RecyclerView.Adapter<ResumenAdapter.ResumenVH> {

    private List<ResumenItem> lista;

    public ResumenAdapter(List<ResumenItem> lista) {
        this.lista = lista;
    }

    public static class ResumenVH extends RecyclerView.ViewHolder {
        TextView tvNombre, tvAsistencias, tvRetardos, tvFaltas;
        public ResumenVH(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvAsistencias = itemView.findViewById(R.id.tvAsistencias);
            tvRetardos = itemView.findViewById(R.id.tvRetardos);
            tvFaltas = itemView.findViewById(R.id.tvFaltas);
        }
    }

    @NonNull
    @Override
    public ResumenVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_resumen, parent, false);
        return new ResumenVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResumenVH holder, int position) {
        ResumenItem item = lista.get(position);
        holder.tvNombre.setText(item.getNombre());
        holder.tvAsistencias.setText(String.valueOf(item.getAsistencias()));
        holder.tvRetardos.setText(String.valueOf(item.getRetardos()));
        holder.tvFaltas.setText(String.valueOf(item.getFaltas()));
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }
}
