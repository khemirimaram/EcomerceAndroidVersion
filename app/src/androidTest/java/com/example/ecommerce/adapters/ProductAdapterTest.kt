package com.example.ecommerce.adapters

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ecommerce.models.Product
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductAdapterTest {
    private lateinit var context: Context
    private lateinit var adapter: ProductAdapter
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        adapter = ProductAdapter(
            onProductClick = { /* Ne rien faire dans le test */ },
            onFavoriteClick = { /* Ne rien faire dans le test */ }
        )
    }
    
    @Test
    fun testSubmitList() {
        val products = listOf(
            Product(
                id = "1",
                name = "Product 1",
                description = "Description 1",
                price = 99.99,
                images = listOf("image1.jpg", "image2.jpg"),
                category = "Electronics",
                sellerId = "seller1",
                sellerName = "Seller 1",
                condition = "New"
            ),
            Product(
                id = "2",
                name = "Product 2",
                description = "Description 2",
                price = 149.99,
                images = listOf("image3.jpg", "image4.jpg"),
                category = "Electronics",
                sellerId = "seller2",
                sellerName = "Seller 2",
                condition = "Used"
            )
        )
        
        adapter.submitList(products)
        
        Truth.assertThat(adapter.itemCount).isEqualTo(2)
        Truth.assertThat(adapter.currentList).isEqualTo(products)
    }
    
    @Test
    fun testEmptyList() {
        adapter.submitList(emptyList())
        
        Truth.assertThat(adapter.itemCount).isEqualTo(0)
        Truth.assertThat(adapter.currentList).isEmpty()
    }
    
    @Test
    fun testUpdateList() {
        val initialProducts = listOf(
            Product(
                id = "1",
                name = "Product 1",
                description = "Description 1",
                price = 99.99,
                images = listOf("image1.jpg"),
                category = "Electronics",
                sellerId = "seller1",
                sellerName = "Seller 1",
                condition = "New"
            )
        )
        
        val updatedProducts = listOf(
            Product(
                id = "1",
                name = "Product 1",
                description = "Description 1",
                price = 99.99,
                images = listOf("image1.jpg"),
                category = "Electronics",
                sellerId = "seller1",
                sellerName = "Seller 1",
                condition = "New"
            ),
            Product(
                id = "2",
                name = "Product 2",
                description = "Description 2",
                price = 149.99,
                images = listOf("image2.jpg"),
                category = "Electronics",
                sellerId = "seller2",
                sellerName = "Seller 2",
                condition = "Used"
            )
        )
        
        adapter.submitList(initialProducts)
        Truth.assertThat(adapter.itemCount).isEqualTo(1)
        
        adapter.submitList(updatedProducts)
        Truth.assertThat(adapter.itemCount).isEqualTo(2)
    }
} 