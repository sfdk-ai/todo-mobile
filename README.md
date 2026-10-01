# Todo for Android

An Android app for [todo-app](https://github.com/sfdk-ai/todo-app). It lists your todos, searches them, and lets you add, edit, tag, complete and delete them. Everything it shows comes from a todo-app server over its REST API.

- Kotlin and Jetpack Compose with Material 3
- Retrofit with kotlinx.serialization for the API
- DataStore for the app's settings
- Minimum Android 8.0 (API 26); compiled and targeted at API 37

## Screens

- **Todos**: the list, newest first, with how many todos are left (not done) in the top bar. Pull down to refresh, type in the search box to filter by title, scroll to load more, tap the box to mark a todo done, and tap + to add one.
- **Todo**: one todo with its tags and creation time. Edit its title and tags, mark it done, or delete it.
- **Settings** (the gear icon): `API_BASE_URL`, the address of the todo-app API, with the value in use.

## What you need

- JDK 17 or later (CI uses 21)
- The Android SDK, with `ANDROID_HOME` pointing at it or `sdk.dir` set in `local.properties`. If the SDK licenses are accepted, the build downloads the platform and build tools it is missing.
- For running the app: an Android emulator or a phone with USB debugging, and `adb` from the SDK's `platform-tools`

Use the Gradle wrapper in this repository, `./gradlew`; you don't need Gradle installed.

## Build and install

```sh
./gradlew assembleDebug
```

builds `app/build/outputs/apk/debug/app-debug.apk`. Start an emulator, then install it:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`./gradlew installDebug` builds and installs in one step.

## Point it at a todo-app server

Start todo-app on your computer; see its README. It listens on port 3000 and serves its API under `/api`.

The app's `API_BASE_URL` setting defaults to `http://10.0.2.2:3000/api`. Inside the Android emulator, `10.0.2.2` is your computer, so with todo-app running on your computer the emulator works without changing anything.

To use another server, open Settings, type its address, including `/api`, into `API_BASE_URL`, and tap Save. A phone is a separate device on your network, so it needs your computer's network address, such as `http://192.168.1.20:3000/api`, and both must be on the same network.

If the phone is redroid, Android in a Docker container on the same Linux machine as the server, it reaches the host at the Docker bridge gateway, usually `http://172.17.0.1:3000/api`, not `10.0.2.2`.

The app talks plain HTTP, so it allows cleartext traffic.

## Tests

```sh
./gradlew test
```

runs the unit tests on the JVM. They need a JDK and the Android SDK, but no emulator or device. The report is at `app/build/reports/tests/testDebugUnitTest/index.html`.

- `TodoApiTest` checks the routes and JSON the client sends and reads, against a local mock server.
- `TodoListViewModelTest`, `TodoDetailViewModelTest` and `SettingsViewModelTest` check each screen's state against a fake API.
- `SettingsRepositoryTest` checks the stored settings.

GitHub Actions runs the tests and builds the debug APK on every push and pull request (`.github/workflows/ci.yml`); the APK is attached to each run.

## The API it uses

Every route and JSON shape lives in one file, `app/src/main/java/ai/sfdk/todomobile/data/TodoApi.kt`. Routes are relative to `API_BASE_URL`, and errors come back as `{ "error": "message" }`.

| Method | Route | Used for |
| --- | --- | --- |
| `GET` | `todos?q=&page=&pageSize=` | The list and search, 20 to a page |
| `POST` | `todos` | Adding a todo |
| `GET` | `todos/{id}` | The todo screen |
| `PATCH` | `todos/{id}` | Saving an edit |
| `POST` | `todos/{id}/done` | Marking a todo done |
| `DELETE` | `todos/{id}` | Deleting a todo |
| `GET` | `tags` | Tags with their counts |

## Project layout

```
app/src/main/java/ai/sfdk/todomobile/
  TodoApplication.kt      app start-up and shared objects
  MainActivity.kt
  data/TodoApi.kt         the API client and its JSON types
  data/SettingsRepository.kt
  ui/TodoNavHost.kt       navigation between the three screens
  ui/list/                the todo list
  ui/detail/              one todo, view and edit
  ui/settings/            the API_BASE_URL setting
app/src/test/             unit tests
```
