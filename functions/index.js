const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendCustomNotification = functions.https.onCall(async (data, context) => {
  const { userId, title, body, dataPayload } = data;

  if (!userId || !title || !body) {
    throw new functions.https.HttpsError('invalid-argument', 'Missing fields');
  }

  const tokenSnapshot = await admin.database()
    .ref(`/users/${userId}/fcmToken`)
    .once('value');

  const token = tokenSnapshot.val();
  if (!token) {
    return { success: false, error: "No token found" };
  }

  const message = {
    token: token,
    notification: { title, body },
    data: dataPayload || {},
    android: { priority: "high" },
    apns: { payload: { aps: { sound: "default" } } }
  };

  try {
    await admin.messaging().send(message);
    return { success: true };
  } catch (error) {
    console.error("Error:", error);
    return { success: false, error: error.message };
  }
});