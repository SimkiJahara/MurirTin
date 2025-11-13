package com.example.muritin

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavHostController, requestId: String, user: FirebaseUser) {
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    var otherUser by remember { mutableStateOf<User?>(null) }
    var request by remember { mutableStateOf<Request?>(null) }
    var isChatEnabled by remember { mutableStateOf(false) }
    val database = FirebaseDatabase.getInstance("https://muritin-78a12-default-rtdb.asia-southeast1.firebasedatabase.app/")

    LaunchedEffect(requestId) {
        try {
            val snapshot = database.getReference("requests").child(requestId).get().await()
            request = snapshot.getValue(Request::class.java)
            if (request != null) {
                isChatEnabled = AuthRepository().isChatEnabled(requestId)
                val otherId = if (user.uid == request!!.riderId) request!!.conductorId else request!!.riderId
                otherUser = AuthRepository().getUser(otherId).getOrNull()
            }
        } catch (e: Exception) {
            Log.e("ChatScreen", "Load request/user failed: ${e.message}")
        }

        // Listen for messages
        AuthRepository().listenToMessages(requestId) { newMessages ->
            messages = newMessages
        }
    }

    if (!isChatEnabled) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Text("This chat is no longer available.", modifier = Modifier.padding(16.dp))
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Chat with ${otherUser?.name ?: "User"}") })
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(modifier = Modifier.weight(1f).padding(8.dp)) {
                items(messages) { message ->
                    val isMe = message.senderId == user.uid
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = message.text,
                                modifier = Modifier.padding(8.dp),
                                color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Type a message") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (text.isNotBlank()) {
                        scope.launch {
                            val result = AuthRepository().sendMessage(requestId, text)
                            if (result.isSuccess) {
                                text = ""
                            } else {
                                // Handle error (e.g., Toast)
                            }
                        }
                    }
                }) {
                    Text("Send")
                }
            }
        }
    }
}