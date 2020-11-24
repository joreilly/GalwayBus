package dev.johnoreilly.galwaybus.ui

import androidx.compose.material.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.surrus.galwaybus.common.GalwayBusDeparture


@Composable
fun BusStopDeparture(departure: GalwayBusDeparture) {
    Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
        Text(departure.timetableId, modifier = Modifier.preferredWidth(48.dp), style = typography.subtitle1)

        Text(departure.displayName, modifier = Modifier.weight(1f).padding(start = 16.dp), style = typography.subtitle1)

        val minutesUntilDeparture = departure.durationUntilDeparture.inMinutes.toInt()
        val departureText = if (minutesUntilDeparture == 0)
            "Due"
        else
            "${minutesUntilDeparture} mins"
        Text(departureText, modifier = Modifier.padding(start = 16.dp), style = typography.subtitle1)
    }
}