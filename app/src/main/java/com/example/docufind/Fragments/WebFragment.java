package com.example.docufind.Fragments;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

    public WebFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_web, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewWebDocs);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        listaTodasLasPublicaciones = new ArrayList<>();
        // Invocamos el adaptador enviando esVistaPropia = false (No muestra botones de editar/borrar)
        adaptador = new PublicacionAdapter(listaTodasLasPublicaciones, getContext(), false, null);
        recyclerView.setAdapter(adaptador);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarFeedGlobal();
    }

    private void cargarFeedGlobal() {
        listaTodasLasPublicaciones.clear();
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
                "WHERE p." + DocuFindContract.TablaPublicacion.COLUMN_STATE + " = 1 " +
                "ORDER BY p." + DocuFindContract.TablaPublicacion.COLUMN_ID + " DESC";

        Cursor cursor = db.rawQuery(query, null);

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