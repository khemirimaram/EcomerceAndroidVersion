package com.example.ecommerce.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ecommerce.databinding.ItemCartBinding
import com.example.ecommerce.models.CartItem

class CartAdapter(
    private val cartItems: MutableList<CartItem>,
    private val onQuantityChanged: (CartItem, Int) -> Unit,
    private val onItemRemoved: (CartItem, Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(private val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(cartItem: CartItem) {
            binding.apply {
                tvProductName.text = cartItem.productName
                tvProductPrice.text = "$${String.format("%.2f", cartItem.productPrice)}"
                tvQuantity.text = cartItem.quantity.toString()

                Glide.with(ivProductImage.context)
                    .load(cartItem.productImage)
                    .into(ivProductImage)

                btnIncrease.setOnClickListener {
                    val newQuantity = cartItem.quantity + 1
                    val updatedItem = cartItem.copy(quantity = newQuantity)
                    cartItems[adapterPosition] = updatedItem
                    tvQuantity.text = newQuantity.toString()
                    onQuantityChanged(updatedItem, adapterPosition)
                }

                btnDecrease.setOnClickListener {
                    if (cartItem.quantity > 1) {
                        val newQuantity = cartItem.quantity - 1
                        val updatedItem = cartItem.copy(quantity = newQuantity)
                        cartItems[adapterPosition] = updatedItem
                        tvQuantity.text = newQuantity.toString()
                        onQuantityChanged(updatedItem, adapterPosition)
                    }
                }

                btnRemove.setOnClickListener {
                    onItemRemoved(cartItem, adapterPosition)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(cartItems[position])
    }

    override fun getItemCount() = cartItems.size
} 