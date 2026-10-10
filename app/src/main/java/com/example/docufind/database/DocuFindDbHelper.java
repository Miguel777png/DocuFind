package com.example.docufind.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DocuFindDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "DocuFind.db";

    public static final int DATABASE_VERSION = 2;


    private static final String SQL_CREATE_TABLE_ROLE =
            "CREATE TABLE " + DocuFindContract.TablaRol.TABLE_NAME + " ("
                    + DocuFindContract.TablaRol.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + DocuFindContract.TablaRol.COLUMN_STATE + " INTEGER NOT NULL, "
                    + DocuFindContract.TablaRol.COLUMN_NAME + " TEXT NOT NULL"
                    + ");";

    private static final String SQL_CREATE_TABLE_USERS =
            "CREATE TABLE " + DocuFindContract.TablaUsuarios.TABLE_NAME + " ("
                    + DocuFindContract.TablaUsuarios.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + DocuFindContract.TablaUsuarios.COLUMN_STATE + " INTEGER NOT NULL, "
                    + DocuFindContract.TablaUsuarios.COLUMN_NAME + " TEXT NOT NULL, "
                    + DocuFindContract.TablaUsuarios.COLUMN_MAIL + " TEXT NOT NULL UNIQUE, "
                    + DocuFindContract.TablaUsuarios.COLUMN_PASSWORD + " TEXT NOT NULL, "
                    + DocuFindContract.TablaUsuarios.COLUMN_ROLE + " INTEGER NOT NULL, "
                    + "FOREIGN KEY (" + DocuFindContract.TablaUsuarios.COLUMN_ROLE + ") REFERENCES "
                    + DocuFindContract.TablaRol.TABLE_NAME + "(" + DocuFindContract.TablaRol.COLUMN_ID + ") ON DELETE CASCADE"
                    + ");";

    private static final String SQL_CREATE_TABLE_FOTOPERFIL =
            "CREATE TABLE " + DocuFindContract.TablaFotoPerfilUsuario.TABLE_NAME + " ("
                    + DocuFindContract.TablaFotoPerfilUsuario.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + DocuFindContract.TablaFotoPerfilUsuario.COLUMN_ID_USUARIO + " INTEGER NOT NULL, "
                    + DocuFindContract.TablaFotoPerfilUsuario.COLUMN_STATE + " INTEGER NOT NULL, "
                    + DocuFindContract.TablaFotoPerfilUsuario.COLUMN_RUTAFILE + " TEXT NOT NULL, "
                    + "FOREIGN KEY (" + DocuFindContract.TablaFotoPerfilUsuario.COLUMN_ID_USUARIO + ") REFERENCES "
                    + DocuFindContract.TablaUsuarios.TABLE_NAME + "(" + DocuFindContract.TablaUsuarios.COLUMN_ID + ") ON DELETE CASCADE"
                    + ");";

    private static final String SQL_CREATE_TABLE_CATEGORIA =
            "CREATE TABLE " + DocuFindContract.TablaCategoria.TABLE_NAME + " ("
                    + DocuFindContract.TablaCategoria.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + DocuFindContract.TablaCategoria.COLUMN_STATE + " INTEGER NOT NULL, "
                    + DocuFindContract.TablaCategoria.COLUMN_NAME + " TEXT NOT NULL"
                    + ");";

    private static final String SQL_CREATE_TABLE_DOCUMENTS =
            "CREATE TABLE " + DocuFindContract.TablaDocumentos.TABLE_NAME + " ("
                    + DocuFindContract.TablaDocumentos.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + DocuFindContract.TablaDocumentos.COLUMN_STATE + " INTEGER NOT NULL, "
                    + DocuFindContract.TablaDocumentos.COLUMN_NAME + " TEXT NOT NULL, "
                    + DocuFindContract.TablaDocumentos.COLUMN_NAMEFILE + " TEXT NOT NULL, "
                    + DocuFindContract.TablaDocumentos.COLUMN_RUTADOCUMENTO + " TEXT, "
                    + DocuFindContract.TablaDocumentos.COLUMN_FECHASUBIDA + " TEXT NOT NULL, "
                    + DocuFindContract.TablaDocumentos.COLUMN_ID_USUARIOS + " INTEGER NOT NULL, "
                    + DocuFindContract.TablaDocumentos.COLUMN_CATEGORIA + " INTEGER NOT NULL, "
                    + "FOREIGN KEY (" + DocuFindContract.TablaDocumentos.COLUMN_ID_USUARIOS + ") REFERENCES "
                    + DocuFindContract.TablaUsuarios.TABLE_NAME + "(" + DocuFindContract.TablaUsuarios.COLUMN_ID + ") ON DELETE CASCADE, "
                    + "FOREIGN KEY (" + DocuFindContract.TablaDocumentos.COLUMN_CATEGORIA + ") REFERENCES "
                    + DocuFindContract.TablaCategoria.TABLE_NAME + "(" + DocuFindContract.TablaCategoria.COLUMN_ID + ") ON DELETE CASCADE"
                    + ");";

    private static final String SQL_CREATE_TABLE_PUBLICACION =
            "CREATE TABLE " + DocuFindContract.TablaPublicacion.TABLE_NAME + " ("
                    + DocuFindContract.TablaPublicacion.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + DocuFindContract.TablaPublicacion.COLUMN_STATE + " INTEGER NOT NULL, "
                    + DocuFindContract.TablaPublicacion.COLUMN_MESSAGE + " TEXT NOT NULL, "
                    + DocuFindContract.TablaPublicacion.COLUMN_ID_USER + " INTEGER NOT NULL, "
                    + DocuFindContract.TablaPublicacion.COLUMN_ID_DOCUMENTO + " INTEGER NOT NULL, "
                    + DocuFindContract.TablaPublicacion.COLUMN_CATEGORIA + " INTEGER NOT NULL, "
                    + "FOREIGN KEY (" + DocuFindContract.TablaPublicacion.COLUMN_ID_USER + ") REFERENCES "
                    + DocuFindContract.TablaUsuarios.TABLE_NAME + "(" + DocuFindContract.TablaUsuarios.COLUMN_ID + ") ON DELETE CASCADE, "
                    + "FOREIGN KEY (" + DocuFindContract.TablaPublicacion.COLUMN_ID_DOCUMENTO + ") REFERENCES "
                    + DocuFindContract.TablaDocumentos.TABLE_NAME + "(" + DocuFindContract.TablaDocumentos.COLUMN_ID + ") ON DELETE CASCADE, "
                    + "FOREIGN KEY (" + DocuFindContract.TablaPublicacion.COLUMN_CATEGORIA + ") REFERENCES "
                    + DocuFindContract.TablaCategoria.TABLE_NAME + "(" + DocuFindContract.TablaCategoria.COLUMN_ID + ") ON DELETE CASCADE"
                    + ");";

    public DocuFindDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(SQL_CREATE_TABLE_ROLE);
        db.execSQL(SQL_CREATE_TABLE_USERS);
        db.execSQL(SQL_CREATE_TABLE_FOTOPERFIL);
        db.execSQL(SQL_CREATE_TABLE_CATEGORIA);
        db.execSQL(SQL_CREATE_TABLE_DOCUMENTS);
        db.execSQL(SQL_CREATE_TABLE_PUBLICACION);


        db.execSQL("INSERT INTO " + DocuFindContract.TablaRol.TABLE_NAME + " (" +
                DocuFindContract.TablaRol.COLUMN_STATE + ", " +
                DocuFindContract.TablaRol.COLUMN_NAME + ") VALUES (1, 'Administrador');");

        db.execSQL("INSERT INTO " + DocuFindContract.TablaRol.TABLE_NAME + " (" +
                DocuFindContract.TablaRol.COLUMN_STATE + ", " +
                DocuFindContract.TablaRol.COLUMN_NAME + ") VALUES (1, 'Usuario');");


        db.execSQL("INSERT INTO " + DocuFindContract.TablaCategoria.TABLE_NAME + " (" +
                DocuFindContract.TablaCategoria.COLUMN_STATE + ", " +
                DocuFindContract.TablaCategoria.COLUMN_NAME + ") VALUES (1, 'General');");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + DocuFindContract.TablaPublicacion.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DocuFindContract.TablaDocumentos.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DocuFindContract.TablaCategoria.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DocuFindContract.TablaFotoPerfilUsuario.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DocuFindContract.TablaUsuarios.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DocuFindContract.TablaRol.TABLE_NAME);


        onCreate(db);
    }
}