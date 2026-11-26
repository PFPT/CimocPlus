package com.haleydu.cimoc.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class PublicHostPolicyTest {
    @Test
    fun allowsPublicHosts() {
        PublicHostPolicy.check("https://example.com/comic")
        PublicHostPolicy.check("http://8.8.8.8/image.jpg")
        assertFalse(PublicHostPolicy.isBlockedHost("images.dmzj.com"))
    }

    @Test
    fun blocksNonHttpAndCredentials() {
        assertThrows(IllegalArgumentException::class.java) {
            PublicHostPolicy.check("file:///etc/passwd")
        }
        assertThrows(IllegalArgumentException::class.java) {
            PublicHostPolicy.check("http://user:pass@example.com/")
        }
        assertThrows(IllegalArgumentException::class.java) {
            PublicHostPolicy.check("not a url")
        }
    }

    @Test
    fun blocksLocalNamesAndAddresses() {
        listOf(
            "http://localhost/",
            "http://router.local/",
            "http://metadata.google.internal/computeMetadata/v1/",
            "http://127.0.0.1/",
            "http://10.1.2.3/",
            "http://172.16.0.1/",
            "http://192.168.1.1/",
            "http://169.254.169.254/",
            "http://100.64.0.1/",
            "http://0.0.0.0/",
            "http://[::1]/",
            "http://[fd00::1]/"
        ).forEach { url ->
            assertThrows(url, IllegalArgumentException::class.java) {
                PublicHostPolicy.check(url)
            }
        }
    }

    @Test
    fun classifiesAddresses() {
        assertTrue(PublicHostPolicy.isPublicAddress(InetAddress.getByName("1.1.1.1")))
        assertTrue(PublicHostPolicy.isPublicAddress(InetAddress.getByName("8.8.4.4")))
        assertFalse(PublicHostPolicy.isPublicAddress(InetAddress.getByName("127.0.0.1")))
        assertFalse(PublicHostPolicy.isPublicAddress(InetAddress.getByName("10.0.0.8")))
        assertFalse(PublicHostPolicy.isPublicAddress(InetAddress.getByName("192.168.0.10")))
        assertFalse(PublicHostPolicy.isPublicAddress(InetAddress.getByName("169.254.1.1")))
        assertFalse(PublicHostPolicy.isPublicAddress(InetAddress.getByName("100.64.1.1")))
        assertFalse(PublicHostPolicy.isPublicAddress(InetAddress.getByName("198.18.0.1")))
        assertFalse(PublicHostPolicy.isPublicAddress(InetAddress.getByName("::1")))
        assertFalse(PublicHostPolicy.isPublicAddress(InetAddress.getByName("fe80::1")))
        assertTrue(
            PublicHostPolicy.selectPublic(
                listOf(InetAddress.getByName("10.0.0.1"), InetAddress.getByName("1.1.1.1"))
            ).size == 1
        )
    }
}
