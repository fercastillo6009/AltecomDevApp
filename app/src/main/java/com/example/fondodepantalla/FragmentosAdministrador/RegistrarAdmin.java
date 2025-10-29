package com.example.fondodepantalla.FragmentosAdministrador;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.anton46.stepsview.StepsView;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.fondodepantalla.R;
import com.github.gcacace.signaturepad.views.SignaturePad;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

public class RegistrarAdmin extends Fragment {
    private ProgressBar progressBarConfirmacion;

    private StepsView stepsView;
    private FirebaseFirestore db;
    private String taskId;
    private Button btnNextStep, btnSubirEvidencia, btnEnviarConfirmacion;
    private int currentStep = 0;

    private CheckBox checkExito;
    private EditText etComentario;
    private SignaturePad signaturePad;

    private static final int PICK_IMAGES_REQUEST = 101;

    private final String[] labels = {"Inicio", "st1", "st2", "st3", "Completa"};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registro_admin, container, false);

        stepsView = view.findViewById(R.id.stepsView);
        btnNextStep = view.findViewById(R.id.btnNextStep);
        btnSubirEvidencia = view.findViewById(R.id.btnSubirEvidencia);
        db = FirebaseFirestore.getInstance();

        // Formularios de confirmación
        btnEnviarConfirmacion = view.findViewById(R.id.btnEnviarConfirmacion);
        checkExito = view.findViewById(R.id.checkExito);
        etComentario = view.findViewById(R.id.etComentario);
        signaturePad = view.findViewById(R.id.signaturePad);

        if (getArguments() != null) {
            taskId = getArguments().getString("taskId");
        }

        initStepsView();

        if (taskId != null) loadTaskProgress();
        else Toast.makeText(getContext(), "ID de tarea nulo", Toast.LENGTH_SHORT).show();

        setupButton();

        // Subir evidencia
        btnSubirEvidencia.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Selecciona evidencia"), PICK_IMAGES_REQUEST);
        });

        // Enviar confirmación
        btnEnviarConfirmacion.setOnClickListener(v -> enviarConfirmacion());

        progressBarConfirmacion = view.findViewById(R.id.progressBarConfirmacion);

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
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al actualizar progreso", Toast.LENGTH_SHORT).show());
    }

    private void refreshStepsView() {
        stepsView.setCompletedPosition(currentStep).drawView();
    }

    private void setupButton() {
        btnNextStep.setOnClickListener(v -> {
            if (currentStep < labels.length - 1) updateProgress(currentStep + 1);
            else Toast.makeText(getContext(), "Ya estás en el último paso", Toast.LENGTH_SHORT).show();
        });
    }

    private void toggleEvidenciaButton() {
        if (btnSubirEvidencia != null)
            btnSubirEvidencia.setVisibility(currentStep == 4 ? View.VISIBLE : View.GONE);
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
                .option("folder", "evidencias/" + taskId)
                .option("use_filename", true)
                .option("unique_filename", true)
                .option("resource_type", "image")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        db.collection("tareas").document(taskId)
                                .update("evidencias", FieldValue.arrayUnion(url));
                        Toast.makeText(getContext(), "Imagen subida correctamente", Toast.LENGTH_SHORT).show();

                        // Mostrar formulario de confirmación
                        View layoutConfirmacion = getView().findViewById(R.id.layoutConfirmacion);
                        layoutConfirmacion.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(getContext(), "Error al subir imagen: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void enviarConfirmacion() {
        boolean exito = checkExito.isChecked();
        String comentario = etComentario.getText().toString();
        Bitmap firma = signaturePad.getSignatureBitmap();

        if (!exito) {
            Toast.makeText(getContext(), "Marca que se completó la tarea para continuar", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar ProgressBar
        progressBarConfirmacion.setVisibility(View.VISIBLE);
        btnEnviarConfirmacion.setEnabled(false); // Evitar múltiples clicks

        Uri firmaUri = bitmapToUri(firma);
        if (firmaUri == null) {
            Toast.makeText(getContext(), "Error al generar archivo de firma", Toast.LENGTH_SHORT).show();
            progressBarConfirmacion.setVisibility(View.GONE);
            btnEnviarConfirmacion.setEnabled(true);
            return;
        }

        MediaManager.get().upload(firmaUri)
                .option("folder", "firmas/" + taskId)
                .option("use_filename", true)
                .option("unique_filename", true)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String firmaUrl = (String) resultData.get("secure_url");

                        db.collection("tareas").document(taskId)
                                .update(
                                        "confirmacionExito", exito,
                                        "comentarioCliente", comentario,
                                        "firmaCliente", firmaUrl,
                                        "fechaConfirmacion", FieldValue.serverTimestamp()
                                )
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Confirmación enviada", Toast.LENGTH_SHORT).show();

                                    // Limpiar formulario
                                    checkExito.setChecked(false);
                                    etComentario.setText("");
                                    signaturePad.clear();

                                    // Ocultar formulario y ProgressBar
                                    View layoutConfirmacion = getView().findViewById(R.id.layoutConfirmacion);
                                    layoutConfirmacion.setVisibility(View.GONE);
                                    progressBarConfirmacion.setVisibility(View.GONE);
                                    btnEnviarConfirmacion.setEnabled(true);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Error al guardar confirmación", Toast.LENGTH_SHORT).show();
                                    progressBarConfirmacion.setVisibility(View.GONE);
                                    btnEnviarConfirmacion.setEnabled(true);
                                });
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(getContext(), "Error al subir firma: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                        progressBarConfirmacion.setVisibility(View.GONE);
                        btnEnviarConfirmacion.setEnabled(true);
                    }

                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    // Convierte Bitmap a Uri usando archivo temporal (recomendado)
    private Uri bitmapToUri(Bitmap bitmap) {
        try {
            File file = new File(getContext().getCacheDir(), "firma_" + System.currentTimeMillis() + ".png");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return Uri.fromFile(file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
