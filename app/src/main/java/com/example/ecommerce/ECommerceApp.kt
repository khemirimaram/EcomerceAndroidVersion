package com.example.ecommerce

import android.app.Application
import android.util.Log
import com.cloudinary.android.MediaManager
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import com.google.android.gms.common.ConnectionResult

class ECommerceApp : Application() {
    
    companion object {
        private const val TAG = "ECommerceApp"
        const val CLOUDINARY_CLOUD_NAME = "dhfckhxho"
        const val CLOUDINARY_API_KEY = "195454145722799"
        const val CLOUDINARY_API_SECRET = "eIt_X-J67PbzXTeA3ICA2KGLSGA"
        
        fun showGooglePlayServicesDialog(activity: Activity) {
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(activity)
            
            val errorMessage = when (resultCode) {
                ConnectionResult.SERVICE_MISSING -> "Google Play Services is missing"
                ConnectionResult.SERVICE_UPDATING -> "Google Play Services is updating"
                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> "Google Play Services update required"
                ConnectionResult.SERVICE_DISABLED -> "Google Play Services is disabled"
                ConnectionResult.SERVICE_INVALID -> "Google Play Services is invalid"
                else -> "Google Play Services error: $resultCode"
            }
            
            Log.e(TAG, errorMessage)
            
            if (resultCode != ConnectionResult.SUCCESS) {
                if (googleApiAvailability.isUserResolvableError(resultCode)) {
                    googleApiAvailability.getErrorDialog(activity, resultCode, 9000)?.show()
                } else {
                    AlertDialog.Builder(activity)
                        .setTitle("Google Play Services requis")
                        .setMessage("Cette application nécessite Google Play Services, qui n'est pas disponible sur votre appareil.\n\nErreur: $errorMessage")
                        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Vérifier Google Play Services avant tout
            val availability = GoogleApiAvailability.getInstance()
            val resultCode = availability.isGooglePlayServicesAvailable(this)
            
            if (resultCode == ConnectionResult.SUCCESS) {
                Log.d(TAG, "Google Play Services est disponible")
                initializeFirebase()
                initializeCloudinary()
            } else {
                Log.e(TAG, "Google Play Services n'est pas disponible (code: $resultCode)")
                // Ne pas initialiser Firebase si Google Play Services n'est pas disponible
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'initialisation de l'application", e)
        }
    }

    private fun initializeFirebase() {
        try {
            // Initialiser Firebase
            FirebaseApp.initializeApp(this)
            
            // Vérifier que l'initialisation a réussi
            if (FirebaseApp.getInstance() != null) {
                Log.d(TAG, "Firebase initialisé avec succès")
                
                // Initialiser les autres services Firebase
                FirebaseAuth.getInstance()
                FirebaseFirestore.getInstance()
                FirebaseStorage.getInstance()
            } else {
                Log.e(TAG, "Échec de l'initialisation de Firebase")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'initialisation de Firebase", e)
        }
    }

    private fun initializeCloudinary() {
        try {
            val config = HashMap<String, String>()
            config["cloud_name"] = "dhfckhxho"
            config["api_key"] = "195454145722799"
            config["api_secret"] = "eIt_X-J67PbzXTeA3ICA2KGLSGA"
            
            MediaManager.init(this, config)
            Log.d(TAG, "Cloudinary initialisé avec succès")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'initialisation de Cloudinary", e)
        }
    }

    fun checkGooglePlayServices(activity: Activity): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(activity)
        
        if (resultCode != ConnectionResult.SUCCESS) {
            if (availability.isUserResolvableError(resultCode)) {
                // Montrer une boîte de dialogue pour résoudre le problème
                AlertDialog.Builder(activity)
                    .setTitle("Google Play Services requis")
                    .setMessage("Cette application nécessite Google Play Services. Veuillez l'installer pour continuer.")
                    .setPositiveButton("Installer") { _, _ ->
                        availability.getErrorDialog(activity, resultCode, 9000)?.show()
                    }
                    .setNegativeButton("Annuler") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
            return false
        }
        return true
    }
} 