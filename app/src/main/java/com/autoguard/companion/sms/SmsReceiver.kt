package com.autoguard.companion.sms

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import com.autoguard.companion.data.AppDatabase
import com.autoguard.companion.data.entity.AlertEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("SmsReceiver", "onReceive triggered with action: $action")

        // 1. Check RECEIVE_SMS permission
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        Log.d("SmsReceiver", "RECEIVE_SMS permission granted: $hasPermission")

        if (action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            Log.d("SmsReceiver", "Number of messages in intent: ${messages?.size ?: 0}")

            for (sms in messages) {
                val sender = sms.displayOriginatingAddress
                val messageBody = sms.displayMessageBody
                
                Log.d("SmsReceiver", "Incoming SMS | From: $sender | Body: $messageBody")
                
                CoroutineScope(Dispatchers.IO).launch {
                    processMessage(context, sender, messageBody)
                }
            }
        }
    }

    private suspend fun processMessage(context: Context, sender: String?, message: String) {
        Log.d("SmsReceiver", "Processing message from: $sender")
        val database = AppDatabase.getDatabase(context)
        val profileDao = database.profileDao()
        val profile = profileDao.getProfileSync()

        // If a bike GSM number is set, only process messages from that number
        if (profile != null && profile.bikeGsmNumber.isNotBlank()) {
            val cleanSender = sender?.replace(Regex("[^0-9+]"), "") ?: ""
            val cleanTarget = profile.bikeGsmNumber.replace(Regex("[^0-9+]"), "")
            
            Log.d("SmsReceiver", "Filtering | Clean Sender: $cleanSender | Clean Target: $cleanTarget")
            
            if (!cleanSender.endsWith(cleanTarget) && !cleanTarget.endsWith(cleanSender)) {
                Log.d("SmsReceiver", "SMS ignored. Sender does not match registered bike number ($cleanTarget).")
                return
            }
            Log.d("SmsReceiver", "Sender matched registered bike number.")
        } else {
            Log.d("SmsReceiver", "No bike GSM number registered in profile. Skipping sender filter.")
        }

        val parsedSms = SmsParser.parseMessage(message)
        if (parsedSms != null) {
            Log.d("SmsReceiver", "Successfully parsed SMS: $parsedSms")
            
            // For accidents, ignore minor alerts
            if (parsedSms.type == "ACCIDENT" && parsedSms.severity == "MINOR") {
                Log.d("SmsReceiver", "Ignored MINOR accident alert.")
                return
            }

            val alertEntity = AlertEntity(
                type = parsedSms.type,
                severity = parsedSms.severity,
                speed = parsedSms.speed,
                latitude = parsedSms.lat,
                longitude = parsedSms.lon,
                timestamp = System.currentTimeMillis(),
                rawMessage = message
            )
            
            val rowId = database.alertDao().insertAlert(alertEntity)
            Log.d("SmsReceiver", "Saved alert to DB with rowId: $rowId")
        } else {
            Log.e("SmsReceiver", "Failed to parse SMS message body: $message")
        }
    }
}
