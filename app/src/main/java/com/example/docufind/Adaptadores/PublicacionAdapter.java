package com.example.docufind.Adaptadores;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.docufind.Modelos.Publicacion;
import com.example.docufind.R;

import java.io.File;
import java.util.List;

public class PublicacionAdapter extends RecyclerView.Adapter<PublicacionAdapter.PublicacionViewHolder> {

    private final List<Publicacion> listaPublicaciones;
    private final Context context;
    private final boolean esVistaPropia; // True en HomeFragment (muestra botones de acción), False en WebFragment
    private final OnPublicacionListener listener;

    public interface OnPublicacionListener {
        void onEditar(Publicacion publicacion);
        void onEliminar(Publicacion publicacion);
    }

    public PublicacionAdapter(List<Publicacion> listaPublicaciones, Context context, boolean esVistaPropia, OnPublicacionListener listener) {
        this.listaPublicaciones = listaPublicaciones;
        this.context = context;
        this.esVistaPropia = esVistaPropia;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PublicacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_publicacion, parent, false);
        return new PublicacionViewHolder(view);
    }

    /**
     * Vincula los datos de una publicación con la vista (ViewHolder) en la posición indicada.
     * Muestra u oculta los botones de editar y eliminar según si la vista corresponde al perfil propio del usuario.
     *
     * @param holder ViewHolder donde se asignarán los datos de la publicación.
     * @param position Posición del elemento dentro de la lista.
     */
    @Override
    public void onBindViewHolder(@NonNull PublicacionViewHolder holder, int position) {
        Publicacion pub = listaPublicaciones.get(position);

        holder.textAutor.setText(pub.getNombreAutor());
        holder.textMensaje.setText(pub.getMensaje());
        holder.textDocNombre.setText(pub.getNombreDocumento());


        if (esVistaPropia) {
            holder.btnEditar.setVisibility(View.VISIBLE);
            holder.btnEliminar.setVisibility(View.VISIBLE);

            holder.btnEditar.setOnClickListener(v -> {
                if (listener != null) listener.onEditar(pub);
            });

            holder.btnEliminar.setOnClickListener(v -> {
                if (listener != null) listener.onEliminar(pub);
            });
        } else {
            holder.btnEditar.setVisibility(View.GONE);
            holder.btnEliminar.setVisibility(View.GONE);
        }


        holder.layoutDocContainer.setOnClickListener(v -> abrirDocumento(pub.getRutaDocumento()));
    }

    /**
     * Abre un archivo local (PDF o imagen) usando FileProvider e Intents implícitos.
     * Detecta dinámicamente el tipo MIME según la extensión para lanzarlo con la aplicación adecuada.
     *
     * @param rutaArchivo Ruta física absoluta del archivo que se desea visualizar.
     */
    private void abrirDocumento(String rutaArchivo) {
        if (rutaArchivo == null || rutaArchivo.isEmpty()) {
            Toast.makeText(context, "Esta publicación no incluye archivo adjunto", Toast.LENGTH_SHORT).show();
            return;
        }

        File archivo = new File(rutaArchivo);
        if (!archivo.exists()) {
            Toast.makeText(context, "El archivo físico ya no existe", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri uriSegura = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", archivo);
            Intent intent = new Intent(Intent.ACTION_VIEW);

            // --- NUEVA LÓGICA: Detección dinámica del tipo de archivo (MIME Type) ---
            String mimeType = "*/*"; // Tipo por defecto (cualquier archivo)
            String rutaLowerCase = rutaArchivo.toLowerCase();

            if (rutaLowerCase.endsWith(".pdf")) {
                mimeType = "application/pdf";
            } else if (rutaLowerCase.endsWith(".jpg") || rutaLowerCase.endsWith(".jpeg")) {
                mimeType = "image/jpeg";
            } else if (rutaLowerCase.endsWith(".png")) {
                mimeType = "image/png";
            }

            // Le decimos a Android exactamente qué tipo de archivo es
            intent.setDataAndType(uriSegura, mimeType);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(intent);

        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No tienes una app instalada para abrir este tipo de archivo", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(context, "Error al intentar abrir el archivo", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return listaPublicaciones.size();
    }

    public static class PublicacionViewHolder extends RecyclerView.ViewHolder {
        TextView textAutor, textMensaje, textDocNombre;
        View layoutDocContainer;
        ImageButton btnEditar, btnEliminar;

        public PublicacionViewHolder(@NonNull View itemView) {
            super(itemView);
            textAutor = itemView.findViewById(R.id.textPubAutor);
            textMensaje = itemView.findViewById(R.id.textPubMensaje);
            textDocNombre = itemView.findViewById(R.id.textPubDocNombre);
            layoutDocContainer = itemView.findViewById(R.id.layoutDocContainer);
            btnEditar = itemView.findViewById(R.id.btnEditarPub);
            btnEliminar = itemView.findViewById(R.id.btnEliminarPub);
        }
    }
}