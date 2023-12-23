package io.github.kuroka3.secure.api

import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.spec.IvParameterSpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalStdlibApi::class, ExperimentalEncodingApi::class)
object CryptionManager {

    fun String.encryptAES256(): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.ENCRYPT_MODE, KeyManager.secretKey)
        val encrypted = cipher.doFinal(this.toByteArray(Charsets.UTF_8))
        return Base64.encode(encrypted) + "?iv\\" + Base64.encode(cipher.iv)
    }

    @Throws(BadPaddingException::class, IllegalBlockSizeException::class)
    fun String.decryptAES256(): String {
        val sets = this.split("?iv\\")

        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.DECRYPT_MODE, KeyManager.secretKey, IvParameterSpec(Base64.decode(sets[1])))
        val decrypted = cipher.doFinal(Base64.decode(sets[0]))
        return String(decrypted, Charsets.UTF_8)
    }
}