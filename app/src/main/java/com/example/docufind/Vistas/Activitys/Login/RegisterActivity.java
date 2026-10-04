package com.example.docufind.Vistas.Activitys.Login;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.docufind.R;
import com.example.docufind.database.DocuFindContract;
import com.example.docufind.database.DocuFindDbHelper;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {


    private TextInputEditText editNombreUsuario, editCorreo, editPassword;
    private AppCompatButton btnRegistrarse, btnIrLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        editNombreUsuario = findViewById(R.id.editTextText);
        editCorreo = findViewById(R.id.editTextText2);
        editPassword = findViewById(R.id.editTextText3);
        btnRegistrarse = findViewById(R.id.button2);
        btnIrLogin = findViewById(R.id.button);


        btnRegistrarse.setOnClickListener(v -> {
            registrarNuevoUsuario();
        });


        btnIrLogin.setOnClickListener(v -> {

            finish();
        });
    }


    private void registrarNuevoUsuario() {

        String nombre = editNombreUsuario.getText().toString().trim();
        String correo = editCorreo.getText().toString().trim();
        String password = editPassword.getText().toString().trim();


        if (nombre.isEmpty() || correo.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }


        DocuFindDbHelper dbHelper = new DocuFindDbHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();


        ContentValues values = new ContentValues();
        values.put(DocuFindContract.TablaUsuarios.COLUMN_NAME, nombre);
        values.put(DocuFindContract.TablaUsuarios.COLUMN_MAIL, correo);
        values.put(DocuFindContract.TablaUsuarios.COLUMN_PASSWORD, password);
        values.put(DocuFindContract.TablaUsuarios.COLUMN_STATE, 1);


        values.put(DocuFindContract.TablaUsuarios.COLUMN_Role, 2);


        long newRowId = db.insert(DocuFindContract.TablaUsuarios.TABLE_NAME, null, values);

        if (newRowId != -1) {
            Toast.makeText(this, "¡Cuenta creada con éxito!", Toast.LENGTH_SHORT).show();
            finish();
        } else {

            Toast.makeText(this, "Error al crear la cuenta", Toast.LENGTH_SHORT).show();
        }


        db.close();
    }
}