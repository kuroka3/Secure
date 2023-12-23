package io.github.kuroka3.secure

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import io.github.kuroka3.secure.api.DataManager
import io.github.kuroka3.secure.api.KeyManager
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    companion object {
        const val KEY_NAME = "SecureAPIKey"
    }

    private lateinit var button: Button
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            KeyManager.generateKey()
            DataManager.init(filesDir)

            button = findViewById(R.id.bioAuth)
            executor = ContextCompat.getMainExecutor(this)
            biometricPrompt = BiometricPrompt(this, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        Toast.makeText(
                            applicationContext,
                            "Authentication error: $errString", Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)

                        val intent = Intent(applicationContext, ValuesActivity::class.java)
                        startActivity(intent)
                    }
                })

            promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("생체 정보로 인증해주세요")
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                .build()

            button.setOnClickListener {
                authenticate()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        authenticate()
    }

    private fun authenticate() {
        biometricPrompt.authenticate(promptInfo)
    }
}