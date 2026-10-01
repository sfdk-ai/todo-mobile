package ai.sfdk.todomobile.ui.list

import ai.sfdk.todomobile.data.NewTodo
import ai.sfdk.todomobile.data.Todo
import ai.sfdk.todomobile.data.TodoApi
import ai.sfdk.todomobile.data.TodoApi.Companion.PAGE_SIZE
import ai.sfdk.todomobile.ui.toUserMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodoListState(
    val todos: List<Todo> = emptyList(),
    val query: String = "",
    val page: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

class TodoListViewModel(
    private val api: suspend () -> TodoApi,
) : ViewModel() {

    private val _state = MutableStateFlow(TodoListState(isLoading = true))
    val state: StateFlow<TodoListState> = _state.asStateFlow()

    init {
        fetchPage(1)
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true) }
        fetchPage(1)
    }

    fun loadMore() {
        val current = _state.value
        if (!current.hasMore || current.isLoading || current.isRefreshing) return
        _state.update { it.copy(isLoading = true) }
        fetchPage(current.page + 1)
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        viewModelScope.launch {
            try {
                val result = api().listTodos(query.searchTerm(), 1, PAGE_SIZE)
                _state.update {
                    it.copy(
                        todos = result.items,
                        page = 1,
                        hasMore = PAGE_SIZE < result.total,
                        error = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.toUserMessage()) }
            }
        }
    }

    fun addTodo(title: String) {
        viewModelScope.launch {
            try {
                val created = api().createTodo(NewTodo(title = title.trim()))
                _state.update { it.copy(todos = listOf(created) + it.todos, error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.toUserMessage()) }
            }
        }
    }

    fun markDone(id: String) {
        viewModelScope.launch {
            try {
                val updated = api().markDone(id)
                _state.update { state ->
                    state.copy(todos = state.todos.map { if (it.id == updated.id) updated else it }, error = null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.toUserMessage()) }
            }
        }
    }

    private fun fetchPage(page: Int) {
        viewModelScope.launch {
            try {
                val result = api().listTodos(_state.value.query.searchTerm(), page, PAGE_SIZE)
                _state.update {
                    it.copy(
                        todos = if (page == 1) result.items else it.todos + result.items,
                        page = page,
                        hasMore = page * PAGE_SIZE < result.total,
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, error = e.toUserMessage()) }
            }
        }
    }

    private fun String.searchTerm(): String? = trim().ifEmpty { null }
}
