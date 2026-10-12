package com.example.docufind.Fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.docufind.Adaptadores.PublicacionAdapter;
import com.example.docufind.Modelos.Publicacion;
import com.example.docufind.R;
import com.example.docufind.database.DocuFindContract;
import com.example.docufind.database.DocuFindDbHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements PublicacionAdapter.OnPublicacionListener {

    private EditText etMensaje;
    private Spinner spinnerCategoria;
    private FloatingActionButton btnPublicar;
    private RecyclerView recyclerView;
    private PublicacionAdapter adaptador;
    private List<Publicacion> listaMisPublicaciones;

    private boolean esFotoCamara = false;

    private Uri uriArchivoSeleccionado = null;
    private List<CategoriaItem> listaCategorias;

    private Uri uriArchivoEdicion = null;
    private Button btnAdjuntarEdicionRef;



    private static class CategoriaItem {
        int id;
        String nombre;

        CategoriaItem(int id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    // 1. Launcher para solicitar el permiso de CÁMARA en tiempo de ejecución
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    abrirCamara();
                } else {
                    Toast.makeText(getContext(), "Permiso denegado. No se puede usar la cámara.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    uriArchivoSeleccionado = result.getData().getData();
                    esFotoCamara = false;
                    Toast.makeText(getContext(), "PDF adjuntado correctamente", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");

                    if (imageBitmap != null) {
                        uriArchivoSeleccionado = guardarBitmapComoArchivo(imageBitmap);
                        if (uriArchivoSeleccionado != null) {
                            esFotoCamara = true;
                            Toast.makeText(getContext(), "¡Foto de la cámara capturada con éxito!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Error al guardar la foto", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> filePickerEditLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    uriArchivoEdicion = result.getData().getData();
                    if (btnAdjuntarEdicionRef != null) {
                        btnAdjuntarEdicionRef.setText("Archivo Seleccionado (Nuevo)");
                        btnAdjuntarEdicionRef.setBackgroundColor(0xFF4CAF50);
                    }
                    Toast.makeText(getContext(), "Nuevo archivo seleccionado para edición", Toast.LENGTH_SHORT).show();
                }
            }
    );

    public HomeFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        etMensaje = view.findViewById(R.id.etMensajePublicacion);
        spinnerCategoria = view.findViewById(R.id.spinnerCategoria);
        btnPublicar = view.findViewById(R.id.btnSubirDoc);
        recyclerView = view.findViewById(R.id.recyclerViewMisDocs);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        listaMisPublicaciones = new ArrayList<>();
        adaptador = new PublicacionAdapter(listaMisPublicaciones, getContext(), true, this);
        recyclerView.setAdapter(adaptador);

        view.findViewById(R.id.btnAdjuntarPdf).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            filePickerLauncher.launch(intent);
        });

        // 2. Lógica para verificar permisos antes de lanzar la cámara
        view.findViewById(R.id.btnTomarFoto).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                // El usuario ya nos dio permiso antes
                abrirCamara();
            } else {
                // Lanzamos el cuadro de diálogo de Android pidiendo el permiso
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        btnPublicar.setOnClickListener(v -> crearPublicacion());
        cargarCategoriasEnSpinner(spinnerCategoria);

        return view;
    }

    // 3. Extraemos el método de abrir cámara para poder reutilizarlo
    private void abrirCamara() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(getContext(), "No se encontró una aplicación de cámara", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarMisPublicaciones();
    }

    private void cargarCategoriasEnSpinner(Spinner spinner) {
        listaCategorias = new ArrayList<>();
        DocuFindDbHelper dbHelper = new DocuFindDbHelper(getContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                DocuFindContract.TablaCategoria.TABLE_NAME,
                new String[]{DocuFindContract.TablaCategoria.COLUMN_ID, DocuFindContract.TablaCategoria.COLUMN_NAME},
                DocuFindContract.TablaCategoria.COLUMN_STATE + " = 1",
                null, null, null, null
        );

        if (cursor.moveToFirst()) {
            do {
                listaCategorias.add(new CategoriaItem(cursor.getInt(0), cursor.getString(1)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        ArrayAdapter<CategoriaItem> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, listaCategorias);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }


    private void crearPublicacion() {
        String mensaje = etMensaje.getText().toString().trim();
        if (mensaje.isEmpty()) {
            Toast.makeText(getContext(), "Escribe un mensaje", Toast.LENGTH_SHORT).show();
            return;
        }

        if (uriArchivoSeleccionado == null) {
            Toast.makeText(getContext(), "Debes adjuntar un PDF o tomar una foto", Toast.LENGTH_SHORT).show();
            return;
        }

        if (spinnerCategoria.getSelectedItem() == null) {
            Toast.makeText(getContext(), "Selecciona una categoría", Toast.LENGTH_SHORT).show();
            return;
        }

        CategoriaItem categoriaSeleccionada = (CategoriaItem) spinnerCategoria.getSelectedItem();
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        int idUsuario = prefs.getInt("loggedUserId", -1);

        if (idUsuario == -1) return;

        //LÓGICA REPARADA: Manejo diferenciado entre Foto y PDF
        String nombreArchivo;
        String rutaFisica;

        if (esFotoCamara) {
            // La foto YA está guardada físicamente por la cámara, no hacemos copia para no borrarla
            File archivoFoto = new File(uriArchivoSeleccionado.getPath());
            nombreArchivo = archivoFoto.getName();
            rutaFisica = archivoFoto.getAbsolutePath();
        } else {
            // Es un PDF externo, SÍ necesitamos hacerle una copia a nuestra carpeta segura
            nombreArchivo = obtenerNombreArchivo(uriArchivoSeleccionado);
            rutaFisica = guardarArchivoEnAlmacenamiento(uriArchivoSeleccionado, nombreArchivo);
        }
        // -------------------------------------------------------------

        if (rutaFisica == null) return;

        DocuFindDbHelper dbHelper = new DocuFindDbHelper(getContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

        ContentValues valuesDoc = new ContentValues();
        valuesDoc.put(DocuFindContract.TablaDocumentos.COLUMN_NAME, nombreArchivo);
        valuesDoc.put(DocuFindContract.TablaDocumentos.COLUMN_NAMEFILE, nombreArchivo);
        valuesDoc.put(DocuFindContract.TablaDocumentos.COLUMN_RUTADOCUMENTO, rutaFisica);
        valuesDoc.put(DocuFindContract.TablaDocumentos.COLUMN_FECHASUBIDA, fecha);
        valuesDoc.put(DocuFindContract.TablaDocumentos.COLUMN_STATE, 1);
        valuesDoc.put(DocuFindContract.TablaDocumentos.COLUMN_ID_USUARIOS, idUsuario);
        valuesDoc.put(DocuFindContract.TablaDocumentos.COLUMN_CATEGORIA, categoriaSeleccionada.id);

        long idDocInsertado = db.insert(DocuFindContract.TablaDocumentos.TABLE_NAME, null, valuesDoc);

        if (idDocInsertado != -1) {
            ContentValues valuesPub = new ContentValues();
            valuesPub.put(DocuFindContract.TablaPublicacion.COLUMN_STATE, 1);
            valuesPub.put(DocuFindContract.TablaPublicacion.COLUMN_MESSAGE, mensaje);
            valuesPub.put(DocuFindContract.TablaPublicacion.COLUMN_ID_USER, idUsuario);
            valuesPub.put(DocuFindContract.TablaPublicacion.COLUMN_ID_DOCUMENTO, (int) idDocInsertado);
            valuesPub.put(DocuFindContract.TablaPublicacion.COLUMN_CATEGORIA, categoriaSeleccionada.id);

            long idPub = db.insert(DocuFindContract.TablaPublicacion.TABLE_NAME, null, valuesPub);

            if (idPub != -1) {
                Toast.makeText(getContext(), "¡Publicación creada con éxito!", Toast.LENGTH_SHORT).show();
                etMensaje.setText("");
                uriArchivoSeleccionado = null;
                esFotoCamara = false;
                cargarMisPublicaciones();
            }
        }
        db.close();
    }

    private void cargarMisPublicaciones() {
        listaMisPublicaciones.clear();
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        int idUsuario = prefs.getInt("loggedUserId", -1);

        DocuFindDbHelper dbHelper = new DocuFindDbHelper(getContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT p." + DocuFindContract.TablaPublicacion.COLUMN_ID + ", " +
                "p." + DocuFindContract.TablaPublicacion.COLUMN_MESSAGE + ", " +
                "u." + DocuFindContract.TablaUsuarios.COLUMN_NAME + ", " +
                "d." + DocuFindContract.TablaDocumentos.COLUMN_NAME + ", " +
                "d." + DocuFindContract.TablaDocumentos.COLUMN_RUTADOCUMENTO + ", " +
                "p." + DocuFindContract.TablaPublicacion.COLUMN_ID_USER + ", " +
                "p." + DocuFindContract.TablaPublicacion.COLUMN_ID_DOCUMENTO + " " +
                "FROM " + DocuFindContract.TablaPublicacion.TABLE_NAME + " p " +
                "INNER JOIN " + DocuFindContract.TablaUsuarios.TABLE_NAME + " u " +
                "ON p." + DocuFindContract.TablaPublicacion.COLUMN_ID_USER + " = u." + DocuFindContract.TablaUsuarios.COLUMN_ID + " " +
                "INNER JOIN " + DocuFindContract.TablaDocumentos.TABLE_NAME + " d " +
                "ON p." + DocuFindContract.TablaPublicacion.COLUMN_ID_DOCUMENTO + " = d." + DocuFindContract.TablaDocumentos.COLUMN_ID + " " +
                "WHERE p." + DocuFindContract.TablaPublicacion.COLUMN_ID_USER + " = ? AND p." + DocuFindContract.TablaPublicacion.COLUMN_STATE + " = 1 " +
                "ORDER BY p." + DocuFindContract.TablaPublicacion.COLUMN_ID + " DESC";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(idUsuario)});

        if (cursor.moveToFirst()) {
            do {
                listaMisPublicaciones.add(new Publicacion(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getInt(5),
                        cursor.getInt(6)
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        adaptador.notifyDataSetChanged();
    }

    @Override
    public void onEditar(Publicacion publicacion) {
        uriArchivoEdicion = null;
        btnAdjuntarEdicionRef = null;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Editar Publicación");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputMensaje = new EditText(getContext());
        inputMensaje.setText(publicacion.getMensaje());
        layout.addView(inputMensaje);

        final Spinner spinnerEditCategoria = new Spinner(getContext());
        cargarCategoriasEnSpinner(spinnerEditCategoria);
        layout.addView(spinnerEditCategoria);

        final Button btnCambiarArchivo = new Button(getContext());
        btnCambiarArchivo.setText("Cambiar Archivo (PDF/Foto)");
        btnAdjuntarEdicionRef = btnCambiarArchivo;

        btnCambiarArchivo.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            filePickerEditLauncher.launch(intent);
        });
        layout.addView(btnCambiarArchivo);

        builder.setView(layout);

        builder.setPositiveButton("Guardar Cambios", (dialog, which) -> {
            String nuevoMensaje = inputMensaje.getText().toString().trim();
            CategoriaItem nuevaCat = (CategoriaItem) spinnerEditCategoria.getSelectedItem();

            if (!nuevoMensaje.isEmpty() && nuevaCat != null) {
                DocuFindDbHelper dbHelper = new DocuFindDbHelper(getContext());
                SQLiteDatabase db = dbHelper.getWritableDatabase();

                ContentValues valuesPub = new ContentValues();
                valuesPub.put(DocuFindContract.TablaPublicacion.COLUMN_MESSAGE, nuevoMensaje);
                valuesPub.put(DocuFindContract.TablaPublicacion.COLUMN_CATEGORIA, nuevaCat.id);

                db.update(DocuFindContract.TablaPublicacion.TABLE_NAME, valuesPub,
                        DocuFindContract.TablaPublicacion.COLUMN_ID + " = ?",
                        new String[]{String.valueOf(publicacion.getId())});

                if (uriArchivoEdicion != null) {
                    String nuevoNombreArchivo = obtenerNombreArchivo(uriArchivoEdicion);
                    String nuevaRutaFisica = guardarArchivoEnAlmacenamiento(uriArchivoEdicion, nuevoNombreArchivo);

                    if (nuevaRutaFisica != null) {
                        ContentValues valuesDoc = new ContentValues();
                        valuesDoc.put(DocuFindContract.TablaDocumentos.COLUMN_NAME, nuevoNombreArchivo);
                        valuesDoc.put(DocuFindContract.TablaDocumentos.COLUMN_NAMEFILE, nuevoNombreArchivo);
                        valuesDoc.put(DocuFindContract.TablaDocumentos.COLUMN_RUTADOCUMENTO, nuevaRutaFisica);
                        valuesDoc.put(DocuFindContract.TablaDocumentos.COLUMN_CATEGORIA, nuevaCat.id);

                        db.update(DocuFindContract.TablaDocumentos.TABLE_NAME, valuesDoc,
                                DocuFindContract.TablaDocumentos.COLUMN_ID + " = ?",
                                new String[]{String.valueOf(publicacion.getIdDocumento())});
                    }
                }

                db.close();
                cargarMisPublicaciones();
                Toast.makeText(getContext(), "Publicación actualizada", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public void onEliminar(Publicacion publicacion) {
        DocuFindDbHelper dbHelper = new DocuFindDbHelper(getContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DocuFindContract.TablaPublicacion.COLUMN_STATE, 0);

        db.update(DocuFindContract.TablaPublicacion.TABLE_NAME, values,
                DocuFindContract.TablaPublicacion.COLUMN_ID + " = ?",
                new String[]{String.valueOf(publicacion.getId())});

        db.close();
        cargarMisPublicaciones();
        Toast.makeText(getContext(), "Publicación eliminada", Toast.LENGTH_SHORT).show();
    }

    private Uri guardarBitmapComoArchivo(Bitmap bitmap) {
        try {
            String nombreFoto = "foto_camara_" + System.currentTimeMillis() + ".jpg";
            File carpeta = new File(requireContext().getFilesDir(), "docs_subidos");
            if (!carpeta.exists()) carpeta.mkdir();

            File archivoFoto = new File(carpeta, nombreFoto);
            FileOutputStream fos = new FileOutputStream(archivoFoto);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();

            return Uri.fromFile(archivoFoto);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String guardarArchivoEnAlmacenamiento(Uri uri, String nombre) {
        File carpeta = new File(requireContext().getFilesDir(), "docs_subidos");
        if (!carpeta.exists()) carpeta.mkdir();
        File destino = new File(carpeta, nombre);

        try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(destino)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) out.write(buffer, 0, len);
            return destino.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    private String obtenerNombreArchivo(Uri uri) {
        String res = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor c = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (i != -1) res = c.getString(i);
                }
            }
        }
        if (res == null) {
            res = uri.getPath();
            if (res != null) {
                int cut = res.lastIndexOf('/');
                if (cut != -1) res = res.substring(cut + 1);
            }
        }
        return res;
    }
}