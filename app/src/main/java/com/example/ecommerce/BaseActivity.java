package com.example.ecommerce;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BaseActivity extends AppCompatActivity {
    protected BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                startActivity(new Intent(this, HomeActivity.class));
                return true;
            } else if (itemId == R.id.navigation_favorites) {
                // TODO: Navigate to favorites
                return true;
            } else if (itemId == R.id.navigation_add) {
                // TODO: Navigate to add product
                return true;
            } else if (itemId == R.id.navigation_messages) {
                // TODO: Navigate to messages
                return true;
            } else if (itemId == R.id.navigation_profile) {
                // TODO: Navigate to profile
                return true;
            }
            return false;
        });
    }
}
