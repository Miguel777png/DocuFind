package com.example.docufind.Adaptadores;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.docufind.Modelos.Publicacion;
import com.example.docufind.R;

import java.io.File;
import java.util.List;

/**
 * Adaptador para el {@link RecyclerView} encargado de gestionar y renderizar la lista de publicaciones.
 * Maneja la visualización diferenciada entre archivos PDF y fotografías a tamaño completo,
 * así como los eventos de interacción (abrir, editar y eliminar).
 */
public class PublicacionAdapter extends RecyclerView.Adapter<PublicacionAdapter.PublicacionViewHolder> {

    private final List<Publicacion> listaPublicaciones;
    private final Context context;
    private final boolean esVistaPropia;
    private final OnPublicacionListener listener;

    /**
     * Interfaz de comunicación para capturar y gestionar las acciones sobre una publicación.
     */
    public interface OnPublicacionListener {
        /**
         * Notifica la solicitud de edición de una publicación.
         *
         * @param publicacion Objeto {@link Publicacion} a modificar.
         */
        void onEditar(Publicacion publicacion);

        /**
         * Notifica la solicitud de eliminación de una publicación.
         *
         * @param publicacion Objeto {@link Publicacion} a eliminar.
         */
        void onEliminar(Publicacion publicacion);
    }

    /**
     * Constructor del adaptador.
     *
     * @param listaPublicaciones Lista de publicaciones a renderizar en la vista.
     * @param context Contexto de la aplicación o actividad que invoca al adaptador.
     * @param esVistaPropia {@code true} si la vista pertenece al usuario en sesión (habilita botones de acción), {@code false} en caso contrario.
     * @param listener Implementación de {@link OnPublicacionListener} para responder a los eventos de usuario.
     */
    public PublicacionAdapter(List<Publicacion> listaPublicaciones, Context context, boolean esVistaPropia, OnPublicacionListener listener) {
        this.listaPublicaciones = listaPublicaciones;
        this.context = context;
        this.esVistaPropia = esVistaPropia;
        this.listener = listener;
    }

    /**
     * Infla la vista del elemento individual e instancia el ViewHolder correspondiente.
     *
     * @param parent Vista padre en la que se integrará el elemento.
     * @param viewType Tipo de vista del elemento.
     * @return Nueva instancia de {@link PublicacionViewHolder}.
     */
    @NonNull
    @Override
    public PublicacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_publicacion, parent, false);
        return new PublicacionViewHolder(view);
    }

    /**
     * Vincula los datos de un objeto {@link Publicacion} con los elementos de interfaz del ViewHolder.
     * Aplica la lógica de visibilidad condicional según el tipo de archivo (PDF o fotografía).
     *
     * @param holder Contenedor de vistas {@link PublicacionViewHolder}.
     * @param position Posición del elemento dentro de la lista.
     */
    @Override
    public void onBindViewHolder(@NonNull PublicacionViewHolder holder, int position) {
        Publicacion pub = listaPublicaciones.get(position);

        holder.textAutor.setText(pub.getNombreAutor());
        holder.textMensaje.setText(pub.getMensaje());
        holder.textDocNombre.setText(pub.getNombreDocumento());

        String ruta = pub.getRutaDocumento();
        if (ruta != null && !ruta.isEmpty()) {
            String rutaLower = ruta.toLowerCase();

            if (rutaLower.endsWith(".pdf")) {
                // Muestra el contenedor de PDF y oculta la imagen completa
                holder.layoutDocContainer.setVisibility(View.VISIBLE);
                if (holder.imgFotoFull != null) holder.imgFotoFull.setVisibility(View.GONE);

                if (holder.imgDocPreview != null) {
                    holder.imgDocPreview.setImageResource(android.R.drawable.ic_menu_agenda);
                    holder.imgDocPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
                }
            } else if (rutaLower.endsWith(".jpg") || rutaLower.endsWith(".jpeg") || rutaLower.endsWith(".png")) {
                // Oculta el contenedor de PDF y muestra la fotografía a tamaño completo
                holder.layoutDocContainer.setVisibility(View.GONE);

                if (holder.imgFotoFull != null) {
                    File archivoFoto = new File(ruta);
                    if (archivoFoto.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(archivoFoto.getAbsolutePath());
                        holder.imgFotoFull.setImageBitmap(bitmap);
                        holder.imgFotoFull.setVisibility(View.VISIBLE);
                    } else {
                        holder.imgFotoFull.setVisibility(View.GONE);
                    }
                }
            } else {
                holder.layoutDocContainer.setVisibility(View.VISIBLE);
                if (holder.imgFotoFull != null) holder.imgFotoFull.setVisibility(View.GONE);
            }
        } else {
            holder.layoutDocContainer.setVisibility(View.GONE);
            if (holder.imgFotoFull != null) holder.imgFotoFull.setVisibility(View.GONE);
        }

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
        if (holder.imgFotoFull != null) {
            holder.imgFotoFull.setOnClickListener(v -> abrirDocumento(pub.getRutaDocumento()));
        }
    }

    /**
     * Abre un archivo local (PDF o imagen) mediante {@link FileProvider} e Intents implícitos.
     * Evalúa la extensión para asignar el tipo MIME correspondiente y lanzar la app adecuada.
     *
     * @param rutaArchivo Ruta física absoluta del archivo almacenado.
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

            String mimeType = "*/*";
            String rutaLowerCase = rutaArchivo.toLowerCase();

            if (rutaLowerCase.endsWith(".pdf")) {
                mimeType = "application/pdf";
            } else if (rutaLowerCase.endsWith(".jpg") || rutaLowerCase.endsWith(".jpeg")) {
                mimeType = "image/jpeg";
            } else if (rutaLowerCase.endsWith(".png")) {
                mimeType = "image/png";
            }

            intent.setDataAndType(uriSegura, mimeType);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(intent);

        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No tienes una app instalada para abrir este tipo de archivo", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(context, "Error al intentar abrir el archivo", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Retorna el número total de publicaciones presentes en la lista.
     *
     * @return Cantidad de elementos de la lista.
     */
    @Override
    public int getItemCount() {
        return listaPublicaciones.size();
    }

    /**
     * Clase contenedora que mantiene las referencias a los componentes visuales de un elemento.
     */
    public static class PublicacionViewHolder extends RecyclerView.ViewHolder {
        TextView textAutor, textMensaje, textDocNombre;
        View layoutDocContainer;
        ImageButton btnEditar, btnEliminar;
        ImageView imgDocPreview, imgFotoFull;

        /**
         * Constructor del ViewHolder. Inicializa la vinculación de vistas mediante sus identificadores XML.
         *
         * @param itemView Vista del elemento individual.
         */
        public PublicacionViewHolder(@NonNull View itemView) {
            super(itemView);
            textAutor = itemView.findViewById(R.id.textPubAutor);
            textMensaje = itemView.findViewById(R.id.textPubMensaje);
            textDocNombre = itemView.findViewById(R.id.textPubDocNombre);
            layoutDocContainer = itemView.findViewById(R.id.layoutDocContainer);
            btnEditar = itemView.findViewById(R.id.btnEditarPub);
            btnEliminar = itemView.findViewById(R.id.btnEliminarPub);
            imgDocPreview = itemView.findViewById(R.id.imgDocPreview);
            imgFotoFull = itemView.findViewById(R.id.imgFotoFull);
        }
    }
}