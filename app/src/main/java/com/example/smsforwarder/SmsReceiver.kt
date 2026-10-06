package com.example.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class SmsReceiver : BroadcastReceiver() {

    private val client = OkHttpClient()
    private val backendUrl = "https://script.google.com/macros/s/AKfycbwi8twf_hSQUqbSLX0A6SX_m-I-7xnAuC3-l_Kfa0g2tpq7Lwt3wLSGnYGJa71cFHUO/exec"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val sender = sms.displayOriginatingAddress ?: "Unknown"
                val messageBody = sms.messageBody ?: ""
                val timestamp = sms.timestampMillis

                Log.d("SmsReceiver", "SMS received from $sender: $messageBody")
                sendApiRequest(sender, messageBody, timestamp)
            }
        }
    }

    private fun sendApiRequest(sender: String, message: String, timestamp: Long) {
        val jsonPayload = JSONObject().apply {
            put("sender", sender)
            put("message", message)
            put("timestamp", timestamp)
            put("secret_key", "YOUR_SECRET_KEY")
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonPayload.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(backendUrl)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SmsReceiver", "Failed to forward SMS", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("SmsReceiver", "SMS forwarded successfully!")
                } else {
                    Log.e("SmsReceiver", "API Error: ${response.code}")
                }
                response.close()
            }
        })
    }
}
