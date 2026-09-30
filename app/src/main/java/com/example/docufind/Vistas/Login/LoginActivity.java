package com.example.docufind.Vistas.Login;

import android.content.Intent;
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

public class LoginActivity extends AppCompatActivity {



    private EditText editgetUser,editgetPassword;

    private AppCompatButton btnLogin,btnredirectReg;
    private String SaveUser,SavePwd;


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



        editgetUser = findViewById(R.id.editgetUser);
        editgetPassword = findViewById(R.id.editgetPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnredirectReg = findViewById(R.id.btnredirectReg);

        /*
        SaveUser = editgetUser.getEditableText().toString();
        SavePwd = editgetPassword.getEditableText().toString();
        */


        btnLogin.setOnClickListener(v -> {

            if (!editgetUser.getEditableText().toString().equals("123456") && !editgetPassword.getEditableText().toString().equals("123")){

                Toast.makeText(LoginActivity.this, "¡Error! Credenciales inválidas", Toast.LENGTH_SHORT).show();



            }else if (editgetUser.getEditableText().toString().trim().isBlank() || editgetPassword.getEditableText().toString().trim().isBlank()){

                Toast.makeText(LoginActivity.this, "¡Error! Los campos no pueden estar vacios", Toast.LENGTH_SHORT).show();
            }else{

                Intent intent = new Intent(LoginActivity.this,MainActivity.class);
                intent.addFlags(intent.FLAG_ACTIVITY_SINGLE_TOP|intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);


            }


        });





        btnredirectReg.setOnClickListener(v -> {

            Intent intent = new Intent(LoginActivity.this,RegisterActivity.class);
            intent.addFlags(intent.FLAG_ACTIVITY_SINGLE_TOP|intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

        });










    }
}