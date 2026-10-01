package ai.sfdk.todomobile.ui.detail

import ai.sfdk.todomobile.data.Todo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoDetailScreen(
    viewModel: TodoDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onBack()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit todo" else "Todo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!state.isEditing && state.todo != null) {
                        IconButton(onClick = viewModel::startEditing) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = viewModel::delete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            val todo = state.todo
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                todo == null -> LoadError(
                    message = state.error ?: "This todo could not be loaded.",
                    onRetry = viewModel::load,
                    modifier = Modifier.align(Alignment.Center),
                )
                state.isEditing -> EditForm(
                    state = state,
                    onTitleChange = viewModel::onTitleChange,
                    onTagsChange = viewModel::onTagsChange,
                    onSave = viewModel::save,
                    onCancel = viewModel::cancelEditing,
                )
                else -> TodoDetails(
                    todo = todo,
                    error = state.error,
                    onMarkDone = viewModel::markDone,
                )
            }
        }
    }
}

@Composable
private fun TodoDetails(
    todo: Todo,
    error: String?,
    onMarkDone: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(text = todo.title, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = if (todo.done) "Done" else "Not done",
            style = MaterialTheme.typography.labelLarge,
            color = if (todo.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        )
        todo.createdAt?.let { createdAt ->
            Text(
                text = "Created ${formatTimestamp(createdAt)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (todo.tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                todo.tags.forEach { tag ->
                    AssistChip(onClick = {}, label = { Text(tag) })
                }
            }
        }
        error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
        if (!todo.done) {
            Button(onClick = onMarkDone) {
                Text("Mark done")
            }
        }
    }
}

@Composable
private fun EditForm(
    state: TodoDetailState,
    onTitleChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        OutlinedTextField(
            value = state.draftTitle,
            onValueChange = onTitleChange,
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.draftTags,
            onValueChange = onTagsChange,
            label = { Text("Tags, separated by commas") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave, enabled = !state.isSaving) {
                Text(if (state.isSaving) "Saving…" else "Save")
            }
            OutlinedButton(onClick = onCancel, enabled = !state.isSaving) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun LoadError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(32.dp),
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(onClick = onRetry) {
            Text("Try again")
        }
    }
}

private val timestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

private fun formatTimestamp(value: String): String =
    runCatching { timestampFormatter.format(Instant.parse(value)) }.getOrDefault(value)
