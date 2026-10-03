package database;

import android.provider.BaseColumns;

public class DocuFindContract {

    /*
    La clase contrato en palabras mas simples , aca definimos ,
    cual sera el nombre y la variable en la que se llamaran las,
    columnas de las TABLAS

     */


    private DocuFindContract(){}


    public static class TablaUsuarios implements BaseColumns{

        //nombre de la tabla
        public static final String TABLE_NAME = "Users";

        //columnas xd
        public static final String COLUMN_ID = "Id";
        public static final String COLUMN_NAME = "Name";
        public static final String COLUMN_STATE = "State";
        public static final String COLUMN_MAIL = "Mail";
        public static final String COLUMN_PASSWORD = "Password";
        public static final String COLUMN_Role = "Role";
    }


    public static class TablaRol implements BaseColumns{

        public static final String TABLE_NAME = "Role";

        public static final String COLUMN_ID  ="Id";
        //columnas xd
        public static final String COLUMN_STATE ="State";
        public static final String COLUMN_NAME  ="Name";
    }


    public static class TablaDocumentos implements  BaseColumns{

        public static final String TABLE_NAME = "Documents";

        public static final String COLUMN_ID  ="id";


        //columnas xd
        public static final String COLUMN_STATE ="State";
        public static final String COLUMN_NAME ="Name";
        public static final String COLUMN_NAMEFILE ="NameFile";
        public static final String COLUMN_DOCUMENTRUTE = "RuteDocument";
        public static final String COLUMN_UPLOADDATE ="UploadDate";

        public static final String COLUMN_ID_USUARIO ="Id_Usuario";

    }

















}
