package com.example.ecommerce

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecommerce.adapters.ProductAdapter
import com.example.ecommerce.adapters.ReviewAdapter
import com.example.ecommerce.databinding.ActivityProfileBinding
import com.example.ecommerce.models.Product
import com.example.ecommerce.models.Review
import com.example.ecommerce.models.User
import com.example.ecommerce.repositories.ReviewRepository
import com.example.ecommerce.ui.ProductDetailsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var productAdapter: ProductAdapter
    private lateinit var reviewAdapter: ReviewAdapter
    private lateinit var reviewRepository: ReviewRepository
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "ProfileActivity"
    
    private var userId: String? = null
    private var currentUser: User? = null
    private var loadProfileJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        Log.d(TAG, "ProfileActivity onCreate")
        
        setupToolbar()
        setupAdapters()
        setupRecyclerViews()
        
        // Get user ID from intent or current user
        userId = intent.getStringExtra("USER_ID")
        Log.d(TAG, "Received USER_ID from intent: $userId")
        
        if (userId == null) {
            userId = auth.currentUser?.uid
            Log.d(TAG, "Using current user ID as fallback: $userId")
        }
        
        if (userId == null) {
            Log.e(TAG, "No user ID available")
            showLoginRequired()
            return
        }
        
        loadUserProfile()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        binding.ivBack.setOnClickListener {
            finish()
        }
    }
    
    private fun setupAdapters() {
        productAdapter = ProductAdapter(
            onProductClick = { product ->
                startActivity(Intent(this, ProductDetailsActivity::class.java).apply {
                    putExtra("PRODUCT_ID", product.id)
                })
            },
            onFavoriteClick = { product ->
                // TODO: Implement favorite functionality
                Toast.makeText(this, getString(R.string.coming_soon), Toast.LENGTH_SHORT).show()
            }
        )
        reviewAdapter = ReviewAdapter()
        reviewRepository = ReviewRepository()
    }
    
    private fun setupRecyclerViews() {
        binding.productsRecyclerView.apply {
            layoutManager = GridLayoutManager(this@ProfileActivity, 2)
            adapter = productAdapter
        }
        
        binding.reviewsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ProfileActivity)
            adapter = reviewAdapter
        }
    }

    private fun updateProfileUI(user: User) {
        binding.apply {
            tvUsername.text = user.username
            
            // Show/hide action buttons based on whether this is the current user's profile
            val isCurrentUser = auth.currentUser?.uid == userId
            btnEditProfile.visibility = if (isCurrentUser) View.VISIBLE else View.GONE
            
            // Set click listeners
            btnEditProfile.setOnClickListener {
                startActivityForResult(
                    Intent(this@ProfileActivity, EditProfileActivity::class.java),
                    REQUEST_EDIT_PROFILE
                )
            }
            
            btnLogout.setOnClickListener {
                auth.signOut()
                startActivity(Intent(this@ProfileActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
        }
    }

    private fun showLoginRequired() {
        binding.apply {
            contentLayout.visibility = View.GONE
            emptyProductsView.visibility = View.VISIBLE
            emptyReviewsView.visibility = View.VISIBLE
            btnEditProfile.visibility = View.GONE
        }
        
        Toast.makeText(this, getString(R.string.login_required), Toast.LENGTH_LONG).show()
        
        // Redirect to login after delay
        binding.root.postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 1500)
    }

    private fun loadUserProfile() {
        showLoadingState()
        
        loadProfileJob?.cancel()
        loadProfileJob = lifecycleScope.launch {
            try {
                val userDoc = firestore.collection("users")
                    .document(userId!!)
                    .get()
                    .await()
                
                if (!userDoc.exists()) {
                    showError(getString(R.string.user_not_found))
                    return@launch
                }
                
                currentUser = userDoc.toObject(User::class.java)
                currentUser?.let { user ->
                    updateProfileUI(user)
                    
                    // Load products and reviews in parallel
                    val productsDeferred = async { loadUserProducts() }
                    val reviewsDeferred = async { loadUserReviews() }
                    val ratingDeferred = async { reviewRepository.getSellerRating(userId!!) }
                    
                    try {
                        val products = productsDeferred.await()
                        updateProductsUI(products)
                        
                        val reviews = reviewsDeferred.await()
                        updateReviewsUI(reviews)
                        
                        ratingDeferred.await().onSuccess { ratingInfo ->
                            updateRatingUI(ratingInfo.averageRating, ratingInfo.numberOfRatings)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading user data", e)
                        showError(getString(R.string.error_loading_data))
                    }
                }
                
                hideLoadingState()
                
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Error in loadUserProfile", e)
                showError(getString(R.string.error_loading_profile))
            }
        }
    }
    
    private fun updateRatingUI(rating: Float, reviewCount: Int) {
        binding.apply {
            tvRating.text = String.format("%.1f", rating)
            tvReviewCount.text = resources.getQuantityString(
                R.plurals.review_count,
                reviewCount,
                reviewCount
            )
        }
    }
    
    private suspend fun loadUserProducts(): List<Product> {
        return firestore.collection("products")
            .whereEqualTo("sellerId", userId)
            .get()
            .await()
            .toObjects(Product::class.java)
    }
    
    private fun updateProductsUI(products: List<Product>) {
        binding.apply {
            if (products.isEmpty()) {
                productsRecyclerView.visibility = View.GONE
                emptyProductsView.visibility = View.VISIBLE
            } else {
                productsRecyclerView.visibility = View.VISIBLE
                emptyProductsView.visibility = View.GONE
                productAdapter.submitList(products)
                tvProductCount.text = resources.getQuantityString(
                    R.plurals.product_count,
                    products.size,
                    products.size
                )
            }
        }
    }
    
    private suspend fun loadUserReviews(): List<Review> {
        return firestore.collection("reviews")
            .whereEqualTo("sellerId", userId)
            .get()
            .await()
            .toObjects(Review::class.java)
    }
    
    private fun updateReviewsUI(reviews: List<Review>) {
        binding.apply {
            if (reviews.isEmpty()) {
                reviewsRecyclerView.visibility = View.GONE
                emptyReviewsView.visibility = View.VISIBLE
            } else {
                reviewsRecyclerView.visibility = View.VISIBLE
                emptyReviewsView.visibility = View.GONE
                reviewAdapter.submitList(reviews)
            }
        }
    }

    private fun showLoadingState() {
        binding.apply {
            progressBar.visibility = View.VISIBLE
            contentLayout.alpha = 0.5f
            contentLayout.isEnabled = false
        }
    }

    private fun hideLoadingState() {
        binding.apply {
            progressBar.visibility = View.GONE
            contentLayout.alpha = 1.0f
            contentLayout.isEnabled = true
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_EDIT_PROFILE && resultCode == Activity.RESULT_OK) {
            loadUserProfile()
        }
    }

    override fun onPause() {
        super.onPause()
        loadProfileJob?.cancel()
    }
    
    companion object {
        private const val REQUEST_EDIT_PROFILE = 100
    }
} 