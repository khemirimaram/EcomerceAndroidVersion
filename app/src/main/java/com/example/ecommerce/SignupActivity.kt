package com.example.ecommerce

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class SignUpActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        val firstName = findViewById<EditText>(R.id.firstNameEditText)
        val lastName = findViewById<EditText>(R.id.lastNameEditText)
        val email = findViewById<EditText>(R.id.emailEditText)
        val password = findViewById<EditText>(R.id.passwordEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val passwordToggle = findViewById<ImageView>(R.id.passwordToggle)

        // Toggle password visibility
        passwordToggle.setOnClickListener {
            if (password.inputType == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) {
                password.inputType = android.text.InputType.TYPE_CLASS_TEXT
                passwordToggle.setImageResource(R.drawable.ic_visibility_off)
            } else {
                password.inputType = android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordToggle.setImageResource(R.drawable.ic_visibility)
            }
        }

        registerButton.setOnClickListener {
            val firstNameStr = firstName.text.toString().trim()
            val lastNameStr = lastName.text.toString().trim()
            val emailStr = email.text.toString().trim()
            val passwordStr = password.text.toString()

            // Validation
            when {
                firstNameStr.isEmpty() || lastNameStr.isEmpty() || emailStr.isEmpty() -> {
                    Toast.makeText(this, "Veuillez remplir tous les champs obligatoires", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                passwordStr.isEmpty() || passwordStr.length < 6 -> {
                    Toast.makeText(this, "Le mot de passe doit contenir au moins 6 caractères", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Create user with email and password
            auth.createUserWithEmailAndPassword(emailStr, passwordStr)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Save additional user info to Firestore
                        val user = hashMapOf(
                            "firstName" to firstNameStr,
                            "lastName" to lastNameStr,
                            "email" to emailStr,
                            "createdAt" to Date()
                        )

                        db.collection("users")
                            .document(auth.currentUser!!.uid)
                            .set(user)
                            .addOnSuccessListener {
                                // Afficher une alerte de réussite avant de rediriger
                                AlertDialog.Builder(this)
                                    .setTitle("Inscription réussie")
                                    .setMessage("Votre compte a été créé avec succès!")
                                    .setPositiveButton("OK") { _, _ ->
                                        val intent = Intent(this, HomeActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                    .setCancelable(false)
                                    .show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Erreur: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
} 