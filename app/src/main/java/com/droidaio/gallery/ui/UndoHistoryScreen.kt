package com.droidaio.gallery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.droidaio.gallery.PendingOpStore
import com.droidaio.gallery.PendingOperation
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UndoHistoryScreen(
    onCancel: (opId: UUID) -> Unit = {},
) {
    val ctx = LocalContext.current
    val store = remember { PendingOpStore(ctx) }
    var ops by remember { mutableStateOf(store.loadAll().sortedByDescending { it.createdAt }) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        ops = store.loadAll().sortedByDescending { it.createdAt }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Operation History") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (ops.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("No recent operations")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(ops) { op ->
                        OperationRow(op = op, onCancel = {
                            // call provided onCancel callback (activity or other handler)
                            onCancel(op.id)
                            // refresh local list from store
                            ops = store.loadAll().sortedByDescending { it.createdAt }
                        }, onRetry = {
                            // enqueue a retry via WorkManager (existing logic)
                            coroutineScope.launch {
                                // reuse existing retry logic from previous implementation
                                val gson = com.google.gson.Gson()
                                val json = gson.toJson(op.copy(attempts = op.attempts))
                                val input =
                                    androidx.work.Data.Builder().putString("pending_op_json", json)
                                        .build()
                                val work =
                                    androidx.work.OneTimeWorkRequestBuilder<com.droidaio.gallery.OperationWorker>()
                                        .setInputData(input)
                                        .setInitialDelay(
                                            0,
                                            java.util.concurrent.TimeUnit.MILLISECONDS
                                        )
                                        .setBackoffCriteria(
                                            androidx.work.BackoffPolicy.EXPONENTIAL,
                                            10_000,
                                            java.util.concurrent.TimeUnit.MILLISECONDS
                                        )
                                        .build()
                                val uniqueName = "op_${op.id}"
                                androidx.work.WorkManager.getInstance(ctx).enqueueUniqueWork(
                                    uniqueName,
                                    androidx.work.ExistingWorkPolicy.REPLACE,
                                    work
                                )
                                op.status = PendingOperation.Status.SCHEDULED
                                store.update(op)
                                ops = store.loadAll().sortedByDescending { it.createdAt }
                                snackbarHostState.showSnackbar("Retry scheduled")
                            }
                        }, onDetails = {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Status: ${op.status} ${op.message ?: ""}") }
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun OperationRow(
    op: PendingOperation,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDetails: () -> Unit
) {
    val df = DateFormat.getDateTimeInstance()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onDetails() }) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "${op.type.name} • ${op.itemIds.size} items",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Status: ${op.status.name}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Created: ${df.format(Date(op.createdAt))}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                if (op.status == PendingOperation.Status.PENDING) {
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
                if (op.status == PendingOperation.Status.FAILED) {
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
                TextButton(onClick = onDetails) { Text("Details") }
            }
        }
    }
}


/*
package com.droidaio.gallery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.droidaio.gallery.PendingOperation
import java.text.DateFormat
import java.util.*
import androidx.work.*
import com.google.gson.Gson
import com.droidaio.gallery.PendingOpStore
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UndoHistoryScreen() {
    val ctx = LocalContext.current
    val activity = ctx as? com.droidaio.gallery.MainActivity
    val store = remember { PendingOpStore(ctx) }
    var ops by remember { mutableStateOf(store.loadAll().sortedByDescending { it.createdAt }) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val gson = remember { Gson() }

    LaunchedEffect(Unit) {
        ops = store.loadAll().sortedByDescending { it.createdAt }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Operation History") }) }, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (ops.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("No recent operations")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(ops) { op ->
                        OperationRow(op = op, onCancel = {
                            // cancel scheduled work
                            activity?.cancelScheduledOp(op.id)
                            ops = store.loadAll().sortedByDescending { it.createdAt }
                        }, onRetry = {
                            // enqueue a new WorkRequest for retry (if attempts < maxAttempts)
                            if (op.attempts >= op.maxAttempts) {
                                coroutineScope.launch { snackbarHostState.showSnackbar("Max attempts reached") }
                            } else {
                                val json = gson.toJson(op.copy(attempts = op.attempts))
                                val input = Data.Builder().putString("pending_op_json", json).build()
                                val work = OneTimeWorkRequestBuilder<com.droidaio.gallery.OperationWorker>()
                                    .setInputData(input)
                                    .setInitialDelay(0, TimeUnit.MILLISECONDS)
                                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10_000, TimeUnit.MILLISECONDS)
                                    .build()
                                val uniqueName = "op_${op.id}"
                                WorkManager.getInstance(ctx).enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, work)
                                // update persisted op status
                                op.status = PendingOperation.Status.SCHEDULED
                                store.update(op)
                                ops = store.loadAll().sortedByDescending { it.createdAt }
                                coroutineScope.launch { snackbarHostState.showSnackbar("Retry scheduled") }
                            }
                        }, onDetails = {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Status: ${op.status} ${op.message ?: ""}") }
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun OperationRow(op: PendingOperation, onCancel: () -> Unit, onRetry: () -> Unit, onDetails: () -> Unit) {
    val df = DateFormat.getDateTimeInstance()
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .clickable { onDetails() }) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${op.type.name} • ${op.itemIds.size} items", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Status: ${op.status.name}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Created: ${df.format(Date(op.createdAt))}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                if (op.status == PendingOperation.Status.PENDING) {
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
                if (op.status == PendingOperation.Status.FAILED) {
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
                TextButton(onClick = onDetails) { Text("Details") }
            }
        }
    }
}

*/

