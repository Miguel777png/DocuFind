package com.example.docufind.Vistas.Activitys.Login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.docufind.R;
import com.example.docufind.Vistas.Activitys.Navegation.MainActivity;

public class HubActivity extends AppCompatActivity {


    //como es java hay es igual que java primero agarra y declara las variables
    private Button btnLogin;
    private Button btnInfo;
    private Button btnExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //LOGICA para el guardado de credenciales TEST
        //como es la primera pagina que esta en la pila de mi app esta debe tener esto para
        //el redireccionamiento

        android.content.SharedPreferences preferences = getSharedPreferences("UserSession",MODE_PRIVATE);
        boolean sesionActiva = preferences.getBoolean("isLoggedIn",false);

        if(sesionActiva){

            Intent intent = new Intent(HubActivity.this, MainActivity.class);
            startActivity(intent);
            finish();

        }


        //si no se inicia sesion carga el hub normal para logearse




        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_hub);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {

            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });



        //esto es parecido a lo que vimos en javascript hay que buscar el id , directamente desde el xml
        // para eso sirve finbyid y basicamente dice que esta va buscar un objeto con un id de este tipo |Button|
        //Set on click listener necesita hacer referencia a la vista por eso usa V y el metodo lambda en conjunto ,
        //CUANDO SE DE EL CLICK TIENES QUE HACER ESTO
        //TOast es una libreria o funcion que se importa.

        btnInfo = findViewById(R.id.Registrarse);
        btnInfo.setOnClickListener(v ->{
            //mensajes de info esto
            Toast.makeText(HubActivity.this,"Esto es un Mensaje de informacion con la libreria Toast",Toast.LENGTH_SHORT).show();
        });

        btnExit = findViewById(R.id.btnExit);
        btnExit.setOnClickListener(v -> {
            //para salir de la app
            finish();
        });


        btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(v ->{

            Intent intent = new Intent(HubActivity.this, LoginActivity.class);

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            startActivity(intent);



        });

    }


















}