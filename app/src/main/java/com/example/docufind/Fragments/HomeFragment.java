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
import android.widget.EditText;
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
    private FloatingActionButton btnPublicar;
    private RecyclerView recyclerView;
    private PublicacionAdapter adaptador;
    private List<Publicacion> listaMisPublicaciones;
    private Uri uriArchivoSeleccionado = null;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    uriArchivoSeleccionado = result.getData().getData();
                    Toast.makeText(getContext(), "PDF adjuntado correctamente", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Asegúrate de que los IDs coincidan con tu nuevo diseño de HomeFragment
        etMensaje = view.findViewById(R.id.etMensajePublicacion);
        btnPublicar = view.findViewById(R.id.btnSubirDoc);
        recyclerView = view.findViewById(R.id.recyclerViewMisDocs);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        listaMisPublicaciones = new ArrayList<>();
        adaptador = new PublicacionAdapter(listaMisPublicaciones, getContext(), true, this);
        recyclerView.setAdapter(adaptador);

        // Botón para seleccionar el PDF a adjuntar (ejemplo: un ImageButton)
        view.findViewById(R.id.btnAdjuntarPdf).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            filePickerLauncher.launch(intent);
        });

        // Botón para enviar la publicación
        btnPublicar.setOnClickListener(v -> crearPublicacion());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarMisPublicaciones();
    }

    private void crearPublicacion() {
        String mensaje = etMensaje.getText().toString().trim();
        if (mensaje.isEmpty()) {
            Toast.makeText(getContext(), "Escribe un mensaje para publicar", Toast.LENGTH_SHORT).show();
            return;
        }

        if (uriArchivoSeleccionado == null) {
            Toast.makeText(getContext(), "Debes adjuntar un archivo PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        int idUsuario = prefs.getInt("loggedUserId", -1);

        if (idUsuario == -1) return;

        // 1. Guardar primero el archivo físico y su registro en la tabla Documentos
        String nombreArchivo = obtenerNombreArchivo(uriArchivoSeleccionado);
        String rutaFisica = guardarArchivoEnAlmacenamiento(uriArchivoSeleccionado, nombreArchivo);

        if (rutaFisica == null) return;

        DocuFindDbHelper dbHelper = new DocuFindDbHelper(getContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

        // Insertar en tabla Documentos
        ContentValues valuesDoc = new ContentValues();
        valuesDoc.put(DocuFindContract.TablaDocumentos.COLUMN_NAME, nombreArchivo);
        valuesDoc.put(DocuFindContract.TablaDocumentos.COLUMN_NAMEFILE, nombreArchivo);
        valuesDoc.put(DocuFindContract.TablaDocumentos.COLUMN_RUTADOCUMENTO, rutaFisica);
        valuesDoc.put(DocuFindContract.TablaDocumentos.COLUMN_FECHASUBIDA, fecha);
        valuesDoc.put(DocuFindContract.TablaDocumentos.COLUMN_STATE, 1);
        valuesDoc.put(DocuFindContract.TablaDocumentos.COLUMN_ID_USUARIOS, idUsuario);
        valuesDoc.put(DocuFindContract.TablaDocumentos.COLUMN_CATEGORIA, 1); // Categoria Por Defecto

        long idDocInsertado = db.insert(DocuFindContract.TablaDocumentos.TABLE_NAME, null, valuesDoc);

        if (idDocInsertado != -1) {
            // 2. Insertar en la tabla Publicacion
            ContentValues valuesPub = new ContentValues();
            valuesPub.put(DocuFindContract.TablaPublicacion.COLUMN_STATE, 1);
            valuesPub.put(DocuFindContract.TablaPublicacion.COLUMN_MESSAGE, mensaje);
            valuesPub.put(DocuFindContract.TablaPublicacion.COLUMN_ID_USER, idUsuario);
            valuesPub.put(DocuFindContract.TablaPublicacion.COLUMN_ID_DOCUMENTO, (int) idDocInsertado);
            valuesPub.put(DocuFindContract.TablaPublicacion.COLUMN_CATEGORIA, 1);

            long idPub = db.insert(DocuFindContract.TablaPublicacion.TABLE_NAME, null, valuesPub);

            if (idPub != -1) {
                Toast.makeText(getContext(), "¡Publicado con éxito!", Toast.LENGTH_SHORT).show();
                etMensaje.setText("");
                uriArchivoSeleccionado = null;
                cargarMisPublicaciones(); // Refrescar la lista de mis publicaciones
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

        // Consulta filtrada por el ID del Usuario logueado y State = 1
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Editar Publicación");

        final EditText input = new EditText(getContext());
        input.setText(publicacion.getMensaje());
        builder.setView(input);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nuevoMensaje = input.getText().toString().trim();
            if (!nuevoMensaje.isEmpty()) {
                DocuFindDbHelper dbHelper = new DocuFindDbHelper(getContext());
                SQLiteDatabase db = dbHelper.getWritableDatabase();

                ContentValues values = new ContentValues();
                values.put(DocuFindContract.TablaPublicacion.COLUMN_MESSAGE, nuevoMensaje);

                db.update(DocuFindContract.TablaPublicacion.TABLE_NAME, values,
                        DocuFindContract.TablaPublicacion.COLUMN_ID + " = ?",
                        new String[]{String.valueOf(publicacion.getId())});

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