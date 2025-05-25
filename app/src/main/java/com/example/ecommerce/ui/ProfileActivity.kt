package com.example.ecommerce.ui

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async

class ProfileActivity : AppCompatActivity() {
    // ... rest of the file content ...
} 