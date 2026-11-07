package com.example.docufind.Vistas.Activitys.Login;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText editNombreUsuario, editCorreo, editPassword;
    private AppCompatButton btnRegistrarse, btnIrLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        // Protección de pantalla con FLAG_SECURE
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );

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

        btnRegistrarse.setOnClickListener(v -> registrarNuevoUsuario());

        btnIrLogin.setOnClickListener(v -> finish());
    }

    /**
     * Valida el formulario de registro y realiza la inserción en SQLite en segundo plano encriptando la contraseña.
     */
    private void registrarNuevoUsuario() {
        // 1. Validaciones en Hilo Principal
        String nombre = editNombreUsuario.getText().toString().trim();
        String correo = editCorreo.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (nombre.isEmpty() || correo.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!correo.matches("^[a-zA-Z0-9._%+-]+@gmail\\.com$")) {
            Toast.makeText(RegisterActivity.this, "Error: El correo no cumple con el formato", Toast.LENGTH_SHORT).show();
            return;
        }

        // Aplicación del algoritmo SHA-256 a la contraseña
        String passwordHash = hashPasswordSHA256(password);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        // 2. Inserción a SQLite en Hilo Secundario
        executor.execute(() -> {
            DocuFindDbHelper dbHelper = new DocuFindDbHelper(RegisterActivity.this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(DocuFindContract.TablaUsuarios.COLUMN_NAME, nombre);
            values.put(DocuFindContract.TablaUsuarios.COLUMN_MAIL, correo);
            values.put(DocuFindContract.TablaUsuarios.COLUMN_PASSWORD, passwordHash);
            values.put(DocuFindContract.TablaUsuarios.COLUMN_STATE, 1);
            values.put(DocuFindContract.TablaUsuarios.COLUMN_ROLE, 2);

            long newRowId = db.insert(DocuFindContract.TablaUsuarios.TABLE_NAME, null, values);
            db.close();

            // 3. Respuesta de UI en Hilo Principal
            handler.post(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (newRowId != -1) {
                    Toast.makeText(RegisterActivity.this, "¡Cuenta creada con éxito!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, "Error al crear la cuenta", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * Genera un hash SHA-256 a partir del texto plano de la contraseña.
     */
    private String hashPasswordSHA256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return data;
        }
    }
}