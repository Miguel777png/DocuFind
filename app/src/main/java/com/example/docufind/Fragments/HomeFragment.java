package com.example.docufind.Fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
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

    // Variables para creación
    private Uri uriArchivoSeleccionado = null;
    private List<CategoriaItem> listaCategorias;

    // Variables para edición
    private Uri uriArchivoEdicion = null;
    private Button btnAdjuntarEdicionRef; // Referencia al botón del diálogo de edición

    // Clase auxiliar para manejar el Spinner
    private static class CategoriaItem {
        int id;
        String nombre;

        CategoriaItem(int id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre; // Lo que mostrará el Spinner visualmente
        }
    }

    // Launcher para crear nueva publicación
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    uriArchivoSeleccionado = result.getData().getData();
                    Toast.makeText(getContext(), "PDF adjuntado para crear", Toast.LENGTH_SHORT).show();
                }
            }
    );

    // Launcher específico para editar una publicación existente
    private final ActivityResultLauncher<Intent> filePickerEditLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    uriArchivoEdicion = result.getData().getData();
                    if (btnAdjuntarEdicionRef != null) {
                        btnAdjuntarEdicionRef.setText("PDF Seleccionado (Nuevo)");
                        btnAdjuntarEdicionRef.setBackgroundColor(0xFF4CAF50); // Verde para indicar éxito
                    }
                    Toast.makeText(getContext(), "Nuevo PDF seleccionado para edición", Toast.LENGTH_SHORT).show();
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

        btnPublicar.setOnClickListener(v -> crearPublicacion());

        cargarCategoriasEnSpinner(spinnerCategoria);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarMisPublicaciones();
    }

    // --- CARGA DE DATOS ---

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

    // --- CREACIÓN ---

    private void crearPublicacion() {
        String mensaje = etMensaje.getText().toString().trim();
        if (mensaje.isEmpty()) {
            Toast.makeText(getContext(), "Escribe un mensaje", Toast.LENGTH_SHORT).show();
            return;
        }

        if (uriArchivoSeleccionado == null) {
            Toast.makeText(getContext(), "Debes adjuntar un PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        if (spinnerCategoria.getSelectedItem() == null) {
            Toast.makeText(getContext(), "Debes seleccionar una categoría", Toast.LENGTH_SHORT).show();
            return;
        }

        CategoriaItem categoriaSeleccionada = (CategoriaItem) spinnerCategoria.getSelectedItem();
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        int idUsuario = prefs.getInt("loggedUserId", -1);

        if (idUsuario == -1) return;

        String nombreArchivo = obtenerNombreArchivo(uriArchivoSeleccionado);
        String rutaFisica = guardarArchivoEnAlmacenamiento(uriArchivoSeleccionado, nombreArchivo);

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
                Toast.makeText(getContext(), "¡Publicado con éxito!", Toast.LENGTH_SHORT).show();
                etMensaje.setText("");
                uriArchivoSeleccionado = null;
                cargarMisPublicaciones();
            }
        }
        db.close();
    }

    // --- CARGAR FEED PERSONAL ---

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

    // --- ACCIONES DE EDICIÓN Y SOFT DELETE ---

    @Override
    public void onEditar(Publicacion publicacion) {
        // Reiniciar variables de edición
        uriArchivoEdicion = null;
        btnAdjuntarEdicionRef = null;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Editar Publicación");

        // Crear un layout vertical para meter múltiples elementos (EditText, Spinner, Botón)
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // 1. EditText para el mensaje
        final EditText inputMensaje = new EditText(getContext());
        inputMensaje.setText(publicacion.getMensaje());
        layout.addView(inputMensaje);

        // 2. Spinner para cambiar la categoría
        final Spinner spinnerEditCategoria = new Spinner(getContext());
        cargarCategoriasEnSpinner(spinnerEditCategoria); // Reutilizamos el método para llenarlo
        layout.addView(spinnerEditCategoria);

        // 3. Botón para cambiar el archivo PDF
        final Button btnCambiarPdf = new Button(getContext());
        btnCambiarPdf.setText("Cambiar Documento (Opcional)");
        btnAdjuntarEdicionRef = btnCambiarPdf; // Guardamos la referencia para el Launcher

        btnCambiarPdf.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            filePickerEditLauncher.launch(intent);
        });
        layout.addView(btnCambiarPdf);

        builder.setView(layout);

        builder.setPositiveButton("Guardar Cambios", (dialog, which) -> {
            String nuevoMensaje = inputMensaje.getText().toString().trim();
            CategoriaItem nuevaCat = (CategoriaItem) spinnerEditCategoria.getSelectedItem();

            if (!nuevoMensaje.isEmpty() && nuevaCat != null) {
                DocuFindDbHelper dbHelper = new DocuFindDbHelper(getContext());
                SQLiteDatabase db = dbHelper.getWritableDatabase();

                // Lógica 1: Actualizar siempre Mensaje y Categoría en la tabla Publicación
                ContentValues valuesPub = new ContentValues();
                valuesPub.put(DocuFindContract.TablaPublicacion.COLUMN_MESSAGE, nuevoMensaje);
                valuesPub.put(DocuFindContract.TablaPublicacion.COLUMN_CATEGORIA, nuevaCat.id);

                db.update(DocuFindContract.TablaPublicacion.TABLE_NAME, valuesPub,
                        DocuFindContract.TablaPublicacion.COLUMN_ID + " = ?",
                        new String[]{String.valueOf(publicacion.getId())});

                // Lógica 2: Si seleccionó un nuevo PDF, actualizar la tabla Documentos
                if (uriArchivoEdicion != null) {
                    String nuevoNombreArchivo = obtenerNombreArchivo(uriArchivoEdicion);
                    String nuevaRutaFisica = guardarArchivoEnAlmacenamiento(uriArchivoEdicion, nuevoNombreArchivo);

                    if (nuevaRutaFisica != null) {
                        ContentValues valuesDoc = new ContentValues();
                        valuesDoc.put(DocuFindContract.TablaDocumentos.COLUMN_NAME, nuevoNombreArchivo);
                        valuesDoc.put(DocuFindContract.TablaDocumentos.COLUMN_NAMEFILE, nuevoNombreArchivo);
                        valuesDoc.put(DocuFindContract.TablaDocumentos.COLUMN_RUTADOCUMENTO, nuevaRutaFisica);
                        valuesDoc.put(DocuFindContract.TablaDocumentos.COLUMN_CATEGORIA, nuevaCat.id); // Actualizar categoría del doc también

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


    // --- MÉTODOS AUXILIARES ARCHIVOS ---

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
            int cut = res.lastIndexOf('/');
            if (cut != -1) res = res.substring(cut + 1);
        }
        return res;
    }
}