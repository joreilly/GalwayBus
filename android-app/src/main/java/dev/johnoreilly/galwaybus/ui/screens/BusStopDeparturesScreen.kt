package dev.johnoreilly.galwaybus.ui

import androidx.compose.material.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.surrus.galwaybus.common.GalwayBusDeparture
import dev.johnoreilly.galwaybus.R
import dev.johnoreilly.galwaybus.ui.utils.quantityStringResource


@Composable
fun BusStopDeparture(departure: GalwayBusDeparture) {
    ProvideTextStyle(typography.subtitle1) {
        Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {

            Text(departure.timetableId, fontWeight = FontWeight.Bold,
                    modifier = Modifier.preferredWidth(36.dp))

            Text(departure.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 16.dp))


            val minutesUntilDeparture = departure.durationUntilDeparture.inMinutes.toInt()
            val departureText = if (minutesUntilDeparture <= 0)
                stringResource(R.string.bus_time_due)
            else
                "$minutesUntilDeparture ${quantityStringResource(R.plurals.mins, minutesUntilDeparture)}"
            Text(departureText, modifier = Modifier.padding(start = 16.dp))
        }
    }
}