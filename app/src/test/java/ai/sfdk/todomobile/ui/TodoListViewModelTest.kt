package ai.sfdk.todomobile.ui

import ai.sfdk.todomobile.FakeTodoApi
import ai.sfdk.todomobile.MainDispatcherRule
import ai.sfdk.todomobile.data.Todo
import ai.sfdk.todomobile.data.TodoApi
import ai.sfdk.todomobile.data.TodoChanges
import ai.sfdk.todomobile.data.TodoPage
import ai.sfdk.todomobile.sampleTodos
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

        assertEquals(1, api.listCalls.count { it.pageSize == TodoApi.PAGE_SIZE })
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
    fun `the top bar count includes todos on pages not loaded yet`() = runTest {
        val api = FakeTodoApi(sampleTodos(125).map { if (it.id in setOf("3", "60", "110")) it.copy(done = true) else it })

        val viewModel = TodoListViewModel { api }

        assertEquals(20, viewModel.state.value.todos.size)
        assertEquals(122, viewModel.state.value.remaining)
    }

    @Test
    fun `adding a todo adds one to the count`() = runTest {
        val api = FakeTodoApi(sampleTodos(3))
        val viewModel = TodoListViewModel { api }
        assertEquals(3, viewModel.state.value.remaining)

        viewModel.addTodo("Water the plants")

        assertEquals(4, viewModel.state.value.remaining)
    }

    @Test
    fun `marking a todo done takes one off the count`() = runTest {
        val api = FakeTodoApi(sampleTodos(3))
        val viewModel = TodoListViewModel { api }

        viewModel.markDone("2")

        assertEquals(2, viewModel.state.value.remaining)
    }

    @Test
    fun `coming back to the list after a delete shows the new list and count`() = runTest {
        val api = FakeTodoApi(sampleTodos(3))
        val viewModel = TodoListViewModel { api }
        viewModel.onScreenShown()

        api.deleteTodo("2")
        viewModel.onScreenShown()

        assertEquals(listOf("1", "3"), viewModel.state.value.todos.map { it.id })
        assertEquals(2, viewModel.state.value.remaining)
    }

    @Test
    fun `showing the list the first time loads it only once`() = runTest {
        val api = FakeTodoApi(sampleTodos(3))
        val viewModel = TodoListViewModel { api }
        val callsWhenOpened = api.listCalls.size

        viewModel.onScreenShown()

        assertEquals(callsWhenOpened, api.listCalls.size)
    }

    @Test
    fun `the count ignores the search box`() = runTest {
        val api = FakeTodoApi(listOf(Todo(id = "1", title = "Buy milk"), Todo(id = "2", title = "Call the bank")))
        val viewModel = TodoListViewModel { api }

        viewModel.onQueryChange("milk")
        viewModel.refresh()

        assertEquals(1, viewModel.state.value.todos.size)
        assertEquals(2, viewModel.state.value.remaining)
    }

    @Test
    fun `a todo repeated on two pages is counted once`() = runTest {
        val todos = sampleTodos(150)
        val api = object : TodoApi by FakeTodoApi(todos) {
            // Like the server, each page also repeats the last todo of the page before it.
            override suspend fun listTodos(query: String?, page: Int, pageSize: Int): TodoPage {
                val first = maxOf(0, (page - 1) * pageSize - 1)
                return TodoPage(items = todos.drop(first).take(pageSize + 1), page = page, pageSize = pageSize, total = todos.size)
            }
        }

        val viewModel = TodoListViewModel { api }

        assertEquals(150, viewModel.state.value.remaining)
    }

    @Test
    fun `no count is shown when the server cannot be reached`() = runTest {
        val api = FakeTodoApi().apply { failure = IOException("connect timed out") }

        val viewModel = TodoListViewModel { api }

        assertNull(viewModel.state.value.remaining)
    }
}
