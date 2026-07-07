package com.gamyam.app

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.gamyam.app.data.AuthManager
import com.gamyam.app.ui.splash.SplashScreen
import com.gamyam.app.ui.splash.SplashViewModel
import com.gamyam.app.ui.main.MainScreen
import com.gamyam.app.ui.main.SearchViewModel
import com.gamyam.app.ui.main.ServiceTab
import com.gamyam.app.ui.search.ReservationSearchListScreen
import com.gamyam.app.ui.search.RouteNumberSearchListScreen
import com.gamyam.app.ui.search.ServiceSearchViewModel
import com.gamyam.app.ui.search.ServiceSearchViewModelFactory
import com.gamyam.app.ui.search.ServiceIdSearchListScreen
import com.gamyam.app.ui.search.VehicleSearchListScreen
import com.gamyam.app.ui.tracker.FromToServicesResultScreen
import com.gamyam.app.ui.tracker.LiveBusTrackerScreen
import com.gamyam.app.ui.tracker.ReservationServicesResultScreen
import com.gamyam.app.ui.tracker.RouteServicesResultScreen
import com.gamyam.app.ui.tracker.ServiceTripDetailScreen
import com.gamyam.app.ui.tracker.TrackerViewModel
import com.gamyam.app.ui.tracker.TrackerViewModelFactory
import com.gamyam.app.ui.tracker.VehicleTripDetailScreen

@Composable
fun MainNavigation(authManager: AuthManager) {
    val backStack = rememberNavBackStack(Splash)
    val mainSearchViewModel: SearchViewModel = viewModel()

    NavDisplay(
        backStack     = backStack,
        onBack        = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {

            entry<Splash> {
                val vm: SplashViewModel = viewModel(factory = SplashViewModelFactory(authManager))
                SplashScreen(
                    viewModel        = vm,
                    onNavigateToHome = {
                        backStack.removeLastOrNull()
                        backStack.add(Main)
                    }
                )
            }

            // NOTE: no safeDrawingPadding here – Scaffold inside MainScreen
            // handles window insets via TopAppBar + bottomBar automatically.
            entry<Main> {
                MainScreen(
                    onItemClick = { backStack.add(it) },
                    searchViewModel = mainSearchViewModel,
                    authManager = authManager
                )
            }

            entry<LiveBusTracker> {
                LiveBusTrackerScreen(serviceIdOrBusNumber = it.serviceIdOrBusNumber)
            }

            entry<ReservationSearchList> {
                val reservationSearchViewModel: ServiceSearchViewModel = viewModel(
                    key = "reservation-search",
                    factory = ServiceSearchViewModelFactory(authManager)
                )
                ReservationSearchListScreen(
                    query       = it.query,
                    authManager = authManager,
                    onBack      = { backStack.removeLastOrNull() },
                    onItemClick = { detail -> backStack.add(detail) },
                    viewModel   = reservationSearchViewModel
                )
            }

            entry<ServiceIdSearchList> {
                val serviceIdSearchViewModel: ServiceSearchViewModel = viewModel(
                    key = "service-id-search-${it.tab}",
                    factory = ServiceSearchViewModelFactory(authManager)
                )
                val tab = runCatching { ServiceTab.valueOf(it.tab) }.getOrDefault(ServiceTab.DISTRICT)
                val operationType = when (tab) {
                    ServiceTab.DISTRICT -> 1
                    ServiceTab.CITY -> 2
                    ServiceTab.AIRPORT -> 3
                }
                ServiceIdSearchListScreen(
                    query       = it.query,
                    operationType = operationType,
                    forceList = it.forceList,
                    authManager = authManager,
                    onBack      = { backStack.removeLastOrNull() },
                    onSearchSaved = { serviceId, label ->
                        mainSearchViewModel.saveServiceIdSearch(serviceId, label, tab)
                    },
                    onItemClick = { detail -> backStack.add(detail) },
                    viewModel = serviceIdSearchViewModel
                )
            }

            entry<RouteNumberSearchList> {
                val routeSearchViewModel: ServiceSearchViewModel = viewModel(
                    key = "route-search-${it.operationType}-${it.cityId}-${it.isAirport}",
                    factory = ServiceSearchViewModelFactory(authManager)
                )
                RouteNumberSearchListScreen(
                    routeNumber   = it.routeNumber,
                    cityId        = it.cityId,
                    operationType = it.operationType,
                    isAirport     = it.isAirport,
                    authManager   = authManager,
                    onBack        = { backStack.removeLastOrNull() },
                    onItemClick   = { detail -> backStack.add(detail) },
                    viewModel     = routeSearchViewModel
                )
            }

            entry<FromToServicesResult> {
                val fromToViewModel: TrackerViewModel = viewModel(
                    key = "from-to-result-${it.operationType}-${it.cityId}-${it.isAirport}-${it.fromId}-${it.fromGeozoneType}-${it.toId}-${it.toGeozoneType}-${it.requestId}",
                    factory = TrackerViewModelFactory(authManager)
                )
                FromToServicesResultScreen(
                    fromId          = it.fromId,
                    fromName        = it.fromName,
                    fromGeozoneType = it.fromGeozoneType,
                    toId            = it.toId,
                    toName          = it.toName,
                    toGeozoneType   = it.toGeozoneType,
                    operationType   = it.operationType,
                    cityId          = it.cityId,
                    isAirport       = it.isAirport,
                    requestId       = it.requestId,
                    authManager     = authManager,
                    onBack          = { backStack.removeLastOrNull() },
                    onServiceClick  = { detail -> backStack.add(detail) },
                    viewModel       = fromToViewModel
                )
            }

            entry<RouteServicesResult> {
                val routeResultViewModel: TrackerViewModel = viewModel(
                    key = "route-result-${it.operationType}-${it.cityId}-${it.isAirport}-${it.routeNumber}-${it.requestId}",
                    factory = TrackerViewModelFactory(authManager)
                )
                RouteServicesResultScreen(
                    routeNumber   = it.routeNumber,
                    cityId        = it.cityId,
                    operationType = it.operationType,
                    isAirport     = it.isAirport,
                    requestId     = it.requestId,
                    authManager   = authManager,
                    onBack        = { backStack.removeLastOrNull() },
                    onServiceClick = { detail -> backStack.add(detail) },
                    viewModel      = routeResultViewModel
                )
            }

            entry<ReservationServicesResult> {
                val reservationResultViewModel: TrackerViewModel = viewModel(
                    key = "reservation-result-${it.operationType}-${it.reservationServiceId}-${it.requestId}",
                    factory = TrackerViewModelFactory(authManager)
                )
                ReservationServicesResultScreen(
                    reservationServiceId = it.reservationServiceId,
                    displayNumber        = it.displayNumber,
                    operationType        = it.operationType,
                    requestId            = it.requestId,
                    authManager          = authManager,
                    onBack               = { backStack.removeLastOrNull() },
                    onServiceClick       = { detail -> backStack.add(detail) },
                    viewModel            = reservationResultViewModel
                )
            }

            entry<VehicleSearchList> {
                val vehicleSearchViewModel: ServiceSearchViewModel = viewModel(
                    key = "vehicle-search-${it.operationType}-${it.cityId}-${it.isAirport}",
                    factory = ServiceSearchViewModelFactory(authManager)
                )
                VehicleSearchListScreen(
                    query         = it.query,
                    operationType = it.operationType,
                    cityId        = it.cityId,
                    isAirport     = it.isAirport,
                    authManager   = authManager,
                    onBack        = { backStack.removeLastOrNull() },
                    onItemClick   = { detail -> backStack.add(detail) },
                    viewModel     = vehicleSearchViewModel
                )
            }

            entry<ServiceTripDetail> {
                ServiceTripDetailScreen(
                    serviceId     = it.serviceId,
                    displayNumber = it.displayNumber,
                    operationType = it.operationType,
                    fromName      = it.fromName,
                    toName        = it.toName,
                    authManager   = authManager,
                    onBack        = { backStack.removeLastOrNull() }
                )
            }

            entry<VehicleTripDetail> {
                val vehicleResultViewModel: TrackerViewModel = viewModel(
                    key = "vehicle-result-${it.operationType}-${it.vehicleId}-${it.requestId}",
                    factory = TrackerViewModelFactory(authManager)
                )
                VehicleTripDetailScreen(
                    vehicleId           = it.vehicleId,
                    registrationNumber  = it.registrationNumber,
                    serviceType         = it.serviceType,
                    operationType       = it.operationType,
                    requestId           = it.requestId,
                    authManager         = authManager,
                    onBack              = { backStack.removeLastOrNull() },
                    onServiceClick      = { detail -> backStack.add(detail) },
                    viewModel           = vehicleResultViewModel
                )
            }
        }
    )
}
