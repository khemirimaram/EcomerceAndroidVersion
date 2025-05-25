package com.example.ecommerce.firebase

import com.example.ecommerce.models.Product
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle

@ExperimentalCoroutinesApi
class ProductRepositoryTest {
    
    @Mock
    private lateinit var firestore: FirebaseFirestore
    
    @Mock
    private lateinit var productsCollection: CollectionReference
    
    @Mock
    private lateinit var documentReference: DocumentReference
    
    @Mock
    private lateinit var documentSnapshot: DocumentSnapshot
    
    @Mock
    private lateinit var querySnapshot: QuerySnapshot
    
    private lateinit var productRepository: ProductRepository
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        `when`(firestore.collection("produits")).thenReturn(productsCollection)
        `when`(productsCollection.document(any())).thenReturn(documentReference)
        
        productRepository = ProductRepository(firestore)
    }
    
    @Test
    fun `test add product`() = testScope.runTest {
        val product = Product(
            id = "test_id",
            name = "Test Product",
            description = "Test Description",
            price = 99.99,
            images = listOf("image1.jpg", "image2.jpg")
        )
        
        val taskMock: Task<Void> = mock()
        `when`(documentReference.set(any())).thenReturn(taskMock)
        `when`(taskMock.isSuccessful).thenReturn(true)
        
        productRepository.addProduct(product)
        advanceUntilIdle()
        
        verify(productsCollection).document(product.id)
        verify(documentReference).set(product)
    }
    
    @Test
    fun `test get product`() = testScope.runTest {
        val productId = "test_id"
        val product = Product(
            id = productId,
            name = "Test Product",
            description = "Test Description",
            price = 99.99,
            images = listOf("image1.jpg", "image2.jpg")
        )
        
        val taskMock: Task<DocumentSnapshot> = mock()
        `when`(documentReference.get()).thenReturn(taskMock)
        `when`(taskMock.isSuccessful).thenReturn(true)
        `when`(taskMock.result).thenReturn(documentSnapshot)
        `when`(documentSnapshot.toObject(Product::class.java)).thenReturn(product)
        
        productRepository.getProduct(productId)
        advanceUntilIdle()
        
        verify(productsCollection).document(productId)
        verify(documentReference).get()
    }
    
    @Test
    fun `test get all products`() = testScope.runTest {
        val products = listOf(
            Product(id = "1", name = "Product 1", images = listOf("image1.jpg")),
            Product(id = "2", name = "Product 2", images = listOf("image2.jpg"))
        )
        
        val taskMock: Task<QuerySnapshot> = mock()
        `when`(productsCollection.get()).thenReturn(taskMock)
        `when`(taskMock.isSuccessful).thenReturn(true)
        `when`(taskMock.result).thenReturn(querySnapshot)
        `when`(querySnapshot.toObjects(Product::class.java)).thenReturn(products)
        
        productRepository.getAllProducts()
        advanceUntilIdle()
        
        verify(productsCollection).get()
    }
    
    @Test
    fun `test update product`() = testScope.runTest {
        val product = Product(
            id = "test_id",
            name = "Updated Product",
            description = "Updated Description",
            price = 149.99,
            images = listOf("image1.jpg", "image2.jpg", "image3.jpg")
        )
        
        val taskMock: Task<Void> = mock()
        `when`(documentReference.set(any())).thenReturn(taskMock)
        `when`(taskMock.isSuccessful).thenReturn(true)
        
        productRepository.updateProduct(product)
        advanceUntilIdle()
        
        verify(productsCollection).document(product.id)
        verify(documentReference).set(product)
    }
    
    @Test
    fun `test delete product`() = testScope.runTest {
        val productId = "test_id"
        
        val taskMock: Task<Void> = mock()
        `when`(documentReference.delete()).thenReturn(taskMock)
        `when`(taskMock.isSuccessful).thenReturn(true)
        
        productRepository.deleteProduct(productId)
        advanceUntilIdle()
        
        verify(productsCollection).document(productId)
        verify(documentReference).delete()
    }
} 