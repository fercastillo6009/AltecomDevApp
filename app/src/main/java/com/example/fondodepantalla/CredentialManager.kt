package com.example.fondodepantalla

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class CredentialManager(context: Context) {

    companion object {
        private const val FILENAME = "auth_prefs"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_PASSWORD = "user_password"
    }

    // Crea una clave maestra para cifrar los datos
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    // Inicializa EncryptedSharedPreferences (cifrado AES256)
    private val sharedPreferences = EncryptedSharedPreferences.create(
        FILENAME,
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /** Guarda el correo y la contraseña cifrados. */
    fun saveCredentials(email: String, password: String) {
        sharedPreferences.edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    /** Recupera el correo y la contraseña. */
    fun getCredentials(): Pair<String?, String?> {
        val email = sharedPreferences.getString(KEY_EMAIL, null)
        val password = sharedPreferences.getString(KEY_PASSWORD, null)
        return Pair(email, password)
    }

    /** Limpia las credenciales (debe usarse al cerrar sesión). */
    fun clearCredentials() {
        sharedPreferences.edit()
            .remove(KEY_EMAIL)
            .remove(KEY_PASSWORD)
            .apply()
    }
}