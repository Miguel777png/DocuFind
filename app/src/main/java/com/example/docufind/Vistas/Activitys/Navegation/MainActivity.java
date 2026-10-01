package com.example.docufind.Vistas.Activitys.Navegation;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.docufind.HomeFragment;
import com.example.docufind.R;
import com.example.docufind.Vistas.Activitys.ConfiguracionesFragment;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Esto es lo que ajusta los bordes con el id "@+id/main" que pusiste en el XML
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;





        });








        navigationView = findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id==R.id.snv_home){

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.contenedor_fragmentos, new HomeFragment())
                        .commit();
            } else if (id == R.id.snv_config) {

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.contenedor_fragmentos , new ConfiguracionesFragment())
                        .commit();
            }

            /*
            DrawerLayout drawerLayout = findViewById(R.id.main);
            drawerLayout.closeDrawers();


             */
            return true;


        });
















    }
}