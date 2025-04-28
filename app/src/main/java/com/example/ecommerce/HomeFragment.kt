package com.example.ecommerce

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecommerce.models.Product

class HomeFragment : Fragment(), ProductAdapter.OnProductClickListener {
    private lateinit var productsRecyclerView: RecyclerView
    private lateinit var progressBar: View
    private lateinit var adapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("HomeFragment", "onCreate called")
        adapter = ProductAdapter(listener = this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("HomeFragment", "onCreateView called")
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        Log.d("HomeFragment", "View inflated successfully")
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("HomeFragment", "onViewCreated called")
        
        try {
            // Initialize views
            productsRecyclerView = view.findViewById(R.id.productsRecyclerView)
            progressBar = view.findViewById(R.id.progressBar)
            
            // Setup RecyclerView
            productsRecyclerView.layoutManager = LinearLayoutManager(context)
            productsRecyclerView.adapter = adapter
            Log.d("HomeFragment", "RecyclerView setup complete")
            
            // Show progress bar while loading
            progressBar.visibility = View.VISIBLE
            
            // Add some test data
            val testProducts = listOf(
                Product(
                    id = "1",
                    name = "Livre de Mathématiques",
                    price = 29.99
                ),
                Product(
                    id = "2",
                    name = "Calculatrice scientifique",
                    price = 79.99
                ),
                Product(
                    id = "3",
                    name = "Cahier d'exercices",
                    price = 9.99
                ),
                Product(
                    id = "4",
                    name = "Kit de géométrie",
                    price = 14.99
                ),
                Product(
                    id = "5",
                    name = "Manuel de physique",
                    price = 34.99
                )
            )
            adapter.setProducts(testProducts)
            
            // Hide progress bar
            progressBar.visibility = View.GONE
            
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error in onViewCreated", e)
        }
    }

    override fun onProductClick(product: Product) {
        // TODO: Handle product click
        Log.d("HomeFragment", "Product clicked: ${product.name}")
    }
} 