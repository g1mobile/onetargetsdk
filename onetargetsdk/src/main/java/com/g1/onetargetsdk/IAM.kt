package com.g1.onetargetsdk

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.g1.onetargetsdk.db.DbUtil
import com.g1.onetargetsdk.model.*
import com.g1.onetargetsdk.ui.ActivityIAM
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


/**
 * Created by Loitp on 13.09.2022
 * Galaxy One company,
 * Vietnam
 * +840766040293
 * freuss47@gmail.com
 */
class IAM {
    companion object {
        private val logTag = "loitpp${IAM::class.java.simpleName}"
        private var configuration: Configuration? = null
        private var isAppInForeground: Boolean? = null
        private val listIAM = ArrayList<IAMData>()

        private fun logD(msg: String) {
            if (this.configuration?.isShowLog == true) {
                Log.d(logTag, msg)
            }
        }

        private fun logE(msg: String) {
            if (this.configuration?.isShowLog == true) {
                Log.e(logTag, msg)
            }
        }

        fun setup(configuration: Configuration, context: Context?): Boolean {
            if (configuration.writeKey.isNullOrEmpty()) {
                logE("writeKey cannot be null or empty")
                return false
            }
            if (configuration.getBaseUrlIAM().isEmpty()) {
                logE("base url cannot be null or empty")
                return false
            }
            this.configuration = configuration
            if (configuration.isEnableIAM) {
                ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> {
                            if (isAppInForeground == null) {
//                                logE(">>>onAppInForeground")
                                isAppInForeground = true
                                checkIAM(context)
                            }
                        }
                        Lifecycle.Event.ON_STOP -> {
                            if (isAppInForeground == null) {
//                                logE(">>>onAppInBackground")
                                isAppInForeground = false
                                checkIAM(context)
                            }
                        }
                        else -> {}
                    }
                })
            }
            return true
        }

        private fun checkIAM(context: Context?) {
            checkIAM(
                context,
                onResponse = { isSuccessful, code, response, data ->
                    logD("isSuccessful $isSuccessful")
                    logD("code $code")
                    logD("response $response")

                    data?.let { dt ->
                        if (data.message.isNullOrEmpty()) {
                            //do nothing
                        } else {
                            listIAM.add(dt)
                        }
                    }

                    val firstIAMData = listIAM.firstOrNull()
                    logD("listIAM.size ${listIAM.size}")
                    logD("firstIAMData $firstIAMData")
                    firstIAMData?.let { dt ->
                        getHtmlContent(dt)?.let { htmlContent ->
//                            logE("htmlContent $htmlContent, $isAppInForeground")
                            if (isAppInForeground == true) {
                                logD("dt.activeType ${dt.activeType}")

                                context?.let { c ->
                                    val popupAIMIsShowing =
                                        DbUtil.getBoolean(
                                            c,
                                            DbUtil.KEY_POPUP_IAM_IS_SHOWING,
                                            false
                                        )
                                    logD("popupAIMIsShowing $popupAIMIsShowing")
                                    if (popupAIMIsShowing) {
                                        logE("popupAIMIsShowing true -> return")
                                    } else {
                                        when (dt.activeType) {
                                            IMMEDIATELY -> {
                                                val intent = Intent(c, ActivityIAM::class.java)
                                                intent.putExtra(ActivityIAM.KEY_IAM_DATA, dt)
                                                intent.putExtra(
                                                    ActivityIAM.KEY_HTML_CONTENT,
                                                    htmlContent
                                                )
                                                intent.putExtra(ActivityIAM.KEY_SCREEN_WIDTH, 1.0)
                                                intent.putExtra(ActivityIAM.KEY_SCREEN_HEIGHT, 1.0)
                                                intent.putExtra(
                                                    ActivityIAM.KEY_ENABLE_TOUCH_OUTSIDE,
                                                    false
                                                )
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                c.startActivity(intent)
                                                listIAM.removeFirst()
                                            }
                                            TIME -> {
                                                // TODO do sth
                                            }
                                            SCROLL_PERCENTAGE -> {
                                                // do nothing, out of sdk's scope
                                            }
                                            else -> {
                                                //do nothing}
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    checkIAM(context)
                },
                onFailure = { t ->
                    t.printStackTrace()
                    checkIAM(context)
                },
            )
        }

        private fun getHtmlContent(data: IAMData?): String? {
            val gson = Gson()
            data?.message?.let { jsonStringMessage ->
//                        logD("jsonString: $jsonStringMessage")
                var mapJsonContent: Map<String, Any> = HashMap()
                mapJsonContent =
                    gson.fromJson(jsonStringMessage, mapJsonContent.javaClass)

                val jsonContent = mapJsonContent["jsonContent"]
//                        logD("jsonContent: $jsonContent")
//                        logD("jsonContent: " + gson.toJson(jsonContent))

                gson.toJson(jsonContent)?.let { jsonStringJsonContent ->
                    var mapMessage: Map<String, Any> = HashMap()
                    mapMessage =
                        gson.fromJson(jsonStringJsonContent, mapMessage.javaClass)

                    val message = mapMessage["message"]
//                            logD("message: $message")

                    message?.toString()?.let { jsonString ->
                        var mapHtmlContent: Map<String, Any> = HashMap()
                        mapHtmlContent =
                            gson.fromJson(jsonString, mapHtmlContent.javaClass)

                        val htmlContent = mapHtmlContent["htmlContent"]
//                        logD("htmlContent: $htmlContent")
                        return htmlContent?.toString()
                    }
                }
            }
            return null
        }

        @JvmStatic
        private fun service(): OneTargetService? {
            if (this.configuration == null) {
                logE("configuration not found")
                return null
            }
            val baseUrl = this.configuration?.getBaseUrlIAM()
            if (baseUrl.isNullOrEmpty()) {
                logE("base url cannot be null or empty")
                return null
            }
            val isShowLog = this.configuration?.isShowLog
            return RetrofitClient.getClientIAM(
                baseUrl = baseUrl,
                isShowLogAPI = isShowLog,
            ).create(OneTargetService::class.java)
        }

        private fun checkIAM(
            context: Context?,
            onResponse: ((isSuccessful: Boolean, code: Int, response: IAMResponse?, data: IAMData?) -> Unit)? = null,
            onFailure: ((Throwable) -> Unit)? = null,
        ) {

            fun isValid(): Boolean {
                if (context?.applicationContext == null) {
                    return false
                }
                if (isAppInForeground == null || isAppInForeground == false) {
                    return false
                }
                return true
            }

            val isValid = isValid()
//            logD(">>>>>>>checkIAM isValid $isValid, isAppInForeground $isAppInForeground")
            if (!isValid) {
                return
            }
            val workSpaceId = this.configuration?.writeKey
            val identityId = this.configuration?.deviceId
            if (workSpaceId.isNullOrEmpty() || identityId.isNullOrEmpty()) {
                return
            }
//            logD(">>>>>>>checkIAM workSpaceId $workSpaceId, identityId $identityId")
            if (this.configuration?.isShowLog == true && BuildConfig.DEBUG) {
                Toast.makeText(context, "checkIAM", Toast.LENGTH_LONG).show()
            }
            service()?.checkIAM(
                workspaceId = workSpaceId,
                identityId = identityId,
            )?.enqueue(object : Callback<IAMResponse> {
                override fun onResponse(
                    call: Call<IAMResponse>, response: Response<IAMResponse>
                ) {
                    if (isValid()) {
                        val jsonString = response.body()?.data
                        var iamData: IAMData? = null
                        jsonString?.let { s ->
                            iamData = Gson().fromJson(s, IAMData::class.java)
                        }
                        onResponse?.invoke(
                            response.isSuccessful, response.code(), response.body(), iamData
                        )
                    }
                }

                override fun onFailure(call: Call<IAMResponse>, t: Throwable) {
                    if (isValid()) {
                        onFailure?.invoke(t)
                    }
                }
            })
        }
    }
}
