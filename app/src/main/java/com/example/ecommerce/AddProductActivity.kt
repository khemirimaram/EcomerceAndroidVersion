package com.example.ecommerce

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.ecommerce.databinding.ActivityAddProductBinding
import com.example.ecommerce.firebase.FirebaseManager
import com.example.ecommerce.models.Product
import java.io.File
import java.io.FileOutputStream
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class AddProductActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddProductBinding
    private val selectedImages = mutableListOf<Uri>()
    private val MAX_IMAGES = 5
    private val STORAGE_PERMISSION_CODE = 1001
    
    // Uri temporaires pour les images converties
    private val tempImageUris = mutableListOf<Uri>()
    
    private val pickImagesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Gestion de la sélection unique
            result.data?.data?.let { uri ->
                if (selectedImages.size < MAX_IMAGES) {
                    selectedImages.add(uri)
                    updateImageCountText()
                } else {
                    Toast.makeText(this, "Maximum $MAX_IMAGES images autorisées", Toast.LENGTH_SHORT).show()
                }
            }
            
            // Gestion de la sélection multiple
            val clipData = result.data?.clipData
            if (clipData != null) {
                val countToAdd = minOf(clipData.itemCount, MAX_IMAGES - selectedImages.size)
                for (i in 0 until countToAdd) {
                    clipData.getItemAt(i)?.uri?.let { selectedImages.add(it) }
                }
                updateImageCountText()
                
                if (clipData.itemCount > countToAdd) {
                    Toast.makeText(this, "Maximum $MAX_IMAGES images autorisées", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Vérifier que l'utilisateur est connecté avant d'autoriser l'ajout de produit
        if (FirebaseManager.getCurrentUserId() == null) {
            Toast.makeText(this, "Vous devez être connecté pour ajouter un produit", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        setupUI()
    }
    
    private fun setupUI() {
        setupCategorySpinner()
        setupConditionSpinner()
        setupClickListeners()
    }
    
    private fun setupCategorySpinner() {
        val categories = arrayOf("Autre", "Électronique", "Livres", "Vêtements", "Sports", "Maison", "Jardin", "Calculatrices")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }
    
    private fun setupConditionSpinner() {
        val conditions = arrayOf("Bon état", "Neuf", "Comme neuf", "État moyen", "À rénover")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, conditions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCondition.adapter = adapter
    }
    
    private fun setupClickListeners() {
        // Bouton de sélection d'images
        binding.btnSelectImages.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            pickImagesLauncher.launch(intent)
        }
        
        // Bouton d'annulation
        binding.btnCancel.setOnClickListener {
            finish()
        }
        
        // Bouton de soumission
        binding.btnSubmit.setOnClickListener {
            submitProduct()
        }
    }
    
    private fun updateImageCountText() {
        binding.tvImageCount.text = "${selectedImages.size}/$MAX_IMAGES images sélectionnées"
        binding.tvImageCount.visibility = if (selectedImages.isEmpty()) View.GONE else View.VISIBLE
    }
    
    private fun submitProduct() {
        // Validation des entrées
        val name = binding.etProductName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val priceText = binding.etPrice.text.toString().trim()
        val category = binding.spinnerCategory.selectedItem.toString()
        val condition = binding.spinnerCondition.selectedItem.toString()
        val quantity = binding.etQuantity.text.toString().trim()
        
        // Vérification des champs obligatoires
        if (name.isEmpty()) {
            binding.etProductName.error = "Champ obligatoire"
            return
        }
        
        if (description.isEmpty()) {
            binding.etDescription.error = "Champ obligatoire"
            return
        }
        
        if (priceText.isEmpty()) {
            binding.etPrice.error = "Champ obligatoire"
            return
        }
        
        val price = priceText.toDoubleOrNull()
        if (price == null || price <= 0) {
            binding.etPrice.error = "Prix invalide"
            return
        }
        
        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "Veuillez sélectionner au moins une image", Toast.LENGTH_SHORT).show()
            return
        }

        // Afficher le chargement
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmit.isEnabled = false
        
        // Log des URIs des images sélectionnées pour débogage
        for (uri in selectedImages) {
            Log.d("AddProductActivity", "Image URI sélectionnée: $uri")
        }
        
        // Vérifier les permissions de stockage si nécessaire
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
                return
            }
        }
        
        // Convertir les URIs en fichiers temporaires si nécessaire
        convertGoogleDriveUris(selectedImages) { convertedUris ->
            // Enregistrer les URIs converties pour les nettoyer plus tard
            tempImageUris.clear()
            tempImageUris.addAll(convertedUris)
            
            // Créer l'objet produit
            val product = Product(
                id = "",
                name = name,
                description = description,
                price = price,
                category = category,
                condition = condition,
                sellerUserId = FirebaseManager.getCurrentUserId() ?: "",
                sellerName = "",
                location = "Tunisie", // Valeur par défaut
                imageUrl = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            // Télécharger le produit avec les images
            FirebaseManager.uploadProduct(product, convertedUris) { success, message ->
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                    
                    if (success) {
                        Toast.makeText(this, "Produit ajouté avec succès", Toast.LENGTH_SHORT).show()
                        // Nettoyer les fichiers temporaires avant de quitter
                        cleanupTempFiles()
                        finish()
                    } else {
                        Toast.makeText(this, "Erreur: $message", Toast.LENGTH_SHORT).show()
                        Log.e("AddProductActivity", "Erreur lors de l'ajout du produit: $message")
                        // Nettoyer les fichiers temporaires même en cas d'erreur
                        cleanupTempFiles()
                    }
                }
            }
        }
    }
    
    private fun convertGoogleDriveUris(originalUris: List<Uri>, callback: (List<Uri>) -> Unit) {
        val convertedUris = mutableListOf<Uri>()
        var remaining = originalUris.size
        
        if (originalUris.isEmpty()) {
            callback(convertedUris)
            return
        }
        
        for (uri in originalUris) {
            try {
                // Pour tous les types d'URIs, on va créer des fichiers temporaires
                // pour s'assurer que Firebase Storage peut les lire
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    try {
                        // Créer un fichier temporaire avec extension .jpg
                        val timestamp = System.currentTimeMillis()
                        val tempFile = File(cacheDir, "temp_img_${timestamp}.jpg")
                        val outputStream = FileOutputStream(tempFile)
                        
                        // Copier le contenu
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }
                        
                        // Fermer les flux
                        outputStream.flush()
                        outputStream.close()
                        inputStream.close()
                        
                        // Ajouter l'URI du fichier temporaire
                        val fileUri = Uri.fromFile(tempFile)
                        Log.d("AddProductActivity", "URI convertie: $fileUri")
                        convertedUris.add(fileUri)
                    } catch (e: Exception) {
                        Log.e("AddProductActivity", "Erreur lors de la conversion de l'URI: ${e.message}", e)
                        // En cas d'erreur, tenter d'utiliser l'URI originale
                        convertedUris.add(uri)
                    }
                } else {
                    Log.e("AddProductActivity", "Impossible d'ouvrir le flux pour: $uri")
                    // Utiliser l'URI originale
                    convertedUris.add(uri)
                }
            } catch (e: Exception) {
                Log.e("AddProductActivity", "Erreur lors de la conversion de l'URI: ${e.message}", e)
                // En cas d'erreur, utiliser l'URI originale
                convertedUris.add(uri)
            } finally {
                remaining--
                if (remaining == 0) {
                    callback(convertedUris)
                }
            }
        }
    }
    
    private fun cleanupTempFiles() {
        for (uri in tempImageUris) {
            try {
                if (uri.scheme == "file") {
                    val path = uri.path
                    if (path != null) {
                        val file = File(path)
                        if (file.exists() && file.isFile) {
                            val deleted = file.delete()
                            Log.d("AddProductActivity", "Fichier temporaire supprimé: $deleted - $path")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AddProductActivity", "Erreur lors de la suppression du fichier temporaire: ${e.message}")
            }
        }
        tempImageUris.clear()
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission accordée, relancer la soumission
                submitProduct()
            } else {
                Toast.makeText(this, "Permission de stockage nécessaire pour envoyer des images", Toast.LENGTH_LONG).show()
                binding.progressBar.visibility = View.GONE
                binding.btnSubmit.isEnabled = true
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // S'assurer que les fichiers temporaires sont supprimés
        cleanupTempFiles()
    }
}
