package com.example.ecommerce

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var emailEditText: EditText
    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var dayEditText: EditText
    private lateinit var monthEditText: EditText
    private lateinit var yearEditText: EditText
    private lateinit var genderRadioGroup: RadioGroup
    private lateinit var femaleRadioButton: RadioButton
    private lateinit var maleRadioButton: RadioButton
    private lateinit var registerButton: Button
    private lateinit var passwordToggle: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        emailEditText = findViewById(R.id.emailEditText)
        firstNameEditText = findViewById(R.id.firstNameEditText)
        lastNameEditText = findViewById(R.id.lastNameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        dayEditText = findViewById(R.id.dayEditText)
        monthEditText = findViewById(R.id.monthEditText)
        yearEditText = findViewById(R.id.yearEditText)
        genderRadioGroup = findViewById(R.id.genderRadioGroup)
        femaleRadioButton = findViewById(R.id.femaleRadioButton)
        maleRadioButton = findViewById(R.id.maleRadioButton)
        registerButton = findViewById(R.id.registerButton)
        passwordToggle = findViewById(R.id.passwordToggle)

        // Set up click listeners
        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val firstName = firstNameEditText.text.toString().trim()
            val lastName = lastNameEditText.text.toString().trim()
            val password = passwordEditText.text.toString()
            val day = dayEditText.text.toString().trim()
            val month = monthEditText.text.toString().trim()
            val year = yearEditText.text.toString().trim()
            val gender = if (femaleRadioButton.isChecked) "female" else "male"

            // Validate inputs
            if (!validateInputs(email, firstName, lastName, password, day, month, year)) {
                return@setOnClickListener
            }
            
            // Create account
            createAccount(email, password, firstName, lastName, "$day/$month/$year", gender)
        }

        // Handle password visibility toggle
        passwordToggle.setOnClickListener {
            passwordVisible = !passwordVisible
            togglePasswordVisibility(passwordVisible)
        }
    }

    private fun togglePasswordVisibility(isVisible: Boolean) {
        if (isVisible) {
            // Show password
            passwordEditText.transformationMethod = null
            passwordToggle.setImageResource(R.drawable.ic_visibility_off)
        } else {
            // Hide password
            passwordEditText.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
            passwordToggle.setImageResource(R.drawable.ic_visibility)
        }
        // Move cursor to the end of text
        passwordEditText.setSelection(passwordEditText.text.length)
    }

    private fun validateInputs(
        email: String,
        firstName: String,
        lastName: String,
        password: String,
        day: String,
        month: String,
        year: String
    ): Boolean {
        when {
            email.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || 
            password.isEmpty() || day.isEmpty() || month.isEmpty() || year.isEmpty() -> {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                Toast.makeText(this, "Veuillez entrer une adresse email valide", Toast.LENGTH_SHORT).show()
                return false
            }
            password.length < 6 -> {
                Toast.makeText(this, "Le mot de passe doit contenir au moins 6 caractères", Toast.LENGTH_SHORT).show()
                return false
            }
            !isValidDate(day, month, year) -> {
                Toast.makeText(this, "Veuillez entrer une date de naissance valide", Toast.LENGTH_SHORT).show()
                return false
            }
            else -> return true
        }
    }

    private fun isValidDate(day: String, month: String, year: String): Boolean {
        try {
            val dayInt = day.toInt()
            val monthInt = month.toInt()
            val yearInt = year.toInt()
            
            if (dayInt < 1 || dayInt > 31 || monthInt < 1 || monthInt > 12 || yearInt < 1900 || yearInt > 2020) {
                return false
            }
            
            // Further date validation could be added here
            return true
        } catch (e: NumberFormatException) {
            return false
        }
    }

    private fun createAccount(email: String, password: String, firstName: String, lastName: String, 
                              dateOfBirth: String, gender: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Save user details to Firestore
                    val user = hashMapOf(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "email" to email,
                        "dateOfBirth" to dateOfBirth,
                        "gender" to gender,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )

                    db.collection("users")
                        .document(auth.currentUser!!.uid)
                        .set(user)
                        .addOnSuccessListener {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Erreur lors de la sauvegarde des données: ${e.message}",
                                Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Échec de l'inscription: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }
} 