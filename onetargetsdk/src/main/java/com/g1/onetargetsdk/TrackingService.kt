package com.g1.onetargetsdk

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface TrackingService {

    @GET("/")
    fun track(
        @Query("workspace_id") workspace_id: String,
        @Query("identity_id") identity_id: String,
        @Query("event_name") event_name: String,
        @Query("event_date") event_date: String,
        @Query("eventData") eventData: String,
    ): Call<Void>
}
