package ai.sfdk.todomobile

import ai.sfdk.todomobile.data.NewTodo
import ai.sfdk.todomobile.data.TagCount
import ai.sfdk.todomobile.data.Todo
import ai.sfdk.todomobile.data.TodoApi
import ai.sfdk.todomobile.data.TodoChanges
import ai.sfdk.todomobile.data.TodoPage

class FakeTodoApi(initial: List<Todo> = emptyList()) : TodoApi {
    data class ListCall(val query: String?, val page: Int, val pageSize: Int)

    val todos = initial.toMutableList()
    val listCalls = mutableListOf<ListCall>()
    val changes = mutableListOf<Pair<String, TodoChanges>>()
    val markDoneCalls = mutableListOf<String>()
    var failure: Exception? = null

    override suspend fun listTodos(query: String?, page: Int, pageSize: Int): TodoPage {
        failure?.let { throw it }
        listCalls += ListCall(query, page, pageSize)
        val matching = todos.filter { query == null || it.title.contains(query, ignoreCase = true) }
        val items = matching.drop((page - 1) * pageSize).take(pageSize)
        return TodoPage(items = items, page = page, pageSize = pageSize, total = matching.size)
    }

    override suspend fun getTodo(id: String): Todo {
        failure?.let { throw it }
        return todos.first { it.id == id }
    }

    override suspend fun createTodo(body: NewTodo): Todo {
        failure?.let { throw it }
        val created = Todo(id = "new-${todos.size + 1}", title = body.title, tags = body.tags)
        todos += created
        return created
    }

    override suspend fun updateTodo(id: String, body: TodoChanges): Todo {
        failure?.let { throw it }
        changes += id to body
        val index = todos.indexOfFirst { it.id == id }
        val current = todos[index]
        val updated = current.copy(
            title = body.title ?: current.title,
            done = body.done ?: current.done,
            tags = body.tags ?: current.tags,
        )
        todos[index] = updated
        return updated
    }

    /** Like todo-app's `POST /api/todos/{id}/done`, which flips `done` (src/todos.ts). */
    override suspend fun markDone(id: String): Todo {
        failure?.let { throw it }
        markDoneCalls += id
        val index = todos.indexOfFirst { it.id == id }
        todos[index] = todos[index].copy(done = !todos[index].done)
        return todos[index]
    }

    override suspend fun deleteTodo(id: String) {
        failure?.let { throw it }
        todos.removeAll { it.id == id }
    }

    override suspend fun listTags(): List<TagCount> =
        todos.flatMap { it.tags }.groupingBy { it }.eachCount().map { (name, count) -> TagCount(name, count) }
}

fun sampleTodos(count: Int): List<Todo> = (1..count).map { n ->
    Todo(id = "$n", title = "Todo $n", createdAt = "2026-09-0${(n % 9) + 1}T09:00:00Z")
}
