package com.example.ecommerce.adapters

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.ecommerce.R
import com.example.ecommerce.databinding.ItemProductGridBinding
import com.example.ecommerce.models.Product
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(
    private val onProductClick: (Product) -> Unit,
    private val onFavoriteClick: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    init {
        Log.d("ProductAdapter", "Adapter initialized")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        Log.d("ProductAdapter", "Creating new ViewHolder")
        val binding = ItemProductGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)
        holder.bind(product)
    }

    override fun submitList(list: List<Product>?) {
        Log.d("ProductAdapter", "Submitting new list with ${list?.size ?: 0} items")
        super.submitList(list)
    }

    inner class ProductViewHolder(
        val binding: ItemProductGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onProductClick(getItem(position))
                }
            }

            binding.favoriteButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onFavoriteClick(getItem(position))
                }
            }
        }

        fun bind(product: Product) {
            binding.apply {
                productTitle.text = product.name
                productPrice.text = formatPrice(product.price)
                favoriteButton.setImageResource(
                    if (product.isFavorite) R.drawable.ic_favorite
                    else R.drawable.ic_favorite_border
                )

                exchangeInfo.apply {
                    visibility = if (product.isAvailableForExchange == true) View.VISIBLE else View.GONE
                    text = if (product.exchangePreferences?.isNotEmpty() == true) {
                        "Échange souhaité: ${product.exchangePreferences}"
                    } else {
                        "Disponible pour échange"
                    }
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_swap_horiz, 0, 0, 0)
                }
                
                try {
                    val imageUrl = product.images.firstOrNull()
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(productImage)
                            .load(imageUrl)
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.error_image)
                            .centerCrop()
                            .into(productImage)
                    } else {
                        productImage.setImageResource(R.drawable.placeholder_image)
                    }
                } catch (e: Exception) {
                    Log.e("ProductAdapter", "Error loading image for ${product.id}: ${e.message}")
                    productImage.setImageResource(R.drawable.error_image)
                }
            }
        }

        private fun formatPrice(price: Double): String {
            return String.format("%.2f DT", price)
        }
    }

    private fun isProductNew(createdAt: Long): Boolean {
        val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L
        return System.currentTimeMillis() - createdAt < sevenDaysInMillis
    }

    private class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
} 