package com.example.ecommerce

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ecommerce.models.Product
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductDetailActivityTest {

    private fun createTestProduct(): Product {
        return Product(
            id = "test_id",
            name = "Test Product",
            description = "Test Description",
            price = 99.99,
            images = listOf("image1.jpg", "image2.jpg"),
            category = "Electronics",
            sellerUserId = "seller123",
            sellerName = "John Doe",
            condition = "New",
            location = "Paris"
        )
    }

    @Test
    fun testProductDetailsDisplayed() {
        val product = createTestProduct()
        
        val intent = Intent(ApplicationProvider.getApplicationContext(), ProductDetailActivity::class.java).apply {
            putExtra("product", product)
        }
        
        ActivityScenario.launch<ProductDetailActivity>(intent).use {
            // Verify product title is displayed
            onView(withId(R.id.productTitle))
                .check(matches(isDisplayed()))
                .check(matches(withText(product.name)))

            // Verify product price is displayed
            onView(withId(R.id.productPrice))
                .check(matches(isDisplayed()))
                .check(matches(withText(String.format("%.2f €", product.price))))

            // Verify product description is displayed
            onView(withId(R.id.productDescription))
                .check(matches(isDisplayed()))
                .check(matches(withText(product.description)))

            // Verify seller name is displayed
            onView(withId(R.id.sellerName))
                .check(matches(isDisplayed()))
                .check(matches(withText(product.sellerName)))

            // Verify location is displayed
            onView(withId(R.id.productLocation))
                .check(matches(isDisplayed()))
                .check(matches(withText(product.location)))

            // Verify condition is displayed
            onView(withId(R.id.productCondition))
                .check(matches(isDisplayed()))
                .check(matches(withText(product.condition)))
        }
    }

    @Test
    fun testImageViewPagerDisplayed() {
        val product = createTestProduct()
        
        val intent = Intent(ApplicationProvider.getApplicationContext(), ProductDetailActivity::class.java).apply {
            putExtra("product", product)
        }
        
        ActivityScenario.launch<ProductDetailActivity>(intent).use {
            // Verify product image is displayed
            onView(withId(R.id.productImage))
                .check(matches(isDisplayed()))
        }
    }
} 