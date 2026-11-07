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
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
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
import androidx.core.content.FileProvider;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fragmento principal para gestionar y visualizar las publicaciones del usuario en DocuFind.
 * Implementa concurrencia multihilo (ExecutorService), seguridad de almacenamiento (FileProvider),
 * consultas parametrizadas SQLite y protección de pantalla en RAM (FLAG_SECURE).
 */
public class HomeFragment extends Fragment implements PublicacionAdapter.OnPublicacionListener {

    private EditText etMensaje;
    private Spinner spinnerCategoria;
    private Spinner spinnerFiltroMisDocs;
    private FloatingActionButton btnPublicar;
    private RecyclerView recyclerView;
    private PublicacionAdapter adaptador;
    private List<Publicacion> listaMisPublicaciones;

    private Button btnAdjuntarPdf;
    private Button btnTomarFoto;

    private boolean esFotoCamara = false;

    private Uri uriArchivoSeleccionado = null;
    private List<CategoriaItem> listaCategoriasCrear;
    private List<CategoriaItem> listaCategoriasFiltro;

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

                    if (btnAdjuntarPdf != null) btnAdjuntarPdf.setText("✓ PDF Listo");
                    if (btnTomarFoto != null) btnTomarFoto.setText("📷 Tomar Foto");

                    Toast.makeText(getContext(), "PDF adjuntado correctamente", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    Bitmap imageBitmap = extras != null ? (Bitmap) extras.get("data") : null;

                    if (imageBitmap != null) {
                        uriArchivoSeleccionado = guardarBitmapComoArchivo(imageBitmap);
                        if (uriArchivoSeleccionado != null) {
                            esFotoCamara = true;

                            if (btnTomarFoto != null) btnTomarFoto.setText("✓ Foto Lista");
                            if (btnAdjuntarPdf != null) btnAdjuntarPdf.setText("📄 Adjuntar PDF");

                            Toast.makeText(getContext(), "¡Foto capturada con éxito!", Toast.LENGTH_SHORT).show();
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

    public HomeFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        etMensaje = view.findViewById(R.id.etMensajePublicacion);
        spinnerCategoria = view.findViewById(R.id.spinnerCategoria);
        spinnerFiltroMisDocs = view.findViewById(R.id.spinnerFiltroMisDocs);
        btnPublicar = view.findViewById(R.id.btnSubirDoc);
        recyclerView = view.findViewById(R.id.recyclerViewMisDocs);
        btnAdjuntarPdf = view.findViewById(R.id.btnAdjuntarPdf);
        btnTomarFoto = view.findViewById(R.id.btnTomarFoto);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        listaMisPublicaciones = new ArrayList<>();
        adaptador = new PublicacionAdapter(listaMisPublicaciones, getContext(), true, this);
        recyclerView.setAdapter(adaptador);

        btnAdjuntarPdf.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            filePickerLauncher.launch(intent);
        });

        btnTomarFoto.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                abrirCamara();
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        btnPublicar.setOnClickListener(v -> crearPublicacion());

        cargarCategoriasSpinners();

        spinnerFiltroMisDocs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                cargarMisPublicaciones();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        return view;
    }

    /**
     * Lanza un Intent implícito para abrir la cámara nativa del dispositivo.
     */
    private void abrirCamara() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(getContext(), "No se encontró cámara", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Aplicación de FLAG_SECURE: bloquea capturas de pantalla y oculta la vista previa en Apps Recientes
        if (getActivity() != null) {
            getActivity().getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
            );
        }

        cargarMisPublicaciones();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Limpieza opcional de la bandera al salir de este Fragment para no afectar otras vistas
        if (getActivity() != null) {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    /**
     * Carga las categorías activas desde la base de datos SQLite en segundo plano y puebla los Spinners en el Main Thread.
     */
    private void cargarCategoriasSpinners() {
        listaCategoriasCrear = new ArrayList<>();
        listaCategoriasFiltro = new ArrayList<>();
        listaCategoriasFiltro.add(new CategoriaItem(-1, "Todas las categorías"));

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            if (getContext() == null) return;

            DocuFindDbHelper dbHelper = new DocuFindDbHelper(getContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor cursor = db.query(
                    DocuFindContract.TablaCategoria.TABLE_NAME,
                    new String[]{DocuFindContract.TablaCategoria.COLUMN_ID, DocuFindContract.TablaCategoria.COLUMN_NAME},
                    DocuFindContract.TablaCategoria.COLUMN_STATE + " = 1",
                    null, null, null, null
            );

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        CategoriaItem cat = new CategoriaItem(cursor.getInt(0), cursor.getString(1));
                        listaCategoriasCrear.add(cat);
                        listaCategoriasFiltro.add(cat);
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
            db.close();

            handler.post(() -> {
                if (getContext() == null || !isAdded()) return;

                ArrayAdapter<CategoriaItem> adapterCrear = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, listaCategoriasCrear);
                adapterCrear.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCategoria.setAdapter(adapterCrear);

                ArrayAdapter<CategoriaItem> adapterFiltro = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, listaCategoriasFiltro);
                adapterFiltro.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerFiltroMisDocs.setAdapter(adapterFiltro);
            });
        });
    }

    /**
     * Valida el formulario en el Hilo Principal, procesa la copia física del archivo y ejecuta la transacción SQLite en segundo plano.
     */
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

        if (idUsuario == -1) {
            Toast.makeText(getContext(), "Error: Sesión no válida", Toast.LENGTH_SHORT).show();
            return;
        }

        // Snapshot seguro de las variables globales antes de delegar a segundo plano
        Uri uriActual = uriArchivoSeleccionado;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            if (getContext() == null) return;

            String nombreArchivo = obtenerNombreArchivo(uriActual);
            if (nombreArchivo == null) {
                nombreArchivo = "doc_" + System.currentTimeMillis();
            }

            String rutaFisica = guardarArchivoEnAlmacenamiento(uriActual, nombreArchivo);

            if (rutaFisica == null) {
                handler.post(() -> {
                    if (getContext() != null && isAdded()) {
                        Toast.makeText(getContext(), "Error al guardar el archivo", Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }

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
            boolean exitoInsercion = false;

            if (idDocInsertado != -1) {
                ContentValues valuesPub = new ContentValues();
                valuesPub.put(DocuFindContract.TablaPublicacion.COLUMN_STATE, 1);
                valuesPub.put(DocuFindContract.TablaPublicacion.COLUMN_MESSAGE, mensaje);
                valuesPub.put(DocuFindContract.TablaPublicacion.COLUMN_ID_USER, idUsuario);
                valuesPub.put(DocuFindContract.TablaPublicacion.COLUMN_ID_DOCUMENTO, (int) idDocInsertado);
                valuesPub.put(DocuFindContract.TablaPublicacion.COLUMN_CATEGORIA, categoriaSeleccionada.id);

                long idPub = db.insert(DocuFindContract.TablaPublicacion.TABLE_NAME, null, valuesPub);
                if (idPub != -1) {
                    exitoInsercion = true;
                }
            }
            db.close();

            final boolean finalExito = exitoInsercion;

            handler.post(() -> {
                if (getContext() == null || !isAdded()) return;

                if (finalExito) {
                    Toast.makeText(getContext(), "¡Publicación creada con éxito!", Toast.LENGTH_SHORT).show();

                    etMensaje.setText("");
                    uriArchivoSeleccionado = null;
                    esFotoCamara = false;

                    if (btnAdjuntarPdf != null) btnAdjuntarPdf.setText("📄 Adjuntar PDF");
                    if (btnTomarFoto != null) btnTomarFoto.setText("📷 Tomar Foto");

                    cargarMisPublicaciones();
                } else {
                    Toast.makeText(getContext(), "Error al guardar la publicación en la base de datos.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * Carga las publicaciones activas del usuario en sesión desde SQLite utilizando consultas parametrizadas en segundo plano.
     */
    private void cargarMisPublicaciones() {
        if (getContext() == null || !isAdded()) return;

        SharedPreferences prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        int idUsuario = prefs.getInt("loggedUserId", -1);
        if (idUsuario == -1) return;

        CategoriaItem filtroSeleccionado = null;
        if (spinnerFiltroMisDocs != null) {
            filtroSeleccionado = (CategoriaItem) spinnerFiltroMisDocs.getSelectedItem();
        }
        int idCatFiltro = (filtroSeleccionado != null) ? filtroSeleccionado.id : -1;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            if (getContext() == null) return;

            List<Publicacion> listaTemporal = new ArrayList<>();
            DocuFindDbHelper dbHelper = new DocuFindDbHelper(getContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            StringBuilder queryBase = new StringBuilder();
            queryBase.append("SELECT p.").append(DocuFindContract.TablaPublicacion.COLUMN_ID).append(", ")
                    .append("p.").append(DocuFindContract.TablaPublicacion.COLUMN_MESSAGE).append(", ")
                    .append("u.").append(DocuFindContract.TablaUsuarios.COLUMN_NAME).append(", ")
                    .append("d.").append(DocuFindContract.TablaDocumentos.COLUMN_NAME).append(", ")
                    .append("d.").append(DocuFindContract.TablaDocumentos.COLUMN_RUTADOCUMENTO).append(", ")
                    .append("p.").append(DocuFindContract.TablaPublicacion.COLUMN_ID_USER).append(", ")
                    .append("p.").append(DocuFindContract.TablaPublicacion.COLUMN_ID_DOCUMENTO).append(" ")
                    .append("FROM ").append(DocuFindContract.TablaPublicacion.TABLE_NAME).append(" p ")
                    .append("INNER JOIN ").append(DocuFindContract.TablaUsuarios.TABLE_NAME).append(" u ")
                    .append("ON p.").append(DocuFindContract.TablaPublicacion.COLUMN_ID_USER).append(" = u.").append(DocuFindContract.TablaUsuarios.COLUMN_ID).append(" ")
                    .append("INNER JOIN ").append(DocuFindContract.TablaDocumentos.TABLE_NAME).append(" d ")
                    .append("ON p.").append(DocuFindContract.TablaPublicacion.COLUMN_ID_DOCUMENTO).append(" = d.").append(DocuFindContract.TablaDocumentos.COLUMN_ID).append(" ")
                    .append("WHERE p.").append(DocuFindContract.TablaPublicacion.COLUMN_ID_USER).append(" = ? AND p.").append(DocuFindContract.TablaPublicacion.COLUMN_STATE).append(" = 1 ");

            List<String> argumentosLista = new ArrayList<>();
            argumentosLista.add(String.valueOf(idUsuario));

            if (idCatFiltro != -1) {
                queryBase.append("AND p.").append(DocuFindContract.TablaPublicacion.COLUMN_CATEGORIA).append(" = ? ");
                argumentosLista.add(String.valueOf(idCatFiltro));
            }

            queryBase.append("ORDER BY p.").append(DocuFindContract.TablaPublicacion.COLUMN_ID).append(" DESC");

            String[] argsArray = argumentosLista.toArray(new String[0]);
            Cursor cursor = db.rawQuery(queryBase.toString(), argsArray);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        listaTemporal.add(new Publicacion(
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
            }
            db.close();

            handler.post(() -> {
                if (getContext() == null || !isAdded()) return;

                listaMisPublicaciones.clear();
                listaMisPublicaciones.addAll(listaTemporal);
                adaptador.notifyDataSetChanged();
            });
        });
    }

    /**
     * Muestra un diálogo para editar texto, categoría y documento en segundo plano.
     *
     * @param publicacion Publicación que se va a modificar.
     */
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

        ArrayAdapter<CategoriaItem> adapterEdit = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, listaCategoriasCrear);
        adapterEdit.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEditCategoria.setAdapter(adapterEdit);

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

            if (nuevoMensaje.isEmpty() || nuevaCat == null) {
                Toast.makeText(getContext(), "Por favor completa los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri uriEdicionFinal = uriArchivoEdicion;

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                if (getContext() == null) return;

                DocuFindDbHelper dbHelper = new DocuFindDbHelper(getContext());
                SQLiteDatabase db = dbHelper.getWritableDatabase();

                ContentValues valuesPub = new ContentValues();
                valuesPub.put(DocuFindContract.TablaPublicacion.COLUMN_MESSAGE, nuevoMensaje);
                valuesPub.put(DocuFindContract.TablaPublicacion.COLUMN_CATEGORIA, nuevaCat.id);

                db.update(DocuFindContract.TablaPublicacion.TABLE_NAME, valuesPub,
                        DocuFindContract.TablaPublicacion.COLUMN_ID + " = ?",
                        new String[]{String.valueOf(publicacion.getId())});

                if (uriEdicionFinal != null) {
                    String nuevoNombreArchivo = obtenerNombreArchivo(uriEdicionFinal);
                    String nuevaRutaFisica = guardarArchivoEnAlmacenamiento(uriEdicionFinal, nuevoNombreArchivo);

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

                handler.post(() -> {
                    if (getContext() == null || !isAdded()) return;

                    cargarMisPublicaciones();
                    Toast.makeText(getContext(), "Publicación actualizada", Toast.LENGTH_SHORT).show();
                });
            });
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Realiza el borrado lógico de una publicación cambiando su estado a inactivo (0) en segundo plano.
     *
     * @param publicacion Publicación que se va a eliminar.
     */
    @Override
    public void onEliminar(Publicacion publicacion) {
        if (getContext() == null) return;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            if (getContext() == null) return;

            DocuFindDbHelper dbHelper = new DocuFindDbHelper(getContext());
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(DocuFindContract.TablaPublicacion.COLUMN_STATE, 0);

            db.update(DocuFindContract.TablaPublicacion.TABLE_NAME, values,
                    DocuFindContract.TablaPublicacion.COLUMN_ID + " = ?",
                    new String[]{String.valueOf(publicacion.getId())});

            db.close();

            handler.post(() -> {
                if (getContext() == null || !isAdded()) return;

                cargarMisPublicaciones();
                Toast.makeText(getContext(), "Publicación eliminada", Toast.LENGTH_SHORT).show();
            });
        });
    }

    /**
     * Guarda una imagen Bitmap en almacenamiento interno generando una Uri segura de tipo content:// mediante FileProvider.
     *
     * @param bitmap Imagen capturada por la cámara.
     * @return Uri segura con esquema content:// o null si ocurre un error.
     */
    private Uri guardarBitmapComoArchivo(Bitmap bitmap) {
        try {
            String nombreFoto = "foto_camara_" + System.currentTimeMillis() + ".jpg";
            File carpeta = new File(requireContext().getFilesDir(), "docs_subidos");
            if (!carpeta.exists()) {
                carpeta.mkdirs();
            }

            File archivoFoto = new File(carpeta, nombreFoto);
            try (FileOutputStream fos = new FileOutputStream(archivoFoto)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.flush();
            }

            String authority = requireContext().getPackageName() + ".fileprovider";
            return FileProvider.getUriForFile(requireContext(), authority, archivoFoto);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Copia un archivo de origen (PDF o Foto) mediante InputStream/FileOutputStream hacia el almacenamiento interno de la app.
     *
     * @param uri    Uri del archivo de origen.
     * @param nombre Nombre del archivo de destino.
     * @return Ruta absoluta del archivo guardado en almacenamiento interno o null si ocurre un error.
     */
    private String guardarArchivoEnAlmacenamiento(Uri uri, String nombre) {
        if (getContext() == null) return null;

        File carpeta = new File(requireContext().getFilesDir(), "docs_subidos");
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }
        File destino = new File(carpeta, nombre);

        try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(destino)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            return destino.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Obtiene el nombre con extensión de un archivo utilizando su Uri (content:// o file://).
     *
     * @param uri Uri del archivo.
     * @return Nombre extraído del archivo o null si no es posible determinarlo.
     */
    private String obtenerNombreArchivo(Uri uri) {
        if (getContext() == null || uri == null) return null;
        String res = null;

        if ("content".equals(uri.getScheme())) {
            try (Cursor c = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (i != -1) res = c.getString(i);
                }
            } catch (Exception e) {
                e.printStackTrace();
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