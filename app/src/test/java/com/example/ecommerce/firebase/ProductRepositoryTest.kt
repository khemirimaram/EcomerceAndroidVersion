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
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        `when`(firestore.collection("products")).thenReturn(productsCollection)
        `when`(productsCollection.document(any())).thenReturn(documentReference)
        
        productRepository = ProductRepository(firestore)
    }
    
    @Test
    fun `test add product`() = runTest {
        val product = Product(
            id = "test_id",
            title = "Test Product",
            description = "Test Description",
            price = 99.99,
            images = listOf("image1.jpg", "image2.jpg")
        )
        
        val taskMock: Task<Void> = mock()
        `when`(documentReference.set(any())).thenReturn(taskMock)
        `when`(taskMock.isSuccessful).thenReturn(true)
        
        productRepository.addProduct(product)
        
        verify(productsCollection).document(product.id)
        verify(documentReference).set(product)
    }
    
    @Test
    fun `test get product`() = runTest {
        val productId = "test_id"
        val product = Product(
            id = productId,
            title = "Test Product",
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
        
        verify(productsCollection).document(productId)
        verify(documentReference).get()
    }
    
    @Test
    fun `test get all products`() = runTest {
        val products = listOf(
            Product(id = "1", title = "Product 1", images = listOf("image1.jpg")),
            Product(id = "2", title = "Product 2", images = listOf("image2.jpg"))
        )
        
        val taskMock: Task<QuerySnapshot> = mock()
        `when`(productsCollection.get()).thenReturn(taskMock)
        `when`(taskMock.isSuccessful).thenReturn(true)
        `when`(taskMock.result).thenReturn(querySnapshot)
        `when`(querySnapshot.toObjects(Product::class.java)).thenReturn(products)
        
        productRepository.getAllProducts()
        
        verify(productsCollection).get()
    }
    
    @Test
    fun `test update product`() = runTest {
        val product = Product(
            id = "test_id",
            title = "Updated Product",
            description = "Updated Description",
            price = 149.99,
            images = listOf("image1.jpg", "image2.jpg", "image3.jpg")
        )
        
        val taskMock: Task<Void> = mock()
        `when`(documentReference.set(any())).thenReturn(taskMock)
        `when`(taskMock.isSuccessful).thenReturn(true)
        
        productRepository.updateProduct(product)
        
        verify(productsCollection).document(product.id)
        verify(documentReference).set(product)
    }
    
    @Test
    fun `test delete product`() = runTest {
        val productId = "test_id"
        
        val taskMock: Task<Void> = mock()
        `when`(documentReference.delete()).thenReturn(taskMock)
        `when`(taskMock.isSuccessful).thenReturn(true)
        
        productRepository.deleteProduct(productId)
        
        verify(productsCollection).document(productId)
        verify(documentReference).delete()
    }
} 