package com.example.docufind.database;

import android.provider.BaseColumns;

public final class DocuFindContract {


    private DocuFindContract() {}

    public static class TablaRol implements BaseColumns {
        public static final String TABLE_NAME = "Rol";
        public static final String COLUMN_ID = "ID";
        public static final String COLUMN_STATE = "State";
        public static final String COLUMN_NAME = "Name";
    }

    public static class TablaUsuarios implements BaseColumns {
        public static final String TABLE_NAME = "Usuario";
        public static final String COLUMN_ID = "ID";
        public static final String COLUMN_STATE = "State";
        public static final String COLUMN_NAME = "Name";
        public static final String COLUMN_MAIL = "Mail";
        public static final String COLUMN_PASSWORD = "Password";
        public static final String COLUMN_ROLE = "Role";
    }

    public static class TablaFotoPerfilUsuario implements BaseColumns {
        public static final String TABLE_NAME = "FotoPerfilUsuario";
        public static final String COLUMN_ID = "ID";
        public static final String COLUMN_ID_USUARIO = "ID_Usuario";
        public static final String COLUMN_STATE = "State";
        public static final String COLUMN_RUTAFILE = "RutaFile";
    }

    public static class TablaCategoria implements BaseColumns {
        public static final String TABLE_NAME = "Categoria";
        public static final String COLUMN_ID = "ID";
        public static final String COLUMN_STATE = "State";
        public static final String COLUMN_NAME = "Name";
    }

    public static class TablaDocumentos implements BaseColumns {
        public static final String TABLE_NAME = "Documentos";
        public static final String COLUMN_ID = "ID";
        public static final String COLUMN_STATE = "State";
        public static final String COLUMN_NAME = "Name";
        public static final String COLUMN_NAMEFILE = "NameFile";
        public static final String COLUMN_RUTADOCUMENTO = "RutaDocumento";
        public static final String COLUMN_FECHASUBIDA = "FechaSubida";
        public static final String COLUMN_ID_USUARIOS = "ID_Usuarios";
        public static final String COLUMN_CATEGORIA = "Categoria";
    }

    public static class TablaPublicacion implements BaseColumns {
        public static final String TABLE_NAME = "Publicacion";
        public static final String COLUMN_ID = "ID";
        public static final String COLUMN_STATE = "State";
        public static final String COLUMN_MESSAGE = "Message";
        public static final String COLUMN_ID_USER = "ID_User";
        public static final String COLUMN_ID_DOCUMENTO = "ID_Documento";
        public static final String COLUMN_CATEGORIA = "Categoria";
    }
}