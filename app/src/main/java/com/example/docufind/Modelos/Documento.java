package com.example.docufind.Modelos;
public class Documento {
    private int id;
    private String titulo;
    private String nombreAutor;
    private String fechaSubida;
    private String rutaArchivo;

    public Documento(int id, String titulo, String nombreAutor, String fechaSubida, String rutaArchivo) {
        this.id = id;
        this.titulo = titulo;
        this.nombreAutor = nombreAutor;
        this.fechaSubida = fechaSubida;
        this.rutaArchivo = rutaArchivo;
    }


    public int getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getNombreAutor() { return nombreAutor; }
    public String getFechaSubida() { return fechaSubida; }
    public String getRutaArchivo() { return rutaArchivo; }
}