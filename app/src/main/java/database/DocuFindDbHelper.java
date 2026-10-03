package  database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DocuFindDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "DocuFind.db";
    public static final int DATABASE_VERSION = 1;

//SQLLITE no lee BOOL o VARCHAR , los convierte lee INTEGER O TEXT , true = 1 , false = 0
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
                    + DocuFindContract.TablaUsuarios.COLUMN_MAIL + " TEXT NOT NULL, "
                    + DocuFindContract.TablaUsuarios.COLUMN_PASSWORD + " TEXT NOT NULL, "
                    + DocuFindContract.TablaUsuarios.COLUMN_Role + " INTEGER NOT NULL, "
                    + "FOREIGN KEY (" + DocuFindContract.TablaUsuarios.COLUMN_Role + ") REFERENCES "
                    + DocuFindContract.TablaRol.TABLE_NAME + "(" + DocuFindContract.TablaRol.COLUMN_ID + ")"
                    + ");";


    private static final String SQL_CREATE_TABLE_DOCUMENTS =
            "CREATE TABLE " + DocuFindContract.TablaDocumentos.TABLE_NAME + " ("
                    + DocuFindContract.TablaDocumentos.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + DocuFindContract.TablaDocumentos.COLUMN_STATE + " INTEGER NOT NULL, "
                    + DocuFindContract.TablaDocumentos.COLUMN_NAME + " TEXT NOT NULL, "
                    + DocuFindContract.TablaDocumentos.COLUMN_NAMEFILE + " TEXT NOT NULL, "
                    + DocuFindContract.TablaDocumentos.COLUMN_DOCUMENTRUTE + " TEXT NOT NULL, "
                    + DocuFindContract.TablaDocumentos.COLUMN_UPLOADDATE + " TEXT NOT NULL, "
                    + DocuFindContract.TablaDocumentos.COLUMN_ID_USUARIO + " INTEGER NOT NULL, "
                    + "FOREIGN KEY (" + DocuFindContract.TablaDocumentos.COLUMN_ID_USUARIO + ") REFERENCES "
                    + DocuFindContract.TablaUsuarios.TABLE_NAME + "(" + DocuFindContract.TablaUsuarios.COLUMN_ID + ")"
                    + ");";

    public DocuFindDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }


    //metodo para crearla por primera vez
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_ROLE);
        db.execSQL(SQL_CREATE_TABLE_USERS);
        db.execSQL(SQL_CREATE_TABLE_DOCUMENTS);
    }


    //metodo para cuando vaya actualizar la base de datos
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {


        db.execSQL("DROP TABLE IF EXISTS " + DocuFindContract.TablaDocumentos.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DocuFindContract.TablaUsuarios.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DocuFindContract.TablaRol.TABLE_NAME);
        onCreate(db);

    }
}