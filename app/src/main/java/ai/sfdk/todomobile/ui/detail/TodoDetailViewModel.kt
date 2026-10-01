package ai.sfdk.todomobile.ui.detail

import ai.sfdk.todomobile.data.Todo
import ai.sfdk.todomobile.data.TodoApi
import ai.sfdk.todomobile.data.TodoChanges
import ai.sfdk.todomobile.ui.toUserMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodoDetailState(
    val todo: Todo? = null,
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val draftTitle: String = "",
    val draftTags: String = "",
    val isSaving: Boolean = false,
    val isDeleted: Boolean = false,
    val isFinished: Boolean = false,
    val error: String? = null,
)

/**
 * One todo, to view and edit. With [editOnOpen] the screen opens straight in the edit form, as a
 * long-press on the list does, and is finished once that edit is saved or cancelled.
 */
class TodoDetailViewModel(
    private val id: String,
    private val editOnOpen: Boolean = false,
    private val api: suspend () -> TodoApi,
) : ViewModel() {

    private val _state = MutableStateFlow(TodoDetailState())
    val state: StateFlow<TodoDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val todo = api().getTodo(id)
                _state.update { it.copy(todo = todo, isLoading = false) }
                if (editOnOpen) startEditing()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.toUserMessage()) }
            }
        }
    }

    fun startEditing() {
        val todo = _state.value.todo ?: return
        _state.update {
            it.copy(
                isEditing = true,
                draftTitle = todo.title,
                draftTags = todo.tags.joinToString(", "),
                error = null,
            )
        }
    }

    fun cancelEditing() {
        _state.update { it.copy(isEditing = false, isFinished = editOnOpen, error = null) }
    }

    fun onTitleChange(title: String) {
        _state.update { it.copy(draftTitle = title) }
    }

    fun onTagsChange(tags: String) {
        _state.update { it.copy(draftTags = tags) }
    }

    fun save() {
        val draft = _state.value
        val title = draft.draftTitle.trim()
        if (title.isEmpty()) {
            _state.update { it.copy(error = EMPTY_TITLE_MESSAGE) }
            return
        }
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val updated = api().updateTodo(id, TodoChanges(title = title, tags = parseTags(draft.draftTags)))
                _state.update { it.copy(todo = updated, isEditing = false, isSaving = false, isFinished = editOnOpen) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.toUserMessage()) }
            }
        }
    }

    fun markDone() {
        viewModelScope.launch {
            try {
                val updated = api().markDone(id)
                _state.update { it.copy(todo = updated, error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.toUserMessage()) }
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                api().deleteTodo(id)
                _state.update { it.copy(isDeleted = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.toUserMessage()) }
            }
        }
    }

    private fun parseTags(text: String): List<String> =
        text.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    companion object {
        const val EMPTY_TITLE_MESSAGE = "The title can't be empty."
    }
}
