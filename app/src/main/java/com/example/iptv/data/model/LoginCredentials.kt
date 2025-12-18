package com.example.iptv.data.model

data class LoginCredentials(
    val host: String,
    val port: String,
    val username: String,
    val password: String
) {
    fun getBaseUrl(): String {
        return "http://$host:$port"
    }
    
    fun isValid(): Boolean {
        return host.isNotBlank() && port.isNotBlank() && 
               username.isNotBlank() && password.isNotBlank()
    }
}