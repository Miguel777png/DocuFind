package com.example.docufind.Fragments;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebFragment extends Fragment {

    private RecyclerView recyclerView;
    private PublicacionAdapter adaptador;
    private List<Publicacion> listaTodasLasPublicaciones;

    private EditText etBuscar;
    private ImageButton btnBuscar;
    private Spinner spinnerFiltroCategoria;
    private List<CategoriaItem> listaCategoriasFiltro;

    // Executor de un solo hilo en segundo plano para encolar consultas SQLite de forma secuencial
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

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

    public WebFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_web, container, false);

        etBuscar = view.findViewById(R.id.etBuscarWeb);
        btnBuscar = view.findViewById(R.id.btnBuscarWeb);
        spinnerFiltroCategoria = view.findViewById(R.id.spinnerFiltroCategoria);
        recyclerView = view.findViewById(R.id.recyclerViewWebDocs);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        listaTodasLasPublicaciones = new ArrayList<>();
        adaptador = new PublicacionAdapter(listaTodasLasPublicaciones, getContext(), false, null);
        recyclerView.setAdapter(adaptador);

        // Búsqueda en tiempo real asíncrona
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                realizarBusqueda();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        etBuscar.setOnEditorActionListener((v, actionId, event) -> {
            realizarBusqueda();
            return true;
        });

        btnBuscar.setOnClickListener(v -> realizarBusqueda());

        cargarFiltroCategorias();

        spinnerFiltroCategoria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                realizarBusqueda();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        realizarBusqueda();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancela o destruye tareas pendientes al salir de la vista para evitar Memory Leaks
    }

    /**
     * Carga las categorías en segundo plano usando ExecutorService.
     */
    private void cargarFiltroCategorias() {
        executor.execute(() -> {
            if (getContext() == null) return;

            List<CategoriaItem> listaTemporal = new ArrayList<>();
            listaTemporal.add(new CategoriaItem(-1, "Todas las categorías"));

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
                        listaTemporal.add(new CategoriaItem(cursor.getInt(0), cursor.getString(1)));
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
            db.close();

            handler.post(() -> {
                if (getContext() == null || !isAdded()) return;
                listaCategoriasFiltro = listaTemporal;
                ArrayAdapter<CategoriaItem> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, listaCategoriasFiltro);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerFiltroCategoria.setAdapter(adapter);
            });
        });
    }

    /**
     * Realiza la consulta SQL multi-campo en un hilo en segundo plano (Worker Thread)
     * y actualiza la lista del RecyclerView en el hilo principal (Main Thread).
     */
    private void realizarBusqueda() {
        if (getContext() == null || etBuscar == null || spinnerFiltroCategoria == null) return;

        // Snapshot de los valores de la UI antes de enviarlos al hilo secundario
        String textoBusqueda = etBuscar.getText().toString().trim();
        CategoriaItem categoriaSeleccionada = (CategoriaItem) spinnerFiltroCategoria.getSelectedItem();
        int idCatSeleccionada = (categoriaSeleccionada != null) ? categoriaSeleccionada.id : -1;

        executor.execute(() -> {
            if (getContext() == null) return;

            List<Publicacion> listaResultados = new ArrayList<>();
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
                    .append("WHERE p.").append(DocuFindContract.TablaPublicacion.COLUMN_STATE).append(" = 1 ");

            List<String> argumentosLista = new ArrayList<>();

            if (!textoBusqueda.isEmpty()) {
                queryBase.append("AND (d.").append(DocuFindContract.TablaDocumentos.COLUMN_NAME).append(" LIKE ? ")
                        .append("OR p.").append(DocuFindContract.TablaPublicacion.COLUMN_MESSAGE).append(" LIKE ?) ");

                String parametro = "%" + textoBusqueda + "%";
                argumentosLista.add(parametro);
                argumentosLista.add(parametro);
            }

            if (idCatSeleccionada != -1) {
                queryBase.append("AND p.").append(DocuFindContract.TablaPublicacion.COLUMN_CATEGORIA).append(" = ? ");
                argumentosLista.add(String.valueOf(idCatSeleccionada));
            }

            queryBase.append("ORDER BY p.").append(DocuFindContract.TablaPublicacion.COLUMN_ID).append(" DESC");

            String[] argsArray = argumentosLista.toArray(new String[0]);
            Cursor cursor = db.rawQuery(queryBase.toString(), argsArray);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        listaResultados.add(new Publicacion(
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

            // Publicar resultados en el hilo principal
            handler.post(() -> {
                if (getContext() == null || !isAdded()) return;

                listaTodasLasPublicaciones.clear();
                listaTodasLasPublicaciones.addAll(listaResultados);
                adaptador.notifyDataSetChanged();
            });
        });
    }
}