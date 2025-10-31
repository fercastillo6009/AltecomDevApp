package com.example.fondodepantalla.FragmentosAdministrador;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fondodepantalla.R;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ResumenFragment extends Fragment {

    private EditText etInicio, etFin;
    private RecyclerView rvResumen;
    private ResumenAdapter adapter;
    private List<ResumenItem> listaResumen = new ArrayList<>();
    private OkHttpClient client = new OkHttpClient();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_resumen, container, false);

        etInicio = view.findViewById(R.id.etInicio);
        etFin = view.findViewById(R.id.etFin);
        rvResumen = view.findViewById(R.id.rvResumen);
        Button btnConsultar = view.findViewById(R.id.btnConsultar);

        adapter = new ResumenAdapter(listaResumen);
        rvResumen.setLayoutManager(new LinearLayoutManager(getContext()));
        rvResumen.setAdapter(adapter);

        etInicio.setOnClickListener(v -> showDatePicker(etInicio));
        etFin.setOnClickListener(v -> showDatePicker(etFin));

        btnConsultar.setOnClickListener(v -> {
            if (!etInicio.getText().toString().isEmpty() && !etFin.getText().toString().isEmpty()) {
                fetchResumen(etInicio.getText().toString(), etFin.getText().toString());
            } else {
                Toast.makeText(getContext(), "Selecciona las fechas", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void showDatePicker(final EditText editText) {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dpd = new DatePickerDialog(getContext(),
                (view, year, month, dayOfMonth) -> editText.setText(String.format("%04d-%02d-%02d", year, month+1, dayOfMonth)),
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dpd.show();
    }

    private void fetchResumen(String inicio, String fin) {
        String url = "https://altecomasistencia.onrender.com/api/resumen";
        String json = "{\"inicio\":\"" + inicio + "\",\"fin\":\"" + fin + "\"}";
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(url).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if(getActivity() != null)
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Error al conectar", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful() && response.body() != null) {
                    try {
                        String res = response.body().string();
                        JSONArray jsonArray = new JSONArray(res);
                        listaResumen.clear();
                        for(int i=0; i<jsonArray.length(); i++){
                            JSONObject obj = jsonArray.getJSONObject(i);
                            listaResumen.add(new ResumenItem(
                                    obj.getString("nombre"),
                                    obj.getInt("asistencias"),
                                    obj.getInt("retardos"),
                                    obj.getInt("faltas")
                            ));
                        }
                        if(getActivity() != null)
                            getActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                } else {
                    if(getActivity() != null)
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Error en respuesta", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
