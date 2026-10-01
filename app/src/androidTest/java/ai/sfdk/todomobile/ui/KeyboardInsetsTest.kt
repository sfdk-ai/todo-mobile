package ai.sfdk.todomobile.ui

import ai.sfdk.todomobile.MainActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Runs on a phone or emulator: ./gradlew connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class KeyboardInsetsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun keyboardTop(): Int {
        val decor = composeRule.activity.window.decorView
        val insets = ViewCompat.getRootWindowInsets(decor) ?: return decor.height
        return decor.height - insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
    }

    private fun keyboardIsShown(): Boolean {
        val decor = composeRule.activity.window.decorView
        return ViewCompat.getRootWindowInsets(decor)?.isVisible(WindowInsetsCompat.Type.ime()) == true
    }

    @Test
    fun addButtonStaysAboveTheKeyboard() {
        composeRule.onNodeWithText("Search todos").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) { keyboardIsShown() }
        composeRule.waitForIdle()

        val addButton = composeRule.onNodeWithContentDescription("Add todo").fetchSemanticsNode().boundsInWindow
        val top = keyboardTop()
        assertTrue(
            "Add todo button bottom ${addButton.bottom} is below the keyboard's top $top",
            addButton.bottom <= top,
        )
    }
}
