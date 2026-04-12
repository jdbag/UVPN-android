package com.uvpn.app.model

data class IpInfo(
    val ip: String = "Loading...",
    val country: String = "--",
    val city: String = "--",
    val isp: String = "--"
)
