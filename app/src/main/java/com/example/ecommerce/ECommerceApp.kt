package com.example.ecommerce

import android.app.Application
import android.util.Log
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ECommerceApp : Application() {
    
    companion object {
        private const val TAG = "ECommerceApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)
            
            // Check Google Play Services
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
            if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
                Log.e(TAG, "Google Play Services is not available (status=$resultCode)")
            } else {
                Log.d(TAG, "Google Play Services is available")
            }
            
            // Log initialization success
            val auth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()
            val storage = FirebaseStorage.getInstance()
            
            Log.d(TAG, "Firebase initialized successfully")
            Log.d(TAG, "Auth instance: ${auth != null}")
            Log.d(TAG, "Firestore instance: ${firestore != null}")
            Log.d(TAG, "Storage instance: ${storage != null}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase: ${e.message}")
        }
    }
} 