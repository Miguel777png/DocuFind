package com.example.docufind.Modelos;

public class Publicacion {
    private int id;
    private String mensaje;
    private String nombreAutor;
    private String nombreDocumento;
    private String rutaDocumento;
    private int idUsuario;
    private int idDocumento;

    public Publicacion(int id, String mensaje, String nombreAutor, String nombreDocumento, String rutaDocumento, int idUsuario, int idDocumento) {
        this.id = id;
        this.mensaje = mensaje;
        this.nombreAutor = nombreAutor;
        this.nombreDocumento = nombreDocumento;
        this.rutaDocumento = rutaDocumento;
        this.idUsuario = idUsuario;
        this.idDocumento = idDocumento;
    }

    public int getId() { return id; }
    public String getMensaje() { return mensaje; }
    public String getNombreAutor() { return nombreAutor; }
    public String getNombreDocumento() { return nombreDocumento; }
    public String getRutaDocumento() { return rutaDocumento; }
    public int getIdUsuario() { return idUsuario; }
    public int getIdDocumento() { return idDocumento; }
}