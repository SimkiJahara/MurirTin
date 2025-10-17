
package com.example.muritin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object GlobalSearchState {
    var origin by mutableStateOf("")
    var destination by mutableStateOf("")
    var stop by mutableStateOf("")

    fun clearAll() {
        origin = ""
        destination = ""
        stop = ""
    }
}