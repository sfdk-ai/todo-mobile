package ai.sfdk.todomobile.ui

import ai.sfdk.todomobile.AppContainer
import ai.sfdk.todomobile.ui.detail.TodoDetailScreen
import ai.sfdk.todomobile.ui.detail.TodoDetailViewModel
import ai.sfdk.todomobile.ui.list.TodoListScreen
import ai.sfdk.todomobile.ui.list.TodoListViewModel
import ai.sfdk.todomobile.ui.settings.SettingsScreen
import ai.sfdk.todomobile.ui.settings.SettingsViewModel
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun TodoNavHost(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "todos") {
        composable("todos") { entry ->
            val listViewModel = viewModel { TodoListViewModel(container::api) }
            // The todo opened from the list may have changed; show the server's copy on return.
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                entry.savedStateHandle.remove<String>(OPENED_TODO_ID)?.let(listViewModel::reloadTodo)
            }
            TodoListScreen(
                viewModel = listViewModel,
                onOpenTodo = { id ->
                    entry.savedStateHandle[OPENED_TODO_ID] = id
                    navController.navigate("todos/${Uri.encode(id)}")
                },
                onEditTodo = { id ->
                    entry.savedStateHandle[OPENED_TODO_ID] = id
                    navController.navigate("todos/${Uri.encode(id)}/edit")
                },
                onOpenSettings = { navController.navigate("settings") },
            )
        }
        composable(
            route = "todos/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = requireNotNull(entry.arguments?.getString("id"))
            TodoDetailScreen(
                viewModel = viewModel { TodoDetailViewModel(id, api = container::api) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "todos/{id}/edit",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = requireNotNull(entry.arguments?.getString("id"))
            TodoDetailScreen(
                viewModel = viewModel { TodoDetailViewModel(id, editOnOpen = true, api = container::api) },
                onBack = { navController.popBackStack() },
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel { SettingsViewModel(container.settings) },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private const val OPENED_TODO_ID = "openedTodoId"
