package com.example.ecommerce

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ecommerce.databinding.ActivityProfileBinding
import com.example.ecommerce.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "ProfileActivity"
    
    private var userId: String = ""
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Log.d(TAG, "1. Début de onCreate")
            
            // Initialiser le binding
            binding = ActivityProfileBinding.inflate(layoutInflater)
            Log.d(TAG, "2. Binding initialisé")
            
            setContentView(binding.root)
            Log.d(TAG, "3. setContentView appelé")
            
            // Masquer immédiatement la section produits
            hideProductSection()
            
            // Vérifier l'état de l'authentification
            val firebaseUser = auth.currentUser
            Log.d(TAG, "Auth currentUser: ${firebaseUser?.uid}, isConnected: ${firebaseUser != null}")
            
            // Récupérer l'ID utilisateur
            userId = intent.getStringExtra("USER_ID") ?: firebaseUser?.uid ?: ""
            Log.d(TAG, "4. ID utilisateur récupéré: $userId")
            
            // Bouton retour
            try {
                binding.ivBack.setOnClickListener {
                finish()
                }
                Log.d(TAG, "5. Bouton retour configuré")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur sur le bouton retour: ${e.message}")
            }
            
            // Si pas d'utilisateur connecté, essayer de récupérer l'ID de Firebase
            if (userId.isEmpty() && firebaseUser != null) {
                userId = firebaseUser.uid
                Log.d(TAG, "Utilisateur trouvé dans Firebase: $userId")
            }
            
            // Si toujours pas d'ID, afficher profil par défaut
            if (userId.isEmpty()) {
                Log.e(TAG, "Utilisateur non connecté - affichage profil par défaut")
                setupDefaultProfile()
                return
            }

            Log.d(TAG, "Utilisateur connecté avec ID: $userId - chargement du profil")
            
            // Charger le profil utilisateur
            loadUserProfile()
            
            // Configurer les boutons
            setupButtons()
            
            Log.d(TAG, "6. ProfileActivity onCreate terminé")

        } catch (e: Exception) {
            Log.e(TAG, "Erreur globale dans ProfileActivity: ${e.message}")
            e.printStackTrace()
            
            // Afficher un message d'erreur à l'utilisateur
            Toast.makeText(this, "Erreur lors du chargement du profil: ${e.message}", Toast.LENGTH_LONG).show()
            finish() // Terminer l'activité en cas d'erreur
        }
    }
    
    private fun setupDefaultProfile() {
        try {
            Log.d(TAG, "Configuration du profil par défaut")
            
            // Informations de base
            binding.tvUsername.text = "Utilisateur non connecté"
            binding.tvRating.text = "N/A"
            binding.tvReviewCount.text = "0 avis"
            binding.tvProductCount.text = "0 annonce"
            binding.profileImage.setImageResource(R.drawable.ic_person)
            
            // Masquer les boutons d'interaction
            binding.btnEditProfile.visibility = View.GONE
            binding.tvLeaveReview.visibility = View.GONE
            binding.btnSendMessage.visibility = View.GONE
            
            // Masquer les sections produits
            hideProductSection()
            
            // Vérifier à nouveau si l'utilisateur est connecté pour l'inviter à se connecter
            if (auth.currentUser != null) {
                Toast.makeText(this, "Erreur lors du chargement de votre profil", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Connectez-vous pour voir votre profil", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur dans setupDefaultProfile: ${e.message}")
        }
    }

    private fun loadUserProfile() {
        try {
            Log.d(TAG, "Chargement du profil utilisateur: $userId")
            
            firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        try {
                            Log.d(TAG, "Document utilisateur trouvé: ${document.data}")
                            currentUser = document.toObject(User::class.java)
                            
                            if (currentUser != null) {
                                Log.d(TAG, "Utilisateur convertit avec succès: ${currentUser?.username}")
                                updateUI(currentUser)
                            } else {
                                Log.e(TAG, "Erreur: conversion en User a renvoyé null")
                                createUserProfileIfNeeded()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Erreur lors de la conversion des données: ${e.message}")
                            createUserProfileIfNeeded()
                        }
                    } else {
                        Log.d(TAG, "Document utilisateur non trouvé - création d'un nouveau profil")
                        createUserProfileIfNeeded()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Erreur lors du chargement: ${e.message}")
                    createUserProfileIfNeeded()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur dans loadUserProfile: ${e.message}")
            setupDefaultProfile()
        }
    }
    
    private fun createUserProfileIfNeeded() {
        // Si l'utilisateur est connecté mais n'a pas de profil, créer un profil de base
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            try {
                Log.d(TAG, "Création d'un nouveau profil utilisateur")
                
                // Assigner un avatar aléatoire (0-5)
                val randomAvatarId = (0..5).random()
                Log.d(TAG, "Avatar aléatoire assigné: $randomAvatarId")
                
                val newUser = User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email,
                    username = firebaseUser.displayName ?: "Utilisateur",
                    firstName = firebaseUser.displayName?.split(" ")?.getOrNull(0) ?: "",
                    lastName = firebaseUser.displayName?.split(" ")?.getOrNull(1) ?: "",
                    profileImageUrl = null, // Pas besoin d'URL d'image car nous utilisons des avatars
                    avatarId = randomAvatarId, // Assigner l'avatar aléatoire
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                
                // Sauvegarder le nouveau profil
                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(newUser)
                    .addOnSuccessListener {
                        Log.d(TAG, "Nouveau profil créé avec succès")
                        currentUser = newUser
                        updateUI(newUser)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Erreur lors de la création du profil: ${e.message}")
                        setupDefaultProfile()
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la création du profil: ${e.message}")
                setupDefaultProfile()
            }
        } else {
            Log.d(TAG, "Aucun utilisateur connecté, impossible de créer un profil")
            setupDefaultProfile()
        }
    }
    
    private fun updateUI(user: User?) {
        try {
            if (user == null) {
                Log.d(TAG, "Utilisateur null dans updateUI")
                setupDefaultProfile()
                return
            }

            Log.d(TAG, "Mise à jour de l'UI avec les données utilisateur: ${user.username}")
            
            // Nom d'utilisateur
            binding.tvUsername.text = user.username ?: user.firstName ?: "Utilisateur"
            
            // Photo de profil - Utilisation du système d'avatars
            try {
                // Charger l'avatar en fonction de l'ID
                val avatarResourceId = when(user.avatarId) {
                    1 -> R.drawable.avatar_default_1
                    2 -> R.drawable.avatar_default_2
                    3 -> R.drawable.avatar_default_3
                    4 -> R.drawable.avatar_default_4
                    5 -> R.drawable.avatar_default_5
                    else -> R.drawable.ic_person // Avatar par défaut (0)
                }
                
                // Appliquer l'avatar
                binding.profileImage.setImageResource(avatarResourceId)
                
                Log.d(TAG, "Avatar chargé avec succès: ID=${user.avatarId}")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du chargement de l'avatar: ${e.message}")
                binding.profileImage.setImageResource(R.drawable.ic_person)
            }
            
            // Pour le moment, afficher des valeurs par défaut pour les autres informations
            binding.tvRating.text = "N/A"
            binding.tvReviewCount.text = "0 avis"
            binding.tvProductCount.text = "0 annonce"
            
            // Masquer les sections produits
            hideProductSection()
            
            // Rendre les boutons d'interaction visibles
            binding.btnEditProfile.visibility = View.VISIBLE
            binding.tvLeaveReview.visibility = View.VISIBLE
            binding.btnSendMessage.visibility = View.VISIBLE
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur dans updateUI: ${e.message}")
            binding.profileImage.setImageResource(R.drawable.ic_person)
        }
    }
    
    private fun setupButtons() {
        try {
            // Modifier profil - uniquement pour le propriétaire du profil
            binding.btnEditProfile.setOnClickListener {
                try {
                    if (userId == auth.currentUser?.uid) {
                        val intent = Intent(this, EditProfileActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Vous ne pouvez pas modifier ce profil", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors du clic sur modifier profil: ${e.message}")
                    Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            
            // Laisser un avis - uniquement si ce n'est pas son propre profil
            binding.tvLeaveReview.setOnClickListener {
                try {
                    if (userId != auth.currentUser?.uid) {
                        // Implémentation temporaire - juste un message
                        Toast.makeText(this, "Fonctionnalité 'Laisser un avis' à venir", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Vous ne pouvez pas laisser un avis sur votre propre profil", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors du clic sur laisser un avis: ${e.message}")
                }
            }
            
            // Envoyer un message
            binding.btnSendMessage.setOnClickListener {
                try {
                    // Implémentation temporaire - juste un message
                    Toast.makeText(this, "Fonctionnalité 'Envoyer un message' à venir", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors du clic sur envoyer un message: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur dans setupButtons: ${e.message}")
        }
    }
    
    // Méthode pour gérer le retour du EditProfileActivity
    override fun onResume() {
        super.onResume()
        
        Log.d(TAG, "onResume appelé")
        
        // Recharger le profil utilisateur à chaque retour à cette activité
        if (userId.isNotEmpty()) {
            Log.d(TAG, "Rechargement du profil utilisateur (ID: $userId)")
            loadUserProfile()
        } else if (auth.currentUser != null) {
            // Si userId est vide mais que l'utilisateur est connecté, récupérer l'ID depuis Firebase
            userId = auth.currentUser!!.uid
            Log.d(TAG, "ID utilisateur récupéré de Firebase Auth: $userId")
            loadUserProfile()
        } else {
            Log.d(TAG, "Aucun utilisateur connecté dans onResume")
        }
    }
    
    // Recevoir le résultat de l'activité de modification de profil
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "Retour de l'activité de modification de profil avec succès")
            // Recharger immédiatement le profil
            loadUserProfile()
        }
    }

    // Nouvelle méthode pour masquer tous les éléments liés aux produits
    private fun hideProductSection() {
        try {
            // Masquer les cards de produits
            binding.cardProduct1?.visibility = View.GONE
            binding.cardProduct2?.visibility = View.GONE
            binding.cardProduct3?.visibility = View.GONE
            binding.cardProduct4?.visibility = View.GONE
            
            // Masquer également le titre de la section produits
            binding.tvProductSectionTitle?.visibility = View.GONE
            
            // Masquer les rangées de produits
            binding.productRowLayout1?.visibility = View.GONE
            binding.productRowLayout2?.visibility = View.GONE
            
            Log.d(TAG, "Section produits masquée avec succès")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la suppression des produits: ${e.message}")
        }
    }
} 