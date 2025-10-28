package com.example.fondodepantalla.FragmentosAdministrador;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.anton46.stepsview.StepsView;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.UploadCallback;
import com.cloudinary.android.callback.ErrorInfo;
import com.example.fondodepantalla.R;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class RegistrarAdmin extends Fragment {

    private StepsView stepsView;
    private FirebaseFirestore db;
    private String taskId;
    private Button btnNextStep, btnSubirEvidencia;
    private int currentStep = 0;

    private static final int PICK_IMAGES_REQUEST = 101;

    private final String[] labels = {
            "Inicio",
            "st1",
            "st2",
            "st3",
            "Completa"
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registro_admin, container, false);

        stepsView = view.findViewById(R.id.stepsView);
        btnNextStep = view.findViewById(R.id.btnNextStep);
        btnSubirEvidencia = view.findViewById(R.id.btnSubirEvidencia);
        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            taskId = getArguments().getString("taskId");
        }

        initStepsView();

        if (taskId != null) {
            loadTaskProgress();
        } else {
            Toast.makeText(getContext(), "ID de tarea nulo", Toast.LENGTH_SHORT).show();
        }

        setupButton();

        btnSubirEvidencia.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Selecciona evidencia"), PICK_IMAGES_REQUEST);
        });

        return view;
    }

    private void initStepsView() {
        stepsView.setLabels(labels)
                .setBarColorIndicator(getResources().getColor(R.color.pastel_gray))
                .setProgressColorIndicator(getResources().getColor(R.color.pastel_purple))
                .setLabelColorIndicator(getResources().getColor(R.color.black))
                .setCompletedPosition(currentStep)
                .drawView();
    }

    private void loadTaskProgress() {
        db.collection("tareas").document(taskId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long progreso = doc.getLong("progress");
                        if (progreso != null) {
                            currentStep = progreso.intValue();
                            refreshStepsView();
                            toggleEvidenciaButton();
                        }
                    }
                });
    }

    private void updateProgress(int progress) {
        if (taskId == null) return;

        db.collection("tareas").document(taskId)
                .update("progress", progress)
                .addOnSuccessListener(aVoid -> {
                    currentStep = progress;
                    refreshStepsView();
                    toggleEvidenciaButton();
                    Toast.makeText(getContext(), "Avanzaste a: " + labels[progress], Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error al actualizar progreso", Toast.LENGTH_SHORT).show());
    }

    private void refreshStepsView() {
        stepsView.setCompletedPosition(currentStep).drawView();
    }

    private void setupButton() {
        btnNextStep.setOnClickListener(v -> {
            if (currentStep < labels.length - 1) {
                updateProgress(currentStep + 1);
            } else {
                Toast.makeText(getContext(), "Ya estás en el último paso", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleEvidenciaButton() {
        if (btnSubirEvidencia != null) {
            btnSubirEvidencia.setVisibility(currentStep == 4 ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGES_REQUEST && resultCode == getActivity().RESULT_OK) {
            if (data.getClipData() != null) { // varias imágenes
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    subirArchivoCloudinary(imageUri);
                }
            } else if (data.getData() != null) { // una sola imagen
                Uri imageUri = data.getData();
                subirArchivoCloudinary(imageUri);
            }
        }
    }

    private void subirArchivoCloudinary(Uri fileUri) {
        if (taskId == null) return;

        MediaManager.get().upload(fileUri)
                .option("folder", "evidencias/" + taskId) // carpeta por tarea
                .option("use_filename", true)            // intenta usar nombre original
                .option("unique_filename", true)         // asegura que no se sobrescriba
                .option("resource_type", "image")        // solo imágenes
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) { }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) { }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        db.collection("tareas").document(taskId)
                                .update("evidencias", FieldValue.arrayUnion(url));
                        Toast.makeText(getContext(), "Imagen subida correctamente", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(getContext(), "Error al subir imagen: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) { }
                })
                .dispatch();
    }
}
