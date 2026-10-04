package com.example.docufind.Vistas.Activitys.Navegation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.example.docufind.Fragments.ConfiguracionesFragment;
import com.example.docufind.Fragments.HomeFragment;
import com.example.docufind.Fragments.WebFragment;
import com.example.docufind.R;
import com.example.docufind.Vistas.Activitys.Login.HubActivity;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private NavigationView navigationView;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.nav_view);


        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.contenedor_fragmentos, new HomeFragment())
                    .commit();
            navigationView.setCheckedItem(R.id.snv_home);
        }


        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.snv_home) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.contenedor_fragmentos, new HomeFragment())
                        .commit();
            } else if (id == R.id.snv_config) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.contenedor_fragmentos, new ConfiguracionesFragment())
                        .commit();
            }else if (id == R.id.snv_web) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.contenedor_fragmentos, new WebFragment())
                        .commit();

            } else if (id == R.id.nav_logout) {

                cerrarSesion();
                return true;
            }


            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void cerrarSesion() {

        SharedPreferences preferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(MainActivity.this, HubActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }
}