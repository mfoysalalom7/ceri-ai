package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PersonaDefaults
import com.example.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    assertEquals("Cherry AI", appName)
  }

  @Test
  fun `verify personas loaded correctly`() {
    val personas = PersonaDefaults.allPersonas
    assertEquals(5, personas.size)
    val cherry = PersonaDefaults.getById("cherry_assistant")
    assertEquals("Cherry AI", cherry.name)
    assertNotNull(cherry.systemInstruction)
  }

  @Test
  fun `verify default user profile`() {
    val profile = UserProfile()
    assertEquals("CherryExplorer", profile.username)
    assertEquals("Cherry Explorer", profile.displayName)
  }
}
