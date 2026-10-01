package ai.sfdk.todomobile.ui

import ai.sfdk.todomobile.FakeTodoApi
import ai.sfdk.todomobile.MainDispatcherRule
import ai.sfdk.todomobile.data.Todo
import ai.sfdk.todomobile.ui.detail.TodoDetailViewModel
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class TodoDetailViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val groceries = Todo(
        id = "1",
        title = "Buy groceries",
        createdAt = "2026-09-30T17:00:00Z",
        tags = listOf("home", "errands"),
    )

    @Test
    fun `shows the todo when opened`() = runTest {
        val viewModel = TodoDetailViewModel("1") { FakeTodoApi(listOf(groceries)) }

        val state = viewModel.state.value
        assertEquals(groceries, state.todo)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `a todo the server does not know shows an error`() = runTest {
        val api = FakeTodoApi().apply {
            failure = HttpException(Response.error<Todo>(404, "".toResponseBody()))
        }

        val viewModel = TodoDetailViewModel("missing") { api }

        val state = viewModel.state.value
        assertNull(state.todo)
        assertEquals("The server answered with an error (404).", state.error)
    }

    @Test
    fun `editing starts from the current title and tags`() = runTest {
        val viewModel = TodoDetailViewModel("1") { FakeTodoApi(listOf(groceries)) }

        viewModel.startEditing()

        val state = viewModel.state.value
        assertTrue(state.isEditing)
        assertEquals("Buy groceries", state.draftTitle)
        assertEquals("home, errands", state.draftTags)
    }

    @Test
    fun `saving sends the trimmed title and tags and shows the result`() = runTest {
        val api = FakeTodoApi(listOf(groceries))
        val viewModel = TodoDetailViewModel("1") { api }
        viewModel.startEditing()

        viewModel.onTitleChange("  Buy groceries and bread ")
        viewModel.onTagsChange("home, errands, , home, weekend")
        viewModel.save()

        val (id, changes) = api.changes.single()
        assertEquals("1", id)
        assertEquals("Buy groceries and bread", changes.title)
        assertEquals(listOf("home", "errands", "weekend"), changes.tags)
        val state = viewModel.state.value
        assertFalse(state.isEditing)
        assertFalse(state.isSaving)
        assertEquals("Buy groceries and bread", state.todo?.title)
    }

    @Test
    fun `saving an empty title shows a message and keeps the todo`() = runTest {
        val api = FakeTodoApi(listOf(groceries))
        val viewModel = TodoDetailViewModel("1") { api }
        viewModel.startEditing()

        viewModel.onTitleChange("   ")
        viewModel.save()

        val state = viewModel.state.value
        assertEquals("The title can't be empty.", state.error)
        assertTrue(state.isEditing)
        assertFalse(state.isSaving)
        assertEquals(groceries, state.todo)
        assertTrue(api.changes.isEmpty())
    }

    @Test
    fun `a save the server refuses shows an error and keeps editing`() = runTest {
        val api = FakeTodoApi(listOf(groceries))
        val viewModel = TodoDetailViewModel("1") { api }
        viewModel.startEditing()
        api.failure = HttpException(Response.error<Todo>(400, "".toResponseBody()))

        viewModel.onTitleChange("Buy groceries and bread")
        viewModel.save()

        val state = viewModel.state.value
        assertEquals("The server answered with an error (400).", state.error)
        assertTrue(state.isEditing)
        assertFalse(state.isSaving)
        assertEquals("Buy groceries and bread", state.draftTitle)
        assertEquals(groceries, state.todo)
    }

    @Test
    fun `a save while the server is unreachable shows an error`() = runTest {
        val api = FakeTodoApi(listOf(groceries))
        val viewModel = TodoDetailViewModel("1") { api }
        viewModel.startEditing()
        api.failure = java.io.IOException("connection refused")

        viewModel.save()

        val state = viewModel.state.value
        assertEquals("Could not reach the server.", state.error)
        assertTrue(state.isEditing)
        assertFalse(state.isSaving)
    }

    @Test
    fun `cancel leaves the todo unchanged`() = runTest {
        val api = FakeTodoApi(listOf(groceries))
        val viewModel = TodoDetailViewModel("1") { api }
        viewModel.startEditing()
        viewModel.onTitleChange("Something else")

        viewModel.cancelEditing()

        assertFalse(viewModel.state.value.isEditing)
        assertEquals(groceries, viewModel.state.value.todo)
        assertTrue(api.changes.isEmpty())
    }

    @Test
    fun `mark done updates the todo`() = runTest {
        val viewModel = TodoDetailViewModel("1") { FakeTodoApi(listOf(groceries)) }

        viewModel.markDone()

        assertEquals(true, viewModel.state.value.todo?.done)
    }

    @Test
    fun `deleting the todo removes it on the server`() = runTest {
        val api = FakeTodoApi(listOf(groceries))
        val viewModel = TodoDetailViewModel("1") { api }

        viewModel.delete()

        assertTrue(viewModel.state.value.isDeleted)
        assertTrue(api.todos.isEmpty())
    }

    @Test
    fun `opened for editing it starts in the edit form`() = runTest {
        val viewModel = TodoDetailViewModel("1", editOnOpen = true) { FakeTodoApi(listOf(groceries)) }

        val state = viewModel.state.value
        assertTrue(state.isEditing)
        assertEquals("Buy groceries", state.draftTitle)
        assertEquals("home, errands", state.draftTags)
        assertFalse(state.isFinished)
    }

    @Test
    fun `opened for editing it is finished once the edit is saved`() = runTest {
        val api = FakeTodoApi(listOf(groceries))
        val viewModel = TodoDetailViewModel("1", editOnOpen = true) { api }

        viewModel.onTitleChange("Buy groceries and bread")
        viewModel.save()

        assertEquals("Buy groceries and bread", api.todos.single().title)
        assertTrue(viewModel.state.value.isFinished)
    }

    @Test
    fun `opened for editing it is finished when the edit is cancelled`() = runTest {
        val api = FakeTodoApi(listOf(groceries))
        val viewModel = TodoDetailViewModel("1", editOnOpen = true) { api }

        viewModel.cancelEditing()

        assertTrue(api.changes.isEmpty())
        assertTrue(viewModel.state.value.isFinished)
    }

    @Test
    fun `opened for editing a failed save keeps the form open`() = runTest {
        val api = FakeTodoApi(listOf(groceries))
        val viewModel = TodoDetailViewModel("1", editOnOpen = true) { api }
        api.failure = HttpException(Response.error<Todo>(500, "".toResponseBody()))

        viewModel.save()

        val state = viewModel.state.value
        assertTrue(state.isEditing)
        assertFalse(state.isFinished)
        assertEquals("The server answered with an error (500).", state.error)
    }

    @Test
    fun `opened from a tap a saved edit stays on the todo`() = runTest {
        val viewModel = TodoDetailViewModel("1") { FakeTodoApi(listOf(groceries)) }
        viewModel.startEditing()

        viewModel.save()

        assertFalse(viewModel.state.value.isFinished)
    }
}
