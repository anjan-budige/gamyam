package com.gamyam.app.ui.tracker

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamyam.app.R
import com.gamyam.app.ServiceTripDetail
import com.gamyam.app.data.ApiClient
import com.gamyam.app.data.AuthManager
import com.gamyam.app.data.BusTripDto
import com.gamyam.app.data.LiveVehicleDto
import com.gamyam.app.data.PlannedServiceDto
import com.gamyam.app.data.TripDetailDto
import com.gamyam.app.data.TripPointDto
import com.gamyam.app.theme.AppBarBlue
import com.gamyam.app.ui.main.PoppinsFamily
import com.google.gson.JsonElement
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val GPS_ROUTE_TOLERANCE_METERS = 350.0

sealed class ServicesResultState {
    object Loading : ServicesResultState()
    data class Success(
        val title: String,
        val subtitle: String,
        val rows: List<ServiceResultRow>
    ) : ServicesResultState()
    data class Error(val message: String) : ServicesResultState()
}

sealed class LiveServiceState {
    object Loading : LiveServiceState()
    data class Success(
        val snapshot: LiveServiceSnapshot,
        val lastError: String = ""
    ) : LiveServiceState()
    data class Error(val message: String) : LiveServiceState()
}

data class LiveServiceSnapshot(
    val detail: TripDetailDto,
    val liveVehicle: LiveVehicleDto?,
    val displayNumber: String,
    val selectedFromName: String,
    val selectedToName: String,
    val loadedAtMillis: Long,
    val refreshing: Boolean = false
)

data class ServiceResultRow(
    val serviceId: Int,
    val serviceName: String,
    val serviceType: String,
    val vehicleId: Int = 0,
    val vehicleNumber: String = "",
    val fromName: String = "",
    val toName: String = "",
    val departureTime: Long? = null,
    val arrivalTime: Long? = null,
    val durationSeconds: Int = 0,
    val depot: String = "",
    val live: Boolean = false
)

private enum class ServiceSortMode(val label: String) {
    ARRIVAL("Arrival"),
    DURATION("Duration")
}

private enum class LiveSheetLevel {
    COLLAPSED,
    PARTIAL,
    EXPANDED
}

private fun LiveSheetLevel.panelHeight(): androidx.compose.ui.unit.Dp =
    when (this) {
        LiveSheetLevel.COLLAPSED -> 126.dp
        LiveSheetLevel.PARTIAL -> 386.dp
        LiveSheetLevel.EXPANDED -> 560.dp
    }

private val TimelineHorizontalPadding = 16.dp
private val TimelineArrivalWidth = 54.dp
private val TimelineRailWidth = 36.dp
private val TimelineDepartureWidth = 82.dp
private const val GamyamInstallLink = "https://innovnext.tech/gamyam.apk"

private enum class AlarmSheetMode {
    CHOICE,
    TIME,
    STATION,
    UNAVAILABLE
}

private data class BusAlarmSpec(
    val triggerAtMillis: Long,
    val title: String,
    val message: String,
    val serviceId: Int
)

private data class LiveTrackerColors(
    val background: Color,
    val rowBackground: Color,
    val currentRow: Color,
    val header: Color,
    val rail: Color,
    val stopDot: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val panel: Color,
    val panelTitle: Color,
    val panelSubtitle: Color,
    val sheetContainer: Color
)

private val LightLiveTrackerColors = LiveTrackerColors(
    background = Color(0xFFEAF4FF),
    rowBackground = Color(0xFFF2F2F2),
    currentRow = Color(0xFFD7E3ED),
    header = Color(0xDD1D1D1D),
    rail = Color(0xFF98D7EE),
    stopDot = AppBarBlue,
    primaryText = Color(0xFF1F1F1F),
    secondaryText = Color(0xFF343434),
    mutedText = Color(0xFF6A6F75),
    panel = Color(0xFFEAF4FF),
    panelTitle = Color(0xFF18864B),
    panelSubtitle = Color(0xFF4E5965),
    sheetContainer = Color(0xFFEAF4FF)
)

private val DarkLiveTrackerColors = LiveTrackerColors(
    background = Color(0xFF111111),
    rowBackground = Color(0xFF111111),
    currentRow = Color(0xFF60737F),
    header = Color.Black,
    rail = Color(0xFF14536B),
    stopDot = Color(0xFFA8CDFB),
    primaryText = Color.White,
    secondaryText = Color.White.copy(alpha = 0.92f),
    mutedText = Color.White.copy(alpha = 0.72f),
    panel = Color(0xFF202B3D),
    panelTitle = Color(0xFFFF8A80),
    panelSubtitle = Color.White.copy(alpha = 0.76f),
    sheetContainer = Color(0xFF202B3D)
)

private val LocalLiveTrackerColors = staticCompositionLocalOf { LightLiveTrackerColors }

@Composable
private fun liveTrackerColors(): LiveTrackerColors =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) DarkLiveTrackerColors else LightLiveTrackerColors

class TrackerViewModel(private val authManager: AuthManager) : ViewModel() {
    private val _state = MutableStateFlow<ServicesResultState>(ServicesResultState.Loading)
    val state: StateFlow<ServicesResultState> = _state.asStateFlow()

    private val _resultRefreshing = MutableStateFlow(false)
    val resultRefreshing: StateFlow<Boolean> = _resultRefreshing.asStateFlow()

    private val _liveServiceState = MutableStateFlow<LiveServiceState>(LiveServiceState.Loading)
    val liveServiceState: StateFlow<LiveServiceState> = _liveServiceState.asStateFlow()

    private var resultRequestToken = 0
    private var activeResultKey = ""
    private var liveServiceRequestToken = 0
    private var liveRefreshJob: Job? = null

    fun ensureFromTo(
        fromId: String,
        fromName: String,
        fromGeozoneType: String,
        toId: String,
        toName: String,
        toGeozoneType: String,
        operationType: Int,
        cityId: String,
        isAirport: Boolean
    ) {
        val key = fromToResultKey(fromId, fromGeozoneType, toId, toGeozoneType, operationType, cityId, isAirport)
        if (hasReusableResult(key)) return
        loadFromTo(fromId, fromName, fromGeozoneType, toId, toName, toGeozoneType, operationType, cityId, isAirport)
    }

    fun ensureRoute(routeNumber: String, cityId: String, operationType: Int, isAirport: Boolean) {
        val key = routeResultKey(routeNumber, cityId, operationType, isAirport)
        if (hasReusableResult(key)) return
        loadRoute(routeNumber, cityId, operationType, isAirport)
    }

    fun ensureService(reservationServiceId: Int, displayNumber: String, operationType: Int) {
        val key = serviceResultKey(reservationServiceId, operationType)
        if (hasReusableResult(key)) return
        loadService(reservationServiceId, displayNumber, operationType)
    }

    fun ensureVehicle(vehicleId: Int, registrationNumber: String, serviceType: String, operationType: Int) {
        val key = vehicleResultKey(vehicleId, operationType)
        if (hasReusableResult(key)) return
        loadVehicle(vehicleId, registrationNumber, serviceType, operationType)
    }

    private fun hasReusableResult(key: String): Boolean =
        activeResultKey == key && _state.value !is ServicesResultState.Error

    private fun fromToResultKey(
        fromId: String,
        fromGeozoneType: String,
        toId: String,
        toGeozoneType: String,
        operationType: Int,
        cityId: String,
        isAirport: Boolean
    ): String = listOf(
        "from-to",
        operationType.toString(),
        cityId.trim(),
        isAirport.toString(),
        fromId.trim(),
        fromGeozoneType.trim(),
        toId.trim(),
        toGeozoneType.trim()
    ).joinToString("|")

    private fun routeResultKey(routeNumber: String, cityId: String, operationType: Int, isAirport: Boolean): String =
        listOf("route", operationType.toString(), cityId.trim(), isAirport.toString(), routeNumber.trim()).joinToString("|")

    private fun serviceResultKey(reservationServiceId: Int, operationType: Int): String =
        listOf("reservation-service", operationType.toString(), reservationServiceId.toString()).joinToString("|")

    private fun vehicleResultKey(vehicleId: Int, operationType: Int): String =
        listOf("vehicle", operationType.toString(), vehicleId.toString()).joinToString("|")

    fun loadFromTo(
        fromId: String,
        fromName: String,
        fromGeozoneType: String,
        toId: String,
        toName: String,
        toGeozoneType: String,
        operationType: Int,
        cityId: String,
        isAirport: Boolean,
        showLoading: Boolean = true
    ) {
        val resultKey = fromToResultKey(fromId, fromGeozoneType, toId, toGeozoneType, operationType, cityId, isAirport)
        activeResultKey = resultKey
        val requestToken = ++resultRequestToken
        val previousState = _state.value
        viewModelScope.launch {
            if (showLoading || _state.value !is ServicesResultState.Success) {
                _state.value = ServicesResultState.Loading
            } else {
                _resultRefreshing.value = true
            }
            try {
                val plannedCall = async {
                    runCatching {
                        withTimeoutOrNull(15_000L) {
                            authManager.authenticatedCall { auth ->
                                ApiClient.tgsrtcApiService.getPlannedTripsBetweenLocations(
                                    auth = auth,
                                    fromGeozoneType = fromGeozoneType,
                                    toGeozoneType = toGeozoneType,
                                    from = fromId,
                                    to = toId,
                                    cityId = cityId,
                                    operationType = operationType,
                                    isAirport = isAirport
                                )
                            }
                        }.orEmpty()
                    }.getOrDefault(emptyList())
                }
                val runningCall = async {
                    runCatching {
                        withTimeoutOrNull(15_000L) {
                            authManager.authenticatedCall { auth ->
                                ApiClient.tgsrtcApiService.getRunningTripsBetweenLocations(
                                    auth = auth,
                                    fromGeozoneType = fromGeozoneType,
                                    toGeozoneType = toGeozoneType,
                                    from = fromId,
                                    to = toId,
                                    cityId = cityId,
                                    operationType = operationType,
                                    isAirport = isAirport
                                ).trips
                            }
                        }.orEmpty()
                    }.getOrDefault(emptyList())
                }
                if (requestToken != resultRequestToken) return@launch
                _state.value = ServicesResultState.Success(
                    title = "Search results",
                    subtitle = "$fromName -> $toName",
                    rows = mergeRows(plannedCall.await(), runningCall.await())
                )
            } catch (e: Exception) {
                if (requestToken != resultRequestToken) return@launch
                _state.value = if (!showLoading && previousState is ServicesResultState.Success) {
                    previousState
                } else {
                    ServicesResultState.Error(e.message ?: "Unable to load services")
                }
            } finally {
                if (requestToken == resultRequestToken) {
                    _resultRefreshing.value = false
                }
            }
        }
    }

    fun loadRoute(routeNumber: String, cityId: String, operationType: Int, isAirport: Boolean, showLoading: Boolean = true) {
        val normalizedRoute = routeNumber.trim()
        activeResultKey = routeResultKey(normalizedRoute, cityId, operationType, isAirport)
        val requestToken = ++resultRequestToken
        val previousState = _state.value
        if (normalizedRoute.isBlank()) {
            _state.value = ServicesResultState.Success(
                title = "Route services",
                subtitle = "Route number required",
                rows = emptyList()
            )
            return
        }
        viewModelScope.launch {
            if (showLoading || _state.value !is ServicesResultState.Success) {
                _state.value = ServicesResultState.Loading
            } else {
                _resultRefreshing.value = true
            }
            try {
                val plannedCall = async {
                    runCatching {
                        withTimeoutOrNull(15_000L) {
                            authManager.authenticatedCall { auth ->
                                ApiClient.tgsrtcApiService.getPlannedTripsByRouteNumber(
                                    auth = auth,
                                    routeNumber = normalizedRoute,
                                    cityId = cityId,
                                    operationType = operationType,
                                    isAirport = isAirport
                                )
                            }
                        }.orEmpty()
                    }.getOrDefault(emptyList())
                }
                val runningCall = async {
                    runCatching {
                        withTimeoutOrNull(15_000L) {
                            authManager.authenticatedCall { auth ->
                                ApiClient.tgsrtcApiService.getRunningTripsByRouteNumber(
                                    auth = auth,
                                    routeNumber = normalizedRoute,
                                    cityId = cityId,
                                    operationType = operationType,
                                    isAirport = isAirport
                                )
                            }
                        }.orEmpty()
                    }.getOrDefault(emptyList())
                }
                if (requestToken != resultRequestToken) return@launch
                _state.value = ServicesResultState.Success(
                    title = normalizedRoute,
                    subtitle = "Route services",
                    rows = mergeRows(plannedCall.await(), runningCall.await())
                )
            } catch (e: Exception) {
                if (requestToken != resultRequestToken) return@launch
                _state.value = if (!showLoading && previousState is ServicesResultState.Success) {
                    previousState
                } else {
                    ServicesResultState.Error(e.message ?: "Unable to load route services")
                }
            } finally {
                if (requestToken == resultRequestToken) {
                    _resultRefreshing.value = false
                }
            }
        }
    }

    fun loadService(serviceId: Int, displayNumber: String, operationType: Int, showLoading: Boolean = true) {
        activeResultKey = serviceResultKey(serviceId, operationType)
        val requestToken = ++resultRequestToken
        val previousState = _state.value
        if (serviceId <= 0) {
            _state.value = ServicesResultState.Success(
                title = displayNumber.ifBlank { "Reservation service" },
                subtitle = "Service number required",
                rows = emptyList()
            )
            return
        }
        viewModelScope.launch {
            if (showLoading || _state.value !is ServicesResultState.Success) {
                _state.value = ServicesResultState.Loading
            } else {
                _resultRefreshing.value = true
            }
            try {
                val plannedCall = async {
                    runCatching {
                        withTimeoutOrNull(15_000L) {
                            authManager.authenticatedCall { auth ->
                                ApiClient.tgsrtcApiService.getPlannedServiceById(auth, serviceId.toString())
                                    .map { it.toBusTrip() }
                            }
                        }.orEmpty()
                    }.getOrDefault(emptyList())
                }
                val runningCall = async {
                    runCatching {
                        withTimeoutOrNull(15_000L) {
                            authManager.authenticatedCall { auth ->
                                ApiClient.tgsrtcApiService.getRunningTripsByService(auth, serviceId.toString(), operationType)
                            }
                        }.orEmpty()
                    }.getOrDefault(emptyList())
                }
                if (requestToken != resultRequestToken) return@launch
                _state.value = ServicesResultState.Success(
                    title = displayNumber.ifBlank { serviceId.toString() },
                    subtitle = "Live and planned services",
                    rows = mergeRows(plannedCall.await(), runningCall.await())
                )
            } catch (e: Exception) {
                if (requestToken != resultRequestToken) return@launch
                _state.value = if (!showLoading && previousState is ServicesResultState.Success) {
                    previousState
                } else {
                    ServicesResultState.Error(e.message ?: "Unable to load reservation services")
                }
            } finally {
                if (requestToken == resultRequestToken) {
                    _resultRefreshing.value = false
                }
            }
        }
    }

    fun loadLiveService(
        serviceId: Int,
        displayNumber: String,
        selectedFromName: String,
        selectedToName: String,
        showLoading: Boolean
    ) {
        val requestToken = ++liveServiceRequestToken
        viewModelScope.launch {
            val previous = _liveServiceState.value as? LiveServiceState.Success
            val sameService = previous?.snapshot?.detail?.serviceId == serviceId ||
                previous?.snapshot?.displayNumber == displayNumber
            if (showLoading || !sameService) {
                _liveServiceState.value = LiveServiceState.Loading
            }
            try {
                val detail = authManager.authenticatedCall { auth ->
                    ApiClient.tgsrtcApiService.getServiceDetails(auth, serviceId.toString())
                }
                val liveResult = if (detail.vehicleId > 0) {
                    runCatching {
                        authManager.authenticatedCall { auth ->
                            ApiClient.tgsrtcApiService.getLiveVehicle(auth, detail.vehicleId.toString())
                        }
                    }
                } else {
                    Result.success(null)
                }
                if (requestToken != liveServiceRequestToken) return@launch
                _liveServiceState.value = LiveServiceState.Success(
                    snapshot = LiveServiceSnapshot(
                        detail = detail,
                        liveVehicle = liveResult.getOrNull(),
                        displayNumber = displayNumber.ifBlank {
                            detail.serviceId.takeIf { it > 0 }?.toString().orEmpty()
                        },
                        selectedFromName = selectedFromName,
                        selectedToName = selectedToName,
                        loadedAtMillis = System.currentTimeMillis()
                    ),
                    lastError = liveResult.exceptionOrNull()?.message.orEmpty()
                )
            } catch (e: Exception) {
                if (requestToken != liveServiceRequestToken) return@launch
                if (previous != null && !showLoading) {
                    _liveServiceState.value = previous.copy(lastError = e.message ?: "Unable to refresh live status")
                } else {
                    _liveServiceState.value = LiveServiceState.Error(e.message ?: "Unable to load service")
                }
            }
        }
    }

    fun refreshLiveService(
        serviceId: Int,
        displayNumber: String,
        selectedFromName: String,
        selectedToName: String
    ) {
        val current = _liveServiceState.value as? LiveServiceState.Success
        if (current != null) {
            _liveServiceState.value = current.copy(
                snapshot = current.snapshot.copy(refreshing = true),
                lastError = ""
            )
        }
        loadLiveService(
            serviceId = serviceId,
            displayNumber = displayNumber,
            selectedFromName = selectedFromName,
            selectedToName = selectedToName,
            showLoading = false
        )
    }

    fun startLiveServiceAutoRefresh(
        serviceId: Int,
        displayNumber: String,
        selectedFromName: String,
        selectedToName: String
    ) {
        liveRefreshJob?.cancel()
        liveRefreshJob = viewModelScope.launch {
            var first = true
            while (true) {
                loadLiveService(
                    serviceId = serviceId,
                    displayNumber = displayNumber,
                    selectedFromName = selectedFromName,
                    selectedToName = selectedToName,
                    showLoading = first
                )
                first = false
                delay(5_000L)
            }
        }
    }

    fun stopLiveServiceAutoRefresh() {
        liveRefreshJob?.cancel()
        liveRefreshJob = null
    }

    fun loadVehicle(vehicleId: Int, registrationNumber: String, serviceType: String, operationType: Int, showLoading: Boolean = true) {
        activeResultKey = vehicleResultKey(vehicleId, operationType)
        val requestToken = ++resultRequestToken
        val previousState = _state.value
        viewModelScope.launch {
            if (showLoading || _state.value !is ServicesResultState.Success) {
                _state.value = ServicesResultState.Loading
            } else {
                _resultRefreshing.value = true
            }
            try {
                val trips = authManager.authenticatedCall { auth ->
                    ApiClient.tgsrtcApiService.getTripsByVehicle(auth, vehicleId.toString(), operationType)
                }
                if (requestToken != resultRequestToken) return@launch
                _state.value = ServicesResultState.Success(
                    title = registrationNumber,
                    subtitle = serviceType.ifBlank { "Bus services" },
                    rows = mergeRows(planned = emptyList(), running = trips)
                )
            } catch (e: Exception) {
                if (requestToken != resultRequestToken) return@launch
                _state.value = if (!showLoading && previousState is ServicesResultState.Success) {
                    previousState
                } else {
                    ServicesResultState.Error(e.message ?: "Unable to load vehicle services")
                }
            } finally {
                if (requestToken == resultRequestToken) {
                    _resultRefreshing.value = false
                }
            }
        }
    }

    private fun mergeRows(planned: List<BusTripDto>, running: List<BusTripDto>): List<ServiceResultRow> {
        val liveByService = running.associateBy { it.serviceId }
        val plannedRows = planned.map { it.toResultRow(live = liveByService.containsKey(it.serviceId)) }
        val plannedIds = plannedRows.map { it.serviceId }.toSet()
        val liveOnlyRows = running.filter { it.serviceId !in plannedIds }.map { it.toResultRow(live = true) }
        return (liveOnlyRows + plannedRows).sortedWith(
            compareBy<ServiceResultRow>(
                { it.nextTime() ?: Long.MAX_VALUE },
                { if (it.durationSeconds > 0) it.durationSeconds else Int.MAX_VALUE },
                { it.serviceName.ifBlank { it.serviceId.toString() } }
            )
        )
    }

    private fun PlannedServiceDto.toBusTrip(): BusTripDto =
        BusTripDto(
            serviceId = serviceId,
            serviceName = serviceName,
            serviceType = serviceType,
            duration = duration,
            depot = depot,
            from = from?.let { TripPointDto(id = it.id, name = it.name, plannedTime = it.plannedTime) },
            to = to?.let { TripPointDto(id = it.id, name = it.name, plannedTime = it.plannedTime) }
        )

    private fun BusTripDto.toResultRow(live: Boolean): ServiceResultRow =
        ServiceResultRow(
            serviceId = serviceId,
            serviceName = serviceName.orEmpty(),
            serviceType = serviceType.orEmpty(),
            vehicleId = vehicleId,
            vehicleNumber = vehicleNumber.orEmpty(),
            fromName = from?.name.orEmpty(),
            toName = to?.name.orEmpty(),
            departureTime = firstTime(from, myPoint),
            arrivalTime = firstTime(to),
            durationSeconds = duration,
            depot = depot.orEmpty(),
            live = live
        )

    private fun firstTime(vararg points: TripPointDto?): Long? =
        points.asSequence()
            .flatMap { point -> sequenceOf(point?.eta, point?.etd, point?.plannedTime, point?.plannedExitTime) }
            .filterNotNull()
            .firstOrNull { it > 0L }

    private fun ServiceResultRow.nextTime(): Long? = departureTime ?: arrivalTime
}

class TrackerViewModelFactory(private val authManager: AuthManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrackerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TrackerViewModel(authManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}

@Composable
fun FromToServicesResultScreen(
    fromId: String,
    fromName: String,
    fromGeozoneType: String,
    toId: String,
    toName: String,
    toGeozoneType: String,
    operationType: Int,
    cityId: String,
    isAirport: Boolean,
    requestId: Long,
    authManager: AuthManager,
    onBack: () -> Unit,
    onServiceClick: (ServiceTripDetail) -> Unit,
    viewModel: TrackerViewModel = viewModel(factory = TrackerViewModelFactory(authManager))
) {
    LaunchedEffect(fromId, toId, operationType, cityId, isAirport, requestId) {
        viewModel.ensureFromTo(fromId, fromName, fromGeozoneType, toId, toName, toGeozoneType, operationType, cityId, isAirport)
    }
    ServicesResultShell(
        viewModel = viewModel,
        operationType = operationType,
        selectedFromName = fromName,
        selectedToName = toName,
        onBack = onBack,
        onRefresh = {
            viewModel.loadFromTo(
                fromId = fromId,
                fromName = fromName,
                fromGeozoneType = fromGeozoneType,
                toId = toId,
                toName = toName,
                toGeozoneType = toGeozoneType,
                operationType = operationType,
                cityId = cityId,
                isAirport = isAirport,
                showLoading = false
            )
        },
        onServiceClick = onServiceClick
    )
}

@Composable
fun RouteServicesResultScreen(
    routeNumber: String,
    cityId: String,
    operationType: Int,
    isAirport: Boolean,
    requestId: Long,
    authManager: AuthManager,
    onBack: () -> Unit,
    onServiceClick: (ServiceTripDetail) -> Unit,
    viewModel: TrackerViewModel = viewModel(factory = TrackerViewModelFactory(authManager))
) {
    LaunchedEffect(routeNumber, cityId, operationType, isAirport, requestId) {
        viewModel.ensureRoute(routeNumber, cityId, operationType, isAirport)
    }
    ServicesResultShell(
        viewModel = viewModel,
        operationType = operationType,
        onBack = onBack,
        onRefresh = {
            viewModel.loadRoute(
                routeNumber = routeNumber,
                cityId = cityId,
                operationType = operationType,
                isAirport = isAirport,
                showLoading = false
            )
        },
        onServiceClick = onServiceClick
    )
}

@Composable
fun ReservationServicesResultScreen(
    reservationServiceId: Int,
    displayNumber: String,
    operationType: Int,
    requestId: Long,
    authManager: AuthManager,
    onBack: () -> Unit,
    onServiceClick: (ServiceTripDetail) -> Unit,
    viewModel: TrackerViewModel = viewModel(factory = TrackerViewModelFactory(authManager))
) {
    LaunchedEffect(reservationServiceId, operationType, requestId) {
        viewModel.ensureService(reservationServiceId, displayNumber, operationType)
    }
    ServicesResultShell(
        viewModel = viewModel,
        operationType = operationType,
        onBack = onBack,
        onRefresh = {
            viewModel.loadService(
                serviceId = reservationServiceId,
                displayNumber = displayNumber,
                operationType = operationType,
                showLoading = false
            )
        },
        onServiceClick = onServiceClick
    )
}

@Composable
fun ServiceTripDetailScreen(
    serviceId: Int,
    displayNumber: String,
    operationType: Int,
    fromName: String,
    toName: String,
    authManager: AuthManager,
    onBack: () -> Unit,
    viewModel: TrackerViewModel = viewModel(factory = TrackerViewModelFactory(authManager))
) {
    LaunchedEffect(serviceId, operationType, displayNumber, fromName, toName) {
        viewModel.startLiveServiceAutoRefresh(serviceId, displayNumber, fromName, toName)
    }
    DisposableEffect(serviceId, operationType) {
        onDispose { viewModel.stopLiveServiceAutoRefresh() }
    }
    LiveServiceDetailShell(
        viewModel = viewModel,
        serviceId = serviceId,
        displayNumber = displayNumber,
        selectedFromName = fromName,
        selectedToName = toName,
        onBack = onBack
    )
}

@Composable
fun VehicleTripDetailScreen(
    vehicleId: Int,
    registrationNumber: String,
    serviceType: String,
    operationType: Int,
    requestId: Long,
    authManager: AuthManager,
    onBack: () -> Unit,
    onServiceClick: (ServiceTripDetail) -> Unit,
    viewModel: TrackerViewModel = viewModel(factory = TrackerViewModelFactory(authManager))
) {
    LaunchedEffect(vehicleId, operationType, requestId) {
        viewModel.ensureVehicle(vehicleId, registrationNumber, serviceType, operationType)
    }
    ServicesResultShell(
        viewModel = viewModel,
        operationType = operationType,
        onBack = onBack,
        onRefresh = {
            viewModel.loadVehicle(
                vehicleId = vehicleId,
                registrationNumber = registrationNumber,
                serviceType = serviceType,
                operationType = operationType,
                showLoading = false
            )
        },
        onServiceClick = onServiceClick
    )
}

@Composable
fun LiveBusTrackerScreen(serviceIdOrBusNumber: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(serviceIdOrBusNumber, color = MaterialTheme.colorScheme.onBackground, fontFamily = PoppinsFamily)
    }
}

@Composable
private fun LiveServiceDetailShell(
    viewModel: TrackerViewModel,
    serviceId: Int,
    displayNumber: String,
    selectedFromName: String,
    selectedToName: String,
    onBack: () -> Unit
) {
    val state by viewModel.liveServiceState.collectAsState()

    when (val s = state) {
        is LiveServiceState.Loading -> {
            Scaffold(
                topBar = { LiveTopBar(title = "Loading service", onBack = onBack) },
                containerColor = Color(0xFFEAF4FF)
            ) { padding ->
                LoadingBox(Modifier.fillMaxSize().padding(padding))
            }
        }
        is LiveServiceState.Error -> {
            Scaffold(
                topBar = { LiveTopBar(title = "Service details", onBack = onBack) },
                containerColor = Color(0xFFEAF4FF)
            ) { padding ->
                ErrorBox(Modifier.fillMaxSize().padding(padding), s.message)
            }
        }
        is LiveServiceState.Success -> LiveServiceScreen(
            viewModel = viewModel,
            snapshot = s.snapshot,
            refreshError = s.lastError,
            serviceId = serviceId,
            displayNumber = displayNumber,
            selectedFromName = selectedFromName,
            selectedToName = selectedToName,
            onBack = onBack
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                title,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
        },
        actions = {
            IconButton(onClick = {}) {
                Icon(Icons.Default.MoreVert, "More", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBarBlue)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveServiceScreen(
    viewModel: TrackerViewModel,
    snapshot: LiveServiceSnapshot,
    refreshError: String,
    serviceId: Int,
    displayNumber: String,
    selectedFromName: String,
    selectedToName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val detail = snapshot.detail
    val points = remember(detail.points) {
        detail.points.sortedWith(
            compareBy<TripPointDto> { it.sequence ?: Int.MAX_VALUE }
                .thenBy { it.plannedTime ?: Long.MAX_VALUE }
        )
    }
    val stopDistanceTexts = remember(points) { cumulativeStopDistanceTexts(points) }
    val fromName = snapshot.selectedFromName.ifBlank { points.firstOrNull()?.name.orEmpty() }
    val toName = snapshot.selectedToName.ifBlank { points.lastOrNull()?.name.orEmpty() }
    val destinationPoint = remember(points, toName) { findJourneyPoint(points, toName) ?: points.lastOrNull() }
    val livePosition = remember(detail, points, snapshot.liveVehicle) { computeLivePosition(detail, points, snapshot.liveVehicle) }
    val segmentIndex = livePosition.stopIndex
    val highlightedIndex = livePosition.nextStopIndex
        ?.takeIf { livePosition.started && livePosition.segmentProgress >= 0.78f }
        ?: segmentIndex
    val statusIndex = livePosition.nextStopIndex
        ?.takeIf { livePosition.started && livePosition.segmentProgress >= 0.90f }
        ?: segmentIndex
    val currentIndex = highlightedIndex
    val currentPoint = points.getOrNull(statusIndex)
    val segmentNextStop = remember(points, segmentIndex, livePosition.started, livePosition.nextStopIndex) {
        if (livePosition.started) {
            points.getOrNull(livePosition.nextStopIndex ?: (segmentIndex + 1))
        } else {
            points.firstOrNull()
        }
    }
    val drawerNextStop = remember(points, currentIndex, segmentNextStop, livePosition.started, livePosition.segmentProgress) {
        when {
            !livePosition.started -> segmentNextStop
            livePosition.segmentProgress >= 0.90f -> points.getOrNull(currentIndex + 1)
            else -> segmentNextStop
        }
    }
    val delayMinutes = remember(points, statusIndex, livePosition.started) {
        if (livePosition.started) computeServiceDelayMinutes(points.getOrNull(statusIndex)) else null
    }
    val serviceName = detail.serviceName.orEmpty().ifBlank { detail.name.orEmpty() }
    val title = "${detail.serviceId.takeIf { it > 0 } ?: snapshot.displayNumber} - $serviceName"
    var sheetLevel by remember(detail.serviceId, snapshot.selectedFromName, snapshot.selectedToName) { mutableStateOf(LiveSheetLevel.COLLAPSED) }
    var busInfoOpen by remember(detail.serviceId) { mutableStateOf(false) }
    var alarmMode by remember(detail.serviceId, toName) { mutableStateOf<AlarmSheetMode?>(null) }
    val trackerColors = liveTrackerColors()
    val listState = rememberLazyListState()
    var lastAutoScrollIndex by remember(detail.serviceId) { mutableStateOf<Int?>(null) }

    fun scheduleOrRequestPermission(alarm: BusAlarmSpec) {
        openClockAlarm(context, alarm)
    }

    fun scheduleTimeAlarm(beforeMinutes: Int) {
        val destination = destinationPoint
        val destinationTime = pointTimeMillis(destination, livePosition.started)
        if (destination == null || destinationTime == null) {
            Toast.makeText(context, "Unable to calculate destination time", Toast.LENGTH_SHORT).show()
            return
        }
        val trigger = destinationTime - beforeMinutes.coerceAtLeast(0) * 60_000L
        if (trigger <= System.currentTimeMillis()) {
            Toast.makeText(context, "That reminder time already passed", Toast.LENGTH_SHORT).show()
            return
        }
        scheduleOrRequestPermission(
            BusAlarmSpec(
                triggerAtMillis = trigger,
                title = "Gamyam bus reminder",
                message = "Service ${detail.serviceId}: ${beforeMinutes.coerceAtLeast(0)} min before ${destination.name.orEmpty().ifBlank { toName }}",
                serviceId = detail.serviceId
            )
        )
        alarmMode = null
    }

    fun scheduleStationAlarm(point: TripPointDto) {
        val stationTime = pointTimeMillis(point, livePosition.started)
        if (stationTime == null || stationTime <= System.currentTimeMillis()) {
            Toast.makeText(context, "Unable to calculate alarm time for this stop", Toast.LENGTH_SHORT).show()
            return
        }
        scheduleOrRequestPermission(
            BusAlarmSpec(
                triggerAtMillis = stationTime,
                title = "Gamyam station reminder",
                message = "Service ${detail.serviceId} is expected at ${point.name.orEmpty().ifBlank { "your selected stop" }}",
                serviceId = detail.serviceId
            )
        )
        alarmMode = null
    }

    LaunchedEffect(points.size, currentIndex) {
        if (points.isNotEmpty()) {
            val visibleRange = listState.layoutInfo.visibleItemsInfo.map { it.index }
            val currentVisible = currentIndex in visibleRange
            val indexChanged = lastAutoScrollIndex != currentIndex
            if (lastAutoScrollIndex == null || (indexChanged && !currentVisible)) {
                listState.animateScrollToItem((currentIndex - 2).coerceAtLeast(0))
            }
            lastAutoScrollIndex = currentIndex
        }
    }

    if (busInfoOpen) {
        BusInfoDialog(
            detail = detail,
            liveVehicle = snapshot.liveVehicle,
            loadedAtMillis = snapshot.loadedAtMillis,
            onDismiss = { busInfoOpen = false }
        )
    }

    val destinationIndex = remember(points, destinationPoint) {
        destinationPoint?.let { points.indexOf(it).takeIf { index -> index >= 0 } } ?: points.lastIndex
    }
    val alarmStationPoints = remember(points, currentIndex, destinationIndex) {
        if (points.isEmpty() || destinationIndex <= currentIndex) {
            emptyList()
        } else {
            points.subList((currentIndex + 1).coerceAtMost(points.size), (destinationIndex + 1).coerceAtMost(points.size))
        }
    }

    alarmMode?.let { mode ->
        BusAlarmDialog(
            mode = mode,
            destinationName = toName.ifBlank { destinationPoint?.name.orEmpty() },
            stationPoints = alarmStationPoints,
            onModeChange = { alarmMode = it },
            onDismiss = { alarmMode = null },
            onTimeAlarm = { minutes -> scheduleTimeAlarm(minutes) },
            onStationAlarm = { point -> scheduleStationAlarm(point) }
        )
    }

    CompositionLocalProvider(LocalLiveTrackerColors provides trackerColors) {
    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(AppBarBlue)) {
                LiveTopBar(title = title, onBack = onBack)
                LiveActionPills(
                    onAlarmClick = { alarmMode = AlarmSheetMode.CHOICE },
                    onBusInfoClick = { busInfoOpen = true },
                    onShareClick = {
                        shareText(
                            context = context,
                            subject = "Gamyam live service ${detail.serviceId}",
                            text = liveServiceShareText(
                                detail = detail,
                                fromName = fromName,
                                toName = toName,
                                nextPoint = drawerNextStop,
                                destinationPoint = destinationPoint,
                                serviceStarted = livePosition.started,
                                delayMinutes = delayMinutes
                            )
                        )
                    }
                )
            }
        },
        containerColor = trackerColors.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TimelineHeader(dayLabel = tripDayLabel(points))
            Box(modifier = Modifier.weight(1f)) {
                if (points.isEmpty()) {
                    EmptyBox("No stops found for this service")
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().background(trackerColors.rowBackground),
                        contentPadding = PaddingValues(bottom = 128.dp)
                    ) {
                        items(points, key = { "${it.id}-${it.sequence}-${it.name}" }) { point ->
                            val index = points.indexOf(point)
                            TimelineStopRow(
                                point = point,
                                index = index,
                                currentIndex = currentIndex,
                                livePosition = livePosition,
                                stopDistanceText = stopDistanceTexts.getOrNull(index).orEmpty(),
                                isLast = index == points.lastIndex
                            )
                        }
                    }
                    LiveBusOverlay(
                        listState = listState,
                        points = points,
                        currentIndex = segmentIndex,
                        nextPoint = segmentNextStop,
                        livePosition = livePosition,
                        delayMinutes = delayMinutes,
                        liveVehicle = snapshot.liveVehicle,
                        bottomInset = sheetLevel.panelHeight(),
                        loadedAtMillis = snapshot.loadedAtMillis
                    )
                }

                LiveBottomPanel(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(3f),
                    fromName = fromName,
                    toName = toName,
                    currentPoint = currentPoint,
                    nextPoint = drawerNextStop,
                    destinationPoint = destinationPoint,
                    progress = if (livePosition.started) progressFor(points, segmentIndex, livePosition.segmentProgress) else 0f,
                    delayMinutes = delayMinutes,
                    liveVehicle = snapshot.liveVehicle,
                    serviceStatus = detail.statusStr.orEmpty(),
                    serviceStarted = livePosition.started,
                    loadedAtMillis = snapshot.loadedAtMillis,
                    refreshError = refreshError,
                    refreshing = snapshot.refreshing,
                    onShareClick = {
                        shareText(
                            context = context,
                            subject = "Gamyam live service ${detail.serviceId}",
                            text = liveServiceShareText(
                                detail = detail,
                                fromName = fromName,
                                toName = toName,
                                nextPoint = drawerNextStop,
                                destinationPoint = destinationPoint,
                                serviceStarted = livePosition.started,
                                delayMinutes = delayMinutes
                            )
                        )
                    },
                    onRefresh = {
                        viewModel.refreshLiveService(
                            serviceId = serviceId,
                            displayNumber = displayNumber,
                            selectedFromName = selectedFromName,
                            selectedToName = selectedToName
                        )
                    },
                    sheetLevel = sheetLevel,
                    onSheetLevelChange = { sheetLevel = it }
                )
            }
        }
    }
    }
}

@Composable
private fun LiveActionPills(
    onAlarmClick: () -> Unit,
    onBusInfoClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LivePill("Alarm", Icons.Default.Notifications, modifier = Modifier.weight(1f), onClick = onAlarmClick)
        LivePill("Bus Info", Icons.Default.DirectionsBus, modifier = Modifier.weight(1.15f), onClick = onBusInfoClick)
        LivePill("Share", Icons.Default.Share, modifier = Modifier.weight(1f), onClick = onShareClick)
    }
}

@Composable
private fun LivePill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .border(1.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(22.dp))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = if (selected) AppBarBlue else Color.White, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            text,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = if (selected) AppBarBlue else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BusAlarmDialog(
    mode: AlarmSheetMode,
    destinationName: String,
    stationPoints: List<TripPointDto>,
    onModeChange: (AlarmSheetMode) -> Unit,
    onDismiss: () -> Unit,
    onTimeAlarm: (Int) -> Unit,
    onStationAlarm: (TripPointDto) -> Unit
) {
    when (mode) {
        AlarmSheetMode.CHOICE -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Set alarm", fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Choose how you want to be reminded for ${destinationName.ifBlank { "your destination" }}.",
                            fontFamily = PoppinsFamily,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { onModeChange(AlarmSheetMode.TIME) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Before time", fontFamily = PoppinsFamily)
                        }
                        Button(onClick = {
                            onModeChange(if (stationPoints.isEmpty()) AlarmSheetMode.UNAVAILABLE else AlarmSheetMode.STATION)
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Select station", fontFamily = PoppinsFamily)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
            )
        }
        AlarmSheetMode.TIME -> {
            var minutes by remember { mutableStateOf("1") }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Time before destination", fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "How many minutes before ${destinationName.ifBlank { "destination" }}?",
                            fontFamily = PoppinsFamily,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = minutes,
                            onValueChange = { value -> minutes = value.filter { it.isDigit() }.take(3) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text("Minutes") }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { minutes.toIntOrNull()?.takeIf { it >= 0 }?.let(onTimeAlarm) }) {
                        Text("Set")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onModeChange(AlarmSheetMode.CHOICE) }) { Text("Back") }
                }
            )
        }
        AlarmSheetMode.STATION -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Select station", fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold) },
                text = {
                    Column(
                        modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())
                    ) {
                        stationPoints.forEach { point ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(remember { MutableInteractionSource() }, null) { onStationAlarm(point) }
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(
                                    point.name.orEmpty().ifBlank { "Stop ${point.sequence ?: ""}".trim() },
                                    fontFamily = PoppinsFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    expectedReachTime(point, serviceStarted = true).ifBlank { formatTime(point.plannedTime ?: point.plannedExitTime) },
                                    fontFamily = PoppinsFamily,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { onModeChange(AlarmSheetMode.CHOICE) }) { Text("Back") }
                }
            )
        }
        AlarmSheetMode.UNAVAILABLE -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Station alarm unavailable", fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold) },
                text = {
                    Text(
                        "No intermediate stops are available between the current bus position and destination.",
                        fontFamily = PoppinsFamily,
                        fontSize = 13.sp
                    )
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
                dismissButton = {
                    TextButton(onClick = { onModeChange(AlarmSheetMode.CHOICE) }) { Text("Back") }
                }
            )
        }
    }
}

@Composable
private fun TimelineHeader(dayLabel: String) {
    val colors = LocalLiveTrackerColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(colors.header)
            .padding(start = TimelineHorizontalPadding, end = TimelineHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Arrival", color = Color.White, fontFamily = PoppinsFamily, fontSize = 14.sp, modifier = Modifier.width(TimelineArrivalWidth))
        Text(
            dayLabel,
            color = Color.White,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Text(
            "Departure",
            color = Color.White,
            fontFamily = PoppinsFamily,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.width(TimelineDepartureWidth)
        )
    }
}

@Composable
private fun TimelineStopRow(
    point: TripPointDto,
    index: Int,
    currentIndex: Int,
    livePosition: LiveBusPosition,
    stopDistanceText: String,
    isLast: Boolean
) {
    val colors = LocalLiveTrackerColors.current
    val serviceStarted = livePosition.started
    val isCurrent = index == currentIndex
    val passed = serviceStarted && (point.entryTime.isUsableTime() || point.exitTime.isUsableTime() || index < currentIndex)
    val delayMinutes = if (serviceStarted) computeServiceDelayMinutes(point) else null
    val accent = when {
        isCurrent -> colors.stopDot
        delayMinutes != null && delayMinutes > 2 -> Color(0xFFB3261E)
        passed -> colors.stopDot
        else -> colors.stopDot
    }
    val rowBackground = colors.rowBackground
    val rowHeight = if (isCurrent) 108.dp else 98.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .background(rowBackground)
            .padding(start = TimelineHorizontalPadding, end = TimelineHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(TimelineArrivalWidth).padding(vertical = 8.dp), horizontalAlignment = Alignment.Start) {
            Text(formatTime(point.plannedTime), fontFamily = PoppinsFamily, fontSize = 12.sp, color = colors.primaryText, maxLines = 1)
            Text(
                liveArrivalDisplay(point, serviceStarted),
                fontFamily = PoppinsFamily,
                fontSize = 13.sp,
                color = liveTimeColor(delayMinutes, serviceStarted, colors)
            )
        }
        Box(
            modifier = Modifier
                .width(TimelineRailWidth)
                .height(rowHeight),
            contentAlignment = Alignment.Center
        ) {
            TimelineRail(
                accent = accent,
                passed = passed,
                isCurrent = isCurrent
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)) {
            Text(
                point.name.orEmpty().ifBlank { "Stop ${point.sequence ?: index + 1}" },
                fontFamily = PoppinsFamily,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                color = colors.primaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (stopDistanceText.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stopDistanceText,
                    fontFamily = PoppinsFamily,
                    fontSize = 11.sp,
                    color = colors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Column(modifier = Modifier.width(TimelineDepartureWidth).padding(vertical = 8.dp), horizontalAlignment = Alignment.End) {
            Text(
                if (isLast) "End" else formatTime(point.plannedExitTime ?: point.plannedTime),
                fontFamily = PoppinsFamily,
                fontSize = 12.sp,
                color = colors.primaryText
            )
            Text(
                liveDepartureDisplay(point, isLast, serviceStarted),
                fontFamily = PoppinsFamily,
                fontSize = 13.sp,
                color = liveTimeColor(delayMinutes, serviceStarted, colors)
            )
        }
    }
}

@Composable
private fun LiveBusOverlay(
    listState: LazyListState,
    points: List<TripPointDto>,
    currentIndex: Int,
    nextPoint: TripPointDto?,
    livePosition: LiveBusPosition,
    delayMinutes: Int?,
    liveVehicle: LiveVehicleDto?,
    bottomInset: androidx.compose.ui.unit.Dp,
    loadedAtMillis: Long
) {
    val point = points.getOrNull(currentIndex) ?: return
    val visibleItems = listState.layoutInfo.visibleItemsInfo
    val currentItem = visibleItems.firstOrNull { it.index == currentIndex } ?: return
    val nextIndex = livePosition.nextStopIndex ?: (currentIndex + 1)
    val nextItem = visibleItems.firstOrNull { it.index == nextIndex }
    val markerSize = 65.dp
    val density = LocalDensity.current
    val progress = if (livePosition.started) livePosition.segmentProgress.coerceIn(0f, 1f) else 0f
    val currentCenter = currentItem.offset.toFloat() + (currentItem.size.toFloat() / 2f)
    val nextCenter = nextItem?.let { it.offset.toFloat() + (it.size.toFloat() / 2f) }
        ?: (currentCenter + currentItem.size.toFloat())
    val markerCenter = currentCenter + ((nextCenter - currentCenter) * progress)
    val markerTop = with(density) {
        (markerCenter - (markerSize.toPx() / 2f)).toDp()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomInset)
            .clipToBounds()
            .zIndex(1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = markerTop)
                .padding(start = TimelineHorizontalPadding, end = TimelineHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(TimelineArrivalWidth))
            Box(
                modifier = Modifier.width(TimelineRailWidth),
                contentAlignment = Alignment.Center
            ) {
                CurrentBusMarker(size = markerSize)
            }
            Spacer(Modifier.width(8.dp))
            BusStopCallout(
                text = currentBusMessage(point, nextPoint, livePosition, delayMinutes, liveVehicle, loadedAtMillis),
                late = (delayMinutes ?: 0) > 2
            )
        }
    }
}

@Composable
private fun TimelineRail(
    accent: Color,
    passed: Boolean,
    isCurrent: Boolean
) {
    val colors = LocalLiveTrackerColors.current
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        drawLine(
            color = colors.rail,
            start = Offset(centerX, 0f),
            end = Offset(centerX, size.height),
            strokeWidth = 19.dp.toPx(),
            cap = StrokeCap.Butt
        )
        drawCircle(
            color = if (isCurrent) AppBarBlue else accent,
            radius = if (passed || isCurrent) 7.5.dp.toPx() else 6.dp.toPx(),
            center = Offset(centerX, size.height / 2f)
        )
    }
}

@Composable
private fun BusStopCallout(text: String, late: Boolean) {
    val bubbleColor = if (late) Color(0xFFC51313) else Color(0xFF18933D)
    Row(
        modifier = Modifier
            .widthIn(max = 154.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.width(8.dp).height(16.dp)) {
            val path = Path().apply {
                moveTo(size.width, 0f)
                lineTo(0f, size.height / 2f)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(path, bubbleColor)
        }
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp)),
            color = bubbleColor,
            shadowElevation = 4.dp
        ) {
            Text(
                text,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                color = Color.White,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CurrentBusMarker(
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.requiredSize(size),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.gamyam),
            contentDescription = "Current bus",
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    }
}

@Composable
private fun LiveBottomPanel(
    modifier: Modifier,
    fromName: String,
    toName: String,
    currentPoint: TripPointDto?,
    nextPoint: TripPointDto?,
    destinationPoint: TripPointDto?,
    progress: Float,
    delayMinutes: Int?,
    liveVehicle: LiveVehicleDto?,
    serviceStatus: String,
    serviceStarted: Boolean,
    loadedAtMillis: Long,
    refreshError: String,
    refreshing: Boolean,
    onShareClick: () -> Unit,
    onRefresh: () -> Unit,
    sheetLevel: LiveSheetLevel,
    onSheetLevelChange: (LiveSheetLevel) -> Unit
) {
    val colors = LocalLiveTrackerColors.current
    var panelDragDistance by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val panelHeight by animateDpAsState(
        targetValue = sheetLevel.panelHeight(),
        label = "live-sheet-height"
    )
    val dragHeight = with(density) { (-panelDragDistance).toDp() }
    val displayedPanelHeight = (panelHeight + dragHeight).coerceIn(126.dp, 560.dp)
    fun expandOneStep() {
        onSheetLevelChange(
            when (sheetLevel) {
                LiveSheetLevel.COLLAPSED -> LiveSheetLevel.PARTIAL
                LiveSheetLevel.PARTIAL -> LiveSheetLevel.EXPANDED
                LiveSheetLevel.EXPANDED -> LiveSheetLevel.EXPANDED
            }
        )
    }
    fun collapseOneStep() {
        onSheetLevelChange(
            when (sheetLevel) {
                LiveSheetLevel.COLLAPSED -> LiveSheetLevel.COLLAPSED
                LiveSheetLevel.PARTIAL -> LiveSheetLevel.COLLAPSED
                LiveSheetLevel.EXPANDED -> LiveSheetLevel.PARTIAL
            }
        )
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(displayedPanelHeight),
        color = colors.panel,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .draggable(
                    state = rememberDraggableState { delta -> panelDragDistance += delta },
                    orientation = Orientation.Vertical,
                    onDragStarted = { panelDragDistance = 0f },
                    onDragStopped = { velocity ->
                        when {
                            panelDragDistance < -8f || velocity < -700f -> expandOneStep()
                            panelDragDistance > 8f || velocity > 700f -> collapseOneStep()
                        }
                        panelDragDistance = 0f
                    }
                )
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(remember { MutableInteractionSource() }, null) {
                        expandOneStep()
                    },
                contentAlignment = Alignment.Center
            ) {
                BottomSheetHandle()
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        bottomStatusText(currentPoint, toName, liveVehicle, serviceStatus, serviceStarted),
                        color = if (delayMinutes != null && delayMinutes > 2) Color(0xFFFF8A80) else colors.panelTitle,
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Updated ${relativeUpdatedText(loadedAtMillis)}",
                        color = colors.panelSubtitle,
                        fontFamily = PoppinsFamily,
                        fontSize = 11.sp
                    )
                    if (refreshError.isNotBlank()) {
                        Text(
                            refreshError,
                            color = Color(0xFFB3261E),
                            fontFamily = PoppinsFamily,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = AppBarBlue,
                    modifier = Modifier
                        .size(56.dp)
                        .clickable(remember { MutableInteractionSource() }, null) { onRefresh() }
                ) {
                    RefreshIcon(spinning = refreshing)
                }
            }
            if (sheetLevel != LiveSheetLevel.COLLAPSED) {
                Spacer(Modifier.height(14.dp))
                LiveBottomSheetJourneyCard(
                    fromName = fromName,
                    toName = toName,
                    nextPoint = nextPoint,
                    destinationPoint = destinationPoint,
                    progress = progress,
                    delayMinutes = delayMinutes,
                    liveVehicle = liveVehicle,
                    serviceStarted = serviceStarted
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    BottomButton("Feedback", Icons.Default.Feedback, Modifier.weight(1f))
                    BottomButton("Share", Icons.Default.Share, Modifier.weight(1f), onClick = onShareClick)
                }
            }
        }
    }
}

@Composable
private fun LiveBottomSheetJourneyCard(
    fromName: String,
    toName: String,
    nextPoint: TripPointDto?,
    destinationPoint: TripPointDto?,
    progress: Float,
    delayMinutes: Int?,
    liveVehicle: LiveVehicleDto?,
    serviceStarted: Boolean
) {
    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFDFBFF), shadowElevation = 1.dp) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(fromName.ifBlank { "--" }, fontFamily = PoppinsFamily, fontSize = 13.sp, color = Color(0xFF1E1E1E), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(toName.ifBlank { "--" }, fontFamily = PoppinsFamily, fontSize = 13.sp, color = Color(0xFF1E1E1E), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(5.dp).padding(horizontal = 22.dp),
                color = Color(0xFF34A853),
                trackColor = Color(0xFFD7D7D7)
            )
            HorizontalDivider(color = Color(0xFFB8B8B8), modifier = Modifier.padding(top = 14.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp)) {
                BottomInfoCell(
                    "Next Stop",
                    nextPoint?.name.orEmpty().ifBlank { "--" },
                    listOf(formatDistanceToNext(liveVehicle, nextPoint), expectedReachTime(nextPoint, serviceStarted)).filter { it.isNotBlank() }.joinToString(" - "),
                    Modifier.weight(1f)
                )
                Box(modifier = Modifier.width(1.dp).height(76.dp).background(Color(0xFFB8B8B8)))
                BottomInfoCell("To Reach", toName.ifBlank { "--" }, expectedReachTime(destinationPoint, serviceStarted).ifBlank { "--" }, Modifier.weight(1f))
            }
            Box(
                modifier = Modifier.fillMaxWidth().background(delayColor(delayMinutes, serviceStarted)).padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(delayText(delayMinutes, serviceStarted), color = Color.White, fontFamily = PoppinsFamily, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun LiveBottomSheetContent(
    fromName: String,
    toName: String,
    currentPoint: TripPointDto?,
    nextPoint: TripPointDto?,
    destinationPoint: TripPointDto?,
    progress: Float,
    delayMinutes: Int?,
    liveVehicle: LiveVehicleDto?,
    serviceStatus: String,
    serviceStarted: Boolean,
    loadedAtMillis: Long,
    refreshError: String,
    refreshing: Boolean,
    onRefresh: () -> Unit
) {
    val colors = LocalLiveTrackerColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    bottomStatusText(currentPoint, toName, liveVehicle, serviceStatus, serviceStarted),
                    color = if (delayMinutes != null && delayMinutes > 2) Color(0xFFFF8A80) else colors.panelTitle,
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Updated ${relativeUpdatedText(loadedAtMillis)}",
                    color = colors.panelSubtitle,
                    fontFamily = PoppinsFamily,
                    fontSize = 11.sp
                )
                if (refreshError.isNotBlank()) {
                    Text(
                        refreshError,
                        color = Color(0xFFB3261E),
                        fontFamily = PoppinsFamily,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = AppBarBlue,
                modifier = Modifier
                    .size(56.dp)
                    .clickable(remember { MutableInteractionSource() }, null) { onRefresh() }
            ) {
                RefreshIcon(spinning = refreshing)
            }
        }
        Spacer(Modifier.height(14.dp))
        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFDFBFF), shadowElevation = 1.dp) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(fromName.ifBlank { "--" }, fontFamily = PoppinsFamily, fontSize = 13.sp, color = Color(0xFF1E1E1E), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(toName.ifBlank { "--" }, fontFamily = PoppinsFamily, fontSize = 13.sp, color = Color(0xFF1E1E1E), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(5.dp).padding(horizontal = 22.dp),
                    color = Color(0xFF34A853),
                    trackColor = Color(0xFFD7D7D7)
                )
                HorizontalDivider(color = Color(0xFFB8B8B8), modifier = Modifier.padding(top = 14.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp)) {
                    BottomInfoCell(
                        "Next Stop",
                        nextPoint?.name.orEmpty().ifBlank { "--" },
                        listOf(formatDistanceToNext(liveVehicle, nextPoint), expectedReachTime(nextPoint, serviceStarted)).filter { it.isNotBlank() }.joinToString(" - "),
                        Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.width(1.dp).height(76.dp).background(Color(0xFFB8B8B8)))
                    BottomInfoCell("To Reach", toName.ifBlank { "--" }, expectedReachTime(destinationPoint, serviceStarted).ifBlank { "--" }, Modifier.weight(1f))
                }
                Box(
                    modifier = Modifier.fillMaxWidth().background(delayColor(delayMinutes, serviceStarted)).padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(delayText(delayMinutes, serviceStarted), color = Color.White, fontFamily = PoppinsFamily, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            BottomButton("Feedback", Icons.Default.Feedback, Modifier.weight(1f))
            BottomButton("Share", Icons.Default.Share, Modifier.weight(1f))
        }
    }
}

@Composable
private fun BottomSheetHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF5F666D))
        )
    }
}

@Composable
private fun RefreshIcon(spinning: Boolean) {
    val rotation = if (spinning) {
        androidx.compose.animation.core.rememberInfiniteTransition(label = "refresh-spin")
            .animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(
                        durationMillis = 850,
                        easing = androidx.compose.animation.core.LinearEasing
                    )
                ),
                label = "refresh-rotation"
            ).value
    } else {
        0f
    }
    Icon(
        Icons.Default.Refresh,
        contentDescription = "Refresh",
        tint = Color.White,
        modifier = Modifier
            .padding(15.dp)
            .rotate(rotation)
    )
}

@Composable
private fun BottomInfoCell(title: String, primary: String, secondary: String, modifier: Modifier) {
    Column(modifier = modifier.padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontFamily = PoppinsFamily, fontSize = 13.sp, color = Color(0xFF222222))
        Spacer(Modifier.height(8.dp))
        Text(primary, fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF222222), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Text(secondary.ifBlank { "--" }, fontFamily = PoppinsFamily, fontSize = 13.sp, color = Color(0xFF222222), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BottomButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit = {}
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppBarBlue, contentColor = Color.White)
    ) {
        Icon(icon, null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontFamily = PoppinsFamily, fontSize = 15.sp)
    }
}

private fun shareText(context: Context, subject: String, text: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share with"))
}

private fun searchShareText(result: ServicesResultState.Success): String {
    val countText = when (result.rows.size) {
        0 -> "No services found"
        1 -> "1 service found"
        else -> "${result.rows.size} services found"
    }
    return buildString {
        appendLine("Gamyam search")
        appendLine(result.title)
        if (result.subtitle.isNotBlank()) appendLine(result.subtitle)
        appendLine(countText)
        appendLine("Open Gamyam and search with the same stops to view live buses.")
        appendLine("If Gamyam is not installed, download it here: $GamyamInstallLink")
    }.trim()
}

private fun liveServiceShareText(
    detail: TripDetailDto,
    fromName: String,
    toName: String,
    nextPoint: TripPointDto?,
    destinationPoint: TripPointDto?,
    serviceStarted: Boolean,
    delayMinutes: Int?
): String {
    val serviceId = detail.serviceId.takeIf { it > 0 }?.toString().orEmpty()
    val serviceName = detail.serviceName.orEmpty().ifBlank { detail.name.orEmpty() }
    val nextStop = nextPoint?.name.orEmpty()
    val destination = toName.ifBlank { destinationPoint?.name.orEmpty() }
    return buildString {
        appendLine("Gamyam live bus")
        if (serviceId.isNotBlank()) appendLine("Service ID: $serviceId")
        if (serviceName.isNotBlank()) appendLine("Service: $serviceName")
        if (fromName.isNotBlank() || destination.isNotBlank()) {
            appendLine("${fromName.ifBlank { "--" }} to ${destination.ifBlank { "--" }}")
        }
        if (nextStop.isNotBlank()) {
            appendLine("Next stop: $nextStop ${expectedReachTime(nextPoint, serviceStarted).ifBlank { "" }}".trim())
        }
        if (destination.isNotBlank()) {
            appendLine("To reach: $destination ${expectedReachTime(destinationPoint, serviceStarted).ifBlank { "" }}".trim())
        }
        delayMinutes?.let {
            if (it > 2) appendLine("$it min late")
            if (it < -2) appendLine("${abs(it)} min early")
        }
        if (serviceId.isNotBlank()) appendLine("Open Gamyam, choose Service ID, and search $serviceId to view live tracking.")
        appendLine("If Gamyam is not installed, download it here: $GamyamInstallLink")
    }.trim()
}

private fun openClockAlarm(context: Context, alarm: BusAlarmSpec) {
    val calendar = Calendar.getInstance().apply { timeInMillis = alarm.triggerAtMillis }
    val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
        putExtra(AlarmClock.EXTRA_HOUR, calendar.get(Calendar.HOUR_OF_DAY))
        putExtra(AlarmClock.EXTRA_MINUTES, calendar.get(Calendar.MINUTE))
        putExtra(AlarmClock.EXTRA_MESSAGE, alarm.message)
        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
    }
    try {
        context.startActivity(alarmIntent)
    } catch (_: ActivityNotFoundException) {
        openClockTimer(context, alarm.message)
    } catch (_: SecurityException) {
        openClockTimer(context, alarm.message)
    }
}

private fun openClockTimer(context: Context, message: String) {
    val timerIntent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
        putExtra(AlarmClock.EXTRA_LENGTH, 60)
        putExtra(AlarmClock.EXTRA_MESSAGE, message)
        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
    }
    try {
        context.startActivity(timerIntent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Unable to open the Clock app", Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, "Unable to open the Clock app", Toast.LENGTH_SHORT).show()
    }
}

private fun findJourneyPoint(points: List<TripPointDto>, stopName: String): TripPointDto? {
    val target = stopName.normalizedStopName()
    if (target.isBlank()) return null
    return points.firstOrNull { it.name.orEmpty().normalizedStopName() == target }
        ?: points.firstOrNull {
            val name = it.name.orEmpty().normalizedStopName()
            name.isNotBlank() && (name.contains(target) || target.contains(name))
        }
}

private fun pointTimeMillis(point: TripPointDto?, serviceStarted: Boolean): Long? {
    if (point == null) return null
    val seconds = if (serviceStarted) {
        point.eta ?: point.etd ?: point.plannedTime ?: point.plannedExitTime
    } else {
        point.plannedTime ?: point.plannedExitTime
    } ?: return null
    return apiSecondsToFutureMillis(seconds)
}

private fun apiSecondsToFutureMillis(seconds: Long): Long? {
    if (seconds <= 0L) return null
    if (seconds > 86_400L) return seconds * 1000L
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    var millis = calendar.timeInMillis + seconds * 1000L
    if (millis < System.currentTimeMillis() - 60_000L) {
        millis += 24L * 60L * 60L * 1000L
    }
    return millis
}

private fun formatAlarmTime(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.US).format(Date(millis))

private fun String.normalizedStopName(): String =
    trim().lowercase(Locale.US).replace(Regex("""[^a-z0-9]+"""), " ").trim()

@Composable
private fun BusInfoDialog(
    detail: TripDetailDto,
    liveVehicle: LiveVehicleDto?,
    loadedAtMillis: Long,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = AppBarBlue, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Bus Info",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoLine("Service", detail.serviceId.takeIf { it > 0 }?.toString().orEmpty().ifBlank { detail.name.orEmpty() })
                InfoLine("Name", detail.serviceName.orEmpty().ifBlank { detail.name.orEmpty() })
                InfoLine("Type", detail.serviceType.orEmpty())
                InfoLine("Vehicle", liveVehicle?.vehicleNumber.orEmpty().ifBlank { detail.vehicleNumber.orEmpty() })
                InfoLine("Status", liveVehicle?.statusStr.orEmpty().ifBlank { detail.statusStr.orEmpty() })
                InfoLine("Depot", detail.depotName.orEmpty())
                InfoLine("Driver", listOf(detail.driverName.orEmpty(), detail.driverMobile.orEmpty()).filter { it.isNotBlank() }.joinToString(" - "))
                InfoLine("Helpline", detail.helpline.orEmpty())
                InfoLine("Updated", relativeUpdatedText(loadedAtMillis))
                if (liveVehicle?.speedKph != null && liveVehicle.speedKph > 0.0) {
                    InfoLine("Speed", "${liveVehicle.speedKph.roundToInt()} km/h")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Close", fontFamily = PoppinsFamily)
            }
        }
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    if (value.isBlank()) return
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            label,
            fontFamily = PoppinsFamily,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(82.dp)
        )
        Text(
            value,
            fontFamily = PoppinsFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServicesResultShell(
    viewModel: TrackerViewModel,
    operationType: Int,
    selectedFromName: String = "",
    selectedToName: String = "",
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onServiceClick: (ServiceTripDetail) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val refreshing by viewModel.resultRefreshing.collectAsState()
    val context = LocalContext.current
    var selectedType by remember(state) { mutableStateOf("All") }
    var sortMode by remember(state) { mutableStateOf(ServiceSortMode.ARRIVAL) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Search results",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val success = state as? ServicesResultState.Success
                            if (success == null) {
                                Toast.makeText(context, "Nothing to share yet", Toast.LENGTH_SHORT).show()
                            } else {
                                shareText(
                                    context = context,
                                    subject = "Gamyam search results",
                                    text = searchShareText(success)
                                )
                            }
                        }
                    ) { Icon(Icons.Default.Share, "Share", tint = Color.White) }
                    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, "More", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBarBlue)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onRefresh,
                containerColor = AppBarBlue,
                contentColor = Color.White
            ) {
                RefreshIcon(spinning = refreshing || state is ServicesResultState.Loading)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when (val s = state) {
            is ServicesResultState.Loading -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            is ServicesResultState.Error -> ErrorBox(Modifier.fillMaxSize().padding(padding), s.message)
            is ServicesResultState.Success -> {
                val types = listOf("All") + s.rows.map { it.serviceType.trim() }.filter { it.isNotBlank() }.distinct().sorted()
                val rows = s.rows
                    .filter { selectedType == "All" || it.serviceType.equals(selectedType, ignoreCase = true) }
                    .sortedWith(serviceResultComparator(sortMode))

                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    ResultControlsRow(
                        types = types,
                        selectedType = selectedType,
                        onTypeSelect = { selectedType = it },
                        sortMode = sortMode,
                        onSortSelect = { sortMode = it }
                    )
                    HeaderBand(title = s.title, subtitle = s.subtitle)
                    if (rows.isEmpty()) {
                        EmptyBox("No services found")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 96.dp)
                        ) {
                            itemsIndexed(
                                items = rows,
                                key = { index, row -> "$index-${row.serviceId}-${row.vehicleNumber}-${row.departureTime}-${row.live}" }
                            ) { _, row ->
                                ServiceResultRowView(
                                    row = row,
                                    onClick = {
                                        onServiceClick(
                                            ServiceTripDetail(
                                                serviceId = row.serviceId,
                                                displayNumber = row.serviceName.ifBlank { row.serviceId.toString() },
                                                operationType = operationType,
                                                fromName = selectedFromName.ifBlank { row.fromName },
                                                toName = selectedToName.ifBlank { row.toName }
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultControlsRow(
    types: List<String>,
    selectedType: String,
    onTypeSelect: (String) -> Unit,
    sortMode: ServiceSortMode,
    onSortSelect: (ServiceSortMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBarBlue)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DropdownPill(
            text = selectedType,
            items = types,
            onSelect = onTypeSelect,
            modifier = Modifier.weight(1f)
        )
        DropdownPill(
            text = "Sort: ${sortMode.label}",
            items = ServiceSortMode.entries.map { it.label },
            onSelect = { label ->
                ServiceSortMode.entries.firstOrNull { it.label == label }?.let(onSortSelect)
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DropdownPill(
    text: String,
    items: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.16f))
                .clickable(remember { MutableInteractionSource() }, null) { expanded = true }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            item,
                            fontFamily = PoppinsFamily,
                            fontWeight = if (item == text || text.endsWith(item)) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun HeaderBand(title: String, subtitle: String) {
    val primary = if (title.equals("Search results", ignoreCase = true)) subtitle else title
    val secondary = if (title.equals("Search results", ignoreCase = true)) "" else subtitle

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBarBlue)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            primary,
            color = Color.White,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (secondary.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                secondary,
                color = Color.White.copy(alpha = 0.82f),
                fontFamily = PoppinsFamily,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ServiceResultRowView(row: ServiceResultRow, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (row.live) Color(0xFF0E7A3C) else AppBarBlue)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    row.serviceId.toString(),
                    color = Color.White,
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                formatTime(row.departureTime).ifBlank { "--" },
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                " - ${formatDuration(row.durationSeconds)} - ",
                fontFamily = PoppinsFamily,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                formatTime(row.arrivalTime).ifBlank { "--" },
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    row.routeText(),
                    fontFamily = PoppinsFamily,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (row.serviceName.isNotBlank()) {
                    Text(
                        row.serviceName,
                        fontFamily = PoppinsFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            TypeChip(row.serviceType)
        }
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                listOf(row.vehicleNumber, row.depot).filter { it.isNotBlank() }.joinToString(" | "),
                fontFamily = PoppinsFamily,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (row.live) {
                Text(
                    "Running",
                    fontFamily = PoppinsFamily,
                    fontSize = 13.sp,
                    color = Color(0xFF0E7A3C),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
    HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun TypeChip(text: String) {
    if (text.isBlank()) return
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(AppBarBlue)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = Color.White,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun LoadingBox(modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AppBarBlue)
    }
}

@Composable
private fun ErrorBox(modifier: Modifier, message: String) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.error, fontFamily = PoppinsFamily, modifier = Modifier.padding(24.dp))
    }
}

@Composable
private fun EmptyBox(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = PoppinsFamily)
    }
}

private fun ServiceResultRow.routeText(): String =
    listOf(fromName, toName).filter { it.isNotBlank() }.joinToString(" -> ").ifBlank { serviceName }

private fun ServiceResultRow.nextDisplayTime(): Long? = departureTime ?: arrivalTime

private fun serviceResultComparator(sortMode: ServiceSortMode): Comparator<ServiceResultRow> {
    val base = compareBy<ServiceResultRow>(
        { if (it.live) 0 else 1 }
    )
    val activeSort = when (sortMode) {
        ServiceSortMode.ARRIVAL -> compareBy<ServiceResultRow>(
            { it.arrivalTime ?: Long.MAX_VALUE },
            { it.nextDisplayTime() ?: Long.MAX_VALUE },
            { if (it.durationSeconds > 0) it.durationSeconds else Int.MAX_VALUE }
        )
        ServiceSortMode.DURATION -> compareBy(
            { if (it.durationSeconds > 0) it.durationSeconds else Int.MAX_VALUE },
            { it.arrivalTime ?: Long.MAX_VALUE },
            { it.nextDisplayTime() ?: Long.MAX_VALUE }
        )
    }
    return base
        .then(activeSort)
        .thenBy { it.serviceName.ifBlank { it.serviceId.toString() } }
}

private fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return "--"
    val totalMinutes = max(1, seconds / 60)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h${minutes.toString().padStart(2, '0')}m" else "${minutes}m"
}

private fun formatTime(value: Long?): String {
    val seconds = value ?: return ""
    if (seconds <= 0L) return ""
    val millis = if (seconds < 86_400L) {
        seconds * 1000L - java.util.TimeZone.getDefault().rawOffset
    } else {
        seconds * 1000L
    }
    return SimpleDateFormat("h:mm a", Locale.US).format(Date(millis))
}

private data class LiveBusPosition(
    val stopIndex: Int,
    val started: Boolean,
    val source: PositionSource,
    val segmentProgress: Float = 0f,
    val nextStopIndex: Int? = null,
    val segmentMeters: Double? = null
)

private enum class PositionSource {
    NOT_STARTED,
    CURRENT_POINT,
    PREVIOUS_POINT,
    NEXT_POINT,
    GPS,
    ACTUAL_TIME,
    EXPECTED_TIME,
    SCHEDULE
}

private data class GpsSegment(
    val previousIndex: Int,
    val nextIndex: Int,
    val progress: Float,
    val distanceMeters: Double,
    val segmentMeters: Double
)

private fun computeLivePosition(
    detail: TripDetailDto,
    points: List<TripPointDto>,
    liveVehicle: LiveVehicleDto?
): LiveBusPosition {
    if (points.isEmpty()) return LiveBusPosition(0, started = false, source = PositionSource.NOT_STARTED)

    fun indexForPointId(id: Int): Int? =
        id.takeIf { it > 0 }
            ?.let { pointId -> points.indexOfFirst { it.id == pointId.toString() }.takeIf { index -> index >= 0 } }

    val firstHasDeparted = points.firstOrNull()?.exitTime.isUsableTime()
    val lastMovedByActualTime = points.indices.lastOrNull { index ->
        val point = points[index]
        point.exitTime.isUsableTime() || (index > 0 && point.entryTime.isUsableTime())
    } ?: -1
    val currentIndexFromLive = indexForPointId(liveVehicle?.currentPointId ?: 0)
    val previousIndexFromLive = indexForPointId(liveVehicle?.previousPointId ?: 0)
    val nextIndexFromLive = indexForPointId(liveVehicle?.nextPointId ?: 0)
    val segmentPreviousIndex = previousIndexFromLive
        ?: currentIndexFromLive?.takeIf { it in 0 until points.lastIndex }
        ?: nextIndexFromLive?.minus(1)?.takeIf { it in points.indices }
    val segmentNextIndex = nextIndexFromLive
        ?: segmentPreviousIndex?.plus(1)?.takeIf { it in points.indices }
    val liveSegmentFromIds = if (segmentPreviousIndex != null && segmentNextIndex != null && segmentNextIndex != segmentPreviousIndex) {
        buildGpsSegment(
            points = points,
            previousIndex = segmentPreviousIndex,
            nextIndex = segmentNextIndex,
            distanceFromPrevious = liveVehicle?.distanceFromPrevious,
            liveLatitude = liveVehicle?.latitude,
            liveLongitude = liveVehicle?.longitude
        )
    } else {
        null
    }
    val projectedGpsSegment = if (
        liveVehicle?.latitude.isUsableCoordinate() &&
        liveVehicle?.longitude.isUsableCoordinate()
    ) {
        liveSegmentFromIds ?: bestGpsSegment(
            points = points,
            liveLatitude = liveVehicle?.latitude,
            liveLongitude = liveVehicle?.longitude,
            minPreviousIndex = maxOf(0, lastMovedByActualTime, (currentIndexFromLive ?: 0) - 1)
        )
    } else {
        liveSegmentFromIds
    }?.takeIf { it.distanceMeters <= GPS_ROUTE_TOLERANCE_METERS || it.nextIndex == points.lastIndex }
    val highestReachedIndex = listOfNotNull(
        lastMovedByActualTime.takeIf { it >= 0 },
        currentIndexFromLive,
        previousIndexFromLive,
        nextIndexFromLive?.takeIf { projectedGpsSegment != null && projectedGpsSegment.progress >= 0.82f },
        projectedGpsSegment?.previousIndex,
        projectedGpsSegment?.nextIndex?.takeIf { projectedGpsSegment.progress >= 0.82f }
    ).maxOrNull()
    val liveIdsShowMovement =
        (currentIndexFromLive ?: 0) > 0 ||
            (previousIndexFromLive ?: 0) > 0 ||
            (nextIndexFromLive ?: 0) > 1
    val segmentShowsMovement = projectedGpsSegment?.let {
        it.previousIndex > 0 || it.progress > 0.035f || firstHasDeparted == true
    } == true
    val status = listOf(detail.statusStr, liveVehicle?.statusStr).joinToString(" ").lowercase(Locale.US)
    val statusSaysNotStarted = listOf("not assigned", "not started", "yet to start", "scheduled", "cancel").any { it in status }
    val statusSaysStarted = !statusSaysNotStarted &&
        listOf("running", "arrived", "departed", "started", "in transit", "delay", "near").any { it in status }
    val started = !statusSaysNotStarted &&
        (lastMovedByActualTime >= 0 || liveIdsShowMovement || segmentShowsMovement || (statusSaysStarted && firstHasDeparted == true))

    if (!started) return LiveBusPosition(0, started = false, source = PositionSource.NOT_STARTED)

    if (highestReachedIndex == points.lastIndex) {
        return LiveBusPosition(
            stopIndex = points.lastIndex,
            started = true,
            source = PositionSource.CURRENT_POINT,
            segmentProgress = 0f,
            nextStopIndex = null,
            segmentMeters = null
        )
    }

    liveVehicle?.let { vehicle ->
        if (projectedGpsSegment != null) {
            val lockedPreviousIndex = maxOf(projectedGpsSegment.previousIndex, highestReachedIndex ?: projectedGpsSegment.previousIndex)
            if (lockedPreviousIndex > projectedGpsSegment.previousIndex) {
                return LiveBusPosition(
                    stopIndex = lockedPreviousIndex,
                    started = true,
                    source = PositionSource.CURRENT_POINT,
                    segmentProgress = 0f,
                    nextStopIndex = (lockedPreviousIndex + 1).takeIf { next -> next in points.indices },
                    segmentMeters = segmentDistanceMeters(points, lockedPreviousIndex, lockedPreviousIndex + 1)
                )
            }
            return LiveBusPosition(
                stopIndex = projectedGpsSegment.previousIndex,
                started = true,
                source = PositionSource.GPS,
                segmentProgress = projectedGpsSegment.progress,
                nextStopIndex = projectedGpsSegment.nextIndex,
                segmentMeters = projectedGpsSegment.segmentMeters
            )
        }

        currentIndexFromLive?.let {
            val lockedIndex = maxOf(it, highestReachedIndex ?: it)
            return LiveBusPosition(
                stopIndex = lockedIndex,
                started = true,
                source = PositionSource.CURRENT_POINT,
                nextStopIndex = (lockedIndex + 1).takeIf { next -> next in points.indices },
                segmentMeters = segmentDistanceMeters(points, lockedIndex, lockedIndex + 1)
            )
        }
        previousIndexFromLive?.let { previousIndex ->
            val lockedPreviousIndex = maxOf(previousIndex, highestReachedIndex ?: previousIndex)
            if (lockedPreviousIndex > previousIndex) {
                return LiveBusPosition(
                    stopIndex = lockedPreviousIndex,
                    started = true,
                    source = PositionSource.CURRENT_POINT,
                    segmentProgress = 0f,
                    nextStopIndex = (lockedPreviousIndex + 1).takeIf { next -> next in points.indices },
                    segmentMeters = segmentDistanceMeters(points, lockedPreviousIndex, lockedPreviousIndex + 1)
                )
            }
            val progress = segmentProgressBetween(
                points = points,
                previousIndex = previousIndex,
                nextIndex = nextIndexFromLive,
                distanceFromPrevious = vehicle.distanceFromPrevious,
                liveLatitude = vehicle.latitude,
                liveLongitude = vehicle.longitude
            )
            return LiveBusPosition(
                stopIndex = previousIndex,
                started = true,
                source = PositionSource.PREVIOUS_POINT,
                segmentProgress = progress,
                nextStopIndex = nextIndexFromLive,
                segmentMeters = nextIndexFromLive?.let { segmentDistanceMeters(points, previousIndex, it) }
            )
        }
        nextIndexFromLive?.let {
            val lockedIndex = maxOf(it, highestReachedIndex ?: it)
            if (lockedIndex > it) {
                return LiveBusPosition(
                    stopIndex = lockedIndex,
                    started = true,
                    source = PositionSource.CURRENT_POINT,
                    segmentProgress = 0f,
                    nextStopIndex = (lockedIndex + 1).takeIf { next -> next in points.indices },
                    segmentMeters = segmentDistanceMeters(points, lockedIndex, lockedIndex + 1)
                )
            }
            val previous = (it - 1).takeIf { previous -> previous in points.indices }
            return LiveBusPosition(
                stopIndex = previous ?: it,
                started = true,
                source = PositionSource.NEXT_POINT,
                segmentProgress = if (previous != null) 0.92f else 0f,
                nextStopIndex = it,
                segmentMeters = previous?.let { prev -> segmentDistanceMeters(points, prev, it) }
            )
        }
    }

    if (lastMovedByActualTime >= 0) {
        return LiveBusPosition(lastMovedByActualTime, started = true, source = PositionSource.ACTUAL_TIME)
    }

    return LiveBusPosition(0, started = false, source = PositionSource.NOT_STARTED)
}

private fun segmentProgressBetween(
    points: List<TripPointDto>,
    previousIndex: Int,
    nextIndex: Int?,
    distanceFromPrevious: Double?,
    liveLatitude: Double?,
    liveLongitude: Double?
): Float {
    val next = nextIndex?.takeIf { it in points.indices && it != previousIndex } ?: return 0f
    val previousCoordinate = points.getOrNull(previousIndex)?.coordinate() ?: return 0f
    val nextCoordinate = points.getOrNull(next)?.coordinate() ?: return 0f
    val segmentMeters = segmentDistanceMeters(points, previousIndex, next)?.takeIf { it > 1.0 } ?: return 0f

    if (liveLatitude.isUsableCoordinate() && liveLongitude.isUsableCoordinate()) {
        return projectToSegment(previousCoordinate, nextCoordinate, liveLatitude!!, liveLongitude!!)
            .first
            .toFloat()
            .coerceIn(0f, 1f)
    }

    distanceFromPrevious?.takeIf { it > 0.0 }?.let { distance ->
        return (distance / segmentMeters).toFloat().coerceIn(0f, 1f)
    }

    return ((distanceFromPrevious ?: 0.0) / segmentMeters).toFloat().coerceIn(0f, 1f)
}

private fun buildGpsSegment(
    points: List<TripPointDto>,
    previousIndex: Int,
    nextIndex: Int,
    distanceFromPrevious: Double?,
    liveLatitude: Double?,
    liveLongitude: Double?
): GpsSegment? {
    val previousCoordinate = points.getOrNull(previousIndex)?.coordinate() ?: return null
    val nextCoordinate = points.getOrNull(nextIndex)?.coordinate() ?: return null
    val segmentMeters = segmentDistanceMeters(points, previousIndex, nextIndex)?.takeIf { it > 1.0 } ?: return null
    val projectedDistance = if (liveLatitude.isUsableCoordinate() && liveLongitude.isUsableCoordinate()) {
        projectToSegment(previousCoordinate, nextCoordinate, liveLatitude!!, liveLongitude!!).second
    } else {
        0.0
    }
    return GpsSegment(
        previousIndex = previousIndex,
        nextIndex = nextIndex,
        progress = segmentProgressBetween(
            points = points,
            previousIndex = previousIndex,
            nextIndex = nextIndex,
            distanceFromPrevious = distanceFromPrevious,
            liveLatitude = liveLatitude,
            liveLongitude = liveLongitude
        ),
        distanceMeters = projectedDistance,
        segmentMeters = segmentMeters
    )
}

private fun bestGpsSegment(
    points: List<TripPointDto>,
    liveLatitude: Double?,
    liveLongitude: Double?,
    minPreviousIndex: Int = 0
): GpsSegment? {
    if (!liveLatitude.isUsableCoordinate() || !liveLongitude.isUsableCoordinate()) return null
    val liveLat = liveLatitude!!
    val liveLng = liveLongitude!!
    val candidates = points.zipWithNext().mapIndexedNotNull { index, pair ->
        if (index < minPreviousIndex) return@mapIndexedNotNull null
        val start = pair.first.coordinate() ?: return@mapIndexedNotNull null
        val end = pair.second.coordinate() ?: return@mapIndexedNotNull null
        val segmentMeters = segmentDistanceMeters(points, index, index + 1)?.takeIf { it > 1.0 }
            ?: return@mapIndexedNotNull null
        val projected = projectToSegment(start, end, liveLat, liveLng)
        GpsSegment(
            previousIndex = index,
            nextIndex = index + 1,
            progress = projected.first.toFloat().coerceIn(0f, 1f),
            distanceMeters = projected.second,
            segmentMeters = segmentMeters
        )
    }
    return candidates.minByOrNull { it.distanceMeters }
}

private fun segmentDistanceMeters(points: List<TripPointDto>, previousIndex: Int, nextIndex: Int): Double? {
    val previousCoordinate = points.getOrNull(previousIndex)?.coordinate() ?: return null
    val nextCoordinate = points.getOrNull(nextIndex)?.coordinate() ?: return null
    return haversineMeters(
        previousCoordinate.latitude,
        previousCoordinate.longitude,
        nextCoordinate.latitude,
        nextCoordinate.longitude
    )
}

private fun projectToSegment(start: Coordinate, end: Coordinate, liveLat: Double, liveLng: Double): Pair<Double, Double> {
    val metersPerDegreeLat = 111_320.0
    val metersPerDegreeLng = metersPerDegreeLat * cos(Math.toRadians((start.latitude + end.latitude) / 2.0))
    val ax = 0.0
    val ay = 0.0
    val bx = (end.longitude - start.longitude) * metersPerDegreeLng
    val by = (end.latitude - start.latitude) * metersPerDegreeLat
    val px = (liveLng - start.longitude) * metersPerDegreeLng
    val py = (liveLat - start.latitude) * metersPerDegreeLat
    val segmentLengthSquared = bx * bx + by * by
    if (segmentLengthSquared <= 1.0) return 0.0 to sqrt(px * px + py * py)
    val t = (((px - ax) * bx) + ((py - ay) * by)) / segmentLengthSquared
    val clamped = t.coerceIn(0.0, 1.0)
    val closestX = bx * clamped
    val closestY = by * clamped
    val distance = sqrt((px - closestX).pow(2) + (py - closestY).pow(2))
    return clamped to distance
}

private fun computeServiceDelayMinutes(point: TripPointDto?): Int? {
    if (point == null) return null
    val planned = point.plannedTime?.takeIf { it > 0 } ?: point.plannedExitTime?.takeIf { it > 0 } ?: return null
    val actual = point.entryTime?.takeIf { it > 0 }
        ?: point.exitTime?.takeIf { it > 0 }
        ?: point.eta?.takeIf { it > 0 }
        ?: point.etd?.takeIf { it > 0 }
        ?: return null
    return ((actual - planned) / 60L).toInt()
}

private fun progressFor(points: List<TripPointDto>, currentIndex: Int, segmentProgress: Float): Float {
    if (points.size <= 1) return if (points.isNotEmpty()) 1f else 0f
    return ((currentIndex.coerceIn(0, points.lastIndex) + segmentProgress.coerceIn(0f, 1f)) / points.lastIndex.toFloat())
        .coerceIn(0f, 1f)
}

private fun stopStatusText(
    point: TripPointDto,
    isFirst: Boolean,
    isLast: Boolean,
    isCurrent: Boolean,
    passed: Boolean,
    delayMinutes: Int?
): String {
    val base = when {
        isCurrent -> "Current bus location"
        isFirst -> "Starting point"
        isLast -> "Destination"
        passed -> "Passed"
        else -> "Upcoming"
    }
    val delay = when {
        delayMinutes == null -> ""
        delayMinutes > 2 -> " • ${delayMinutes} min late"
        delayMinutes < -2 -> " • ${kotlin.math.abs(delayMinutes)} min early"
        else -> " • On time"
    }
    val stopNo = point.sequence?.let { "Stop $it" }.orEmpty()
    return listOf(stopNo, base + delay).filter { it.isNotBlank() }.joinToString("  ")
}

private fun bottomStatusText(currentPoint: TripPointDto?, toName: String, liveVehicle: LiveVehicleDto?): String {
    val status = liveVehicle?.statusStr.orEmpty()
    val stop = currentPoint?.name.orEmpty()
    return when {
        status.isNotBlank() && stop.isNotBlank() -> "$status $stop"
        stop.isNotBlank() && stop.equals(toName, ignoreCase = true) -> "Arrived $stop"
        stop.isNotBlank() -> "Near $stop"
        status.isNotBlank() -> status
        else -> "Service status unavailable"
    }
}

private fun delayText(delayMinutes: Int?): String =
    when {
        delayMinutes == null -> "Delay unknown"
        delayMinutes > 2 -> "${delayMinutes} min Late"
        delayMinutes < -2 -> "${kotlin.math.abs(delayMinutes)} min Early"
        else -> "No Delay"
    }

private fun delayColor(delayMinutes: Int?): Color =
    when {
        delayMinutes == null -> Color(0xFF5F6368)
        delayMinutes > 2 -> Color(0xFFC51313)
        else -> Color(0xFF18864B)
    }

private fun liveArrivalDisplay(point: TripPointDto, serviceStarted: Boolean): String {
    if (!serviceStarted) return "---"
    return formatTime(point.entryTime ?: point.eta).ifBlank { "---" }
}

private fun liveDepartureDisplay(point: TripPointDto, isLast: Boolean, serviceStarted: Boolean): String {
    if (!serviceStarted) return "---"
    if (isLast && !point.exitTime.isUsableTime()) return "---"
    return formatTime(point.exitTime ?: point.etd).ifBlank { "---" }
}

private fun liveTimeColor(delayMinutes: Int?, serviceStarted: Boolean, colors: LiveTrackerColors): Color =
    when {
        !serviceStarted -> colors.mutedText
        delayMinutes != null && delayMinutes > 2 -> Color(0xFFFF6B6B)
        delayMinutes != null && delayMinutes < -2 -> Color(0xFF71D48C)
        else -> Color(0xFFFF6B6B)
    }

private fun stopStatusText(
    point: TripPointDto,
    isFirst: Boolean,
    isLast: Boolean,
    isCurrent: Boolean,
    passed: Boolean,
    serviceStarted: Boolean,
    delayMinutes: Int?
): String {
    val base = when {
        isCurrent && !serviceStarted -> "Not yet started"
        isCurrent -> "Current bus location"
        isFirst -> "Starting point"
        isLast -> "Destination"
        passed -> "Passed"
        else -> "Upcoming"
    }
    val delay = when {
        !serviceStarted -> ""
        delayMinutes == null -> ""
        delayMinutes > 2 -> " - ${delayMinutes} min late"
        delayMinutes < -2 -> " - ${abs(delayMinutes)} min early"
        else -> " - On time"
    }
    val stopNo = point.sequence?.let { "Stop $it" }.orEmpty()
    return listOf(stopNo, base + delay).filter { it.isNotBlank() }.joinToString("  ")
}

private fun currentBusMessage(
    point: TripPointDto,
    nextPoint: TripPointDto?,
    position: LiveBusPosition,
    delayMinutes: Int?,
    liveVehicle: LiveVehicleDto?,
    loadedAtMillis: Long
): String {
    val stop = point.name.orEmpty().ifBlank { "Stop ${point.sequence ?: ""}".trim() }
    val nextStop = nextPoint?.name.orEmpty()
    val firstStopWaiting = position.stopIndex == 0 &&
        !point.exitTime.isUsableTime() &&
        position.segmentProgress <= 0.035f
    if (!position.started || firstStopWaiting) {
        val scheduled = formatTime(point.plannedTime ?: point.plannedExitTime)
        return if (scheduled.isBlank()) {
            "Not yet started $stop"
        } else {
            "Not yet started $stop\n(Scheduled $scheduled)"
        }
    }

    val nextEntryTime = nextPoint?.entryTime
    val nextExitTime = nextPoint?.exitTime
    if (nextStop.isBlank()) {
        return "Arrived $stop"
    }

    val remainingMeters = distanceToNextOnSegmentMeters(position, liveVehicle, nextPoint)
    val distanceToNext = remainingMeters?.let(::formatDistanceMeters).orEmpty()
    val currentExitTime = point.exitTime
    val apiDepartedCurrent = currentExitTime.isUsableTime()
    val gpsShowsMovement =
        position.source == PositionSource.PREVIOUS_POINT ||
            (position.source == PositionSource.GPS && position.segmentProgress > 0.08f)
    val reachedNextStop =
        (remainingMeters != null && remainingMeters <= 40.0) ||
        position.segmentProgress >= 0.90f ||
        position.source == PositionSource.NEXT_POINT ||
            nextEntryTime.isUsableTime()
    if (reachedNextStop) {
        return if (nextExitTime.isUsableTime()) "Departed $nextStop" else "Reached $nextStop"
    }

    if (apiDepartedCurrent && position.segmentProgress <= 0.18f) {
        val distanceLine = if (distanceToNext.isNotBlank() && nextStop.isNotBlank()) {
            "\n$distanceToNext to $nextStop"
        } else {
            ""
        }
        return "Departed $stop$distanceLine"
    }

    if (
        distanceToNext.isNotBlank() &&
        nextStop.isNotBlank() &&
        remainingMeters != null &&
        (remainingMeters > 250.0 || apiDepartedCurrent || gpsShowsMovement) &&
        position.segmentProgress >= 0.08f
    ) {
        return "$distanceToNext to $nextStop"
    }

    if ((remainingMeters != null && remainingMeters <= 250.0) || position.segmentProgress > 0.72f) {
        val distanceLine = distanceToNext.takeIf { it.isNotBlank() }?.let { "\n$it to stop" }.orEmpty()
        return "Approaching $nextStop$distanceLine"
    }

    val verb = when {
        point.entryTime.isUsableTime() || position.source == PositionSource.CURRENT_POINT -> "Arrived"
        position.source == PositionSource.NEXT_POINT -> "Approaching"
        else -> "Near"
    }
    val displayStop = if (verb == "Approaching") nextStop else stop
    val delay = when {
        delayMinutes == null -> ""
        delayMinutes > 2 -> " ${delayMinutes} min late"
        delayMinutes < -2 -> " ${abs(delayMinutes)} min early"
        else -> ""
    }
    return "$verb $displayStop$delay"
}

private fun bottomStatusText(
    currentPoint: TripPointDto?,
    toName: String,
    liveVehicle: LiveVehicleDto?,
    serviceStatus: String,
    serviceStarted: Boolean
): String {
    if (!serviceStarted) return "Not yet started"
    val status = liveVehicle?.statusStr.orEmpty().ifBlank { serviceStatus }
    val stop = currentPoint?.name.orEmpty()
    return when {
        stop.isNotBlank() && stop.equals(toName, ignoreCase = true) -> "Arrived $stop"
        status.isNotBlank() && stop.isNotBlank() -> "$status $stop"
        stop.isNotBlank() -> "Near $stop"
        status.isNotBlank() -> status
        else -> "Service status unavailable"
    }
}

private fun delayText(delayMinutes: Int?, serviceStarted: Boolean): String =
    when {
        !serviceStarted -> "Not yet started"
        delayMinutes == null -> "Delay unknown"
        delayMinutes > 2 -> "${delayMinutes} min Late"
        delayMinutes < -2 -> "${abs(delayMinutes)} min Early"
        else -> "No Delay"
    }

private fun delayColor(delayMinutes: Int?, serviceStarted: Boolean): Color =
    when {
        !serviceStarted -> Color(0xFF5F6368)
        delayMinutes == null -> Color(0xFF5F6368)
        delayMinutes > 2 -> Color(0xFFC51313)
        else -> Color(0xFF18864B)
    }

private fun expectedReachTime(point: TripPointDto?, serviceStarted: Boolean): String {
    if (point == null) return ""
    val value = if (serviceStarted) {
        point.eta ?: point.etd ?: point.plannedTime ?: point.plannedExitTime
    } else {
        point.plannedTime ?: point.plannedExitTime
    }
    return formatTime(value)
}

private fun tripDayLabel(points: List<TripPointDto>): String {
    val first = points.asSequence()
        .flatMap { sequenceOf(it.plannedTime, it.plannedExitTime, it.entryTime, it.exitTime) }
        .filterNotNull()
        .firstOrNull { it > 86_400L }
        ?: return "Day 1"
    return "Day 1 - ${SimpleDateFormat("MMM dd, EEE", Locale.US).format(Date(first * 1000L))}"
}

private fun eventAgeText(eventSeconds: Long?, loadedAtMillis: Long): String {
    val eventMillis = eventSeconds?.takeIf { it > 86_400L }?.let { it * 1000L }
    if (eventMillis == null) return "Updated ${relativeUpdatedText(loadedAtMillis)}"
    val diffSeconds = ((System.currentTimeMillis() - eventMillis) / 1000L).coerceAtLeast(0L)
    return when {
        diffSeconds < 60 -> "Updated few seconds ago"
        diffSeconds < 3600 -> "Updated ${diffSeconds / 60} min ago"
        else -> "Updated ${diffSeconds / 3600} hours ago"
    }
}

private fun relativeUpdatedText(loadedAtMillis: Long): String {
    val diffSeconds = ((System.currentTimeMillis() - loadedAtMillis) / 1000L).coerceAtLeast(0L)
    return when {
        diffSeconds < 5 -> "just now"
        diffSeconds < 60 -> "$diffSeconds sec ago"
        diffSeconds < 3600 -> "${diffSeconds / 60} min ago"
        else -> "${diffSeconds / 3600} hours ago"
    }
}

private fun distanceToPointText(liveVehicle: LiveVehicleDto?, point: TripPointDto?): String {
    val meters = distanceToPointMeters(liveVehicle, point) ?: return ""
    if (meters <= 0.0) return ""
    return formatDistanceMeters(meters)
}

private fun distanceToNextOnSegmentMeters(
    position: LiveBusPosition,
    liveVehicle: LiveVehicleDto?,
    nextPoint: TripPointDto?
): Double? {
    return position.segmentMeters?.let { meters ->
        meters * (1.0 - position.segmentProgress.coerceIn(0f, 1f).toDouble())
    } ?: distanceToPointMeters(liveVehicle, nextPoint)
}

private fun distanceToNextOnSegmentText(
    position: LiveBusPosition,
    liveVehicle: LiveVehicleDto?,
    nextPoint: TripPointDto?
): String {
    val remainingMeters = distanceToNextOnSegmentMeters(position, liveVehicle, nextPoint)
    val value = remainingMeters?.takeIf { it > 0.0 } ?: return ""
    return formatDistanceMeters(value)
}

private fun formatDistanceMeters(value: Double): String {
    return if (value >= 1000.0) {
        String.format(Locale.US, "%.1f km", value / 1000.0)
    } else {
        "${value.roundToInt()} m"
    }
}

private fun formatDistanceToNext(liveVehicle: LiveVehicleDto?, nextPoint: TripPointDto?): String {
    val meters = distanceToPointMeters(liveVehicle, nextPoint) ?: return ""
    if (meters <= 0.0) return ""
    return if (meters >= 1000.0) {
        String.format(Locale.US, "%.1f km", meters / 1000.0)
    } else {
        "${meters.roundToInt()} m"
    }
}

private fun cumulativeStopDistanceTexts(points: List<TripPointDto>): List<String> {
    var previousCoordinate: Coordinate? = null
    var totalMeters = 0.0
    return points.mapIndexed { index, point ->
        val coordinate = point.coordinate()
        if (coordinate != null && previousCoordinate != null) {
            totalMeters += haversineMeters(
                previousCoordinate!!.latitude,
                previousCoordinate!!.longitude,
                coordinate.latitude,
                coordinate.longitude
            )
        }
        if (coordinate != null) previousCoordinate = coordinate
        when {
            coordinate == null -> ""
            index == 0 -> "0 km"
            totalMeters >= 1000.0 -> "${(totalMeters / 1000.0).roundToInt()} km"
            else -> "${totalMeters.roundToInt()} m"
        }
    }
}

private fun distanceToPointMeters(liveVehicle: LiveVehicleDto?, point: TripPointDto?): Double? {
    val liveLat = liveVehicle?.latitude
    val liveLng = liveVehicle?.longitude
    val coordinate = point?.coordinate()
    return if (
        liveLat.isUsableCoordinate() &&
        liveLng.isUsableCoordinate() &&
        coordinate != null
    ) {
        haversineMeters(liveLat!!, liveLng!!, coordinate.latitude, coordinate.longitude)
    } else {
        null
    }
}

private data class Coordinate(val latitude: Double, val longitude: Double)

private fun TripPointDto.coordinate(): Coordinate? {
    val json = geometry?.coordinates ?: return null
    return firstCoordinate(json)
}

private fun firstCoordinate(element: JsonElement?): Coordinate? {
    if (element == null || !element.isJsonArray) return null
    val array = element.asJsonArray
    if (array.size() >= 2 && array[0].isJsonPrimitive && array[1].isJsonPrimitive) {
        val longitude = runCatching { array[0].asDouble }.getOrNull()
        val latitude = runCatching { array[1].asDouble }.getOrNull()
        if (latitude != null && longitude != null && latitude != 0.0 && longitude != 0.0) {
            return Coordinate(latitude = latitude, longitude = longitude)
        }
    }
    for (child in array) {
        firstCoordinate(child)?.let { return it }
    }
    return null
}

private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val rLat1 = Math.toRadians(lat1)
    val rLat2 = Math.toRadians(lat2)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(rLat1) * cos(rLat2) * sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadius * c
}

private fun Long?.isUsableTime(): Boolean = this != null && this > 0L

private fun Double?.isUsableCoordinate(): Boolean = this != null && this != 0.0
