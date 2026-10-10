package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.security.EncryptionHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SecureShield", appName)
  }

  @Test
  fun `verify random otp generation length and digits`() {
    val otp = EncryptionHelper.generateRandomOtp()
    assertEquals(6, otp.length)
    assertTrue(otp.all { it.isDigit() })
  }

  @Test
  fun `verify master pin hash verification`() {
    val pin = "1234"
    val hash = EncryptionHelper.hashSecret(pin)
    assertTrue(EncryptionHelper.verifySecret("1234", hash))
    assertTrue(!EncryptionHelper.verifySecret("0000", hash))
  }

  @Test
  fun `verify password strength evaluation`() {
    val strength = EncryptionHelper.evaluatePasswordStrength("Secure@Shield2026")
    assertTrue(strength.score >= 80)
  }
}
