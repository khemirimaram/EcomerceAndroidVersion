package com.example.ecommerce

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import android.util.Patterns
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes

class LoginActivity : AppCompatActivity() {
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var loginButton: MaterialButton
    private lateinit var googleSignInButton: MaterialButton
    private lateinit var signUpTextView: View
    private lateinit var forgotPasswordTextView: View
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Configure Google Sign In with web client ID
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(this, gso)
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring Google Sign In", e)
            // Désactiver le bouton Google si la configuration échoue
            googleSignInButton.isEnabled = false
        }

        // Initialize views
        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        emailLayout = findViewById(R.id.emailLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        loginButton = findViewById(R.id.loginButton)
        googleSignInButton = findViewById(R.id.googleSignInButton)
        signUpTextView = findViewById(R.id.signUpTextView)
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView)
    }

    private fun setupClickListeners() {
        loginButton.setOnClickListener {
            val email = emailEditText.text?.toString()?.trim() ?: ""
            val password = passwordEditText.text?.toString() ?: ""

            // Reset errors
            emailLayout.error = null
            passwordLayout.error = null

            if (validateLoginInputs(email, password)) {
                signInWithEmailPassword(email, password)
            }
        }

        googleSignInButton.setOnClickListener {
            startGoogleSignIn()
        }

        signUpTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }

        forgotPasswordTextView.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun validateLoginInputs(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            emailLayout.error = "Veuillez entrer votre email"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Veuillez entrer un email valide"
            isValid = false
        }

        if (password.isEmpty()) {
            passwordLayout.error = "Veuillez entrer votre mot de passe"
            isValid = false
        }

        return isValid
    }

    private fun showForgotPasswordDialog() {
        val email = emailEditText.text?.toString()?.trim() ?: ""
        
        if (email.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Réinitialisation du mot de passe")
                .setMessage("Veuillez entrer votre adresse email dans le champ email ci-dessus pour recevoir le lien de réinitialisation.")
                .setPositiveButton("OK", null)
                .show()
            emailLayout.error = "Email requis pour la réinitialisation"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Veuillez entrer un email valide"
            return
        }

        val loadingDialog = AlertDialog.Builder(this)
            .setMessage("Envoi en cours...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                loadingDialog.dismiss()
                if (task.isSuccessful) {
                    showSuccessDialog(email)
                } else {
                    showErrorDialog(task.exception?.message ?: "Une erreur s'est produite")
                    Log.e(TAG, "Password reset failed", task.exception)
                }
            }
    }

    private fun startGoogleSignIn() {
        try {
            googleSignInButton.isEnabled = false
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Google Sign In", e)
            showErrorDialog("Erreur lors de la connexion Google. Veuillez réessayer.")
            googleSignInButton.isEnabled = true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            googleSignInButton.isEnabled = true
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                    account.idToken?.let { token ->
                        firebaseAuthWithGoogle(token)
                    } ?: run {
                        Log.w(TAG, "Google Sign In failed - no ID token")
                        showErrorDialog("Échec de l'authentification Google")
                    }
                } catch (e: ApiException) {
                    Log.w(TAG, "Google sign in failed", e)
                    val errorMessage = when (e.statusCode) {
                        GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Connexion Google annulée"
                        GoogleSignInStatusCodes.NETWORK_ERROR -> "Erreur réseau. Vérifiez votre connexion internet."
                        else -> "Échec de la connexion Google (${e.statusCode})"
                    }
                    showErrorDialog(errorMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in Google Sign In result", e)
                showErrorDialog("Une erreur s'est produite. Veuillez réessayer.")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val loadingDialog = AlertDialog.Builder(this)
            .setMessage("Connexion en cours...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                loadingDialog.dismiss()
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    showErrorDialog("Échec de l'authentification: ${task.exception?.message}")
                }
            }
    }

    private fun signInWithEmailPassword(email: String, password: String) {
        try {
            loginButton.isEnabled = false
            val loadingDialog = AlertDialog.Builder(this)
                .setMessage("Connexion en cours...")
                .setCancelable(false)
                .create()
            loadingDialog.show()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    try {
                        loadingDialog.dismiss()
                        loginButton.isEnabled = true
                        
                        if (task.isSuccessful) {
                            Log.d(TAG, "Connexion réussie")
                            // Créer l'intent pour MainActivity
                            val intent = Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            startActivity(intent)
                            finish()
                        } else {
                            val errorMessage = when (task.exception?.message) {
                                "The password is invalid or the user does not have a password." -> 
                                    "Mot de passe incorrect"
                                "There is no user record corresponding to this identifier. The user may have been deleted." -> 
                                    "Aucun compte trouvé avec cet email"
                                else -> "Échec de la connexion: ${task.exception?.message}"
                            }
                            showErrorDialog(errorMessage)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur lors du traitement de la connexion", e)
                        try {
                            loadingDialog.dismiss()
                        } catch (e2: Exception) {
                            Log.e(TAG, "Erreur lors de la fermeture du dialogue", e2)
                        }
                        loginButton.isEnabled = true
                        showErrorDialog("Une erreur s'est produite lors de la connexion")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Erreur de connexion", e)
                    try {
                        loadingDialog.dismiss()
                    } catch (e2: Exception) {
                        Log.e(TAG, "Erreur lors de la fermeture du dialogue", e2)
                    }
                    loginButton.isEnabled = true
                    showErrorDialog("Erreur de connexion: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur critique lors de la connexion", e)
            loginButton.isEnabled = true
            showErrorDialog("Une erreur critique s'est produite")
        }
    }

    private fun showErrorDialog(message: String) {
        try {
            if (!isFinishing) {
                AlertDialog.Builder(this)
                    .setTitle("Erreur")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'affichage du dialogue d'erreur", e)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showSuccessDialog(email: String) {
        try {
            if (!isFinishing) {
                AlertDialog.Builder(this)
                    .setTitle("Email envoyé")
                    .setMessage("Un email de réinitialisation a été envoyé à $email")
                    .setPositiveButton("OK", null)
                    .show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'affichage du dialogue de succès", e)
            Toast.makeText(this, "Email de réinitialisation envoyé à $email", Toast.LENGTH_LONG).show()
        }
    }
} 