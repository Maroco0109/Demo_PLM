package com.maroco.demo_plm

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession

class ModelClassifier(
    private val ortEnv: OrtEnvironment,
    private val tokenizer: Tokenizer,
    private val session: OrtSession
) {
    fun classify(text: String): Pair<Float, Float> {
        val (inputIds, attentionMask, tokenTypeIds) = tokenizer.tokenize(text)
        val inputNames = session.inputNames.toList()

        val inputs = mutableMapOf<String, OnnxTensor>()
        inputs[inputNames[0]] = OnnxTensor.createTensor(ortEnv, arrayOf(inputIds))
        inputs[inputNames[1]] = OnnxTensor.createTensor(ortEnv, arrayOf(attentionMask))
        if (inputNames.size > 2) {
            inputs[inputNames[2]] = OnnxTensor.createTensor(ortEnv, arrayOf(tokenTypeIds))
        }

        val result = session.run(inputs)
        val logits = (result[0].value as Array<FloatArray>)[0]
        val probs = softmax(logits)
        return Pair(probs[0] * 100, probs[1] * 100)  // spam%, ham%
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.maxOrNull() ?: 0f
        val exps = logits.map { Math.exp((it - max).toDouble()) }
        val sum = exps.sum()
        return exps.map { (it / sum).toFloat() }.toFloatArray()
    }
}