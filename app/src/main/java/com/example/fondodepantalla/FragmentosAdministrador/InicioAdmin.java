package com.example.fondodepantalla.FragmentosAdministrador;

import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.fondodepantalla.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class InicioAdmin extends Fragment {

    BarChart barChartTareas, barChartInventario;
    FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_inicio_admin, container, false);

        barChartTareas = view.findViewById(R.id.barChart);
        barChartInventario = view.findViewById(R.id.barChartInventario);

        db = FirebaseFirestore.getInstance();

        cargarDatosTareas();
        cargarDatosInventario();

        return view;
    }

    // ------------------- TAREAS -------------------
    private void cargarDatosTareas() {
        db.collection("tareas")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int completadas = 0;
                    int enProceso = 0;
                    int faltantes = 0;

                    for (DocumentSnapshot doc : querySnapshot) {
                        Boolean taken = doc.getBoolean("taken");
                        Long progress = doc.getLong("progress");
                        boolean evidenciaExiste = doc.contains("evidencias");

                        if (taken != null && taken) {
                            if (progress != null && progress == 4 && evidenciaExiste) {
                                completadas++;
                            } else {
                                enProceso++;
                            }
                        } else {
                            faltantes++;
                        }
                    }

                    mostrarGraficoTareas(completadas, enProceso, faltantes);
                });
    }

    private void mostrarGraficoTareas(int completadas, int enProceso, int faltantes) {
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, completadas));
        entries.add(new BarEntry(1f, enProceso));
        entries.add(new BarEntry(2f, faltantes));

        BarDataSet set = new BarDataSet(entries, "Tareas");
        // Colores pastel
        set.setColors(Color.rgb(186, 255, 201), Color.rgb(255, 255, 186), Color.rgb(255, 186, 186));
        set.setValueTextColor(Color.BLACK);
        set.setValueTextSize(14f);

        BarData data = new BarData(set);
        data.setBarWidth(0.9f);

        barChartTareas.setData(data);
        barChartTareas.getDescription().setEnabled(false);

        barChartTareas.getXAxis().setDrawLabels(true);
        barChartTareas.getXAxis().setGranularity(1f);
        barChartTareas.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                switch (index) {
                    case 0: return "Completadas";
                    case 1: return "En proceso";
                    case 2: return "Faltantes";
                    default: return "";
                }
            }
        });

        barChartTareas.setFitBars(true);
        barChartTareas.invalidate();
    }

    // ------------------- INVENTARIO -------------------
    private void cargarDatosInventario() {
        db.collection("inventario")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<BarEntry> entries = new ArrayList<>();
                    List<String> nombres = new ArrayList<>();
                    int index = 0;

                    for (DocumentSnapshot doc : querySnapshot) {
                        Long cantidad = doc.getLong("cantidad");
                        String nombre = doc.getString("nombre");
                        if (cantidad != null && nombre != null) {
                            entries.add(new BarEntry(index, cantidad));
                            nombres.add(nombre);
                            index++;
                        }
                    }

                    mostrarGraficoInventario(entries, nombres);
                });
    }

    private void mostrarGraficoInventario(List<BarEntry> entries, List<String> nombres) {
        BarDataSet set = new BarDataSet(entries, "Inventario");

        // Colores pastel variados
        int[] coloresPastel = {
                Color.rgb(255, 179, 186),
                Color.rgb(255, 223, 186),
                Color.rgb(255, 255, 186),
                Color.rgb(186, 255, 201),
                Color.rgb(186, 225, 255),
                Color.rgb(201, 186, 255)
        };
        List<Integer> listaColores = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            listaColores.add(coloresPastel[i % coloresPastel.length]);
        }
        set.setColors(listaColores);

        // No mostrar valores arriba de la barra
        set.setDrawValues(false);

        BarData data = new BarData(set);
        data.setBarWidth(0.9f);

        barChartInventario.setData(data);
        barChartInventario.getDescription().setEnabled(false);
        barChartInventario.getXAxis().setDrawLabels(false);
        barChartInventario.setFitBars(true);

        // MarkerView para mostrar nombre y cantidad al tocar
        barChartInventario.setMarker(new MarkerView(requireContext(), R.layout.marker_view) {
            @Override
            public void refreshContent(com.github.mikephil.charting.data.Entry e, Highlight highlight) {
                int i = (int) e.getX();
                String texto = nombres.get(i) + ": " + (int)e.getY();
                ((android.widget.TextView)findViewById(R.id.tvMarker)).setText(texto);
                super.refreshContent(e, highlight);
            }

            @Override
            public MPPointF getOffset() {
                return new MPPointF(-(getWidth()/2f), -getHeight());
            }
        });

        barChartInventario.invalidate();
    }
}
