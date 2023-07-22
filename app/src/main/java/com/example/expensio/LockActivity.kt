import android.content.Context
import android.content.SharedPreferences
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.expensio.MainActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class LockActivity(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("AppLockPreferences", Context.MODE_PRIVATE)
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_NAME = "AppLockKey"
        private const val PREFERENCES_KEY = "app_locked"
    }

    init {
        keyStore.load(null)
    }

    fun isAppLocked(): Boolean {
        return sharedPreferences.getBoolean(PREFERENCES_KEY, false)
    }

    fun setAppLocked(isLocked: Boolean) {
        sharedPreferences.edit().putBoolean(PREFERENCES_KEY, isLocked).apply()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun showFingerprintPrompt(onSuccess: () -> Unit, onError: () -> Unit) {
        val biometricPrompt = createBiometricPrompt(onSuccess, onError)
        val promptInfo = createPromptInfo()

        biometricPrompt.authenticate(promptInfo)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun createBiometricPrompt(
        onSuccess: () -> Unit,
        onError: () -> Unit
    ): androidx.biometric.BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(context)

        return androidx.biometric.BiometricPrompt(
            context as MainActivity,
            executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: androidx.biometric.BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError()
                }
            }
        )
    }

    private fun createPromptInfo(): androidx.biometric.BiometricPrompt.PromptInfo {
        return androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("App Lock")
            .setSubtitle("Verify your fingerprint to unlock the app")
            .setNegativeButtonText("Cancel")
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun createCipher(): Cipher {
        val cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                + KeyProperties.BLOCK_MODE_CBC + "/"
                + KeyProperties.ENCRYPTION_PADDING_PKCS7)

        val key = getKey()

        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher
    }

    private fun getKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_NAME)) {
            generateKey()
        }

        val key = keyStore.getKey(KEY_NAME, null) as SecretKey
        return key
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_NAME,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setUserAuthenticationRequired(true)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .build()

        keyGenerator.generateKey()
    }

    fun encryptData(data: ByteArray): ByteArray {
        val cipher = createCipher()
        return cipher.doFinal(data)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun decryptData(encryptedData: ByteArray): ByteArray {
        val cipher = createCipher()
        return cipher.doFinal(encryptedData)
    }

    fun clearKeys() {
        keyStore.deleteEntry(KEY_NAME)
    }
}
