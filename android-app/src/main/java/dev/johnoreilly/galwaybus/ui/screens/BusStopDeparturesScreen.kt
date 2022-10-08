package dev.johnoreilly.galwaybus.ui

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.surrus.galwaybus.common.model.GalwayBusDeparture
import dev.johnoreilly.galwaybus.R
import dev.johnoreilly.galwaybus.ui.utils.quantityStringResource


@Composable
fun BusStopDeparture(departure: GalwayBusDeparture, departureSelected : (departure : GalwayBusDeparture) -> Unit) {
    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
        Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = { departureSelected(departure) })
                    .padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {

            Text(departure.timetableId, fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp))

            Text(departure.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 16.dp))


            val minutesUntilDeparture = departure.durationUntilDeparture.inWholeMinutes.toInt()
            val departureText = if (minutesUntilDeparture <= 0)
                stringResource(R.string.bus_time_due)
            else
                "$minutesUntilDeparture ${quantityStringResource(R.plurals.mins, minutesUntilDeparture)}"
            Text(departureText, modifier = Modifier.padding(start = 16.dp))
        }
    }
}