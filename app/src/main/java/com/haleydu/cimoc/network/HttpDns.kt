package com.haleydu.cimoc.network

import android.util.Log
import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException

class HttpDns : Dns {
    @Throws(UnknownHostException::class)
    override fun lookup(hostname: String): List<InetAddress> {
        Log.e("HttpDns", "lookup:$hostname")
        val ip = DNSHelper.getIpByHost(hostname)
        if (ip != null && ip != "") {
            val inetAddresses = InetAddress.getAllByName(ip).toList()
            Log.e("HttpDns", "inetAddresses:$inetAddresses")
            return inetAddresses
        }
        return SYSTEM.lookup(hostname)
    }

    companion object {
        private val SYSTEM = Dns.SYSTEM
    }
}
