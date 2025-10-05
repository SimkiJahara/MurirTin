package com.example.muritin

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class BusRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://muritin-78a12-default-rtdb.asia-southeast1.firebasedatabase.app/")
) {
    suspend fun registerBus(bus: Bus): Result<Bus> {
        Log.d("BusRepository", "Attempting to register bus: $bus")
        return try {
            val busId = database.getReference("buses").push().key ?: return Result.failure(Exception("No bus ID"))
            val busWithId = bus.copy(busId = busId)
            database.getReference("buses").child(busId).setValue(busWithId).await()
            Log.d("BusRepository", "Bus registered successfully: $busWithId")
            Result.success(busWithId)
        } catch (e: Exception) {
            Log.e("BusRepository", "Bus registration failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun assignConductor(busId: String, conductorId: String): Result<Boolean> {
        Log.d("BusRepository", "Assigning conductor $conductorId to bus $busId")
        return try {
            database.getReference("buses").child(busId).child("conductorId").setValue(conductorId).await()
            Log.d("BusRepository", "Conductor assigned successfully")
            Result.success(true)
        } catch (e: Exception) {
            Log.e("BusRepository", "Conductor assignment failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getBusesForOwner(ownerId: String): List<Bus> {
        Log.d("BusRepository", "Fetching buses for owner $ownerId")
        return try {
            val snapshot = database.getReference("buses").orderByChild("ownerId").equalTo(ownerId).get().await()
            val buses = snapshot.children.mapNotNull { it.getValue(Bus::class.java) }
            Log.d("BusRepository", "Fetched ${buses.size} buses")
            buses
        } catch (e: Exception) {
            Log.e("BusRepository", "Error fetching buses: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getConductorsForOwner(ownerId: String): List<User> {
        Log.d("BusRepository", "Fetching conductors for owner $ownerId")
        return try {
            val snapshot = database.getReference("users").orderByChild("createdBy").equalTo(ownerId).get().await()
            val conductors = snapshot.children.mapNotNull { snap ->
                snap.getValue(User::class.java)?.copy(uid = snap.key ?: "")
            }
            Log.d("BusRepository", "Fetched ${conductors.size} conductors")
            conductors
        } catch (e: Exception) {
            Log.e("BusRepository", "Error fetching conductors: ${e.message}", e)
            emptyList()
        }
    }
}