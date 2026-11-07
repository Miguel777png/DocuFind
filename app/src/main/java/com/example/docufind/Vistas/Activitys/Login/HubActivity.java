package com.example.docufind.Vistas.Activitys.Login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.docufind.R;
import com.example.docufind.Vistas.Activitys.Navegation.MainActivity;

/**
 * Actividad principal de bienvenida (Hub) que actúa como punto de entrada de la aplicación.
 * Comprueba si existe una sesión activa para redirigir al MainActivity o permite navegar a las
 * pantallas de inicio de sesión y registro.
 */
public class HubActivity extends AppCompatActivity {

    private Button btnLogin;
    private Button btnRegister;
    private Button btnSalir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        // Verificación de sesión activa en SharedPreferences
        SharedPreferences preferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean sesionActiva = preferences.getBoolean("isLoggedIn", false);

        if (sesionActiva) {
            Intent intent = new Intent(HubActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }


        // Configuración de vista inmersiva y layout
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_hub);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Inicialización y vinculación de botones UI
        btnLogin = findViewById(R.id.btnHubLogin);
        btnRegister = findViewById(R.id.btnHubRegister);
        btnSalir = findViewById(R.id.btnHubSalir);

        // Redirección a LoginActivity
        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                Intent intent = new Intent(HubActivity.this, LoginActivity.class);
                startActivity(intent);
            });
        }

        // Redirección a RegisterActivity
        if (btnRegister != null) {
            btnRegister.setOnClickListener(v -> {
                Intent intent = new Intent(HubActivity.this, RegisterActivity.class);
                startActivity(intent);
            });
        }

        // Cierre completo de la aplicación
        if (btnSalir != null) {
            btnSalir.setOnClickListener(v -> finishAffinity());
        }
    }
}