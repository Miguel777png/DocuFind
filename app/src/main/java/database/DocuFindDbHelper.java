package database;

public class DocuFindDbHelper
{

    public static final String DATABASE_NAME ="DocuFind.db";

    public static final int DATABASE_VERSION =1;

    private  static  final  String SQL_CREATE_TABLE_ROLE =
            "CREATE TABLE" + DocuFindContract.TablaRol.TABLE_NAME +
            "("
                    + DocuFindContract.TablaRol.COLUMN_ID + " INTEGER PRIMARY KEY AUTO_INCREMENT,"
                    + DocuFindContract.TablaRol.COLUMN_STATE + "BOOL NOT NULL,"
                    + DocuFindContract.TablaRol.COLUMN_NAME + "VARCHAR(55) NOT NULL );";




    private static final String SQL_CREATE_TABLE_USERS =
            "CREATE TABLE" + DocuFindContract.TablaUsuarios.TABLE_NAME +
                    "("
                        + DocuFindContract.TablaUsuarios.COLUMN_ID + "INTEGER PRIMARY KEY AUTO_INCREMENT,"
                        + DocuFindContract.TablaUsuarios.COLUMN_STATE + "BOOL NOT NULL,"
                        + DocuFindContract.TablaUsuarios.COLUMN_NAME + "VARCHAR(55) NOT NULL,"
                        + DocuFindContract.TablaUsuarios.COLUMN_MAIL + "VARCHAR(55) NOT NULL,"
                        + DocuFindContract.TablaUsuarios.COLUMN_PASSWORD + "VARCHAR(55) NOT NULL,"
                        + DocuFindContract.TablaUsuarios.COLUMN_Role +"INTEGER NOT NULL,"
                        + "FOREIGN KEY (" + DocuFindContract.TablaUsuarios.COLUMN_Role + ") REFERENCES " + DocuFindContract.TablaRol.TABLE_NAME
                        + "(" + DocuFindContract.TablaRol.COLUMN_ID + "));";



    public static final String SQL_CREATE_TABLE_DOCUMENTS =
            "CREATE TABLE" + DocuFindContract.TablaDocumentos.TABLE_NAME +
                    "("
                        + DocuFindContract.TablaDocumentos.COLUMN_ID + "INTEGER PRIMARY KEY AUTO_INCREMENT,"
                        + DocuFindContract.TablaDocumentos.COLUMN_STATE + "BOOL NOT NULL,"
                        + DocuFindContract.TablaDocumentos.COLUMN_NAME + "VARCHAR(55) NOT NULL,"
                        + DocuFindContract.TablaDocumentos.COLUMN_NAMEFILE + "VARCHAR(55) NOT NULL,"
                    + DocuFindContract.TablaDocumentos.COLUMN_DOCUMENTRUTE + "VARCHAR(255) NOT NULL,"
                    + DocuFindContract.TablaDocumentos.COLUMN_UPLOADDATE + "TIMESTAMP NOT NULL."
                    + DocuFindContract.TablaDocumentos.COLUMN_ID_USUARIO + "INTEGER NOT NULL," +
                    "FOREIGN KEY (" + DocuFindContract.TablaDocumentos.COLUMN_ID_USUARIO + ")REFERENCES " + DocuFindContract.TablaUsuarios.TABLE_NAME + "(" + DocuFindContract.TablaUsuarios.COLUMN_ID + "));";






}
