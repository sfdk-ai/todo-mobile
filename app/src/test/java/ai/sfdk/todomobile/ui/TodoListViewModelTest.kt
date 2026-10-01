package ai.sfdk.todomobile.ui

import ai.sfdk.todomobile.FakeTodoApi
import ai.sfdk.todomobile.MainDispatcherRule
import ai.sfdk.todomobile.data.Todo
import ai.sfdk.todomobile.data.TodoChanges
import ai.sfdk.todomobile.sampleTodos
import ai.sfdk.todomobile.ui.list.TodoListState
import ai.sfdk.todomobile.ui.list.TodoListViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class TodoListViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Test
    fun `shows the first page of todos when opened`() = runTest {
        val api = FakeTodoApi(sampleTodos(3))

        val viewModel = TodoListViewModel { api }

        val state = viewModel.state.value
        assertEquals(listOf("Todo 1", "Todo 2", "Todo 3"), state.todos.map { it.title })
        assertFalse(state.isLoading)
        assertFalse(state.hasMore)
        assertNull(state.error)
    }

    @Test
    fun `shows an error when the server cannot be reached`() = runTest {
        val api = FakeTodoApi().apply { failure = IOException("connect timed out") }

        val viewModel = TodoListViewModel { api }

        val state = viewModel.state.value
        assertEquals("Could not reach the server.", state.error)
        assertFalse(state.isLoading)
        assertTrue(state.todos.isEmpty())
    }

    @Test
    fun `loads the next page when the end of the list is reached`() = runTest {
        val api = FakeTodoApi(sampleTodos(25))
        val viewModel = TodoListViewModel { api }
        assertEquals(20, viewModel.state.value.todos.size)
        assertTrue(viewModel.state.value.hasMore)

        viewModel.loadMore()

        val state = viewModel.state.value
        assertEquals((1..25).map { "Todo $it" }, state.todos.map { it.title })
        assertEquals(2, state.page)
        assertFalse(state.hasMore)
        assertEquals(2, api.listCalls.last().page)
    }

    @Test
    fun `does not ask for another page when every todo is shown`() = runTest {
        val api = FakeTodoApi(sampleTodos(5))
        val viewModel = TodoListViewModel { api }

        viewModel.loadMore()

        assertEquals(1, api.listCalls.size)
        assertEquals(5, viewModel.state.value.todos.size)
    }

    @Test
    fun `search shows only the matching todos`() = runTest {
        val api = FakeTodoApi(
            listOf(
                Todo(id = "1", title = "Buy milk"),
                Todo(id = "2", title = "Call the bank"),
                Todo(id = "3", title = "Milk the cows"),
            ),
        )
        val viewModel = TodoListViewModel { api }

        viewModel.onQueryChange("milk")

        val state = viewModel.state.value
        assertEquals("milk", state.query)
        assertEquals(listOf("Buy milk", "Milk the cows"), state.todos.map { it.title })
        assertEquals("milk", api.listCalls.last().query)
    }

    @Test
    fun `clearing the search shows every todo again`() = runTest {
        val api = FakeTodoApi(
            listOf(
                Todo(id = "1", title = "Buy milk"),
                Todo(id = "2", title = "Call the bank"),
            ),
        )
        val viewModel = TodoListViewModel { api }
        viewModel.onQueryChange("bank")

        viewModel.onQueryChange("")

        assertEquals(listOf("Buy milk", "Call the bank"), viewModel.state.value.todos.map { it.title })
        assertNull(api.listCalls.last().query)
    }

    @Test
    fun `marking a todo done updates it in place`() = runTest {
        val api = FakeTodoApi(sampleTodos(3))
        val viewModel = TodoListViewModel { api }

        viewModel.markDone("2")

        val todos = viewModel.state.value.todos
        assertEquals(listOf(false, true, false), todos.map { it.done })
        assertEquals(listOf("1", "2", "3"), todos.map { it.id })
    }

    @Test
    fun `a new todo appears at the top of the list`() = runTest {
        val api = FakeTodoApi(sampleTodos(2))
        val viewModel = TodoListViewModel { api }

        viewModel.addTodo("  Water the plants ")

        val todos = viewModel.state.value.todos
        assertEquals("Water the plants", todos.first().title)
        assertEquals(3, todos.size)
    }

    @Test
    fun `pull to refresh hides the indicator when it finishes`() = runTest {
        val api = FakeTodoApi(sampleTodos(2))
        val viewModel = TodoListViewModel { api }

        viewModel.refresh()

        val state = viewModel.state.value
        assertFalse(state.isRefreshing)
        assertNull(state.error)
    }

    @Test
    fun `pull to refresh shows each todo once`() = runTest {
        val api = FakeTodoApi(sampleTodos(3))
        val viewModel = TodoListViewModel { api }

        viewModel.refresh()
        viewModel.refresh()

        assertEquals(listOf("Todo 1", "Todo 2", "Todo 3"), viewModel.state.value.todos.map { it.title })
        assertEquals(1, viewModel.state.value.page)
    }

    @Test
    fun `pull to refresh replaces an edited todo with the server's copy`() = runTest {
        val api = FakeTodoApi(sampleTodos(3))
        val viewModel = TodoListViewModel { api }
        api.updateTodo("2", TodoChanges(title = "Renamed"))

        viewModel.refresh()

        assertEquals(listOf("Todo 1", "Renamed", "Todo 3"), viewModel.state.value.todos.map { it.title })
    }

    @Test
    fun `pull to refresh after loading more starts again from the first page`() = runTest {
        val api = FakeTodoApi(sampleTodos(25))
        val viewModel = TodoListViewModel { api }
        viewModel.loadMore()

        viewModel.refresh()

        val state = viewModel.state.value
        assertEquals((1..20).map { "Todo $it" }, state.todos.map { it.title })
        assertEquals(1, state.page)
        assertTrue(state.hasMore)
    }

    @Test
    fun `shows the empty state when there are no todos`() = runTest {
        val api = FakeTodoApi()

        val viewModel = TodoListViewModel { api }

        assertTrue(viewModel.state.value.showsEmptyState)
    }

    @Test
    fun `hides the empty state when there are todos`() = runTest {
        val api = FakeTodoApi(sampleTodos(2))

        val viewModel = TodoListViewModel { api }

        assertFalse(viewModel.state.value.showsEmptyState)
    }

    @Test
    fun `hides the empty state while the list is loading`() {
        assertFalse(TodoListState(isLoading = true).showsEmptyState)
    }

    @Test
    fun `hides the empty state when the server cannot be reached`() = runTest {
        val api = FakeTodoApi().apply { failure = IOException("connect timed out") }

        val viewModel = TodoListViewModel { api }

        assertFalse(viewModel.state.value.showsEmptyState)
    }

    @Test
    fun `hides the empty state when a search matches nothing`() = runTest {
        val api = FakeTodoApi(sampleTodos(2))
        val viewModel = TodoListViewModel { api }

        viewModel.onQueryChange("milk")

        assertTrue(viewModel.state.value.todos.isEmpty())
        assertFalse(viewModel.state.value.showsEmptyState)
    }

    @Test
    fun `adding the first todo hides the empty state`() = runTest {
        val api = FakeTodoApi()
        val viewModel = TodoListViewModel { api }

        viewModel.addTodo("Buy milk")

        assertEquals(listOf("Buy milk"), viewModel.state.value.todos.map { it.title })
        assertFalse(viewModel.state.value.showsEmptyState)
    }
}
