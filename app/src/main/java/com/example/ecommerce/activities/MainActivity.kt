package com.example.ecommerce.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.ecommerce.R
import com.example.ecommerce.databinding.ActivityMainBinding
import com.example.ecommerce.fragments.HomeFragment
import com.example.ecommerce.fragments.CategoriesFragment
import com.example.ecommerce.fragments.CartFragment
import com.example.ecommerce.fragments.ProfileFragment
import com.example.ecommerce.firebase.FirebaseManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val firebaseManager = FirebaseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Configurer la bottom navigation
            setupBottomNavigation()
            
            // Charger le fragment Home par défaut
            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, HomeFragment())
                    .commit()
            }
            
        } catch (e: Exception) {
            // Gérer les erreurs lors de l'initialisation
            Log.e("MainActivity", "Erreur lors de l'initialisation: ${e.message}")
            Toast.makeText(this, "Erreur lors du démarrage de l'application", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupBottomNavigation() {
        try {
            binding.bottomNavigation.setOnItemSelectedListener { item ->
                var fragment: Fragment? = null
                
                when (item.itemId) {
                    R.id.nav_home -> fragment = HomeFragment()
                    R.id.nav_categories -> fragment = CategoriesFragment()
                    R.id.nav_cart -> fragment = CartFragment()
                    R.id.nav_profile -> fragment = ProfileFragment()
                }
                
                fragment?.let {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, it)
                        .commit()
                }
                
                true
            }
        } catch (e: Exception) {
            // Gérer les erreurs de navigation
            Log.e("MainActivity", "Erreur lors de la configuration de la navigation: ${e.message}")
        }
    }
    
    // Méthode pour gérer les erreurs de l'application
    fun handleAppError(errorMessage: String, fatal: Boolean = false) {
        try {
            Log.e("MainActivity", "Erreur de l'application: $errorMessage")
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            
            if (fatal) {
                AlertDialog.Builder(this)
                    .setTitle("Erreur critique")
                    .setMessage("Une erreur critique s'est produite: $errorMessage\n\nL'application va redémarrer.")
                    .setPositiveButton("OK") { _, _ ->
                        // Redémarrer l'activité
                        val intent = intent
                        finish()
                        startActivity(intent)
                    }
                    .setCancelable(false)
                    .show()
            }
        } catch (e: Exception) {
            // Fallback pour les erreurs critiques
            Log.e("MainActivity", "Erreur fatale: ${e.message}")
            finish()
        }
    }
} 