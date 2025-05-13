package com.maroco.demo_plm

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import ai.onnxruntime.OrtEnvironment

class SplashActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar

    companion object {
        var inboxMessages: List<String> = listOf()
        var spamMessages: List<String> = listOf()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        progressBar = findViewById(R.id.progressBar)

        // 백그라운드에서 SMS 자동 분류 작업 실행
        Thread {
            val ortEnv = OrtEnvironment.getEnvironment()
            val sessionKoBert = ortEnv.createSession(assets.open("kobert_spam.onnx").readBytes())
            val sessionKoElectra = ortEnv.createSession(assets.open("koelectra_spam.onnx").readBytes())

            val tokenizerBert = Tokenizer(this, "vocab_kobert.txt", "tokenizer_config_kobert.json")
            val tokenizerElectra = Tokenizer(this, "vocab_koelectra.txt", "tokenizer_config_koelectra.json")

            val classifierBert = ModelClassifier(ortEnv, tokenizerBert, sessionKoBert)
            val classifierElectra = ModelClassifier(ortEnv, tokenizerElectra, sessionKoElectra)

            val inboxList = mutableListOf<String>()
            val spamList = mutableListOf<String>()

            val cursor = contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.BODY),
                null, null, null
            )

            cursor?.use {
                val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                while (it.moveToNext()) {
                    val text = it.getString(bodyIndex)
                    val (spam1, _) = classifierBert.classify(text)
                    val (spam2, _) = classifierElectra.classify(text)

                    val isSpam = spam1 >= 60f && spam2 >= 60f
                    if (isSpam) spamList.add(text) else inboxList.add(text)
                }
            }

            inboxMessages = inboxList
            spamMessages = spamList

            Handler(Looper.getMainLooper()).post {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }

        }.start()
    }
}