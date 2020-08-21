package com.chohtet.mydevicelocation

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceService : BroadcastReceiver() {
    private val TAG = "MYGEO"
    private val GEO_NOTI_ID = 777
    private val NOTI_CHANNEL_ID = "notification_channel_id"
    override fun onReceive(context: Context?, intent: Intent?) {
        //retrieve geo fencing intent
        val geoEvent = GeofencingEvent.fromIntent(intent)
        //handle error
        if (geoEvent.hasError()) {
            Log.e(TAG, "Geo Event Error: ${geoEvent.errorCode}")
            return
        }
        //retrieve geo transition
        val transition = geoEvent.geofenceTransition
        //check transition type
        if (Geofence.GEOFENCE_TRANSITION_ENTER == transition || Geofence.GEOFENCE_TRANSITION_EXIT == transition) {
            //get geo that was triggered
            val triggers = geoEvent.triggeringGeofences
            //create detail message with geo received
            val transitionDetail = getGeoTransitionDetail(transition,triggers)
            sendNotification(context!!,transitionDetail)
        }

    }

    private fun getGeoTransitionDetail(geoTransition: Int, triggerGeo: List<Geofence>): String {
        //get id of each geo trigger
        val triggerGeoList = arrayListOf<String>()
        triggerGeo.forEach {
            triggerGeoList.add(it.requestId)
        }
        var result = when (geoTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "Entering ..."
            Geofence.GEOFENCE_TRANSITION_EXIT -> "Exiting ...."
            else -> "No transition"
        }
        return result.plus(TextUtils.join(",", triggerGeoList))
    }

    private fun sendNotification(context: Context, message: String) {
        Log.d(TAG,"Send Notification")
        val notificationIntent = GeofencingActivity.newIntent(context.applicationContext, message)
        val taskBuilder = TaskStackBuilder.create(context.applicationContext)
        taskBuilder.addParentStack(GeofencingActivity::class.java)
        taskBuilder.addNextIntent(notificationIntent)
        val notiPendingIntent = taskBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        //create and send notification
       createNotification(context,message,notiPendingIntent)
    }

    private fun createNotification(context: Context, msg: String, pendingIntent: PendingIntent) {
        val notiManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTI_CHANNEL_ID,
                "My Device Location",
                NotificationManager.IMPORTANCE_HIGH
            )
            notiManager.createNotificationChannel(channel)
        }
        val notificationBuilder =
            NotificationCompat.Builder(context.applicationContext, NOTI_CHANNEL_ID)
        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher_round)
            .setColor(Color.GREEN)
            .setContentTitle("My Device Location")
            .setContentText(msg)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)

        notiManager.notify(GEO_NOTI_ID,notificationBuilder.build())

    }
}