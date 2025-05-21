
package com.maroco.demo_plm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ai.onnxruntime.OrtEnvironment

// Log Tag: SMSClassifier

class SplashActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView

    companion object {
        var inboxMessages: List<Pair<String, Boolean>> = listOf()
        var spamMessages: List<Pair<String, Boolean>> = listOf()
        private const val SMS_PERMISSION_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)
        progressText.text = "SMS 분류 중입니다..."

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_SMS),
                SMS_PERMISSION_REQUEST
            )
        } else {
            classifySmsInBackground()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_REQUEST && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            classifySmsInBackground()
        }
    }

    private fun classifySmsInBackground() {
        Thread {
            val ortEnv = OrtEnvironment.getEnvironment()
            val sessionKoBert = ortEnv.createSession(assets.open("kobert_spam.onnx").readBytes())
            val sessionKoElectra = ortEnv.createSession(assets.open("koelectra_spam.onnx").readBytes())
            val sessionKoRoberta  = ortEnv.createSession(assets.open("koroberta_spam.onnx").readBytes())

            Log.d("SMSClassifier", "모델 및 토크나이저 초기화 완료")

            val tokenizerBert = Tokenizer(this, "vocab_kobert.txt", "tokenizer_config_kobert.json", subwordPrefix = "##")
            val tokenizerElectra = Tokenizer(this, "vocab_koelectra.txt", "tokenizer_config_koelectra.json", subwordPrefix = "##")
            val tokenizerRoberta = Tokenizer(this, "vocab_koroberta.txt", "tokenizer_config_koroberta.json", subwordPrefix="Ġ")


            val classifierBert = ModelClassifier(ortEnv, tokenizerBert, sessionKoBert)
            val classifierElectra = ModelClassifier(ortEnv, tokenizerElectra, sessionKoElectra)
            val classifierRoberta = ModelClassifier(ortEnv, tokenizerRoberta, sessionKoRoberta)

            val inboxList = mutableListOf<Pair<String, Boolean>>()
            val spamList = mutableListOf<Pair<String, Boolean>>()

            val cursor = contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.BODY),
                null, null, null
            )

            val total = cursor?.count ?: 0
            Log.d("SMSClassifier", "총 ${total}개의 메시지 분류 시작")

            var index = 0
            cursor?.use {
                val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                while (it.moveToNext()) {
                    val text = it.getString(bodyIndex)
                    Log.d("SMSClassifier", "SMS[$index]: $text")

                    val (spam1, _) = classifierBert.classify(text)
                    val (spam2, _) = classifierElectra.classify(text)
                    val (spam3, _) = classifierRoberta.classify(text)

                    Log.d("ModelBERT", "Spam: $spam1")
                    Log.d("ModelELECTRA", "Spam: $spam2")
                    Log.d("ModelROBERTA", "Spam: $spam3")

                    val avgspam = (spam1 + spam2 + spam3) / 3
                    val isSpam = avgspam >= 60.0f
                    if (isSpam) {
                        spamList.add(Pair(text, true))
                        Log.d("SMSClassifier", "→ 이 메시지는 스팸으로 분류됨")
                    } else {
                        inboxList.add(Pair(text, false))
                        Log.d("SMSClassifier", "→ 이 메시지는 인박스로 분류됨")
                    }

                    index++
                }
            }

            inboxMessages = inboxList
            spamMessages = spamList

            Log.d("SMSClassifier", "SMS 분류 완료. Spam: ${spamList.size}, Inbox: ${inboxList.size}")

            Handler(Looper.getMainLooper()).post {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }.start()
    }
}
