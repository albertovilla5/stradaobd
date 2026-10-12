package com.torqueobd2.dash.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AppPrivacyPolicyTest {

    private fun resolveProjectRoot(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".")
        return if (File(cwd, "docs/privacy-policy.html").exists()) {
            cwd
        } else {
            cwd.parentFile ?: cwd
        }
    }

    @Test
    fun `AppPrivacyPolicy exposes valid HTTPS URLs and developer contact details`() {
        assertEquals("StradaOBD", AppPrivacyPolicy.APP_NAME)
        assertEquals("com.stradaobd.dash", AppPrivacyPolicy.APPLICATION_ID)
        assertTrue(AppPrivacyPolicy.DEVELOPER_NAME.contains("albertovilla5"))
        assertEquals("22d7rrcwzm@privaterelay.appleid.com", AppPrivacyPolicy.DEVELOPER_CONTACT_EMAIL)
        assertTrue(AppPrivacyPolicy.PRIVACY_POLICY_URL.startsWith("https://albertovilla5.github.io/"))
        assertEquals(
            "https://albertovilla5.github.io/stradaobd/privacy-policy.html",
            AppPrivacyPolicy.PRIVACY_POLICY_URL
        )
        assertEquals(
            "https://github.com/albertovilla5/stradaobd/issues",
            AppPrivacyPolicy.GITHUB_SUPPORT_URL
        )
    }

    @Test
    fun `docs privacy-policy html and PRIVACY_POLICY md match Play Console requirements`() {
        val root = resolveProjectRoot()
        val htmlFile = File(root, "docs/privacy-policy.html")
        val indexFile = File(root, "docs/index.html")
        val markdownFile = File(root, "PRIVACY_POLICY.md")

        assertTrue("docs/privacy-policy.html must exist", htmlFile.exists())
        assertTrue("docs/index.html must exist", indexFile.exists())
        assertTrue("PRIVACY_POLICY.md must exist", markdownFile.exists())

        val html = htmlFile.readText(Charsets.UTF_8)
        val md = markdownFile.readText(Charsets.UTF_8)

        val requiredTokens = listOf(
            AppPrivacyPolicy.APP_NAME,
            AppPrivacyPolicy.APPLICATION_ID,
            AppPrivacyPolicy.DEVELOPER_CONTACT_EMAIL,
            AppPrivacyPolicy.PRIVACY_POLICY_URL,
            AppPrivacyPolicy.GITHUB_SUPPORT_URL,
            "BLUETOOTH_CONNECT",
            "BLUETOOTH_SCAN",
            "neverForLocation",
            "ACCESS_FINE_LOCATION",
            "FOREGROUND_SERVICE_CONNECTED_DEVICE",
            "backup_rules.xml",
            "data_extraction_rules.xml",
            "AD_ID"
        )

        for (token in requiredTokens) {
            assertTrue("docs/privacy-policy.html missing token: $token", html.contains(token))
            assertTrue("PRIVACY_POLICY.md missing token: $token", md.contains(token))
        }
    }
}
