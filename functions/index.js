const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

exports.test = functions.https.onRequest((req, res) => {
    res.json({ 
        success: true, 
        message: 'Firebase Functions funcionando!',
        timestamp: new Date().toISOString()
    });
});

exports.sendNotification = functions.https.onRequest(async (req, res) => {
    res.set('Access-Control-Allow-Origin', '*');
    res.set('Access-Control-Allow-Methods', 'POST');
    res.set('Access-Control-Allow-Headers', 'Content-Type');
    
    if (req.method === 'OPTIONS') {
        res.status(204).send('');
        return;
    }

    try {
        const { playerId, type, title, body } = req.body;
        
        await admin.database().ref(`notificationQueue/${playerId}`).push({
            type: type || 'general',
            title: title || 'Notificación',
            body: body || '',
            message: body || '',
            timestamp: Date.now(),
            processed: false
        });

        res.json({ 
            success: true, 
            message: 'Notificación guardada en Firebase' 
        });
        
    } catch (error) {
        console.error('Error:', error);
        res.status(500).json({ 
            success: false, 
            error: error.message 
        });
    }
});