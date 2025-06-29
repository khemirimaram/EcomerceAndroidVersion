package com.example.ecommerce.models

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProductTest {
    
    @Test
    fun `test product creation with primary constructor`() {
        val images = listOf("image1.jpg", "image2.jpg")
        val currentTime = System.currentTimeMillis()
        
        val product = Product(
            id = "123",
            name = "Test Product",
            description = "Test Description",
            price = 99.99,
            images = images,
            category = "Electronics",
            sellerId = "seller123",
            sellerName = "John Doe",
            condition = "New",
            createdAt = currentTime,
            status = "active"
        )
        
        assertThat(product.id).isEqualTo("123")
        assertThat(product.name).isEqualTo("Test Product")
        assertThat(product.description).isEqualTo("Test Description")
        assertThat(product.price).isEqualTo(99.99)
        assertThat(product.images).containsExactlyElementsIn(images)
        assertThat(product.category).isEqualTo("Electronics")
        assertThat(product.sellerId).isEqualTo("seller123")
        assertThat(product.sellerName).isEqualTo("John Doe")
        assertThat(product.condition).isEqualTo("New")
        assertThat(product.createdAt).isEqualTo(currentTime)
        assertThat(product.status).isEqualTo("active")
    }
    
    @Test
    fun `test product creation with secondary constructor`() {
        val images = listOf("image1.jpg", "image2.jpg")
        val currentTime = System.currentTimeMillis()
        
        val product = Product(
            id = "123",
            name = "Test Product",
            description = "Test Description",
            price = 99.99,
            category = "Electronics",
            condition = "New",
            sellerId = "seller123",
            sellerName = "John Doe",
            location = "Paris",
            images = images,
            createdAt = currentTime,
            updatedAt = currentTime
        )
        
        assertThat(product.id).isEqualTo("123")
        assertThat(product.name).isEqualTo("Test Product")
        assertThat(product.description).isEqualTo("Test Description")
        assertThat(product.price).isEqualTo(99.99)
        assertThat(product.images).containsExactlyElementsIn(images)
        assertThat(product.category).isEqualTo("Electronics")
        assertThat(product.sellerId).isEqualTo("seller123")
        assertThat(product.sellerName).isEqualTo("John Doe")
        assertThat(product.condition).isEqualTo("New")
        assertThat(product.location).isEqualTo("Paris")
    }
    
    @Test
    fun `test isNew property for recent product`() {
        val currentTime = System.currentTimeMillis()
        val product = Product(createdAt = currentTime)
        assertThat(product.isNew).isTrue()
    }
    
    @Test
    fun `test isNew property for old product`() {
        val oldTime = System.currentTimeMillis() - (8 * 24 * 60 * 60 * 1000) // 8 days old
        val product = Product(createdAt = oldTime)
        assertThat(product.isNew).isFalse()
    }
    
    @Test
    fun `test default values`() {
        val product = Product()
        
        assertThat(product.id).isEmpty()
        assertThat(product.name).isEmpty()
        assertThat(product.description).isEmpty()
        assertThat(product.price).isEqualTo(0.0)
        assertThat(product.images).isEmpty()
        assertThat(product.category).isEmpty()
        assertThat(product.sellerId).isEmpty()
        assertThat(product.sellerName).isEmpty()
        assertThat(product.condition).isEmpty()
        assertThat(product.status).isEqualTo("active")
        assertThat(product.isFavorite).isFalse()
    }
    
    @Test
    fun `test product status constants`() {
        assertThat(Product.STATUS_ACTIVE).isEqualTo("active")
        assertThat(Product.STATUS_SOLD).isEqualTo("sold")
        assertThat(Product.STATUS_ARCHIVED).isEqualTo("archived")
    }
    
    @Test
    fun `test product condition constants`() {
        assertThat(Product.CONDITION_NEW).isEqualTo("new")
        assertThat(Product.CONDITION_LIKE_NEW).isEqualTo("likeNew")
        assertThat(Product.CONDITION_GOOD).isEqualTo("good")
        assertThat(Product.CONDITION_FAIR).isEqualTo("fair")
        assertThat(Product.CONDITION_POOR).isEqualTo("poor")
    }
} 