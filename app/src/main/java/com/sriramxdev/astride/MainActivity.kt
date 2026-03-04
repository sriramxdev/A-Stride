package com.sriramxdev.astride

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.sriramxdev.astride.ui.theme.AStrideTheme
import com.sriramxdev.astride.ui.theme.AccentBlue
import com.sriramxdev.astride.ui.theme.BackgroundBlack
import com.sriramxdev.astride.ui.theme.SurfaceDark
import com.sriramxdev.astride.ui.theme.TextPrimary
import com.sriramxdev.astride.ui.theme.TextSecondary
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null

    // UI State
    private var currentSteps by mutableStateOf(0)
    private var initialSteps by mutableStateOf(-1f)
    // Inside MainActivity class
    private var hourlySteps = mutableStateListOf(0f, 0f, 0f, 0f, 0f, 0f) // Last 6 hours

    private fun updateHourlyData(currentDaySteps: Int) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        // Simple logic: Update the last element of the list with current steps
        // In a real app, we would map 'hour' to an index
        hourlySteps[hourlySteps.size - 1] = currentDaySteps.toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. Tell the Window to ignore the "safe area" blocks
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // 1. Initialize SharedPreferences safely
        val prefs = getSharedPreferences("AStridePrefs", Context.MODE_PRIVATE)
        initialSteps = prefs.getFloat("start_steps", -1f)

        // 2. Initialize Sensors with null-safety
        try {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        } catch (e: Exception) {
            Log.e("A-Stride", "Sensor initialization failed", e)
        }
        enableEdgeToEdge() // Import: androidx.activity.enableEdgeToEdge

        setContent {
            AStrideTheme {
                // Use Scaffolding to handle the "Insets" (Notch and Nav bar)
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = BackgroundBlack // This ensures the notch area is black
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding), // This pushes the content away from the physical notch
                        contentAlignment = Alignment.Center
                    ) {
                        MainDashboard(
                            stepCount = currentSteps,
                            target = 6000,
                            onReset = {
                                // This clears the memory and resets the UI to 0
                                initialSteps = -1f
                                getSharedPreferences("AStridePrefs", Context.MODE_PRIVATE).edit().clear().apply()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSensorSteps = event.values[0]

            // 1. Get SharedPreferences safely
            val prefs = getSharedPreferences("AStridePrefs", Context.MODE_PRIVATE)

            // 2. If initialSteps is -1, try to load it from memory
            if (initialSteps == -1f) {
                initialSteps = prefs.getFloat("start_steps", -1f)
            }

            // 3. If it's STILL -1 (first time ever), save the current sensor value
            if (initialSteps == -1f) {
                initialSteps = totalSensorSteps
                prefs.edit().putFloat("start_steps", initialSteps).apply() // Fixed .edit()
            }

            // 4. Update the UI State
            val delta = (totalSensorSteps - initialSteps).toInt()
            currentSteps = if (delta < 0) 0 else delta
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        stepSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        // Stay registered if you want background counting, but for now, we unregister to save battery
        sensorManager?.unregisterListener(this)
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun MainDashboard(stepCount: Int, target: Int, onReset: () -> Unit) {
    val progress = (stepCount.toFloat() / target).coerceIn(0f, 1f)
    val distanceKm = "%.2f".format((stepCount * 0.73) / 1000.0)
    val calories = "%.0f".format(stepCount * 0.04)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp, start = 24.dp, end = 24.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- THE GLOWY RING (Top) ---
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
            Canvas(modifier = Modifier.size(240.dp)) {
                val strokeWidth = 14.dp.toPx()
                val radius = size.minDimension / 2
                val sweepAngle = 360f * progress

                drawArc(color = SurfaceDark, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                drawArc(color = AccentBlue.copy(alpha = 0.2f), startAngle = -90f, sweepAngle = sweepAngle, useCenter = false, style = Stroke(width = strokeWidth + 12f, cap = StrokeCap.Round))
                drawArc(color = AccentBlue, startAngle = -90f, sweepAngle = sweepAngle, useCenter = false, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

                if (progress > 0f) {
                    val angleRad = (sweepAngle - 90f) * (Math.PI / 180f).toFloat()
                    val x = center.x + radius * cos(angleRad)
                    val y = center.y + radius * sin(angleRad)
                    drawCircle(color = Color.White, radius = 7.dp.toPx(), center = Offset(x, y))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stepCount.toString(), style = MaterialTheme.typography.displayLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(text = "of $target steps", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(64.dp))

        // --- STATS ROW ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatItem("Distance", "$distanceKm km")
            StatItem("Calories", "$calories kcal")
            StatItem("Avg Pace", "0'00''")
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- THE RESET BUTTON (Bottom) ---
        // Helpful for your morning walk test
        TextButton(onClick = onReset) {
            Text("RESET SESSION", color = AccentBlue, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun PaceGraph(data: List<Float>, modifier: Modifier = Modifier) {
    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 24f
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    Canvas(modifier = modifier) {
        val padding = 40f // Space for labels
        val graphWidth = size.width - padding
        val graphHeight = size.height - padding
        val spacing = graphWidth / (data.size - 1).coerceAtLeast(1)
        val maxVal = data.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val heightMultiplier = graphHeight / maxVal

        // 1. DRAW AXES (White)
        // Y-Axis
        drawLine(color = Color.White, start = Offset(padding, 0f), end = Offset(padding, graphHeight), strokeWidth = 2.dp.toPx())
        // X-Axis
        drawLine(color = Color.White, start = Offset(padding, graphHeight), end = Offset(size.width, graphHeight), strokeWidth = 2.dp.toPx())

        // 2. DRAW LABELS (Hours and Steps)
        drawContext.canvas.nativeCanvas.apply {
            // Y-Axis Label (Max steps)
            drawText(maxVal.toInt().toString(), padding - 10f, 20f, textPaint.apply { textAlign = android.graphics.Paint.Align.RIGHT })
            // X-Axis Labels (Time slots)
            data.forEachIndexed { index, _ ->
                val x = padding + (index * spacing)
                drawText("${index}h", x, size.height, textPaint.apply { textAlign = android.graphics.Paint.Align.CENTER })
            }
        }

        // 3. DRAW DATA (Only if data exists and isn't all zeros)
        if (data.any { it > 0 }) {
            val spacePath = Path().apply {
                data.forEachIndexed { index, value ->
                    val x = padding + (index * spacing)
                    val y = graphHeight - (value * heightMultiplier)
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }

            drawPath(path = spacePath, color = AccentBlue, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

            data.forEachIndexed { index, value ->
                val x = padding + (index * spacing)
                val y = graphHeight - (value * heightMultiplier)
                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(x, y))
            }
        }
    }
}

fun moveTo(x: Float, y: Float) {}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)

    }
}