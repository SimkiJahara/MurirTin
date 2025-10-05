package com.example.muritin

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await

class BusRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://muritin-78a12-default-rtdb.asia-southeast1.firebasedatabase.app/"),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    init {
        Log.d("BusRepository", "Initializing BusRepository")
    }

    suspend fun registerBus(bus: Bus): Result<String> {
        Log.d("BusRepository", "Attempting to register bus: ${bus.name}")
        if (auth.currentUser == null) {
            return Result.failure(Exception("No authenticated user"))
        }
        return try {
            val busId = database.getReference("buses").push().key
                ?: return Result.failure(Exception("Could not generate bus ID"))
            val busWithId = bus.copy(
                busId = busId,
                ownerUid = auth.currentUser!!.uid
            )
            database.getReference("buses").child(busId).setValue(busWithId).await()
            Log.d("BusRepository", "Bus registered successfully, busId: $busId")
            Result.success(busId)
        } catch (e: Exception) {
            Log.e("BusRepository", "Bus registration failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun uploadDocument(busId: String, type: String, uri: Uri): Result<String> {
        Log.d("BusRepository", "Uploading $type document for busId: $busId")
        return try {
            val fileExtension = when (type) {
                "fitness", "tax" -> "pdf"
                else -> "pdf"
            }
            val storageRef: StorageReference = storage.getReference("buses/$busId/${type}_doc.$fileExtension")
            storageRef.putFile(uri).await()
            val downloadUrl: String = storageRef.downloadUrl.await().toString()
            database.getReference("buses").child(busId).child("${type}Url").setValue(downloadUrl).await()
            Log.d("BusRepository", "$type document uploaded successfully: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e("BusRepository", "Document upload failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getBusesForOwner(ownerUid: String): List<Bus> {
        Log.d("BusRepository", "Fetching buses for ownerUid: $ownerUid")
        return try {
            val snapshot = database.getReference("buses")
                .orderByChild("ownerUid").equalTo(ownerUid).get().await()
            snapshot.children.mapNotNull { it.getValue(Bus::class.java) }
        } catch (e: Exception) {
            Log.e("BusRepository", "Error fetching buses: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun assignConductor(busId: String, conductorUid: String): Result<Boolean> {
        Log.d("BusRepository", "Assigning conductor $conductorUid to bus $busId")
        return try {
            val busSnapshot = database.getReference("buses").child(busId).get().await()
            val bus = busSnapshot.getValue(Bus::class.java)
                ?: return Result.failure(Exception("Bus not found"))
            if (bus.ownerUid != auth.currentUser?.uid) {
                return Result.failure(Exception("Not authorized"))
            }
            database.getReference("buses").child(busId).child("conductorUid").setValue(conductorUid).await()
            database.getReference("users").child(conductorUid).child("assignedBusId").setValue(busId).await()
            Log.d("BusRepository", "Conductor assigned successfully")
            Result.success(true)
        } catch (e: Exception) {
            Log.e("BusRepository", "Assignment failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}