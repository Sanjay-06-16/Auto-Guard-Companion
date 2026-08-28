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

    private val TAG = "AutoGuardSms"

    override fun onReceive(context: Context, intent: Intent) {
        // 1. SMS receiver triggered
        Log.d(TAG, "SMS receiver triggered")

        val action = intent.action
        if (action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) {
                Log.w(TAG, "No messages found in intent.")
                return
            }

            // 2. Incoming SMS sender: ...
            val sender = messages[0].displayOriginatingAddress
            Log.d(TAG, "Incoming SMS sender: $sender")

            // 3. Incoming SMS body: ... (Concatenate all PDUs for long messages)
            val fullBody = StringBuilder()
            for (sms in messages) {
                fullBody.append(sms.displayMessageBody)
            }
            val messageBody = fullBody.toString()
            Log.d(TAG, "Incoming SMS body: $messageBody")

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    processMessage(context, sender, messageBody)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing SMS", e)
                } finally {
                    pendingResult.finish()
                    Log.d(TAG, "Receiver async work finished.")
                }
            }
        }
    }

    private suspend fun processMessage(context: Context, sender: String?, message: String) {
        val database = AppDatabase.getDatabase(context)
        val profileDao = database.profileDao()
        val profile = profileDao.getProfileSync()

        val bikeGsm = profile?.bikeGsmNumber ?: ""
        // 4. Configured bike GSM number: ...
        Log.d(TAG, "Configured bike GSM number: $bikeGsm")

        if (bikeGsm.isNotBlank()) {
            val normalizedIncoming = normalizePhoneNumber(sender ?: "")
            val normalizedConfigured = normalizePhoneNumber(bikeGsm)

            // 5. Normalized incoming number: ...
            Log.d(TAG, "Normalized incoming number: $normalizedIncoming")
            // 6. Normalized configured number: ...
            Log.d(TAG, "Normalized configured number: $normalizedConfigured")

            // 7. Sender match: true/false (robust Indian number comparison - final 10 digits)
            val isMatch = normalizedIncoming.length >= 10 && normalizedConfigured.length >= 10 &&
                    normalizedIncoming.takeLast(10) == normalizedConfigured.takeLast(10)
            
            Log.d(TAG, "Sender match: $isMatch")

            if (!isMatch) {
                Log.d(TAG, "SMS ignored. Sender does not match registered bike number.")
                return
            }
        }

        val parsedSms = SmsParser.parseMessage(message)
        if (parsedSms != null) {
            // 8. Location SMS detected
            if (parsedSms.type == "LOCATION") {
                Log.d(TAG, "Location SMS detected")
            }

            if (parsedSms.type == "ACCIDENT" && parsedSms.severity == "MINOR") {
                Log.d(TAG, "Ignored MINOR accident alert.")
                return
            }

            // 9. Parsed latitude: ...
            Log.d(TAG, "Parsed latitude: ${parsedSms.lat}")
            // 10. Parsed longitude: ...
            Log.d(TAG, "Parsed longitude: ${parsedSms.lon}")

            val alertEntity = AlertEntity(
                type = parsedSms.type,
                severity = if (parsedSms.type == "LOCATION") "LOCATION" else parsedSms.severity,
                speed = parsedSms.speed,
                latitude = parsedSms.lat,
                longitude = parsedSms.lon,
                timestamp = System.currentTimeMillis(),
                rawMessage = message,
                locationSource = if (parsedSms.type == "LOCATION") "BIKE_GSM_GPS" else "ESP32",
                smsStatus = "RECEIVED"
            )
            
            // 11. Saving LOCATION alert to Room
            Log.d(TAG, "Saving ${parsedSms.type} alert to Room")
            database.alertDao().insertAlert(alertEntity)
            
            // 12. LOCATION alert saved successfully
            Log.d(TAG, "${parsedSms.type} alert saved successfully")
        } else {
            Log.e(TAG, "SmsParser could not parse message: $message")
        }
    }

    private fun normalizePhoneNumber(number: String): String {
        return number.replace(Regex("[^0-9]"), "")
    }
}
