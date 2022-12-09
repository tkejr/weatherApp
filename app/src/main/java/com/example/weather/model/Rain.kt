package edu.tcu.bmei.weather.model

import com.google.gson.annotations.SerializedName

data class Rain(
    @SerializedName("1h")
    val one_h: Double,

    @SerializedName("3h")
    val three_h: Double
)