package com.example.ecommerce.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ecommerce.databinding.ItemCategoryBinding
import com.example.ecommerce.R

class CategoryAdapter(
    private val categories: List<String>,
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: String) {
            binding.textViewCategoryName.text = category
            
            // Set icon based on category
            val iconResId = when (category.lowercase()) {
                "écriture" -> R.drawable.ic_pen
                "cahier et classeur" -> R.drawable.ic_notebook
                "sacs et cartable" -> R.drawable.ic_bag
                "arts plastique" -> R.drawable.ic_art
                "électronique" -> R.drawable.ic_electronics_new
                "livres scolaires" -> R.drawable.ic_books
                "papeterie" -> R.drawable.ic_stationery
                else -> R.drawable.ic_category_default
            }
            binding.imageViewCategory.setImageResource(iconResId)

            itemView.setOnClickListener {
                onCategoryClick(category)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount() = categories.size
} 