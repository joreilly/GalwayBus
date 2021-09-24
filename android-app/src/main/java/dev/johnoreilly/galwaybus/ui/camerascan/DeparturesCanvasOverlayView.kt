@file:OptIn(ExperimentalTime::class)

package dev.johnoreilly.galwaybus.ui.camerascan

import android.graphics.Typeface
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.surrus.galwaybus.common.model.BusStop
import dev.johnoreilly.galwaybus.R
import dev.johnoreilly.galwaybus.ui.viewmodel.GalwayBusViewModel
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DeparturesCanvasOverlayView (galwayBusViewModel: GalwayBusViewModel, busStop: BusStop, detectedText: String) {

    val departureList by galwayBusViewModel.busDepartureList.collectAsState(emptyList())
    val dueString = stringResource(R.string.bus_time_due)
    val context = LocalContext.current

    val paint = Paint().asFrameworkPaint()
    Canvas(modifier = Modifier.fillMaxSize()) {
        paint.apply {
            isAntiAlias = true
            textSize = 24.sp.toPx()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.WHITE
        }


        drawRect(
            color = Color.Black,
            topLeft = Offset(28f, 28f),
            size = Size(this.size.width - 56f, 700f),
            alpha = 0.1f
        )

        drawContext.canvas.nativeCanvas.drawText("$detectedText (${busStop.shortName})", 50f, 150f, paint)

        paint.textSize = 16.sp.toPx()
        departureList.forEachIndexed { index, departure ->
            val yPos = 150f + (index + 1)*100f
            drawContext.canvas.nativeCanvas.drawText(departure.timetableId, 50f, yPos, paint)

            drawContext.canvas.nativeCanvas.drawText(departure.displayName, 150f, yPos, paint)

            val minutesUntilDeparture = departure.durationUntilDeparture.inWholeMinutes.toInt()
            val departureText = if (minutesUntilDeparture <= 0)
                dueString
            else
                "$minutesUntilDeparture ${context.resources.getQuantityString(R.plurals.mins, minutesUntilDeparture)}"

            drawContext.canvas.nativeCanvas.drawText(departureText, 850f, yPos, paint)
        }
    }
}

