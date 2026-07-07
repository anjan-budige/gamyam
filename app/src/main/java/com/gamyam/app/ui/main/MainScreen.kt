package com.gamyam.app.ui.main

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.gamyam.app.BuildConfig
import com.gamyam.app.FromToServicesResult
import com.gamyam.app.R
import com.gamyam.app.ReservationSearchList
import com.gamyam.app.RouteNumberSearchList
import com.gamyam.app.ServiceIdSearchList
import com.gamyam.app.VehicleSearchList
import com.gamyam.app.data.ApiClient
import com.gamyam.app.data.AppThemeMode
import com.gamyam.app.data.AuthManager
import com.gamyam.app.data.FeedbackSender
import com.gamyam.app.data.SavedHomeSearchState
import com.gamyam.app.data.SavedTabSearchState
import com.gamyam.app.theme.ActionGreen
import com.gamyam.app.theme.AppBarBlue
import kotlinx.coroutines.launch

// ── Poppins via Google Fonts ──────────────────────────────────────────────────
private val gfProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs
)
val PoppinsFamily: FontFamily = FontFamily(
    Font(GoogleFont("Poppins"), gfProvider, FontWeight.Normal),
    Font(GoogleFont("Poppins"), gfProvider, FontWeight.Medium),
    Font(GoogleFont("Poppins"), gfProvider, FontWeight.SemiBold),
    Font(GoogleFont("Poppins"), gfProvider, FontWeight.Bold)
)

// ── Tabs ──────────────────────────────────────────────────────────────────────
enum class ServiceTab { DISTRICT, CITY, AIRPORT }

// ── Fixed brand colours ───────────────────────────────────────────────────────
private val HeaderBlue = AppBarBlue      // #1448A6
private val BtnGreen   = ActionGreen     // #2E7D32

// ── Stop code abbreviation helper ─────────────────────────────────────────────
internal fun stopCode(stop: Stop): String {
    val id = stop.id.trim()
    if (id.isNotBlank() && id.length <= 6 && id.all { it.isLetterOrDigit() }) return id.uppercase()
    val words = stop.name.trim().split(Regex("""[\s\-/]+"""))
    return words.take(3).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
        .ifEmpty { stop.name.take(3).uppercase() }
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun lastKnownLocation(context: Context): Location? {
    if (!hasLocationPermission(context)) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    return listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    ).mapNotNull { provider ->
        runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
    }.maxByOrNull { it.time }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ROOT SCREEN
// ─────────────────────────────────────────────────────────────────────────────
private fun SavedHomeSearchState.savedTab(): ServiceTab =
    runCatching { ServiceTab.valueOf(tab) }.getOrDefault(ServiceTab.DISTRICT)

private fun SavedHomeSearchState.tabStateFor(tab: ServiceTab): SavedTabSearchState =
    tabStates[tab.name] ?: if (savedTab() == tab && (fromName.isNotBlank() || toName.isNotBlank())) {
        SavedTabSearchState(
            fromName = fromName,
            fromId = fromId,
            fromGeozoneType = fromGeozoneType,
            fromSearchGeozoneType = fromSearchGeozoneType,
            toName = toName,
            toId = toId,
            toGeozoneType = toGeozoneType,
            toSearchGeozoneType = toSearchGeozoneType
        )
    } else {
        SavedTabSearchState()
    }

private fun SavedTabSearchState.savedFromStop(): Stop? =
    if (fromName.isBlank()) null else Stop(
        id = fromId,
        name = fromName,
        searchName = fromName,
        geozoneType = fromGeozoneType,
        searchGeozoneType = fromSearchGeozoneType
    )

private fun SavedTabSearchState.savedToStop(): Stop? =
    if (toName.isBlank()) null else Stop(
        id = toId,
        name = toName,
        searchName = toName,
        geozoneType = toGeozoneType,
        searchGeozoneType = toSearchGeozoneType
    )

private fun savedTabStateFor(
    fromText: String,
    from: Stop?,
    toText: String,
    to: Stop?
): SavedTabSearchState =
    SavedTabSearchState(
        fromName = from?.name ?: fromText,
        fromId = from?.id.orEmpty(),
        fromGeozoneType = from?.geozoneType.orEmpty(),
        fromSearchGeozoneType = from?.searchGeozoneType.orEmpty(),
        toName = to?.name ?: toText,
        toId = to?.id.orEmpty(),
        toGeozoneType = to?.geozoneType.orEmpty(),
        toSearchGeozoneType = to?.searchGeozoneType.orEmpty()
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick:     (NavKey) -> Unit,
    modifier:        Modifier = Modifier,
    searchViewModel: SearchViewModel = viewModel(),
    authManager: AuthManager? = null
) {
    val selectedTab by searchViewModel.selectedTab.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // From/To state lifted to MainScreen — passed into search card
    var fromStop     by remember { mutableStateOf("") }
    var toStop       by remember { mutableStateOf("") }
    var selectedFrom by remember { mutableStateOf<Stop?>(null) }
    var selectedTo   by remember { mutableStateOf<Stop?>(null) }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded   by remember { mutableStateOf(false) }
    var districtServiceIdQuery by remember { mutableStateOf("") }
    var districtReservationQuery by remember { mutableStateOf("") }
    var districtVehicleQuery by remember { mutableStateOf("") }
    var cityRouteQuery by remember { mutableStateOf("") }
    var cityVehicleQuery by remember { mutableStateOf("") }
    var airportRouteQuery by remember { mutableStateOf("") }
    var airportVehicleQuery by remember { mutableStateOf("") }

    val fromList by searchViewModel.filteredFromStops.collectAsState()
    val toList   by searchViewModel.filteredToStops.collectAsState()
    val selectedCityId by searchViewModel.selectedCityId.collectAsState()
    val selectedCityName by searchViewModel.selectedCityName.collectAsState()
    val savedHomeState by searchViewModel.savedHomeSearchState.collectAsState()
    val themeMode by searchViewModel.themeMode.collectAsState()
    val updateStopsStatus by searchViewModel.updateStopsStatus.collectAsState()
    val cityRestoreCandidates by searchViewModel.filteredCities.collectAsState()
    var nearbyStatus by remember { mutableStateOf("") }
    var isLoadingNearby by remember { mutableStateOf(false) }
    var autoNearbyLoadedFor by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerWidth = LocalConfiguration.current.screenWidthDp.dp * 0.8f
    var lastBackPressedAt by remember { mutableLongStateOf(0L) }

    BackHandler {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
            return@BackHandler
        }

        val now = System.currentTimeMillis()
        if (now - lastBackPressedAt <= 2_000L) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressedAt = now
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    fun nearbyCacheKey(): String = listOf(
        selectedTab.name,
        if (selectedTab == ServiceTab.CITY) selectedCityId else ""
    ).joinToString(":")

    fun saveCurrentHomeState(
        tab: ServiceTab = selectedTab,
        fromText: String = fromStop,
        from: Stop? = selectedFrom,
        toText: String = toStop,
        to: Stop? = selectedTo,
        cityId: String = selectedCityId,
        cityName: String = selectedCityName
    ) {
        val stateForTab = savedTabStateFor(
            fromText = fromText,
            from = from?.takeIf { it.name == fromText },
            toText = toText,
            to = to?.takeIf { it.name == toText }
        )
        searchViewModel.saveHomeSearchState(
            savedHomeState.copy(
                tab = tab.name,
                cityId = cityId,
                cityName = cityName,
                tabStates = savedHomeState.tabStates + (tab.name to stateForTab)
            )
        )
    }

    fun restoreInputsForTab(tab: ServiceTab) {
        val saved = savedHomeState.tabStateFor(tab)
        fromStop = saved.fromName
        toStop = saved.toName
        selectedFrom = saved.savedFromStop()
        selectedTo = saved.savedToStop()
        fromExpanded = false
        toExpanded = false
        searchViewModel.searchFromStops(saved.fromName, tab)
        searchViewModel.searchToStops(saved.toName, tab)
    }

    fun loadNearbyStopsForCurrentTab(onLoaded: ((Boolean) -> Unit)? = null) {
        val manager = authManager ?: run {
            nearbyStatus = "Location search unavailable"
            onLoaded?.invoke(false)
            return
        }
        if (selectedTab == ServiceTab.CITY && selectedCityId.isBlank()) {
            nearbyStatus = "Select a city first"
            onLoaded?.invoke(false)
            return
        }
        val location = lastKnownLocation(context) ?: run {
            nearbyStatus = "Location not available"
            onLoaded?.invoke(false)
            return
        }
        scope.launch {
            isLoadingNearby = true
            nearbyStatus = "Finding nearby stops"
            var loaded = false
            try {
                val operationType = when (selectedTab) {
                    ServiceTab.DISTRICT -> 1
                    ServiceTab.CITY -> 2
                    ServiceTab.AIRPORT -> 3
                }
                val nearbyStops = manager.authenticatedCall { auth ->
                    ApiClient.tgsrtcApiService.getNearestStops(
                        auth = auth,
                        operationType = operationType,
                        cityId = if (selectedTab == ServiceTab.CITY) selectedCityId else "",
                        latitude = location.latitude,
                        longitude = location.longitude,
                        isAirport = selectedTab == ServiceTab.AIRPORT
                    )
                }
                val matchedCount = searchViewModel.prioritizeNearbyStops(
                    selectedTab,
                    nearbyStops.map { NearbyStopHint(id = it.id, name = it.name.orEmpty()) }
                )
                loaded = matchedCount > 0
                nearbyStatus = if (matchedCount == 0) "No nearby stops found" else "$matchedCount nearby stops"
            } catch (e: Exception) {
                nearbyStatus = e.message ?: "Unable to find nearby stops"
            } finally {
                isLoadingNearby = false
                onLoaded?.invoke(loaded)
            }
        }
    }

    fun openNearbySuggestions(isFrom: Boolean) {
        if (isFrom) {
            toExpanded = false
        } else {
            fromExpanded = false
        }

        val shown = if (isFrom) {
            searchViewModel.showNearbyFromStops(selectedTab)
        } else {
            searchViewModel.showNearbyToStops(selectedTab)
        }
        if (shown) {
            if (isFrom) fromExpanded = true else toExpanded = true
            return
        }

        val key = nearbyCacheKey()
        if (!isLoadingNearby && key !in autoNearbyLoadedFor) {
            autoNearbyLoadedFor = autoNearbyLoadedFor + key
            loadNearbyStopsForCurrentTab { loaded ->
                if (loaded) {
                    val opened = if (isFrom) {
                        searchViewModel.showNearbyFromStops(selectedTab)
                    } else {
                        searchViewModel.showNearbyToStops(selectedTab)
                    }
                    if (isFrom) fromExpanded = opened else toExpanded = opened
                }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) loadNearbyStopsForCurrentTab() else nearbyStatus = "Location permission denied"
    }

    LaunchedEffect(Unit) {
        searchViewModel.loadData()
    }

    LaunchedEffect(savedHomeState) {
        if (searchViewModel.shouldRestoreHomeState(savedHomeState)) {
            val restoredTab = savedHomeState.savedTab()
            if (restoredTab != selectedTab) {
                searchViewModel.selectTab(restoredTab)
            }
            restoreInputsForTab(restoredTab)
        }
    }

    LaunchedEffect(cityRestoreCandidates.size, savedHomeState.cityId, savedHomeState.cityName) {
        val savedCity = if (
            cityRestoreCandidates.isNotEmpty() &&
            searchViewModel.shouldRestoreSavedCity(savedHomeState)
        ) {
            cityRestoreCandidates.firstOrNull { it.id == savedHomeState.cityId }
                ?: cityRestoreCandidates.firstOrNull { it.name.equals(savedHomeState.cityName, ignoreCase = true) }
        } else {
            null
        }
        savedCity?.let { city ->
            searchViewModel.selectCity(city)
        }
    }

    LaunchedEffect(selectedTab) {
        restoreInputsForTab(selectedTab)
        nearbyStatus = ""
        autoNearbyLoadedFor = emptySet()
    }

    LaunchedEffect(selectedCityId) {
        if (selectedTab == ServiceTab.CITY) {
            fromExpanded = false
            toExpanded = false
            searchViewModel.searchFromStops(fromStop, selectedTab)
            searchViewModel.searchToStops(toStop, selectedTab)
            saveCurrentHomeState(cityId = selectedCityId, cityName = selectedCityName)
        }
    }

    if (showAboutDialog) {
        AboutGamyamDialog(onDismiss = { showAboutDialog = false })
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            selectedMode = themeMode,
            onDismiss = { showThemeDialog = false },
            onSelect = {
                searchViewModel.saveThemeMode(it)
                showThemeDialog = false
            }
        )
    }

    if (showFeedbackDialog) {
        FeedbackDialog(
            source = "Home menu",
            onDismiss = { showFeedbackDialog = false }
        )
    }

    if (updateStopsStatus.isNotBlank()) {
        UpdateStopsStatusDialog(
            status = updateStopsStatus,
            onDismiss = { searchViewModel.clearUpdateStopsStatus() }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            GamyamMenuDrawer(
                width = drawerWidth,
                themeMode = themeMode,
                onCloseClick = { scope.launch { drawerState.close() } },
                onThemeClick = {
                    scope.launch { drawerState.close() }
                    showThemeDialog = true
                },
                onAboutClick = {
                    scope.launch { drawerState.close() }
                    showAboutDialog = true
                },
                onUpdateStopsClick = {
                    scope.launch { drawerState.close() }
                    searchViewModel.updateStops(authManager)
                },
                onFeedbackClick = {
                    scope.launch { drawerState.close() }
                    showFeedbackDialog = true
                }
            )
        }
    ) {
    Scaffold(
        topBar         = {
            GamyamTopBar(
                selectedTab = selectedTab,
                onMenuClick = { scope.launch { drawerState.open() } },
                onTabSelected = {
                    searchViewModel.selectTab(it)
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier       = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Main search card (suggestions live INSIDE the card) ────────
            MainSearchPanel(
                selectedTab  = selectedTab,
                fromStop     = fromStop,
                toStop       = toStop,
                fromExpanded = fromExpanded,
                toExpanded   = toExpanded,
                selectedFrom = selectedFrom,
                selectedTo   = selectedTo,
                fromList     = fromList,
                toList       = toList,
                onFromChange = { q ->
                    fromStop     = q
                    selectedFrom = null
                    fromExpanded = q.isNotEmpty()
                    toExpanded   = false
                    searchViewModel.searchFromStops(q, selectedTab)
                    saveCurrentHomeState(fromText = q, from = null)
                },
                onToChange   = { q ->
                    toStop       = q
                    selectedTo   = null
                    toExpanded   = q.isNotEmpty()
                    fromExpanded = false
                    searchViewModel.searchToStops(q, selectedTab)
                    saveCurrentHomeState(toText = q, to = null)
                },
                onFromSelect = { stop ->
                    fromStop = stop.name
                    selectedFrom = stop
                    fromExpanded = false
                    saveCurrentHomeState(fromText = stop.name, from = stop)
                },
                onToSelect   = { stop ->
                    toStop = stop.name
                    selectedTo = stop
                    toExpanded = false
                    saveCurrentHomeState(toText = stop.name, to = stop)
                },
                onFromClear  = {
                    fromStop = ""
                    selectedFrom = null
                    fromExpanded = false
                    saveCurrentHomeState(fromText = "", from = null)
                },
                onToClear    = {
                    toStop = ""
                    selectedTo = null
                    toExpanded = false
                    saveCurrentHomeState(toText = "", to = null)
                },
                onFromActivate = {
                    toExpanded = false
                    fromExpanded = if (fromStop.isBlank()) {
                        openNearbySuggestions(isFrom = true)
                        fromExpanded
                    } else {
                        searchViewModel.searchFromStops(fromStop, selectedTab)
                        true
                    }
                },
                onToActivate = {
                    fromExpanded = false
                    toExpanded = if (toStop.isBlank()) {
                        openNearbySuggestions(isFrom = false)
                        toExpanded
                    } else {
                        searchViewModel.searchToStops(toStop, selectedTab)
                        true
                    }
                },
                onSwap       = {
                    val t = fromStop; fromStop = toStop; toStop = t
                    val selected = selectedFrom; selectedFrom = selectedTo; selectedTo = selected
                    saveCurrentHomeState(
                        fromText = fromStop,
                        from = selectedFrom,
                        toText = toStop,
                        to = selectedTo
                    )
                },
                nearbyStatus = nearbyStatus,
                isLoadingNearby = isLoadingNearby,
                onNearbyClick = {
                    if (hasLocationPermission(context)) {
                        autoNearbyLoadedFor = autoNearbyLoadedFor - nearbyCacheKey()
                        loadNearbyStopsForCurrentTab()
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                onSearchClick = {
                    val fv = selectedFrom?.takeIf { it.name == fromStop } ?: fromList.find { it.name == fromStop }
                    val tv = selectedTo?.takeIf { it.name == toStop } ?: toList.find { it.name == toStop }
                    if (fv != null && tv != null) {
                        searchViewModel.saveSearch(fv, tv, stopCode(fv), stopCode(tv), selectedTab)
                        val operationType = when (selectedTab) {
                            ServiceTab.DISTRICT -> 1
                            ServiceTab.CITY -> 2
                            ServiceTab.AIRPORT -> 3
                        }
                        fun Stop.requestGeozoneType(): String =
                            (if (selectedTab == ServiceTab.AIRPORT) searchGeozoneType.ifBlank { geozoneType } else geozoneType)
                                .ifBlank { "stage" }

                        onItemClick(
                            FromToServicesResult(
                                fromId = fv.id,
                                fromName = fv.name,
                                fromGeozoneType = fv.requestGeozoneType(),
                                toId = tv.id,
                                toName = tv.name,
                                toGeozoneType = tv.requestGeozoneType(),
                                operationType = operationType,
                                cityId = if (selectedTab == ServiceTab.CITY) selectedCityId else "",
                                isAirport = selectedTab == ServiceTab.AIRPORT,
                                requestId = System.nanoTime()
                            )
                        )
                    }
                },
                viewModel    = searchViewModel
            )

            // ── Secondary panels (always visible) ─────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Crossfade(
                    targetState   = selectedTab,
                    animationSpec = tween(200),
                    label         = "secondaryPanels"
                ) { tab ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        when (tab) {
                            ServiceTab.DISTRICT -> {
                                SecondarySearchPanel(
                                    icon          = Icons.Default.Numbers,
                                    placeholder   = "Service ID",
                                    query         = districtServiceIdQuery,
                                    onQueryChange = { districtServiceIdQuery = it },
                                    onSearchClick = { onItemClick(ServiceIdSearchList(districtServiceIdQuery.trim(), selectedTab.name)) },
                                    onCardClick   = { onItemClick(ServiceIdSearchList(districtServiceIdQuery.trim(), selectedTab.name)) }
                                )
                                SecondarySearchPanel(
                                    icon          = Icons.Default.DirectionsBus,
                                    placeholder   = "Reservation Service Number",
                                    query         = districtReservationQuery,
                                    onQueryChange = { districtReservationQuery = it },
                                    onSearchClick = { onItemClick(ReservationSearchList(districtReservationQuery.trim())) },
                                    onCardClick   = { onItemClick(ReservationSearchList(districtReservationQuery.trim())) }
                                )
                                SecondarySearchPanel(
                                    icon          = Icons.Default.ConfirmationNumber,
                                    placeholder   = "Bus / Vehicle Number",
                                    query         = districtVehicleQuery,
                                    onQueryChange = { districtVehicleQuery = it },
                                    onSearchClick = { onItemClick(VehicleSearchList(districtVehicleQuery.trim(), operationType = 1)) },
                                    onCardClick   = { onItemClick(VehicleSearchList(districtVehicleQuery.trim(), operationType = 1)) }
                                )
                            }
                            ServiceTab.CITY -> {
                                SecondarySearchPanel(
                                    icon          = Icons.Default.Route,
                                    placeholder   = "Search Route Number",
                                    query         = cityRouteQuery,
                                    onQueryChange = { cityRouteQuery = it },
                                    onSearchClick = { onItemClick(RouteNumberSearchList(cityRouteQuery.trim(), selectedCityId, operationType = 2)) },
                                    onCardClick   = { onItemClick(RouteNumberSearchList(cityRouteQuery.trim(), selectedCityId, operationType = 2)) }
                                )
                                SecondarySearchPanel(
                                    icon          = Icons.Default.AltRoute,
                                    placeholder   = "City Bus Number",
                                    query         = cityVehicleQuery,
                                    onQueryChange = { cityVehicleQuery = it },
                                    onSearchClick = { onItemClick(VehicleSearchList(cityVehicleQuery.trim(), operationType = 2, cityId = selectedCityId)) },
                                    onCardClick   = { onItemClick(VehicleSearchList(cityVehicleQuery.trim(), operationType = 2, cityId = selectedCityId)) }
                                )
                            }
                            ServiceTab.AIRPORT -> {
                                SecondarySearchPanel(
                                    icon          = Icons.Default.Route,
                                    placeholder   = "Search Airport Route",
                                    query         = airportRouteQuery,
                                    onQueryChange = { airportRouteQuery = it },
                                    onSearchClick = { onItemClick(RouteNumberSearchList(airportRouteQuery.trim(), "", operationType = 3, isAirport = true)) },
                                    onCardClick   = { onItemClick(RouteNumberSearchList(airportRouteQuery.trim(), "", operationType = 3, isAirport = true)) }
                                )
                                SecondarySearchPanel(
                                    icon          = Icons.Default.AltRoute,
                                    placeholder   = "Airport Bus Number",
                                    query         = airportVehicleQuery,
                                    onQueryChange = { airportVehicleQuery = it },
                                    onSearchClick = { onItemClick(VehicleSearchList(airportVehicleQuery.trim(), operationType = 3, isAirport = true)) },
                                    onCardClick   = { onItemClick(VehicleSearchList(airportVehicleQuery.trim(), operationType = 3, isAirport = true)) }
                                )
                            }
                        }
                    }
                }

                SearchHistoryCard(
                    viewModel = searchViewModel,
                    onOpenSearch = { entry ->
                        val historyServiceId = entry.serviceId.takeIf { it > 0 } ?: entry.toName.toIntOrNull()
                        if (entry.isServiceIdSearch && historyServiceId != null) {
                            onItemClick(
                                com.gamyam.app.ServiceTripDetail(
                                    serviceId = historyServiceId,
                                    displayNumber = entry.serviceLabel.ifBlank { historyServiceId.toString() },
                                    operationType = entry.operationType
                                )
                            )
                        } else if (entry.fromId.isNotBlank() && entry.toId.isNotBlank()) {
                            onItemClick(
                                FromToServicesResult(
                                    fromId = entry.fromId,
                                    fromName = entry.fromName,
                                    fromGeozoneType = entry.fromGeozoneType.ifBlank { "stage" },
                                    toId = entry.toId,
                                    toName = entry.toName,
                                    toGeozoneType = entry.toGeozoneType.ifBlank { "stage" },
                                    operationType = entry.operationType,
                                    cityId = entry.cityId,
                                    isAirport = entry.isAirport,
                                    requestId = System.nanoTime()
                                )
                            )
                        }
                    }
                )
            }
        }
    }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TOP APP-BAR + tab row  (both on the same blue background)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamyamTopBar(
    selectedTab: ServiceTab,
    onMenuClick: () -> Unit,
    onTabSelected: (ServiceTab) -> Unit
) {
    Column(modifier = Modifier.background(HeaderBlue)) {
        TopAppBar(
            title = {
                Text(
                    "Gamyam",
                    fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold,
                    fontSize = 21.sp, color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, "Menu", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = HeaderBlue)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderBlue)
                .padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
        ) {
            ServiceTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (selected) Color.White else Color.Transparent)
                        .clickable(remember { MutableInteractionSource() }, null) { onTabSelected(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        tab.name,
                        fontFamily    = PoppinsFamily,
                        fontWeight    = FontWeight.SemiBold,
                        fontSize      = 12.sp,
                        color         = if (selected) HeaderBlue else Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  BOTTOM NAV  (fully theme-adaptive)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GamyamMenuDrawer(
    width: Dp,
    themeMode: AppThemeMode,
    onCloseClick: () -> Unit,
    onThemeClick: () -> Unit,
    onAboutClick: () -> Unit,
    onUpdateStopsClick: () -> Unit,
    onFeedbackClick: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .width(width),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        drawerShape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HeaderBlue)
                    .padding(start = 18.dp, end = 8.dp, top = 18.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(54.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.gamyam),
                            contentDescription = "Gamyam",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Gamyam",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Smart bus companion ♥",
                        fontFamily = PoppinsFamily,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onCloseClick) {
                    Icon(Icons.Default.Close, "Close menu", tint = Color.White)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DrawerActionRow(Icons.Default.Palette, "Theme", themeMode.label(), onThemeClick)
                DrawerActionRow(Icons.Default.Refresh, "Update stops", "Fetch latest stations", onUpdateStopsClick)
                DrawerActionRow(Icons.Default.Feedback, "Feedback", "Send a message", onFeedbackClick)
                DrawerActionRow(Icons.Default.Info, "About", "Version ${BuildConfig.VERSION_NAME}", onAboutClick)
            }
        }
    }
}

@Composable
private fun DrawerActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = HeaderBlue.copy(alpha = 0.10f),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = HeaderBlue, modifier = Modifier.size(21.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    fontFamily = PoppinsFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun AppThemeMode.label(): String =
    when (this) {
        AppThemeMode.SYSTEM -> "System (default)"
        AppThemeMode.LIGHT -> "Light"
        AppThemeMode.DARK -> "Dark"
    }

@Composable
private fun ThemePickerDialog(
    selectedMode: AppThemeMode,
    onDismiss: () -> Unit,
    onSelect: (AppThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme", fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ThemeModeRow(AppThemeMode.SYSTEM, selectedMode, Icons.Default.Settings, onSelect)
                ThemeModeRow(AppThemeMode.LIGHT, selectedMode, Icons.Default.LightMode, onSelect)
                ThemeModeRow(AppThemeMode.DARK, selectedMode, Icons.Default.DarkMode, onSelect)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", fontFamily = PoppinsFamily) }
        }
    )
}

@Composable
private fun ThemeModeRow(
    mode: AppThemeMode,
    selectedMode: AppThemeMode,
    icon: ImageVector,
    onSelect: (AppThemeMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onSelect(mode) }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = HeaderBlue, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            mode.label(),
            fontFamily = PoppinsFamily,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        RadioButton(selected = mode == selectedMode, onClick = { onSelect(mode) })
    }
}

@Composable
private fun AboutGamyamDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About", fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.gamyam),
                    contentDescription = "Gamyam",
                    modifier = Modifier.size(72.dp)
                )
                Text(
                    "Gamyam",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Version ${BuildConfig.VERSION_NAME}",
                    fontFamily = PoppinsFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Developed by : Anjan",
                    fontFamily = PoppinsFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Live bus tracking, route search, stop updates, trip history and arrival alarms for smoother daily travel.",
                    fontFamily = PoppinsFamily,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", fontFamily = PoppinsFamily) }
        }
    )
}

@Composable
private fun FeedbackDialog(
    source: String,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        title = { Text("Feedback", fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (result.isNotBlank()) {
                    Text(
                        result,
                        fontFamily = PoppinsFamily,
                        fontSize = 14.sp,
                        color = if (result.startsWith("Successfully")) BtnGreen else MaterialTheme.colorScheme.error
                    )
                } else {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it.take(1000) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        label = { Text("Message") },
                        placeholder = { Text("Write your feedback") },
                        enabled = !sending,
                        minLines = 4,
                        textStyle = TextStyle(fontFamily = PoppinsFamily, color = MaterialTheme.colorScheme.onSurface)
                    )
                }
            }
        },
        confirmButton = {
            if (result.isNotBlank()) {
                TextButton(onClick = onDismiss) { Text("OK", fontFamily = PoppinsFamily) }
            } else {
                TextButton(
                    enabled = message.isNotBlank() && !sending,
                    onClick = {
                        sending = true
                        scope.launch {
                            val sent = FeedbackSender.send(source, message)
                            sending = false
                            result = if (sent.isSuccess) {
                                "Successfully sent feedback. Thank you."
                            } else {
                                sent.exceptionOrNull()?.message ?: "Unable to send feedback"
                            }
                        }
                    }
                ) {
                    if (sending) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Send", fontFamily = PoppinsFamily)
                    }
                }
            }
        },
        dismissButton = {
            if (result.isBlank()) {
                TextButton(enabled = !sending, onClick = onDismiss) {
                    Text("Close", fontFamily = PoppinsFamily)
                }
            }
        }
    )
}

@Composable
private fun UpdateStopsStatusDialog(status: String, onDismiss: () -> Unit) {
    val updating = status.equals("Updating stops", ignoreCase = true)
    AlertDialog(
        onDismissRequest = { if (!updating) onDismiss() },
        title = {
            Text(
                if (updating) "Updating stops" else "Stops update",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (updating) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Fetching district stops, cities, city stops, and airport stops.",
                            fontFamily = PoppinsFamily,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Text(
                        status,
                        fontFamily = PoppinsFamily,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            if (!updating) {
                TextButton(onClick = onDismiss) { Text("OK", fontFamily = PoppinsFamily) }
            }
        }
    )
}

@Composable
fun GamyamBottomBar() {
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val content = MaterialTheme.colorScheme.secondary
    val divider = MaterialTheme.colorScheme.outlineVariant
    Surface(color = surface, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().height(52.dp)) {
            BottomBarBtn(Icons.Default.CreditCard, "PASS",      content, Modifier.weight(1f))
            VerticalDivider(color = divider, modifier = Modifier.height(28.dp).align(Alignment.CenterVertically))
            BottomBarBtn(Icons.Default.Schedule,   "SCHEDULES", content, Modifier.weight(1f))
        }
    }
}

@Composable
private fun BottomBarBtn(icon: ImageVector, label: String, tint: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clickable(remember { MutableInteractionSource() }, null) {}
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = tint)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  MAIN SEARCH CARD  — stateless, all from/to state comes from parent
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSearchPanel(
    selectedTab:   ServiceTab,
    fromStop:      String,
    toStop:        String,
    fromExpanded:  Boolean,
    toExpanded:    Boolean,
    selectedFrom:  Stop?,
    selectedTo:    Stop?,
    fromList:      List<Stop>,
    toList:        List<Stop>,
    onFromChange:  (String) -> Unit,
    onToChange:    (String) -> Unit,
    onFromSelect:  (Stop) -> Unit,
    onToSelect:    (Stop) -> Unit,
    onFromClear:   () -> Unit,
    onToClear:     () -> Unit,
    onFromActivate: () -> Unit,
    onToActivate:  () -> Unit,
    onSwap:        () -> Unit,
    nearbyStatus:  String,
    isLoadingNearby: Boolean,
    onNearbyClick: () -> Unit,
    onSearchClick: () -> Unit,
    viewModel:     SearchViewModel
) {
    var selectedCity by remember { mutableStateOf("") }
    var cityExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val storedCityName by viewModel.selectedCityName.collectAsState()

    LaunchedEffect(selectedTab, storedCityName) {
        if (selectedTab == ServiceTab.CITY && storedCityName.isNotBlank()) {
            selectedCity = storedCityName
        } else if (selectedTab != ServiceTab.CITY) {
            selectedCity = ""
        }
        cityExpanded = false
        errorMessage = ""
        viewModel.searchCities("")
    }

    val cityList       by viewModel.filteredCities.collectAsState()
    val connectorColor  = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val dividerColor    = MaterialTheme.colorScheme.outlineVariant
    val cardSurface     = MaterialTheme.colorScheme.surface
    val swapBorder      = MaterialTheme.colorScheme.outline

    Card(
        modifier  = Modifier.fillMaxWidth().animateContentSize(tween(200)),
        colors    = CardDefaults.cardColors(containerColor = cardSurface),
        shape     = RoundedCornerShape(6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {

            // ── City selector (CITY tab only) ──────────────────────────────
            AnimatedVisibility(
                visible = selectedTab == ServiceTab.CITY,
                enter   = expandVertically(tween(200)),
                exit    = shrinkVertically(tween(200))
            ) {
                Column {
                    ExposedDropdownMenuBox(
                        expanded         = cityExpanded,
                        onExpandedChange = {
                            cityExpanded = it
                            if (it) viewModel.searchCities("")
                        }
                    ) {
                        OutlinedTextField(
                            value         = selectedCity,
                            onValueChange = { selectedCity = it; viewModel.searchCities(it); cityExpanded = true },
                            modifier      = Modifier.fillMaxWidth().menuAnchor(),
                            placeholder   = {
                                Text("Select City", fontFamily = PoppinsFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                            },
                            textStyle  = TextStyle(fontFamily = PoppinsFamily, fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface),
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = HeaderBlue,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor     = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor   = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                        ExposedDropdownMenu(
                            expanded         = cityExpanded && cityList.isNotEmpty(),
                            onDismissRequest = { cityExpanded = false },
                            modifier         = Modifier.heightIn(max = 280.dp)
                        ) {
                            cityList.forEach { city ->
                                DropdownMenuItem(
                                    text    = { Text(city.name, fontFamily = PoppinsFamily, fontSize = 14.sp) },
                                    onClick = { selectedCity = city.name; viewModel.selectCity(city); cityExpanded = false }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ── From / To connector box ──────────────────────────────────────
            // FROM suggestions go between From and To rows (below From, above To).
            // TO suggestions go below the To row.
            // drawBehind: circles placed at center of each row; line hidden while
            // suggestions are open (it would pass through suggestion items otherwise).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (nearbyStatus.isBlank()) "Nearby stops stay in this list" else nearbyStatus,
                    fontFamily = PoppinsFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                OutlinedButton(
                    onClick = onNearbyClick,
                    enabled = !isLoadingNearby,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    if (isLoadingNearby) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = HeaderBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(Icons.Default.MyLocation, "Nearby", tint = HeaderBlue, modifier = Modifier.size(17.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Nearby", fontFamily = PoppinsFamily, fontSize = 12.sp, color = HeaderBlue)
                }
            }

            val showFrom = fromExpanded && fromList.isNotEmpty()
            val showTo   = toExpanded   && toList.isNotEmpty()

            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 26.dp)
                        .drawBehind {
                            val cx     = (-12).dp.toPx()
                            val r      = 5.dp.toPx()
                            val sw     = 1.6.dp.toPx()
                            val fromCY = 26.dp.toPx()
                            // From/To expanded are mutually exclusive (onFromChange collapses To
                            // and vice versa), so only one can push the column height at a time.
                            // When showTo=true, To row is always at a fixed offset from top
                            // (fromRow=52 + divider=1 + toRowHalf=26 = 79dp) because FROM
                            // suggestions are collapsed. Otherwise size.height-26dp is correct
                            // (either no suggestions, or FROM suggestions pushed To row down and
                            // To row is still at the bottom of the column).
                            val toCY = if (showTo) (52 + 1 + 26).dp.toPx()
                                       else size.height - 26.dp.toPx()
                            drawCircle(connectorColor, r, Offset(cx, fromCY), style = Stroke(sw))
                            drawCircle(connectorColor, r, Offset(cx, toCY), style = Stroke(sw))
                            if (!showFrom && !showTo) {
                                drawLine(
                                    color       = connectorColor,
                                    start       = Offset(cx, fromCY + r + 3),
                                    end         = Offset(cx, toCY - r - 3),
                                    strokeWidth = sw,
                                    pathEffect  = PathEffect.dashPathEffect(floatArrayOf(7f, 7f))
                                )
                            }
                        }
                ) {
                    // FROM row
                    StationRow(fromStop, "From Stop", onFromChange, onFromClear, onFromActivate)

                    // FROM suggestions — directly below From row, above To row
                    AnimatedVisibility(
                        visible = showFrom,
                        enter   = expandVertically(tween(150)),
                        exit    = shrinkVertically(tween(150))
                    ) {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            HorizontalDivider(color = dividerColor)
                            fromList.forEach { stop ->
                                SuggestionRow(stop) { onFromSelect(stop) }
                            }
                        }
                    }

                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(end = 52.dp))

                    // TO row
                    StationRow(toStop, "To Stop", onToChange, onToClear, onToActivate)

                    // TO suggestions — directly below To row
                    AnimatedVisibility(
                        visible = showTo,
                        enter   = expandVertically(tween(150)),
                        exit    = shrinkVertically(tween(150))
                    ) {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            HorizontalDivider(color = dividerColor)
                            toList.forEach { stop ->
                                SuggestionRow(stop) { onToSelect(stop) }
                            }
                        }
                    }
                }

                // Swap button — offset so its centre sits on the From/To divider (52dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = 32.dp)   // 52dp divider mark − 40dp/2 = 32dp from top
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(cardSurface)
                        .border(1.5.dp, swapBorder, CircleShape)
                        .clickable(remember { MutableInteractionSource() }, null, onClick = onSwap),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SwapVert, "Swap", tint = BtnGreen, modifier = Modifier.size(22.dp))
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontFamily = PoppinsFamily)
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = {
                    val fv = selectedFrom?.takeIf { it.name == fromStop } ?: fromList.find { it.name == fromStop }
                    val tv = selectedTo?.takeIf { it.name == toStop } ?: toList.find { it.name == toStop }
                    errorMessage = if (fv == null || tv == null) "Please select a stop from the suggestions" else { onSearchClick(); "" }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BtnGreen),
                shape    = RoundedCornerShape(6.dp)
            ) {
                Text("Find buses", fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

// ── Editable field row (shared by From and To) ────────────────────────────────
@Composable
private fun StationRow(
    value:        String,
    placeholder:  String,
    onValueChange:(String) -> Unit,
    onClear:      () -> Unit,
    onActivate:   () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(end = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value         = value,
            onValueChange = onValueChange,
            singleLine    = true,
            textStyle     = TextStyle(color = textColor, fontSize = 15.sp, fontFamily = PoppinsFamily, fontWeight = FontWeight.Medium),
            cursorBrush   = SolidColor(HeaderBlue),
            modifier      = Modifier
                .weight(1f)
                .onFocusChanged { if (it.isFocused) onActivate() },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, color = hintColor, fontSize = 15.sp, fontFamily = PoppinsFamily)
                    inner()
                }
            }
        )
        if (value.isNotEmpty()) {
            IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Close, "Clear", tint = hintColor, modifier = Modifier.size(18.dp))
            }
        }
    }
}


// ── Single suggestion row used by both From and To suggestion lists ────────────
@Composable
private fun SuggestionRow(stop: Stop, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stop.name,
            fontFamily = PoppinsFamily,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

// ─────────────────────────────────────────────────────────────────────────────
//  SECONDARY SEARCH PANEL  (icon + field + green search button)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SecondarySearchPanel(
    icon:          ImageVector,
    placeholder:   String,
    query:         String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    // If set, tapping anywhere on the card (including the text field) navigates immediately.
    // Used for Reservation Services which loads all results on open.
    onCardClick:   (() -> Unit)? = null
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .then(if (onCardClick != null) Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onCardClick) else Modifier),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape     = RoundedCornerShape(6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp).padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = HeaderBlue, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(14.dp))
            BasicTextField(
                value         = query,
                onValueChange = onQueryChange,
                singleLine    = true,
                textStyle     = TextStyle(color = onSurface, fontSize = 14.5.sp, fontFamily = PoppinsFamily),
                cursorBrush   = SolidColor(HeaderBlue),
                modifier      = Modifier.weight(1f),
                enabled       = onCardClick == null,  // disable typing when card navigates on tap
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text(placeholder, color = hintColor, fontSize = 14.5.sp, fontFamily = PoppinsFamily)
                        inner()
                    }
                }
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(BtnGreen)
                    .clickable(remember { MutableInteractionSource() }, null, onClick = onCardClick ?: onSearchClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Search, "Search", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SEARCH HISTORY CARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SearchHistoryCard(
    viewModel: SearchViewModel,
    onOpenSearch: (SearchHistoryItem) -> Unit
) {
    val history by viewModel.recentSearches.collectAsState()
    if (history.isEmpty()) return

    val onSurface    = MaterialTheme.colorScheme.onSurface
    val labelColor   = MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape     = RoundedCornerShape(6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                "SEARCH HISTORY",
                fontFamily    = PoppinsFamily,
                fontWeight    = FontWeight.SemiBold,
                fontSize      = 11.sp,
                color         = labelColor,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(6.dp))
            history.forEachIndexed { idx, entry ->
                if (idx > 0) HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(remember { MutableInteractionSource() }, null) { onOpenSearch(entry) }
                        .padding(vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (entry.isServiceIdSearch) {
                            val serviceIdText = entry.serviceId.takeIf { it > 0 }?.toString()
                                ?: entry.toName.ifBlank { entry.serviceLabel }
                            Text(
                                "Service ID $serviceIdText",
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    entry.fromName,
                                    fontFamily = PoppinsFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    null,
                                    tint = labelColor,
                                    modifier = Modifier.padding(horizontal = 6.dp).size(18.dp)
                                )
                                Text(
                                    entry.toName,
                                    fontFamily = PoppinsFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Text(
                            entry.tab.lowercase().replaceFirstChar { it.uppercase() },
                            fontFamily = PoppinsFamily, fontSize = 11.sp, color = labelColor
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PREVIEWS
// ─────────────────────────────────────────────────────────────────────────────
@androidx.compose.ui.tooling.preview.Preview(name = "Light", showSystemUi = true, showBackground = true)
@Composable
fun MainScreenPreviewLight() {
    com.gamyam.app.theme.GamyamTheme(darkTheme = false) { MainScreen(onItemClick = {}) }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Dark", showSystemUi = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MainScreenPreviewDark() {
    com.gamyam.app.theme.GamyamTheme(darkTheme = true) { MainScreen(onItemClick = {}) }
}
