package io.github.sof3.andtransfer

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder

class ShareActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val domain = prefs.getString(
            getString(R.string.pref_domain_key),
            getString(R.string.pref_domain_default)
        )!!
        val auth = if (prefs.getBoolean(getString(R.string.uses_basic_auth_key), false)) {
            HttpAuthCredentials(
                prefs.getString(getString(R.string.basic_auth_username_key), "")!!,
                prefs.getString(getString(R.string.basic_auth_password_key), "")!!
            )
        } else {
            null
        }

        val uploadables = getUploadablesFromIntent() ?: return

        for (u in uploadables) {
            val request = MultipartUploadRequest(this, String.format("%s/%s", domain, u.name))
            u.apply(request)
        }
    }

    private fun getUploadablesFromIntent(): List<Uploadable>? {
        val uploadables = mutableListOf<Uploadable>()

        try {
            when (intent?.action ?: throw BadIntentException) {
                Intent.ACTION_SEND -> {
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                    val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    when {
                        text != null -> {
                            uploadables.add(TextUploadable(text, 0))
                        }
                        uri != null -> {
                            uploadables.add(UriUploadable(uri))
                        }
                        else -> {
                            throw BadIntentException
                        }
                    }
                }
                Intent.ACTION_SEND_MULTIPLE -> {
                    val texts = intent.getStringArrayListExtra(Intent.EXTRA_TEXT)
                    val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                    when {
                        texts != null -> {
                            uploadables.addAll(texts.mapIndexed { ord, text ->
                                TextUploadable(text, ord)
                            })
                        }
                        uris != null -> {
                            uploadables.addAll(uris.map { UriUploadable(it) })
                        }
                        else -> {
                            throw BadIntentException
                        }
                    }
                }
                Intent.ACTION_VIEW -> {
                    val data = intent.data ?: throw BadIntentException
                    uploadables.add(UriUploadable(data))
                }
            }
            if (uploadables.isEmpty()) {
                throw BadIntentException
            }

        } catch (e: BadIntentException) {
            AlertDialog.Builder(this).apply {
                setTitle(R.string.error)
                setMessage(R.string.malformed_intent)
                setPositiveButton(android.R.string.ok) { _, _ -> finish() }
                show()
            }
            return null
        }

        return uploadables
    }
}

private interface Uploadable {
    val name: String

    fun apply(request: MultipartUploadRequest)
}

private class TextUploadable(private val text: String, private val ord: Int) : Uploadable {
    override val name: String
        get() = String.format("%d.txt", ord)

    override fun apply(request: MultipartUploadRequest) {
        request.add
        return SimpleMultiPartRequest(
            Request.Method.POST,
            String.format("%s/%s", domain, name), {
            }, {
            }
        ).apply {
            addMultipartParam(name, "text/plain", name)
        }

    }
}

private class UriUploadable(private val uri: Uri) : Uploadable {
    init {
        if (uri.lastPathSegment == null) {
            throw BadIntentException
        }
    }

    override val name: String
        get() = uri.lastPathSegment!! // asserted nonempty

    override fun toRequest(domain: String, auth: HttpAuthCredentials?): SimpleMultiPartRequest {
        return SimpleMultiPartRequest(
            Request.Method.POST,
            String.format("%s/%s", domain, name), {

            }, {

            }
        ).apply {
            addFile(name, uri.toFile().absolutePath)
        }
    }
}

private object BadIntentException : Exception("Malformed intent")

private data class HttpAuthCredentials(
    val username: String,
    val password: String
)

private class MultiUploadTask : AsyncTask<List<Uploadable>, List<Float>, List<String>>() {
    override fun doInBackground(vararg params: List<Uploadable>): List<String> {
        HttpClientBuilder.create().apply {
        }
    }
}