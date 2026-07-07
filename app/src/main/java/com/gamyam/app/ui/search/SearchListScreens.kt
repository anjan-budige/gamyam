package com.gamyam.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamyam.app.data.AuthManager
import com.gamyam.app.data.PlannedServiceDto
import com.gamyam.app.data.ServiceDto
import com.gamyam.app.data.VehicleDto
import com.gamyam.app.ReservationServicesResult
import com.gamyam.app.RouteServicesResult
import com.gamyam.app.ServiceTripDetail
import com.gamyam.app.VehicleTripDetail
import com.gamyam.app.theme.AppBarBlue
import com.gamyam.app.ui.main.PoppinsFamily

// ─────────────────────────────────────────────────────────────────────────────
//  Shared: Blue search AppBar with editable pill + Search/More icons
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    query:         String,
    onQueryChange: (String) -> Unit,
    placeholder:   String,
    onBack:        () -> Unit,
    onSearch:      () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    TopAppBar(
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value         = query,
                    onValueChange = onQueryChange,
                    singleLine    = true,
                    textStyle     = TextStyle(color = Color.White, fontSize = 14.sp, fontFamily = PoppinsFamily),
                    cursorBrush   = SolidColor(Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); onSearch() }),
                    modifier      = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) Text(
                                placeholder,
                                color      = Color.White.copy(alpha = 0.55f),
                                fontSize   = 14.sp,
                                fontFamily = PoppinsFamily
                            )
                            inner()
                        }
                    }
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
        },
        actions = {
            IconButton(onClick = { keyboard?.hide(); onSearch() }) {
                Icon(Icons.Default.Search, "Search", tint = Color.White)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.MoreVert, "More", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBarBlue)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Shared: blue service-type chip
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ServiceTypeChip(text: String) {
    if (text.isBlank()) return
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(AppBarBlue)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text.trim(),
            color = Color.White,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.3.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun MetaText(text: String) {
    if (text.isBlank()) return
    Text(
        text,
        fontFamily = PoppinsFamily,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Shared: empty/loading/error state overlay
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IdleHint() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.Search, null,
            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.size(52.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Enter a name or number and tap Search",
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = PoppinsFamily,
            fontSize   = 13.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Service list item (reservation / route search)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ServiceListItem(
    service: ServiceDto,
    routeNumberMode: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        val primaryNumber = if (routeNumberMode) {
            service.routeNumber.orEmpty().trim().ifBlank { service.name.orEmpty().trim() }
        } else {
            service.name.orEmpty().trim()
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                primaryNumber.ifBlank { service.id.toString() },
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 15.sp,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                modifier   = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            ServiceTypeChip(service.serviceType.orEmpty())
        }
        Spacer(Modifier.height(3.dp))
        val meta = if (routeNumberMode) {
            service.name.orEmpty().trim().takeIf { it.isNotBlank() && !it.equals(primaryNumber, ignoreCase = true) }.orEmpty()
        } else {
            service.routeNumber?.let { "Route $it" }.orEmpty()
        }
        MetaText(meta)
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun PlannedServiceListItem(service: PlannedServiceDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                service.serviceId.toString(),
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            ServiceTypeChip(service.serviceType.orEmpty())
        }
        Spacer(Modifier.height(3.dp))
        MetaText(service.serviceName.orEmpty())
        MetaText(listOfNotNull(service.from?.name, service.to?.name).joinToString(" - "))
        MetaText(service.depot?.let { "Depot $it" }.orEmpty())
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

// ─────────────────────────────────────────────────────────────────────────────
//  Vehicle list item (bus number search)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun VehicleListItem(vehicle: VehicleDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                vehicle.registrationNumber.orEmpty().trim().ifBlank { vehicle.name.orEmpty().trim() },
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 15.sp,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                modifier   = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            ServiceTypeChip(vehicle.serviceType.orEmpty())
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

// ─────────────────────────────────────────────────────────────────────────────
//  RESERVATION SERVICE SEARCH  (DISTRICT – operationType = 1)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationSearchListScreen(
    query:       String,
    authManager: AuthManager,
    onBack:      () -> Unit,
    onItemClick: (ReservationServicesResult) -> Unit,
    viewModel:   ServiceSearchViewModel = viewModel(factory = ServiceSearchViewModelFactory(authManager))
) {
    var searchQuery by remember(query) {
        mutableStateOf(viewModel.serviceQueryFor(operationType = 1, fallback = query))
    }
    val state by viewModel.serviceState.collectAsState()
    val handleBack = {
        viewModel.resetServiceSearch()
        onBack()
    }

    LaunchedEffect(Unit) {
        viewModel.ensureServiceSearch(query, operationType = 1)
    }
    BackHandler(onBack = handleBack)

    Scaffold(
        topBar = {
            SearchTopBar(
                query         = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder   = "Reservation Service Number",
                onBack        = handleBack,
                onSearch      = { viewModel.searchServices(searchQuery, operationType = 1) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier          = Modifier.fillMaxSize().padding(padding),
            contentAlignment  = Alignment.Center
        ) {
            when (val s = state) {
                is ServiceSearchState.Idle    -> IdleHint()
                is ServiceSearchState.Loading -> CircularProgressIndicator(color = AppBarBlue)
                is ServiceSearchState.Error   -> Text(
                    s.message,
                    color      = MaterialTheme.colorScheme.error,
                    fontFamily = PoppinsFamily,
                    fontSize   = 14.sp,
                    modifier   = Modifier.padding(24.dp)
                )
                is ServiceSearchState.Success -> {
                    if (s.results.isEmpty()) {
                        Text("No services found", color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = PoppinsFamily, fontSize = 14.sp)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().align(Alignment.TopStart)) {
                            itemsIndexed(
                                items = s.results,
                                key = { index, service -> "$index-${service.id}-${service.name.orEmpty()}" }
                            ) { _, service ->
                                ServiceListItem(
                                    service = service,
                                    onClick = {
                                        onItemClick(
                                            ReservationServicesResult(
                                                reservationServiceId = service.id,
                                                displayNumber = service.name.orEmpty().ifBlank { service.id.toString() },
                                                operationType = 1,
                                                requestId = System.nanoTime()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceIdSearchListScreen(
    query:       String,
    operationType: Int = 1,
    forceList: Boolean = false,
    authManager: AuthManager,
    onBack:      () -> Unit,
    onSearchSaved: (serviceId: Int, label: String) -> Unit = { _, _ -> },
    onItemClick: (ServiceTripDetail) -> Unit,
    viewModel:   ServiceSearchViewModel = viewModel(factory = ServiceSearchViewModelFactory(authManager))
) {
    var searchQuery by remember(query) { mutableStateOf(query) }
    val state by viewModel.serviceIdState.collectAsState()

    LaunchedEffect(query, forceList) {
        viewModel.resetServiceIdSearch()
        if (forceList && query.isNotBlank()) {
            viewModel.searchServiceId(query)
        }
    }

    Scaffold(
        topBar = {
            SearchTopBar(
                query         = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder   = "Service ID",
                onBack        = onBack,
                onSearch      = {
                    val serviceId = searchQuery.trim().toIntOrNull()
                    if (!forceList && serviceId != null && serviceId > 0) {
                        onSearchSaved(serviceId, serviceId.toString())
                        onItemClick(
                            ServiceTripDetail(
                                serviceId = serviceId,
                                displayNumber = serviceId.toString(),
                                operationType = operationType
                            )
                        )
                    } else {
                        viewModel.searchServiceId(searchQuery)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val s = state) {
                is ServiceIdSearchState.Idle -> IdleHint()
                is ServiceIdSearchState.Loading -> CircularProgressIndicator(color = AppBarBlue)
                is ServiceIdSearchState.Error -> Text(
                    s.message,
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = PoppinsFamily,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(24.dp)
                )
                is ServiceIdSearchState.Success -> {
                    if (s.results.isEmpty()) {
                        Text("No service found", color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = PoppinsFamily, fontSize = 14.sp)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().align(Alignment.TopStart)) {
                            itemsIndexed(
                                items = s.results,
                                key = { index, service -> "$index-${service.serviceId}-${service.from?.id}-${service.to?.id}" }
                            ) { _, service ->
                                PlannedServiceListItem(
                                    service,
                                    onClick = {
                                        onSearchSaved(
                                            service.serviceId,
                                            service.serviceName.orEmpty().ifBlank { service.serviceId.toString() }
                                        )
                                        onItemClick(
                                            ServiceTripDetail(
                                                serviceId = service.serviceId,
                                                displayNumber = service.serviceName.orEmpty(),
                                                operationType = operationType,
                                                fromName = service.from?.name.orEmpty(),
                                                toName = service.to?.name.orEmpty()
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

// ─────────────────────────────────────────────────────────────────────────────
//  ROUTE NUMBER SEARCH  (CITY – operationType = 2 / AIRPORT – operationType = 3)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteNumberSearchListScreen(
    routeNumber:   String,
    cityId:        String,
    operationType: Int = 2,
    isAirport:     Boolean = false,
    authManager:   AuthManager,
    onBack:        () -> Unit,
    onItemClick:   (RouteServicesResult) -> Unit,
    viewModel:     ServiceSearchViewModel = viewModel(factory = ServiceSearchViewModelFactory(authManager))
) {
    val effectiveCityId = if (cityId == "TODO_CITY_ID") "" else cityId
    var searchQuery by remember(routeNumber, operationType, effectiveCityId, isAirport) {
        mutableStateOf(
            viewModel.serviceQueryFor(
                operationType = operationType,
                cityId = effectiveCityId,
                isAirport = isAirport,
                routeNumberOnly = true,
                fallback = routeNumber
            )
        )
    }
    val state by viewModel.serviceState.collectAsState()
    val needsCity = operationType == 2 && effectiveCityId.isBlank() && !isAirport
    val handleBack = {
        viewModel.resetServiceSearch()
        onBack()
    }

    LaunchedEffect(routeNumber, operationType, effectiveCityId, isAirport) {
        if (needsCity) {
            viewModel.resetServiceSearch("Select a city first")
        } else {
            viewModel.ensureServiceSearch(routeNumber, operationType, effectiveCityId, isAirport = isAirport, routeNumberOnly = true)
        }
    }
    BackHandler(onBack = handleBack)

    Scaffold(
        topBar = {
            SearchTopBar(
                query         = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder   = "Route Number / Name",
                onBack        = handleBack,
                onSearch      = {
                    if (needsCity) {
                        viewModel.resetServiceSearch("Select a city first")
                    } else {
                        viewModel.searchServices(searchQuery, operationType, effectiveCityId, isAirport = isAirport, routeNumberOnly = true)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier         = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val s = state) {
                is ServiceSearchState.Idle    -> IdleHint()
                is ServiceSearchState.Loading -> CircularProgressIndicator(color = AppBarBlue)
                is ServiceSearchState.Error   -> Text(
                    s.message,
                    color      = MaterialTheme.colorScheme.error,
                    fontFamily = PoppinsFamily,
                    fontSize   = 14.sp,
                    modifier   = Modifier.padding(24.dp)
                )
                is ServiceSearchState.Success -> {
                    if (s.results.isEmpty()) {
                        Text("No routes found", color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = PoppinsFamily, fontSize = 14.sp)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().align(Alignment.TopStart)) {
                            itemsIndexed(
                                items = s.results,
                                key = { index, service -> "$index-${service.id}-${service.routeNumber.orEmpty()}-${service.name.orEmpty()}" }
                            ) { _, service ->
                                ServiceListItem(
                                    service = service,
                                    routeNumberMode = true,
                                    onClick = {
                                        onItemClick(
                                            RouteServicesResult(
                                                routeNumber = service.routeNumber.orEmpty().ifBlank { service.name.orEmpty() },
                                                cityId = effectiveCityId,
                                                operationType = operationType,
                                                isAirport = isAirport,
                                                requestId = System.nanoTime()
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

// ─────────────────────────────────────────────────────────────────────────────
//  VEHICLE / BUS NUMBER SEARCH
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleSearchListScreen(
    query:         String,
    operationType: Int = 1,
    cityId:        String = "",
    isAirport:     Boolean = false,
    authManager:   AuthManager,
    onBack:        () -> Unit,
    onItemClick:   (VehicleTripDetail) -> Unit,
    viewModel:     ServiceSearchViewModel = viewModel(factory = ServiceSearchViewModelFactory(authManager))
) {
    val effectiveCityId = if (cityId == "TODO_CITY_ID") "" else cityId
    var searchQuery by remember(query, operationType, effectiveCityId, isAirport) {
        mutableStateOf(
            viewModel.vehicleQueryFor(
                operationType = operationType,
                cityId = effectiveCityId,
                isAirport = isAirport,
                fallback = query
            )
        )
    }
    val state by viewModel.vehicleState.collectAsState()
    val needsCity = operationType == 2 && effectiveCityId.isBlank() && !isAirport
    val handleBack = {
        viewModel.resetVehicleSearch()
        onBack()
    }

    LaunchedEffect(query, operationType, effectiveCityId, isAirport) {
        if (needsCity) {
            viewModel.resetVehicleSearch("Select a city first")
        } else {
            viewModel.ensureVehicleSearch(query, operationType, effectiveCityId, isAirport = isAirport)
        }
    }
    BackHandler(onBack = handleBack)

    Scaffold(
        topBar = {
            SearchTopBar(
                query         = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder   = "Bus Number / Vehicle No.",
                onBack        = handleBack,
                onSearch      = {
                    if (needsCity) {
                        viewModel.resetVehicleSearch("Select a city first")
                    } else {
                        viewModel.searchVehicles(searchQuery, operationType, effectiveCityId, isAirport = isAirport)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier         = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val s = state) {
                is VehicleSearchState.Idle    -> IdleHint()
                is VehicleSearchState.Loading -> CircularProgressIndicator(color = AppBarBlue)
                is VehicleSearchState.Error   -> Text(
                    s.message,
                    color      = MaterialTheme.colorScheme.error,
                    fontFamily = PoppinsFamily,
                    fontSize   = 14.sp,
                    modifier   = Modifier.padding(24.dp)
                )
                is VehicleSearchState.Success -> {
                    if (s.results.isEmpty()) {
                        Text("No vehicles found", color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = PoppinsFamily, fontSize = 14.sp)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().align(Alignment.TopStart)) {
                            itemsIndexed(
                                items = s.results,
                                key = { index, vehicle -> "$index-${vehicle.id}-${vehicle.registrationNumber.orEmpty()}-${vehicle.name.orEmpty()}" }
                            ) { _, vehicle ->
                                VehicleListItem(
                                    vehicle,
                                    onClick = {
                                        onItemClick(
                                            VehicleTripDetail(
                                                vehicleId = vehicle.id,
                                                registrationNumber = vehicle.registrationNumber.orEmpty().ifBlank { vehicle.name.orEmpty() },
                                                serviceType = vehicle.serviceType.orEmpty(),
                                                operationType = operationType,
                                                requestId = System.nanoTime()
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
