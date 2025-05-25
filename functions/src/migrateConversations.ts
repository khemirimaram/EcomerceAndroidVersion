import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

export const migrateConversations = functions.https.onRequest(async (req, res) => {
    try {
        const db = admin.firestore();
        const conversationsRef = db.collection('conversations');
        const snapshot = await conversationsRef.get();
        
        const batch = db.batch();
        let count = 0;
        
        for (const doc of snapshot.docs) {
            const conversation = doc.data();
            
            // Create new conversation structure
            const newData = {
                participants: conversation.participants || [],
                lastMessage: conversation.lastMessage ? {
                    content: conversation.lastMessage,
                    timestamp: conversation.lastMessageTimestamp?.toMillis() || Date.now(),
                    senderId: conversation.lastMessageSenderId || '',
                    type: 'text'
                } : null,
                lastMessageTimestamp: conversation.lastMessageTimestamp?.toMillis() || Date.now(),
                unreadCount: conversation.unreadCount || {},
                productId: conversation.productId || null,
                productInfo: conversation.productId ? {
                    name: conversation.productName || '',
                    price: 0.0,
                    imageUrl: '',
                    status: 'active'
                } : null,
                status: 'active',
                updatedAt: conversation.lastMessageTimestamp || admin.firestore.FieldValue.serverTimestamp(),
                createdAt: conversation.createdAt || admin.firestore.FieldValue.serverTimestamp()
            };
            
            // Update document
            batch.update(doc.ref, newData);
            count++;
            
            // Commit batch every 500 documents
            if (count % 500 === 0) {
                await batch.commit();
                batch = db.batch();
            }
        }
        
        // Commit remaining documents
        if (count % 500 !== 0) {
            await batch.commit();
        }
        
        res.json({ success: true, migratedCount: count });
    } catch (error) {
        console.error('Migration error:', error);
        res.status(500).json({ error: error.message });
    }
}); 