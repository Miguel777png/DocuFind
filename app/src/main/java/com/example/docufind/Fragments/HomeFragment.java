package com.example.docufind.Fragments;

import android.app.Activity;
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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.docufind.R;
import com.example.docufind.database.DocuFindContract;
import com.example.docufind.database.DocuFindDbHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FloatingActionButton btnSubirDoc;

    public HomeFragment() {
    }

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uriSeleccionada = result.getData().getData();
                    if (uriSeleccionada != null) {
                        procesarArchivoSeleccionado(uriSeleccionada);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        btnSubirDoc = view.findViewById(R.id.btnSubirDoc);

        btnSubirDoc.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            filePickerLauncher.launch(intent);
        });

        return view;
    }

    private void procesarArchivoSeleccionado(Uri uri) {
        String nombreArchivo = obtenerNombreArchivo(uri);
        if (nombreArchivo == null) {
            nombreArchivo = "documento_desconocido_" + System.currentTimeMillis() + ".pdf";
        }

        File carpetaDocs = new File(requireContext().getFilesDir(), "docs_subidos");
        if (!carpetaDocs.exists()) {
            carpetaDocs.mkdir();
        }

        File archivoDestino = new File(carpetaDocs, nombreArchivo);

        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(archivoDestino);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            if (inputStream != null) {
                inputStream.close();
            }

            guardarEnBaseDeDatos(nombreArchivo, archivoDestino.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error al copiar el archivo físico", Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarEnBaseDeDatos(String nombreArchivo, String rutaAbsoluta) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        int idUsuario = prefs.getInt("loggedUserId", -1);

        if (idUsuario == -1) {
            Toast.makeText(getContext(), "Error: Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            return;
        }

        DocuFindDbHelper dbHelper = new DocuFindDbHelper(getContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String fechaActual = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

        ContentValues values = new ContentValues();
        values.put(DocuFindContract.TablaDocumentos.COLUMN_NAME, nombreArchivo);
        values.put(DocuFindContract.TablaDocumentos.COLUMN_NAMEFILE, nombreArchivo);
        values.put(DocuFindContract.TablaDocumentos.COLUMN_DOCUMENTRUTE, rutaAbsoluta);
        values.put(DocuFindContract.TablaDocumentos.COLUMN_UPLOADDATE, fechaActual);
        values.put(DocuFindContract.TablaDocumentos.COLUMN_STATE, 1);
        values.put(DocuFindContract.TablaDocumentos.COLUMN_ID_USUARIO, idUsuario);

        long newRowId = db.insert(DocuFindContract.TablaDocumentos.TABLE_NAME, null, values);

        if (newRowId != -1) {
            Toast.makeText(getContext(), "¡Documento subido con éxito!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), "Error al guardar en SQLite", Toast.LENGTH_SHORT).show();
        }

        db.close();
    }

    private String obtenerNombreArchivo(Uri uri) {
        String resultado = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        resultado = cursor.getString(index);
                    }
                }
            }
        }
        if (resultado == null) {
            resultado = uri.getPath();
            int cut = resultado.lastIndexOf('/');
            if (cut != -1) {
                resultado = resultado.substring(cut + 1);
            }
        }
        return resultado;
    }
}