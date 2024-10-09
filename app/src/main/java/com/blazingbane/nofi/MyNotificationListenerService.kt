package com.blazingbane.nofi
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
class MyNotificationListenerService:NotificationListenerService() {

    private fun sendPostRequest(title:CharSequence?,text:CharSequence?) {
        // Use CoroutineScope to launch a coroutine
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject()
                if (title == "Comment") {
                    val un = text.toString().substringBefore("commented")
                    json.put("platform", "instagram")
                    json.put("userName", un)
                    json.put("action", "commented")
                } else if (text?.contains("liked your") == true) {
                    json.put("platform", "instagram")
                    json.put("userName", title)
                    json.put("action", "liked")
                }
                // Get the saved URL from SharedPreferences
                val savedUrl = getUrlFromPreferences()

                // Check if the URL is not null or empty
                if (savedUrl.isNullOrEmpty()) {
                    Log.e("NotificationListener", "Saved URL is null or empty")
                    // Show a Toast on the main thread
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Saved URL is null or empty", Toast.LENGTH_SHORT).show()
                    }
                    return@launch // Exit if the URL is not valid
                }

                // Send POST request
                val url = URL(savedUrl)
                // Send POST request
//                val url = URL("http://10.0.0.128:3000/insta")
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "POST"
                urlConnection.setRequestProperty("Content-Type", "application/json; utf-8")
                urlConnection.doOutput = true

                OutputStreamWriter(urlConnection.outputStream).use { writer ->
                    writer.write(json.toString())
                }

                val responseCode = urlConnection.responseCode
                Log.d("NotificationListener", "Response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = urlConnection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("NotificationListener", "Response: $response")
                } else {
                    val errorResponse = urlConnection.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.d("NotificationListener", "Error response: $errorResponse")
                }



            } catch (e: Exception) {
                Log.e("NotificationListener", "Error: ${e.message}")
            }
        }
    }
    private fun getUrlFromPreferences(): String? {
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return sharedPreferences.getString("notification_url", null)
    }
    override fun onCreate() {
        super.onCreate()
        Log.d("NotificationListener", "Service created")
    }
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            if (it.packageName == "com.instagram.android") {
                val notification = it.notification
                val extras = notification.extras
                val text = extras.getCharSequence("android.text")
                val title = extras.getCharSequence("android.title")
                val selfDisplayName = extras.getCharSequence("android.selfDisplayName")
                val subText = extras.getCharSequence("android.subText")

                Log.d("NotificationListener","android.text: $text")
                Log.d("NotificationListener","android.title: $title")
                Log.d("NotificationListener","android.selfDisplayName: $selfDisplayName")
                Log.d("NotificationListener","android.subText: $subText")

                if(text?.isNotEmpty() == true) {
                    sendPostRequest(title, text)
                }
                // Remove the notification
                cancelNotification(sbn.key)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn?.let {
            Log.d("NotificationListener", "Notification removed: ${it.packageName}")
        }
    }
}