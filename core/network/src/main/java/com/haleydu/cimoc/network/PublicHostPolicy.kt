package com.haleydu.cimoc.network

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException

object PublicHostPolicy {
    fun check(url: String) {
        val parsed = url.trim().toHttpUrlOrNull() ?: throw IllegalArgumentException("blocked url")
        if (parsed.scheme != "http" && parsed.scheme != "https") {
            throw IllegalArgumentException("blocked url")
        }
        if (parsed.username.isNotEmpty() || parsed.password.isNotEmpty()) {
            throw IllegalArgumentException("blocked url")
        }
        if (isBlockedHost(parsed.host)) {
            throw IllegalArgumentException("blocked url")
        }
    }

    fun isBlockedHost(host: String): Boolean {
        val name = host.trim().trimEnd('.').lowercase()
        if (name.isEmpty()) return true
        if (name == "localhost" || name == "localhost.localdomain" || name == "metadata.google.internal") {
            return true
        }
        if (name.endsWith(".localhost") || name.endsWith(".local") || name.endsWith(".localdomain") || name.endsWith(".internal")) {
            return true
        }
        val literal = literalAddress(name) ?: return false
        return !isPublicAddress(literal)
    }

    fun isPublicAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress || address.isSiteLocalAddress || address.isMulticastAddress) {
            return false
        }
        if (address is Inet6Address) {
            val mapped = ipv4Mapped(address)
            if (mapped != null) return isPublicAddress(mapped)
            val bytes = address.address
            val first = bytes[0].toInt() and 0xff
            if (first and 0xfe == 0xfc) return false
            if (first == 0xfe && (bytes[1].toInt() and 0xc0) == 0xc0) return false
            if (first == 0x20 && (bytes[1].toInt() and 0xff) == 0x01 && (bytes[2].toInt() and 0xff) == 0x0d && (bytes[3].toInt() and 0xff) == 0xb8) {
                return false
            }
            return true
        }
        if (address is Inet4Address) {
            val bytes = address.address
            val b0 = bytes[0].toInt() and 0xff
            val b1 = bytes[1].toInt() and 0xff
            val b2 = bytes[2].toInt() and 0xff
            if (b0 == 0 || b0 >= 224) return false
            if (b0 == 100 && b1 in 64..127) return false
            if (b0 == 192 && b1 == 0 && (b2 == 0 || b2 == 2)) return false
            if (b0 == 198 && b1 == 51 && b2 == 100) return false
            if (b0 == 203 && b1 == 0 && b2 == 113) return false
            if (b0 == 198 && (b1 == 18 || b1 == 19)) return false
        }
        return true
    }

    fun selectPublic(addresses: List<InetAddress>): List<InetAddress> {
        return addresses.filter { isPublicAddress(it) }
    }

    val dns: Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            if (isBlockedHost(hostname)) throw UnknownHostException("blocked")
            val allowed = selectPublic(Dns.SYSTEM.lookup(hostname))
            if (allowed.isEmpty()) throw UnknownHostException("blocked")
            return allowed
        }
    }

    private fun literalAddress(host: String): InetAddress? {
        val text = if (host.startsWith("[") && host.endsWith("]") && host.length > 2) {
            host.substring(1, host.length - 1)
        } else {
            host
        }
        val ipv4 = text.matches(IPV4)
        val ipv6 = text.contains(':')
        if (!ipv4 && !ipv6) return null
        return try {
            InetAddress.getByName(text)
        } catch (_: UnknownHostException) {
            null
        }
    }

    private fun ipv4Mapped(address: Inet6Address): Inet4Address? {
        val bytes = address.address
        val mapped = (0..9).all { bytes[it].toInt() == 0 } &&
            bytes[10] == 0xff.toByte() &&
            bytes[11] == 0xff.toByte()
        if (!mapped) return null
        val v4 = InetAddress.getByAddress(bytes.copyOfRange(12, 16))
        return v4 as? Inet4Address
    }

    private val IPV4 = Regex("""\d{1,3}(?:\.\d{1,3}){3}""")
}
