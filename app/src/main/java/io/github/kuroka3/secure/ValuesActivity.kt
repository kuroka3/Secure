package io.github.kuroka3.secure

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.kuroka3.secure.api.CryptionManager.decryptAES256
import io.github.kuroka3.secure.api.DataManager
import java.util.concurrent.Executor

class ValuesActivity : AppCompatActivity() {

    private lateinit var origin: LinearLayout
    private lateinit var addValue: FloatingActionButton
    private val showValues: MutableList<Boolean> = mutableListOf()
    private var isEdit: Boolean = false

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private var values: List<Pair<String, String>>? = null

    private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), ActivityResultCallback {
        if (it.resultCode != RESULT_OK) return@ActivityResultCallback
        refresh(true)
    })

    private fun runAfterAuth(activity: androidx.fragment.app.FragmentActivity, run: BiometricPrompt.AuthenticationCallback) {
        executor = ContextCompat.getMainExecutor(applicationContext)
        biometricPrompt = BiometricPrompt(activity, executor, run)

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("생체 정보로 인증해주세요")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_values)

        origin = findViewById(R.id.originLayout)
        addValue = findViewById(R.id.add_value)

        refresh()

        addValue.setOnClickListener {
            val intent = Intent(applicationContext, AddActivity::class.java)
            activityResultLauncher.launch(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)

        menu?.findItem(R.id.action_edit)?.icon?.let {
            val drawable = DrawableCompat.wrap(it)
            DrawableCompat.setTint(drawable, getColor(R.color.white))
            menu.findItem(R.id.action_edit).icon = drawable
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_edit -> edit()
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun edit(): Boolean {
        isEdit = !isEdit
        showValues.fill(false)
        refresh()
        return true
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh(reload: Boolean = false) {
        if (reload || values == null) {
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

                    values = DataManager.values.map { it.first to try { it.second.decryptAES256() } catch (e: Exception) { e.stackTraceToString() } }
                    review()
                }
            })
        } else {
            review()
        }
    }

    private fun review() {
        origin.removeAllViewsInLayout()

        if (values == null) { refresh(true); return }

        for ((id, pair) in values!!.withIndex()) {
            showValues.add(false)

            origin.addView(
                LinearLayout(applicationContext).also { innerLayout ->
                    innerLayout.orientation = LinearLayout.HORIZONTAL
                    innerLayout.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.setMargins(dipToPixels(applicationContext, 10f).toInt(), 0, dipToPixels(applicationContext, 10f).toInt(), 0) }

                    innerLayout.addView(LinearLayout(applicationContext).also { doubleInnerLayout ->
                        doubleInnerLayout.orientation = LinearLayout.VERTICAL
                        doubleInnerLayout.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                        ).also { it.weight = 1f }
                        doubleInnerLayout.isClickable = true
                        doubleInnerLayout.setOnClickListener {
                            if (!isEdit) {
                                showValues[id] = !showValues[id]
                                refresh()
                            } else {
                                remove(id)
                            }
                        }

                        doubleInnerLayout.addView(TextView(applicationContext).also { name ->
                            name.text = pair.first
                            name.textSize = 30f
                            name.layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            name.paintFlags = name.paintFlags or Paint.FAKE_BOLD_TEXT_FLAG
                        })

                        doubleInnerLayout.addView(TextView(applicationContext).also { value ->
                            value.text = if (!isEdit && showValues[id]) pair.second else "********"
                            value.textSize = 17f
                            value.layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            value.setTextColor(getColor(R.color.reverse_background))
                        })
                    })

                    innerLayout.addView(Button(applicationContext).also { copy ->
                        copy.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                        ).also { it.gravity = Gravity.CENTER; it.weight = 0f }
                        copy.text = getString(R.string.copy)
                        copy.visibility = if (isEdit) LinearLayout.GONE else LinearLayout.VISIBLE
                        copy.setOnClickListener {
                            copy(pair.second)
                        }
                    })

                    innerLayout.addView(ImageButton(applicationContext).also { upper ->
                        upper.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                        ).also { it.gravity = Gravity.CENTER; it.weight = 0f }
                        upper.setImageResource(R.drawable.baseline_keyboard_double_arrow_up_24)
                        upper.setColorFilter(getColor(R.color.reverse_background))
                        upper.visibility = if (!isEdit) LinearLayout.GONE else LinearLayout.VISIBLE
                        upper.setOnClickListener { go(id, false) }
                    })

                    innerLayout.addView(ImageButton(applicationContext).also { downer ->
                        downer.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                        ).also { it.gravity = Gravity.CENTER; it.weight = 0f }
                        downer.setImageResource(R.drawable.baseline_keyboard_double_arrow_down_24)
                        downer.setColorFilter(getColor(R.color.reverse_background))
                        downer.visibility = if (!isEdit) LinearLayout.GONE else LinearLayout.VISIBLE
                        downer.setOnClickListener { go(id, true) }
                    })
                })

        }
    }

    private fun remove(id: Int) {
        AlertDialog.Builder(this)
            .setTitle("삭제")
            .setMessage("정말로 삭제하시겠습니까? id: $id")
            .setIcon(R.drawable.baseline_delete_forever_24)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                DataManager.remove(id)
                values = values?.filterIndexed { index, _ -> index != id }
                refresh()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun go(id: Int, goUp: Boolean) {
        val goto = if (goUp) { if (id == values?.size?.minus(1)) id else id+1 } else { if (id == 0) 0 else id-1 }
        DataManager.move(id, goto)
        values = values?.move(id, goto)
        refresh()
    }

    private fun dipToPixels(context: Context, dipValue: Float): Float {
        val metrics = context.resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics)
    }

    private fun copy(str: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("value", str)
        clipboardManager.setPrimaryClip(clipData)
    }

    private fun <T> List<T>.move(fromIndex: Int, toIndex: Int): List<T> {
        if(fromIndex !in this.indices || toIndex !in this.indices) throw Exception("Invalid index")
        val element = this[fromIndex]
        val mutableList = this.toMutableList()
        mutableList.removeAt(fromIndex)
        mutableList.add(toIndex, element)
        return mutableList.toList()
    }
}