package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Audio Sync Hub", appName)
  }

  @Test
  fun `verify license and core engine strings`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("Licence NETSECUREPRO.CA", context.getString(R.string.license_netsecurepro_ia))
    assertEquals("GEMIN CORE V7 PRO FINAL", context.getString(R.string.gemini_core_v7_engine))
    assertEquals("IA_LOGIC_SINGNATURE:ZOUBIROU-IA-2025", context.getString(R.string.ia_logic_signature))
    assertEquals("NETSECUREPRO.CA", com.example.data.viewmodel.RecordingViewModel.LICENSE)
    assertEquals("GEMIN CORE V7 PRO FINAL", com.example.data.viewmodel.RecordingViewModel.CORE_ENGINE)
    assertEquals("IA_LOGIC_SINGNATURE:ZOUBIROU-IA-2025", com.example.data.viewmodel.RecordingViewModel.IA_LOGIC_SIGNATURE)
  }
}
