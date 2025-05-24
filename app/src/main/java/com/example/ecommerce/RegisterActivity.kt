package com.example.ecommerce

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var firstNameEditText: TextInputEditText
    private lateinit var lastNameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    
    private lateinit var firstNameLayout: TextInputLayout
    private lateinit var lastNameLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        firstNameEditText = findViewById(R.id.firstNameEditText)
        lastNameEditText = findViewById(R.id.lastNameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        
        firstNameLayout = findViewById(R.id.firstNameLayout)
        lastNameLayout = findViewById(R.id.lastNameLayout)
        emailLayout = findViewById(R.id.emailLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout)
    }

    private fun setupClickListeners() {
        findViewById<android.view.View>(R.id.registerButton).setOnClickListener {
            if (validateInputs()) {
                registerUser()
            }
        }

        findViewById<android.view.View>(R.id.loginTextView).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        
        // Reset all errors
        firstNameLayout.error = null
        lastNameLayout.error = null
        emailLayout.error = null
        passwordLayout.error = null
        confirmPasswordLayout.error = null

        val firstName = firstNameEditText.text?.toString()?.trim() ?: ""
        val lastName = lastNameEditText.text?.toString()?.trim() ?: ""
        val email = emailEditText.text?.toString()?.trim() ?: ""
        val password = passwordEditText.text?.toString() ?: ""
        val confirmPassword = confirmPasswordEditText.text?.toString() ?: ""

        if (firstName.isEmpty()) {
            firstNameLayout.error = "Veuillez entrer votre prénom"
            isValid = false
        }

        if (lastName.isEmpty()) {
            lastNameLayout.error = "Veuillez entrer votre nom"
            isValid = false
        }

        if (email.isEmpty()) {
            emailLayout.error = "Veuillez entrer votre email"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Veuillez entrer un email valide"
            isValid = false
        }

        if (password.isEmpty()) {
            passwordLayout.error = "Veuillez entrer un mot de passe"
            isValid = false
        } else if (password.length < 6) {
            passwordLayout.error = "Le mot de passe doit contenir au moins 6 caractères"
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.error = "Veuillez confirmer votre mot de passe"
            isValid = false
        } else if (password != confirmPassword) {
            confirmPasswordLayout.error = "Les mots de passe ne correspondent pas"
            isValid = false
        }

        return isValid
    }

    private fun registerUser() {
        val email = emailEditText.text?.toString()?.trim() ?: ""
        val password = passwordEditText.text?.toString() ?: ""
        val firstName = firstNameEditText.text?.toString()?.trim() ?: ""
        val lastName = lastNameEditText.text?.toString()?.trim() ?: ""

        val loadingDialog = AlertDialog.Builder(this)
            .setMessage("Création du compte en cours...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Save user additional info in Firestore
                    val user = auth.currentUser
                    val userInfo = hashMapOf(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "email" to email
                    )

                    user?.let { firebaseUser ->
                        db.collection("users")
                            .document(firebaseUser.uid)
                            .set(userInfo)
                            .addOnSuccessListener {
                                loadingDialog.dismiss()
                                showSuccessDialog()
                            }
                            .addOnFailureListener { e ->
                                loadingDialog.dismiss()
                                showErrorDialog("Erreur lors de la sauvegarde des informations: ${e.message}")
                            }
                    }
                } else {
                    loadingDialog.dismiss()
                    val errorMessage = when (task.exception?.message) {
                        "The email address is already in use by another account." ->
                            "Cette adresse email est déjà utilisée"
                        "The email address is badly formatted." ->
                            "Format d'email invalide"
                        else -> "Erreur lors de la création du compte: ${task.exception?.message}"
                    }
                    showErrorDialog(errorMessage)
                }
            }
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Compte créé")
            .setMessage("Votre compte a été créé avec succès !")
            .setPositiveButton("OK") { _, _ ->
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Erreur")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
} 