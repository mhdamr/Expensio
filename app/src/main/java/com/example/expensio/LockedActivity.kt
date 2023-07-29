package com.example.expensio

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

class LockedActivity : AppCompatActivity() {

    private lateinit var keyguardManager: KeyguardManager
    private lateinit var keyStore: KeyStore
    private lateinit var cipher: Cipher
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var btnReOpenFingerprint: Button

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locked)


        // Initialize views
        btnReOpenFingerprint = findViewById(R.id.btnReOpenFingerprint)

        keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager

        // Check if the device supports fingerprint authentication
        if (keyguardManager.isKeyguardSecure) {
            // Initialize the Cipher and KeyStore for fingerprint authentication
            initCipher()
            // Create BiometricPrompt instance
            biometricPrompt = BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(this),
                authenticationCallback
            )
            // Build the prompt message and display the BiometricPrompt
            promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Fingerprint Authentication")
                .setSubtitle("Please authenticate using your fingerprint.")
                .setNegativeButtonText("Cancel")
                .build()
            biometricPrompt.authenticate(promptInfo)

            // Set click listener for the "Re-open Fingerprint" button
            btnReOpenFingerprint.setOnClickListener {
                // Re-open the fingerprint prompt
                biometricPrompt.authenticate(promptInfo)
            }

        } else {
            // Device does not support fingerprint authentication or lock screen is not secure
            // Handle the situation accordingly (e.g., show a message or navigate back to LoginActivity)
            // For simplicity, we'll just finish the LockedActivity and go back to MainActivity.
            finish()
        }
    }

    // Initialize the Cipher and KeyStore for fingerprint authentication
    @RequiresApi(Build.VERSION_CODES.M)
    private fun initCipher() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            val builder = KeyGenParameterSpec.Builder(
                "biometric_key",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(30) // Adjust the duration as needed
            keyGenerator.init(builder.build())
            val secretKey = keyGenerator.generateKey()
            cipher = Cipher.getInstance(
                KeyProperties.KEY_ALGORITHM_AES + "/"
                        + KeyProperties.BLOCK_MODE_CBC + "/"
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7
            )
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // BiometricPrompt authentication callback
    private val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            // Fingerprint authentication succeeded, navigate to MainActivity and HomeFragment
            navigateToHomeFragment()
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            // Fingerprint authentication failed, you may handle it as needed (e.g., show a message)
            // In this case, since there's no fallback mechanism, let's navigate to HomeFragment as well.
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            // Handle authentication errors (e.g., no fingerprint enrolled, etc.)
            // For simplicity, we'll just navigate to HomeFragment in all cases.
        }


    }

    // Function to navigate to MainActivity and HomeFragment
    private fun navigateToHomeFragment() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("Unlocked", true)
        startActivity(intent)
        finish()
    }

}