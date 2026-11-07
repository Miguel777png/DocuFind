package com.example.docufind.Vistas.Activitys.Login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.docufind.R;
import com.example.docufind.Vistas.Activitys.Navegation.MainActivity;
import com.example.docufind.database.DocuFindContract;
import com.example.docufind.database.DocuFindDbHelper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText editgetMail, editgetPassword;
    private AppCompatButton btnLogin, btnredirectReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Protección de pantalla con FLAG_SECURE
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editgetMail = findViewById(R.id.editgetMail);
        editgetPassword = findViewById(R.id.editgetPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnredirectReg = findViewById(R.id.btnredirectReg);

        btnLogin.setOnClickListener(v -> procesarLogin());

        btnredirectReg.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
    }

    /**
     * Valida credenciales de usuario consultando la base de datos en segundo plano mediante ExecutorService.
     */
    private void procesarLogin() {
        // 1. Validaciones en Hilo Principal
        String correo = editgetMail.getText().toString().trim();
        String password = editgetPassword.getText().toString().trim();

        if (correo.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "¡Error! Los campos no pueden estar vacíos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!correo.matches("^[a-zA-Z0-9._%+-]+@gmail\\.com$")) {
            Toast.makeText(LoginActivity.this, "Error: El correo no cumple con el formato requerido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hashing de la contraseña ingresada para comparar con SQLite
        String passwordHash = hashPasswordSHA256(password);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        // 2. Consulta a SQLite en Hilo Secundario
        executor.execute(() -> {
            DocuFindDbHelper dbHelper = new DocuFindDbHelper(LoginActivity.this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            String[] projection = { DocuFindContract.TablaUsuarios.COLUMN_ID };
            String selection = DocuFindContract.TablaUsuarios.COLUMN_MAIL + " = ? AND " +
                    DocuFindContract.TablaUsuarios.COLUMN_PASSWORD + " = ? AND " +
                    DocuFindContract.TablaUsuarios.COLUMN_STATE + " = 1";
            String[] selectionArgs = { correo, passwordHash };

            Cursor cursor = db.query(
                    DocuFindContract.TablaUsuarios.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null, null, null
            );

            int userId = -1;
            if (cursor != null && cursor.moveToFirst()) {
                userId = cursor.getInt(cursor.getColumnIndexOrThrow(DocuFindContract.TablaUsuarios.COLUMN_ID));
                cursor.close();
            }
            db.close();

            final int finalUserId = userId;

            // 3. Respuesta de UI en Hilo Principal
            handler.post(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (finalUserId != -1) {
                    SharedPreferences preferences = getSharedPreferences("UserSession", MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("isLoggedIn", true);
                    editor.putInt("loggedUserId", finalUserId);
                    editor.apply();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "¡Error! Credenciales inválidas", Toast.LENGTH_SHORT).show();
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