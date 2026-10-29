package com.example.docufind.Fragments;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
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

public class WebFragment extends Fragment {

    private RecyclerView recyclerView;
    private PublicacionAdapter adaptador;
    private List<Publicacion> listaTodasLasPublicaciones;


    private EditText etBuscar;
    private ImageButton btnBuscar;
    private Spinner spinnerFiltroCategoria;
    private List<CategoriaItem> listaCategoriasFiltro;


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


        cargarFiltroCategorias();


        btnBuscar.setOnClickListener(v -> realizarBusqueda());


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


    /**
     * Consulta las categorías activas en la base de datos y las carga en el Spinner de filtros.
     * Inicializa la lista insertando una opción predeterminada ("Todas las categorías") en la
     * primera posición, y luego recorre el cursor para poblar el resto de opciones disponibles.
     *
     * @throws android.database.SQLException Si ocurre un error al consultar la tabla de categorías.
     */
    private void cargarFiltroCategorias() {
        listaCategoriasFiltro = new ArrayList<>();
        // Opción predeterminada para ver todos los documentos sin importar la categoría
        listaCategoriasFiltro.add(new CategoriaItem(-1, "Todas las categorías"));

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
                listaCategoriasFiltro.add(new CategoriaItem(cursor.getInt(0), cursor.getString(1)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        ArrayAdapter<CategoriaItem> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, listaCategoriasFiltro);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFiltroCategoria.setAdapter(adapter);
    }

    /**
     * Ejecuta una consulta dinámica en la base de datos para filtrar las publicaciones del muro.
     * Lee el texto ingresado por el usuario y la categoría seleccionada en el UI, construyendo
     * un INNER JOIN para obtener los datos cruzados de publicaciones, usuarios y documentos.
     * Finalmente, actualiza la lista local y notifica al adaptador para refrescar la vista.
     *
     * @throws android.database.SQLException Si ocurre un error de sintaxis al ejecutar el rawQuery.
     */
    private void realizarBusqueda() {
        listaTodasLasPublicaciones.clear();
        String textoBusqueda = etBuscar.getText().toString().trim();

        CategoriaItem categoriaSeleccionada = (CategoriaItem) spinnerFiltroCategoria.getSelectedItem();
        int idCatSeleccionada = (categoriaSeleccionada != null) ? categoriaSeleccionada.id : -1;

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
            queryBase.append("AND d.").append(DocuFindContract.TablaDocumentos.COLUMN_NAME).append(" LIKE ? ");
            argumentosLista.add("%" + textoBusqueda + "%");
        }


        if (idCatSeleccionada != -1) {
            queryBase.append("AND p.").append(DocuFindContract.TablaPublicacion.COLUMN_CATEGORIA).append(" = ? ");
            argumentosLista.add(String.valueOf(idCatSeleccionada));
        }


        queryBase.append("ORDER BY p.").append(DocuFindContract.TablaPublicacion.COLUMN_ID).append(" DESC");

        String[] argsArray = argumentosLista.toArray(new String[0]);
        Cursor cursor = db.rawQuery(queryBase.toString(), argsArray);

        if (cursor.moveToFirst()) {
            do {
                listaTodasLasPublicaciones.add(new Publicacion(
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
}