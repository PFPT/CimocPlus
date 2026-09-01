package com.haleydu.cimoc.core

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import com.haleydu.cimoc.App
import com.haleydu.cimoc.R
import com.king.app.dialog.AppDialog
import com.king.app.updater.AppUpdater
import com.king.app.updater.http.OkHttpManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject

class Update {

    private var mAppUpdater: AppUpdater? = null

    fun startUpdate(versionName: String, content: String, mUrl: String, versionCode: Int, md5: String) {
        val view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_update, null)

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        tvTitle.setText(R.string.main_start_update)
        tvTitle.append(versionName)
        val tvContent = view.findViewById<TextView>(R.id.tvContent)
        tvContent.text = content

        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        btnCancel.setOnClickListener {
            AppDialog.dismissDialog()
        }
        val btnOK = view.findViewById<Button>(R.id.btnOK)
        btnOK.setOnClickListener {
            mAppUpdater = AppUpdater.Builder()
                .setApkMD5(md5)
                .serUrl(mUrl)
                .setVersionCode(versionCode)
                .setVibrate(true)
                .setFilename("Cimoc_$versionName.apk")
                .build(getContext())

            mAppUpdater!!.setHttpManager(OkHttpManager.getInstance()).start()
            AppDialog.dismissDialog()
        }

        AppDialog.showDialog(getContext(), view)
    }

    fun getContext(): Context = App.getActivity()

    companion object {
        private const val UPDATE_URL = "https://api.github.com/repos/Haleydu/cimoc/releases/latest"
        private const val UPDATE_URL_GITEE = "https://gitee.com/Haleydu/update/raw/master/Update.json"
        private const val SERVER_FILENAME = "tag_name"

        @JvmStatic
        fun check(client: OkHttpClient): String {
            val request = Request.Builder().url(UPDATE_URL).build()
            var response: Response? = null
            try {
                response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = response.body!!.string()
                    return JSONObject(json).getString(SERVER_FILENAME)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                response?.close()
            }
            throw RuntimeException()
        }

        @JvmStatic
        fun checkGitee(client: OkHttpClient): String {
            val request = Request.Builder().url(UPDATE_URL_GITEE).build()
            var response: Response? = null
            try {
                response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    return response.body!!.string()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                response?.close()
            }
            throw RuntimeException()
        }
    }
}
