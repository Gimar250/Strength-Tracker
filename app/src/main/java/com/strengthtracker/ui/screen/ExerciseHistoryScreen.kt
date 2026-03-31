package com.strengthtracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strengthtracker.data.db.entity.ExerciseType
import com.strengthtracker.data.db.entity.HistoryLog
import com.strengthtracker.data.repository.WorkoutRepository
import com.strengthtracker.ui.viewmodel.ExerciseHistoryViewModel
import com.strengthtracker.ui.viewmodel.ExerciseStats
import com.strengthtracker.ui.viewmodel.ProgressPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseHistoryScreen(
    repository: WorkoutRepository,
    exerciseId: Long,
    onBack: () -> Unit
) {
    val viewModel: ExerciseHistoryViewModel = viewModel(
        factory = ExerciseHistoryViewModel.Factory(repository, exerciseId)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val exercise = state.exercise
    val isTimed = exercise?.exerciseType == ExerciseType.TIMED

    // Toggle state: true = show weight, false = show reps/secs
    var showWeight by remember { mutableStateOf(
        (state.stats?.bestWeight ?: 0f) > 0f
    ) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = exercise?.name?.uppercase() ?: "",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = state.workoutName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack, "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Stats row ─────────────────────────────────────────────────
            state.stats?.let { stats ->
                item { StatsRow(stats = stats, isTimed = isTimed) }
            }

            // ── Progress chart ────────────────────────────────────────────
            if (state.progressPoints.size >= 2) {
                item {
                    ChartSection(
                        points = state.progressPoints,
                        showWeight = showWeight,
                        hasWeightData = (state.stats?.bestWeight ?: 0f) > 0f,
                        isTimed = isTimed,
                        onToggle = { showWeight = !showWeight }
                    )
                }
            } else if (state.progressPoints.size == 1) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = "Complete more sessions to see your progress chart.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            }

            // ── Log table header ──────────────────────────────────────────
            if (state.allLogs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "FULL LOG",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "${state.allLogs.size} sets",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                item { LogTableHeader(isTimed = isTimed) }
                items(state.allLogs, key = { it.id }) { log ->
                    LogTableRow(log = log, isTimed = isTimed)
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Stats row ─────────────────────────────────────────────────────────────────

@Composable
private fun StatsRow(stats: ExerciseStats, isTimed: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (stats.bestWeight != null) {
            StatCard(
                label = "BEST WEIGHT",
                value = "${formatWeight(stats.bestWeight)} kg",
                modifier = Modifier.weight(1f)
            )
        }
        StatCard(
            label = if (isTimed) "BEST TIME" else "BEST REPS",
            value = if (isTimed) formatSeconds(stats.bestValue) else "${stats.bestValue}",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "SESSIONS",
            value = "${stats.totalSessions}",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Chart section ─────────────────────────────────────────────────────────────

@Composable
private fun ChartSection(
    points: List<ProgressPoint>,
    showWeight: Boolean,
    hasWeightData: Boolean,
    isTimed: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Toggle row (only if there is weight data worth showing)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PROGRESS",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (hasWeightData) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ChartToggleChip(
                            label = "WEIGHT",
                            selected = showWeight,
                            onClick = { if (!showWeight) onToggle() }
                        )
                        ChartToggleChip(
                            label = if (isTimed) "SECS" else "REPS",
                            selected = !showWeight,
                            onClick = { if (showWeight) onToggle() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            ProgressLineChart(
                points = points,
                showWeight = showWeight,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))
            Text(
                text = "Last ${points.size} sessions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ChartToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.secondary
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    MaterialTheme.colorScheme.outline
                )
            )
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ── Line chart — pure Canvas, no external libraries ──────────────────────────

@Composable
private fun ProgressLineChart(
    points: List<ProgressPoint>,
    showWeight: Boolean,
    modifier: Modifier = Modifier
) {
    val values = if (showWeight) points.map { it.maxWeight }
    else points.map { it.maxValue.toFloat() }

    val rawMin = values.minOrNull() ?: 0f
    val rawMax = values.maxOrNull() ?: 1f
    val rawRange = rawMax - rawMin
    // Vertical padding so points don't sit exactly on the edge
    val padFraction = if (rawRange == 0f) 0.5f else 0.15f
    val minVal = rawMin - rawRange * padFraction
    val maxVal = rawMax + rawRange * padFraction
    val range = (maxVal - minVal).coerceAtLeast(0.001f)

    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val bgColor = MaterialTheme.colorScheme.surfaceVariant
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val primaryArgb = primaryColor.toArgb()
    val outlineArgb = outlineColor.toArgb()
    val secondaryArgb = secondaryColor.toArgb()
    val bgArgb = bgColor.toArgb()

    Box(
        modifier = modifier
            .height(180.dp)
            .drawBehind {
                val leftPad = 52.dp.toPx()
                val rightPad = 12.dp.toPx()
                val topPad = 12.dp.toPx()
                val bottomPad = 30.dp.toPx()
                val chartW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                val chartH = (size.height - topPad - bottomPad).coerceAtLeast(1f)

                val textPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = 10.sp.toPx()
                }

                // ── Grid lines + Y axis labels (3 levels) ──
                for (level in 0..2) {
                    val fraction = level / 2f
                    val gridVal = maxVal - fraction * range
                    val gy = topPad + fraction * chartH

                    drawLine(
                        color = outlineColor.copy(alpha = 0.25f),
                        start = Offset(leftPad, gy),
                        end = Offset(leftPad + chartW, gy),
                        strokeWidth = 0.5.dp.toPx()
                    )

                    val label = if (showWeight) {
                        if (gridVal % 1 == 0f) "${gridVal.toInt()}kg" else "%.1fkg".format(gridVal)
                    } else {
                        "${gridVal.toInt()}"
                    }
                    textPaint.color = outlineArgb
                    textPaint.textAlign = android.graphics.Paint.Align.RIGHT
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        leftPad - 6.dp.toPx(),
                        gy + textPaint.textSize / 3f,
                        textPaint
                    )
                }

                // ── Line path ──
                if (points.size >= 2) {
                    val path = Path()
                    points.forEachIndexed { i, point ->
                        val v = if (showWeight) point.maxWeight else point.maxValue.toFloat()
                        val px = leftPad + i.toFloat() / (points.size - 1) * chartW
                        val py = topPad + (1f - (v - minVal) / range) * chartH
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    drawPath(
                        path = path,
                        color = primaryColor,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // ── Dots + X axis labels ──
                val xLabelIndices = when {
                    points.size <= 3 -> points.indices.toList()
                    else -> listOf(0, points.size / 2, points.size - 1)
                }

                textPaint.textAlign = android.graphics.Paint.Align.CENTER
                textPaint.color = secondaryArgb

                points.forEachIndexed { i, point ->
                    val v = if (showWeight) point.maxWeight else point.maxValue.toFloat()
                    val xFrac = if (points.size > 1) i.toFloat() / (points.size - 1) else 0.5f
                    val px = leftPad + xFrac * chartW
                    val py = topPad + (1f - (v - minVal) / range) * chartH

                    // Outer dot
                    drawCircle(primaryColor, radius = 5.dp.toPx(), center = Offset(px, py))
                    // Inner hollow
                    drawCircle(
                        color = Color(bgArgb),
                        radius = 2.5.dp.toPx(),
                        center = Offset(px, py)
                    )

                    // X label at selected positions
                    if (i in xLabelIndices) {
                        drawContext.canvas.nativeCanvas.drawText(
                            point.dateLabel,
                            px,
                            size.height - 4.dp.toPx(),
                            textPaint
                        )
                    }
                }
            }
    )
}

// ── Log table ────────────────────────────────────────────────────────────────

private val logDateFmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

@Composable
private fun LogTableHeader(isTimed: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("DATE", Modifier.weight(2f), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline)
            Text("SET", Modifier.weight(0.6f), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
            Text("KG", Modifier.weight(0.8f), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.End)
            Text(
                if (isTimed) "SECS" else "REPS",
                Modifier.weight(0.8f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun LogTableRow(log: HistoryLog, isTimed: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            logDateFmt.format(Date(log.timestamp)),
            Modifier.weight(2f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            "${log.setNumber}",
            Modifier.weight(0.6f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            if (log.weightKg > 0f) formatWeight(log.weightKg) else "—",
            Modifier.weight(0.8f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
        Text(
            if (isTimed) formatSeconds(log.reps) else "${log.reps}",
            Modifier.weight(0.8f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.surfaceVariant,
        thickness = 0.5.dp
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatWeight(kg: Float): String =
    if (kg % 1 == 0f) kg.toInt().toString() else "%.1f".format(kg)

private fun formatSeconds(secs: Int): String {
    val m = secs / 60
    val s = secs % 60
    return if (m > 0) "%d:%02d".format(m, s) else "${s}s"
}