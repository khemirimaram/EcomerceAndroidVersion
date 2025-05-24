package com.example.ecommerce

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.ecommerce.adapters.ProductAdapter
import com.example.ecommerce.databinding.FragmentHomeBinding
import com.example.ecommerce.models.Product

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            onProductClick = { product -> handleProductClick(product) },
            onFavoriteClick = { product -> handleFavoriteClick(product) }
        )

        binding.productsRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = productAdapter
        }
    }

    private fun handleProductClick(product: Product) {
        // Implémenter la logique de clic sur le produit
    }

    private fun handleFavoriteClick(product: Product) {
        // Implémenter la logique de favori
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 