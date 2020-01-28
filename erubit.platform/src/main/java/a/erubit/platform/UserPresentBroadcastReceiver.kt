package a.erubit.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build


class UserPresentBroadcastReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		/*
		 * Sent when the user is present after
		 * device wakes up (e.g when the keyguard is gone)
		 */

		if (Intent.ACTION_USER_PRESENT == intent.action) {
			showFloatingView(context)
		}
	}

	private fun showFloatingView(context: Context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			context.startForegroundService(Intent(context.applicationContext, InteractionService::class.java))
		else
			context.startService(Intent(context.applicationContext, InteractionService::class.java))
	}
}
