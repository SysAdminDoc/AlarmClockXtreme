package com.sysadmindoc.alarmclock.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.data.remote.GeocodingResult
import com.sysadmindoc.alarmclock.data.repository.CalendarEvent
import com.sysadmindoc.alarmclock.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .verticalScroll(rememberScrollState())
    ) {
        // Header with date
        DashboardHeader(state.todayDate)

        // Weather card
        WeatherCard(
            state = state,
            onChangeLocation = viewModel::showLocationPicker
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Forecast row
        if (state.forecast.isNotEmpty()) {
            ForecastRow(state.forecast)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Calendar section
        CalendarSection(state)

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Location picker dialog
    if (state.showLocationPicker) {
        LocationPickerDialog(
            results = state.locationSearchResults,
            isSearching = state.locationSearching,
            onSearch = viewModel::searchLocation,
            onSelect = viewModel::selectLocation,
            onUseDevice = viewModel::useDeviceLocation,
            onDismiss = viewModel::hideLocationPicker
        )
    }
}

@Composable
private fun DashboardHeader(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(HeaderTop, HeaderBottom))
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column {
            Text(
                text = "My Day",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = date,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun WeatherCard(state: DashboardUiState, onChangeLocation: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        if (state.weatherLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentBlue, strokeWidth = 2.dp)
            }
        } else if (state.weatherError != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CloudOff,
                    contentDescription = "Weather unavailable",
                    tint = TextMuted,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.weatherError,
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onChangeLocation,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue)
                ) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Set Location")
                }
            }
        } else {
            Column(modifier = Modifier.padding(20.dp)) {
                // Location name row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            state.locationName.ifBlank { "Weather" },
                            color = AccentBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onChangeLocation,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Edit, "Change location", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
                // Main temp row
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Weather icon
                    Icon(
                        imageVector = weatherIconFor(state.weatherIcon),
                        contentDescription = state.weatherDescription,
                        tint = weatherColorFor(state.weatherIcon),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))

                    // Temperature
                    Column {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = state.temperature,
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Light,
                                color = TextPrimary
                            )
                            Text(
                                text = "\u00B0${state.tempUnit}",
                                fontSize = 20.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Text(
                            text = state.weatherDescription,
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // High/Low
                    Column(horizontalAlignment = Alignment.End) {
                        Row {
                            Icon(Icons.Default.ArrowUpward, null, tint = AccentRed, modifier = Modifier.size(16.dp))
                            Text("${state.highTemp}\u00B0", color = TextPrimary, fontSize = 14.sp)
                        }
                        Row {
                            Icon(Icons.Default.ArrowDownward, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                            Text("${state.lowTemp}\u00B0", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = TextMuted.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(12.dp))

                // Detail row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    WeatherDetail(Icons.Default.Thermostat, state.feelsLike)
                    WeatherDetail(Icons.Default.WaterDrop, state.humidity)
                    WeatherDetail(Icons.Default.Air, state.windSpeed)
                    if (state.precipChance.isNotBlank()) {
                        WeatherDetail(Icons.Default.Umbrella, state.precipChance)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherDetail(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TextMuted, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(value, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun ForecastRow(forecast: List<ForecastDay>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        forecast.forEach { day ->
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(day.dayName, color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${day.high}/${day.low}", color = TextPrimary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    if (day.precipChance.isNotBlank()) {
                        Text(day.precipChance, color = TextMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarSection(state: DashboardUiState) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Today's Schedule",
            style = MaterialTheme.typography.labelLarge,
            color = AccentBlue,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (state.calendarPermissionNeeded) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarMonth, null, tint = TextMuted)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Calendar permission needed to show events", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else if (state.calendarEvents.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.EventAvailable, null, tint = DismissGreen)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("No events scheduled today", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            state.calendarEvents.forEach { event ->
                EventCard(event)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun EventCard(event: CalendarEvent) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceMedium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Color indicator bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (event.calendarColor != 0) Color(event.calendarColor)
                        else AccentBlue
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = event.timeRange,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                if (event.location.isNotBlank()) {
                    Text(
                        text = event.location,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun weatherIconFor(icon: String): ImageVector = when (icon) {
    "clear" -> Icons.Default.WbSunny
    "partly_cloudy" -> Icons.Default.WbCloudy
    "cloudy" -> Icons.Default.Cloud
    "fog" -> Icons.Default.Cloud
    "drizzle", "rain", "showers" -> Icons.Default.WaterDrop
    "snow" -> Icons.Default.AcUnit
    "thunderstorm" -> Icons.Default.Bolt
    else -> Icons.Default.Cloud
}

private fun weatherColorFor(icon: String): Color = when (icon) {
    "clear" -> SnoozeYellow
    "partly_cloudy" -> BlueLight
    "thunderstorm" -> AccentRed
    "snow" -> TextPrimary
    else -> TextSecondary
}

@Composable
private fun LocationPickerDialog(
    results: List<GeocodingResult>,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    onSelect: (GeocodingResult) -> Unit,
    onUseDevice: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        title = {
            Text("Set Weather Location", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { newQuery ->
                        query = newQuery
                        if (newQuery.length >= 2) onSearch(newQuery)
                    },
                    placeholder = { Text("City name or zip code", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = SurfaceCard,
                        cursorColor = AccentBlue,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Use device location button
                TextButton(
                    onClick = onUseDevice,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.MyLocation, null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Use device location", color = AccentBlue)
                }

                if (isSearching) {
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentBlue, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    }
                } else if (results.isNotEmpty()) {
                    HorizontalDivider(color = TextMuted.copy(alpha = 0.15f))
                    LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                        items(results) { result ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(result) }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, null, tint = TextMuted, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        result.name ?: "Unknown",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        buildString {
                                            if (!result.state.isNullOrBlank()) append("${result.state}, ")
                                            append(result.country ?: "")
                                        },
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                } else if (query.length >= 2) {
                    Text("No results found", color = TextMuted, modifier = Modifier.padding(8.dp))
                }
            }
        },
        containerColor = SurfaceMedium
    )
}
