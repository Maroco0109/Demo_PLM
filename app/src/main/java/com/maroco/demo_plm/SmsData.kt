package com.maroco.demo_plm

data class SmsData(
    val address: String,
    val body: String,
    val spamProbability: Float,
    val hamProbability: Float,
    val isSpam: Boolean
)