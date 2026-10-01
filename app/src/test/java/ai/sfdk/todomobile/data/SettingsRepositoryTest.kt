package ai.sfdk.todomobile.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun repository() = SettingsRepository(
        dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            folder.root.resolve("settings.preferences_pb")
        },
        scope = scope,
    )

    @Test
    fun `uses the emulator host address by default`() = runTest {
        val settings = repository()

        assertEquals("http://10.0.2.2:3000/api", settings.currentBaseUrl())
        assertEquals("http://10.0.2.2:3000/api", settings.baseUrl.value)
    }

    @Test
    fun `a new address takes effect right away`() = runTest {
        val settings = repository()

        settings.setBaseUrl("http://192.168.1.20:3000/api")

        assertEquals("http://192.168.1.20:3000/api", settings.currentBaseUrl())
        assertEquals("http://192.168.1.20:3000/api", settings.baseUrl.value)
    }

    @Test
    fun `a saved address is still used after the app restarts`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            folder.root.resolve("settings.preferences_pb")
        }
        SettingsRepository(dataStore, scope).setBaseUrl("http://192.168.1.20:3000/api")
        dataStore.data.first { it.asMap().isNotEmpty() }

        val restarted = SettingsRepository(dataStore, scope)

        assertEquals("http://192.168.1.20:3000/api", restarted.currentBaseUrl())
        assertEquals("http://192.168.1.20:3000/api", restarted.baseUrl.value)
    }

    @Test
    fun `an address an earlier build saved under api_base_url is restored`() = runTest {
        assertRestoredFromLegacyKey("api_base_url")
    }

    @Test
    fun `an address an earlier build saved under base_url is restored`() = runTest {
        assertRestoredFromLegacyKey("base_url")
    }

    private suspend fun assertRestoredFromLegacyKey(name: String) {
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            folder.root.resolve("settings.preferences_pb")
        }
        dataStore.edit { it[stringPreferencesKey(name)] = "http://192.168.1.20:3000/api" }

        val settings = SettingsRepository(dataStore, scope)

        assertEquals("http://192.168.1.20:3000/api", settings.currentBaseUrl())
    }
}
