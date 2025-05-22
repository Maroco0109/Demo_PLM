package com.maroco.demo_plm


import android.content.Context
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession

object ModelManager {
    lateinit var sessionKoBert: OrtSession
    lateinit var sessionKoElectra: OrtSession
    lateinit var sessionKoRoberta: OrtSession

    fun loadModels(context: Context) {
        val ortEnv = OrtEnvironment.getEnvironment()


        // assets 디렉토리에서 모델 로드
        sessionKoBert = ortEnv.createSession(context.assets.open("kobert_spam.onnx").readBytes())
        sessionKoElectra = ortEnv.createSession(context.assets.open("koelectra_spam.onnx").readBytes())
        sessionKoRoberta = ortEnv.createSession(context.assets.open("koroberta_spam.onnx").readBytes())
    }
}