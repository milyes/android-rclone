package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SyncLog
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.FloatEntry

@Composable
fun SyncStatusChart(syncLogs: List<SyncLog>) {
    // Process logs to get the latest progress for the last 5-7 unique sync tasks
    val latestProgressByTask = syncLogs
        .groupBy { it.commandExecuted + "_" + (it.recordingId ?: 0) }
        .mapValues { (_, logs) -> logs.maxByOrNull { it.timestamp } }
        .values
        .filterNotNull()
        .sortedByDescending { it.timestamp }
        .take(7)
        .reversed()

    val entries = if (latestProgressByTask.isEmpty()) {
        listOf(FloatEntry(0f, 0f))
    } else {
        latestProgressByTask.mapIndexed { index, log ->
            FloatEntry(x = index.toFloat(), y = log.progressPercent.toFloat())
        }
    }

    val model = entryModelOf(entries)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Progression des Transferts",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (latestProgressByTask.isEmpty()) {
                Text(
                    text = "Aucun transfert récent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Chart(
                    chart = columnChart(),
                    model = model,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
            }
        }
    }
}
