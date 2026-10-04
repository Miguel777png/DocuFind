package com.example.docufind.Fragments; // O el paquete donde tengas tus fragmentos web

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

import com.example.docufind.Adaptadores.DocumentoAdapter;
import com.example.docufind.Modelos.Documento;
import com.example.docufind.R;
import com.example.docufind.database.DocuFindContract;
import com.example.docufind.database.DocuFindDbHelper;

import java.util.ArrayList;
import java.util.List;

public class WebFragment extends Fragment {

    private RecyclerView recyclerView;
    private DocumentoAdapter adaptador;
    private List<Documento> listaDocumentos;

    public WebFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_web, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewWebDocs);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        listaDocumentos = new ArrayList<>();
        adaptador = new DocumentoAdapter(listaDocumentos, getContext());
        recyclerView.setAdapter(adaptador);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        cargarDocumentos();
    }

    private void cargarDocumentos() {
        listaDocumentos.clear();
        DocuFindDbHelper dbHelper = new DocuFindDbHelper(getContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT d." + DocuFindContract.TablaDocumentos.COLUMN_ID + ", " +
                "d." + DocuFindContract.TablaDocumentos.COLUMN_NAME + ", " +
                "d." + DocuFindContract.TablaDocumentos.COLUMN_UPLOADDATE + ", " +
                "d." + DocuFindContract.TablaDocumentos.COLUMN_DOCUMENTRUTE + ", " +
                "u." + DocuFindContract.TablaUsuarios.COLUMN_NAME + " " +
                "FROM " + DocuFindContract.TablaDocumentos.TABLE_NAME + " d " +
                "INNER JOIN " + DocuFindContract.TablaUsuarios.TABLE_NAME + " u " +
                "ON d." + DocuFindContract.TablaDocumentos.COLUMN_ID_USUARIO + " = u." + DocuFindContract.TablaUsuarios.COLUMN_ID +
                " ORDER BY d." + DocuFindContract.TablaDocumentos.COLUMN_ID + " DESC";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String titulo = cursor.getString(1);
                String fecha = cursor.getString(2);
                String ruta = cursor.getString(3);
                String autor = cursor.getString(4);

                listaDocumentos.add(new Documento(id, titulo, autor, fecha, ruta));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        adaptador.notifyDataSetChanged();
    }
}