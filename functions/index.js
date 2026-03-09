const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

// Sends notification for new messages
exports.onNewMessage = functions.firestore
    .document('chat_rooms/{roomId}/messages/{messageId}')
    .onCreate(async (snapshot, context) => {
        const message = snapshot.data();
        const roomId = context.params.roomId;

        const roomSnap = await admin.firestore().collection('chat_rooms').doc(roomId).get();
        const participants = roomSnap.data().participantIds;

        const senderSnap = await admin.firestore().collection('users').doc(message.senderId).get();
        const senderName = senderSnap.data().displayName || "New Message";

        const promises = participants
            .filter(uid => uid !== message.senderId)
            .map(async (uid) => {
                const userSnap = await admin.firestore().collection('users').doc(uid).get();
                const token = userSnap.data().fcmToken;
                if (token) {
                    return admin.messaging().send({
                        token: token,
                        data: {
                            type: 'MESSAGE',
                            roomId: roomId,
                            senderName: senderName,
                            message: message.type === 'TEXT' ? message.text : "Sent an attachment"
                        }
                    });
                }
            });
        return Promise.all(promises);
    });

// Sends full-screen invitation for calls
exports.onCallStarted = functions.firestore
    .document('calls/{callId}')
    .onCreate(async (snapshot) => {
        const callData = snapshot.data();
        const receiverSnap = await admin.firestore().collection('users').doc(callData.receiverId).get();
        const token = receiverSnap.data().fcmToken;

        if (token) {
            return admin.messaging().send({
                token: token,
                data: {
                    type: 'CALL',
                    roomId: callData.roomId,
                    senderName: callData.callerName,
                    senderImage: callData.callerImage || "",
                    isVideo: String(callData.isVideo)
                }
            });
        }
    });