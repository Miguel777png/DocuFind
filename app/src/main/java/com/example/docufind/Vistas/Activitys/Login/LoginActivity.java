package com.example.docufind.Vistas.Activitys.Login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
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

public class LoginActivity extends AppCompatActivity {

    private EditText editgetMail, editgetPassword;
    private AppCompatButton btnLogin, btnredirectReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        btnLogin.setOnClickListener(v -> {


            String correo = editgetMail.getText().toString().trim();
            String password = editgetPassword.getText().toString().trim();


            if (correo.isEmpty() || password.isEmpty()){
                Toast.makeText(LoginActivity.this, "¡Error! Los campos no pueden estar vacíos", Toast.LENGTH_SHORT).show();
                return;
            }


            DocuFindDbHelper dbHelper = new DocuFindDbHelper(LoginActivity.this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();


            String[] projection = { DocuFindContract.TablaUsuarios.COLUMN_ID };


            String selection = DocuFindContract.TablaUsuarios.COLUMN_MAIL + " = ? AND " +
                    DocuFindContract.TablaUsuarios.COLUMN_PASSWORD + " = ?";
            String[] selectionArgs = { correo, password };


            Cursor cursor = db.query(
                    DocuFindContract.TablaUsuarios.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null, null, null
            );


            if (cursor != null && cursor.moveToFirst()) {

                int userId = cursor.getInt(cursor.getColumnIndexOrThrow(DocuFindContract.TablaUsuarios.COLUMN_ID));

                SharedPreferences preferences = getSharedPreferences("UserSession", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("isLoggedIn", true);
                editor.putInt("loggedUserId", userId);
                editor.apply();


                cursor.close();
                db.close();


                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();

            } else {

                Toast.makeText(LoginActivity.this, "¡Error! Credenciales inválidas", Toast.LENGTH_SHORT).show();
                if (cursor != null) cursor.close();
                db.close();
            }
        });

        btnredirectReg.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
    }
}