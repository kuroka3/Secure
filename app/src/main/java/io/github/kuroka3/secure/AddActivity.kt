package io.github.kuroka3.secure

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import io.github.kuroka3.secure.api.CryptionManager.encryptAES256
import io.github.kuroka3.secure.api.DataManager
import java.util.concurrent.Executor

class AddActivity : AppCompatActivity() {

    private lateinit var name: EditText
    private lateinit var valueE: EditText
    private lateinit var confirm: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)

        name = findViewById(R.id.input_name)
        valueE = findViewById(R.id.input_value)
        confirm = findViewById(R.id.add_confirm)

        confirm.setOnClickListener {
            runAfterAuth(this, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)

                    Toast.makeText(
                        applicationContext,
                        "Authentication error: $errString", Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)

                    try {
                        DataManager.add(name.text.toString(), valueE.text.toString().encryptAES256())
                        setResult(RESULT_OK)
                        finish()
                    } catch (e: Exception) {
                        Toast.makeText(
                            applicationContext,
                            e.stackTraceToString(), Toast.LENGTH_LONG
                        ).show()
                    }
                }
            })
        }
    }

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private fun runAfterAuth(activity: androidx.fragment.app.FragmentActivity, run: BiometricPrompt.AuthenticationCallback) {
        executor = ContextCompat.getMainExecutor(applicationContext)
        biometricPrompt = BiometricPrompt(activity, executor, run)

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("생체 정보로 인증해주세요")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        biometricPrompt.authenticate(promptInfo)
    }
}