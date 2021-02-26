package com.ujujzk.tryspeechrecognition

import android.Manifest
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ibm.cloud.sdk.core.security.Authenticator
import com.ibm.cloud.sdk.core.security.IamAuthenticator
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType
import com.ibm.watson.speech_to_text.v1.SpeechToText
import com.ibm.watson.speech_to_text.v1.model.RecognizeOptions
import com.ibm.watson.speech_to_text.v1.model.RecognizeWithWebsocketsOptions
import com.ibm.watson.speech_to_text.v1.model.SpeechRecognitionResults
import com.ibm.watson.speech_to_text.v1.websocket.RecognizeCallback
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.io.InputStream

/**
 * https://github.com/watson-developer-cloud/android-sdk/blob/609157c502b05487c1557b870d9be3faca87c21d/example/src/main/java/com/ibm/watson/developer_cloud/android/myapplication/MainActivity.java
 */
class MainActivity : AppCompatActivity() {


    private var speechService: SpeechToText? = null
    private var listening = false
    private var capture: MicrophoneInputStream? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Dexter.withContext(this)
            .withPermission(Manifest.permission.RECORD_AUDIO)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    recordMessage()

                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) { /* ... */
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest?,
                    token: PermissionToken?
                ) {
                }
            }).check()
    }


    private fun recordMessage() {
        speechService = initSpeechToTextService()
        if (listening.not()) {
            capture = MicrophoneInputStream(true)
            Thread {
                try {
                    speechService?.recognizeUsingWebSocket(
                        getRecognizeOptions(capture!!),
                        MicrophoneRecognizeDelegate()
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    showError(e)
                }
            }.start()
            listening = true
            Toast.makeText(this@MainActivity, "Listening....Click to Stop", Toast.LENGTH_LONG).show()
        } else {
            try {
                capture?.close()
                listening = false
                Toast.makeText(this@MainActivity, "Stopped Listening....Click to Start", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun initSpeechToTextService(): SpeechToText {
        return SpeechToText(IamAuthenticator(BuildConfig.WATSON_API_KEY))
            .apply { serviceUrl = BuildConfig.WATSON_SERVICE_URL }
    }

    private fun getRecognizeOptions(captureStream: InputStream): RecognizeWithWebsocketsOptions {
        return RecognizeWithWebsocketsOptions.Builder()
            .audio(captureStream)
            .contentType(ContentType.OPUS.toString())
            .model(RecognizeOptions.Model.EN_US_BROADBANDMODEL)
            .interimResults(true)
            .inactivityTimeout(-1)
            .build()
    }

    inner class MicrophoneRecognizeDelegate : RecognizeCallback {
        override fun onInactivityTimeout(runtimeException: RuntimeException?) {

        }

        override fun onListening() {

        }

        override fun onTranscriptionComplete() {

        }

        override fun onTranscription(speechResults: SpeechRecognitionResults) {
            println(speechResults)
            if (speechResults.results.isNullOrEmpty().not()) {
                val text = speechResults.results[0].alternatives[0].transcript
                showMicText(text)
            }
        }

        override fun onConnected() {}
        override fun onError(e: java.lang.Exception) {
            showError(e)
        }

        override fun onDisconnected() {
        }
    }

    private fun showMicText(text: String) {
        runOnUiThread { findViewById<TextView>(R.id.txtResult).text = text }
    }

    private fun showError(e: java.lang.Exception) {
        runOnUiThread {
            Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

}