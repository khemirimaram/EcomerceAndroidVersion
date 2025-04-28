package com.example.ecommerce

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Rediriger vers HomeActivity
        startActivity(Intent(this, HomeActivity::class.java))
        finish() // Ferme MainActivity pour qu'on ne puisse pas y revenir avec le bouton retour
    }
}