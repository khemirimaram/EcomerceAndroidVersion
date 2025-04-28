package com.example.ecommerce

import android.app.Activity
import android.content.Intent
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ecommerce.databinding.ActivityEditProfileBinding
import com.example.ecommerce.models.User
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    private var currentUser: User? = null
    private val TAG = "EditProfileActivity"

    // Variables pour la sélection d'avatar
    private var currentAvatarId: Int = 0
    private val availableAvatars = listOf(
        R.drawable.ic_person,
        R.drawable.avatar_default_1,
        R.drawable.avatar_default_2,
        R.drawable.avatar_default_3,
        R.drawable.avatar_default_4,
        R.drawable.avatar_default_5
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Vérifier si Firebase est correctement initialisé
        try {
            val firebaseApp = FirebaseApp.getInstance()
            Log.d(TAG, "Firebase app name: ${firebaseApp.name}")
            
            // Vérifier les instances Firebase
            Log.d(TAG, "Auth instance: ${auth.app.name}")
            Log.d(TAG, "Firestore instance: ${firestore.app.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur Firebase: ${e.message}")
        }
        
        setupToolbar()
        loadUserData()
        setupListeners()
    }
    
    private fun setupToolbar() {
        binding.ivBack.setOnClickListener {
            finish()
        }
    }
    
    private fun loadUserData() {
        val userId = auth.currentUser?.uid
        
        if (userId == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    currentUser = document.toObject(User::class.java)
                    updateUI(currentUser)
                } else {
                    Toast.makeText(this, "Données utilisateur non trouvées", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading user data: ${e.message}")
                Toast.makeText(this, "Erreur de chargement des données", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
    
    private fun updateUI(user: User?) {
        if (user == null) return
        
        // Remplir les champs avec les données de l'utilisateur
        binding.etFirstName.setText(user.firstName)
        binding.etLastName.setText(user.lastName)
        binding.etEmail.setText(user.email)
        binding.etPhone.setText(user.phoneNumber)
        binding.etLocation.setText(user.location)
        binding.etBio.setText(user.bio)
        
        // Charger l'avatar actuel
        val avatarResourceId = when(user.avatarId) {
            1 -> R.drawable.avatar_default_1
            2 -> R.drawable.avatar_default_2
            3 -> R.drawable.avatar_default_3
            4 -> R.drawable.avatar_default_4
            5 -> R.drawable.avatar_default_5
            else -> R.drawable.ic_person // Avatar par défaut (0)
        }
        binding.profileImage.setImageResource(avatarResourceId)
        currentAvatarId = user.avatarId
    }
    
    private fun setupListeners() {
        // Changer le texte pour refléter le changement d'avatar
        binding.tvChangePhoto.text = "Changer d'avatar"
        
        // Pour changer l'avatar
        binding.tvChangePhoto.setOnClickListener {
            showAvatarSelectionDialog()
        }
        
        // Pour enregistrer les modifications
        binding.btnSave.setOnClickListener {
            saveChanges()
        }
    }
    
    private fun showAvatarSelectionDialog() {
        val avatarNames = arrayOf(
            "Avatar gris", "Avatar vert", "Avatar bleu", 
            "Avatar orange", "Avatar rose", "Avatar violet"
        )
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Choisir un avatar")
        
        builder.setSingleChoiceItems(avatarNames, currentAvatarId) { dialog, which ->
            // Mettre à jour l'avatar sélectionné
            currentAvatarId = which
            binding.profileImage.setImageResource(availableAvatars[which])
            dialog.dismiss()
            
            // Confirmer la sélection
            Toast.makeText(this, "Avatar sélectionné: ${avatarNames[which]}", Toast.LENGTH_SHORT).show()
        }
        
        builder.setNegativeButton("Annuler") { dialog, _ ->
            dialog.dismiss()
        }
        
        val dialog = builder.create()
        dialog.show()
    }
    
    private fun saveChanges() {
        val userId = auth.currentUser?.uid ?: return
        
        // Récupérer les valeurs des champs
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val phoneNumber = binding.etPhone.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val bio = binding.etBio.text.toString().trim()
        
        // Valider les champs obligatoires
        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir les champs obligatoires", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Créer un hashmap avec les données à mettre à jour
        val userUpdates = hashMapOf<String, Any>(
            "firstName" to firstName,
            "lastName" to lastName,
            "username" to "$firstName $lastName",
            "phoneNumber" to phoneNumber,
            "location" to location,
            "bio" to bio,
            "avatarId" to currentAvatarId,
            "updatedAt" to System.currentTimeMillis()
        )
        
        // Commencer la mise à jour
        binding.btnSave.isEnabled = false
        Snackbar.make(binding.root, "Mise à jour en cours...", Snackbar.LENGTH_SHORT).show()
        
        // Mettre à jour directement les données dans Firestore
        updateUserData(userId, userUpdates)
    }
    
    // Vérifier si l'appareil est connecté à Internet
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
        }
    }
    
    private fun updateUserData(userId: String, updates: Map<String, Any>) {
        try {
            firestore.collection("users")
                .document(userId)
                .update(updates)
                .addOnSuccessListener {
                    Log.d(TAG, "Profil mis à jour avec succès")
                    Snackbar.make(binding.root, "Profil mis à jour avec succès", Snackbar.LENGTH_SHORT).show()
                    binding.btnSave.isEnabled = true
                    
                    // Attendre un peu avant de terminer l'activité
                    binding.root.postDelayed({
                        setResult(Activity.RESULT_OK)
                        finish()
                    }, 1500)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update user data: ${e.message}")
                    Toast.makeText(this, "Échec de la mise à jour : ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnSave.isEnabled = true
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de la mise à jour des données: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Erreur inattendue: ${e.message}", Toast.LENGTH_SHORT).show()
            binding.btnSave.isEnabled = true
        }
    }
} 