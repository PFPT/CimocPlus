package com.haleydu.cimoc.misc

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.util.LinkedList

class ActivityLifecycle : Application.ActivityLifecycleCallbacks {

    private val mActivityList: MutableList<Activity> = LinkedList()

    fun clear() {
        for (activity in mActivityList) {
            activity.finish()
        }
        mActivityList.clear()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        mActivityList.add(activity)
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
        mActivityList.remove(activity)
    }
}
