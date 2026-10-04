package com.example.docufind.Adaptadores;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.docufind.Modelos.Documento;
import com.example.docufind.R;

import java.io.File;
import java.util.List;

public class DocumentoAdapter extends RecyclerView.Adapter<DocumentoAdapter.DocumentoViewHolder> {

    private List<Documento> listaDocumentos;
    private Context context;

    public DocumentoAdapter(List<Documento> listaDocumentos, Context context) {
        this.listaDocumentos = listaDocumentos;
        this.context = context;
    }

    @NonNull
    @Override
    public DocumentoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_documento, parent, false);
        return new DocumentoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentoViewHolder holder, int position) {
        Documento docActual = listaDocumentos.get(position);

        holder.textTitulo.setText(docActual.getTitulo());
        holder.textAutor.setText("Subido por: " + docActual.getNombreAutor());
        holder.textFecha.setText(docActual.getFechaSubida());


        holder.itemView.setOnClickListener(v -> {


            File archivoPdf = new File(docActual.getRutaArchivo());

            if (archivoPdf.exists()) {

                Uri uriSegura = FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".provider",
                        archivoPdf
                );

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uriSegura, "application/pdf");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Damos permiso temporal de lectura


                try {
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(context, "No tienes ninguna aplicación instalada para leer PDFs", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(context, "El archivo físico ya no existe o fue movido", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaDocumentos.size();
    }

    public static class DocumentoViewHolder extends RecyclerView.ViewHolder {
        TextView textTitulo, textAutor, textFecha;
        ImageView iconDoc, iconDownload;

        public DocumentoViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitulo = itemView.findViewById(R.id.textDocTitulo);
            textAutor = itemView.findViewById(R.id.textDocAutor);
            textFecha = itemView.findViewById(R.id.textDocFecha);
            iconDoc = itemView.findViewById(R.id.iconDoc);
            iconDownload = itemView.findViewById(R.id.iconDownload);
        }
    }
}