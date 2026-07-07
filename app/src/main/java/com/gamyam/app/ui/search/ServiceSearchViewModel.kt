package com.gamyam.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gamyam.app.data.ApiClient
import com.gamyam.app.data.AuthManager
import com.gamyam.app.data.PlannedServiceDto
import com.gamyam.app.data.ServiceDto
import com.gamyam.app.data.VehicleDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ServiceSearchState {
    object Idle : ServiceSearchState()
    object Loading : ServiceSearchState()
    data class Success(val results: List<ServiceDto>) : ServiceSearchState()
    data class Error(val message: String) : ServiceSearchState()
}

sealed class VehicleSearchState {
    object Idle : VehicleSearchState()
    object Loading : VehicleSearchState()
    data class Success(val results: List<VehicleDto>) : VehicleSearchState()
    data class Error(val message: String) : VehicleSearchState()
}

sealed class ServiceIdSearchState {
    object Idle : ServiceIdSearchState()
    object Loading : ServiceIdSearchState()
    data class Success(val results: List<PlannedServiceDto>) : ServiceIdSearchState()
    data class Error(val message: String) : ServiceIdSearchState()
}

class ServiceSearchViewModel(private val authManager: AuthManager) : ViewModel() {

    private val _serviceState = MutableStateFlow<ServiceSearchState>(ServiceSearchState.Idle)
    val serviceState: StateFlow<ServiceSearchState> = _serviceState.asStateFlow()

    private val _vehicleState = MutableStateFlow<VehicleSearchState>(VehicleSearchState.Idle)
    val vehicleState: StateFlow<VehicleSearchState> = _vehicleState.asStateFlow()

    private val _serviceIdState = MutableStateFlow<ServiceIdSearchState>(ServiceIdSearchState.Idle)
    val serviceIdState: StateFlow<ServiceIdSearchState> = _serviceIdState.asStateFlow()

    private var serviceRequestToken = 0
    private var vehicleRequestToken = 0
    private var serviceIdRequestToken = 0

    private var cachedServices: List<ServiceDto> = emptyList()
    private var lastServiceOpType: Int = -1
    private var lastServiceCityId: String = ""
    private var lastServiceIsAirport: Boolean = false
    private var activeServiceQuery: String = ""
    private var activeServiceOpType: Int = -1
    private var activeServiceCityId: String = ""
    private var activeServiceIsAirport: Boolean = false
    private var activeServiceRouteNumberOnly: Boolean = false

    private var cachedVehicles: List<VehicleDto> = emptyList()
    private var lastVehicleOpType: Int = -1
    private var lastVehicleCityId: String = ""
    private var lastVehicleIsAirport: Boolean = false
    private var activeVehicleQuery: String = ""
    private var activeVehicleOpType: Int = -1
    private var activeVehicleCityId: String = ""
    private var activeVehicleIsAirport: Boolean = false

    fun resetServiceSearch(message: String? = null) {
        serviceRequestToken++
        activeServiceQuery = ""
        activeServiceOpType = -1
        activeServiceCityId = ""
        activeServiceIsAirport = false
        activeServiceRouteNumberOnly = false
        _serviceState.value = message?.let { ServiceSearchState.Error(it) } ?: ServiceSearchState.Idle
    }

    fun resetVehicleSearch(message: String? = null) {
        vehicleRequestToken++
        activeVehicleQuery = ""
        activeVehicleOpType = -1
        activeVehicleCityId = ""
        activeVehicleIsAirport = false
        _vehicleState.value = message?.let { VehicleSearchState.Error(it) } ?: VehicleSearchState.Idle
    }

    fun resetServiceIdSearch() {
        serviceIdRequestToken++
        _serviceIdState.value = ServiceIdSearchState.Idle
    }

    fun serviceQueryFor(
        operationType: Int,
        cityId: String = "",
        isAirport: Boolean = false,
        routeNumberOnly: Boolean = false,
        fallback: String = ""
    ): String {
        val normalizedCityId = cityId.trim()
        return if (hasActiveServiceScope(operationType, normalizedCityId, isAirport, routeNumberOnly)) {
            activeServiceQuery
        } else {
            fallback
        }
    }

    fun vehicleQueryFor(
        operationType: Int,
        cityId: String = "",
        isAirport: Boolean = false,
        fallback: String = ""
    ): String {
        val normalizedCityId = cityId.trim()
        return if (hasActiveVehicleScope(operationType, normalizedCityId, isAirport)) {
            activeVehicleQuery
        } else {
            fallback
        }
    }

    fun ensureServiceSearch(
        query: String,
        operationType: Int,
        cityId: String = "",
        isAirport: Boolean = false,
        routeNumberOnly: Boolean = false
    ) {
        val normalizedCityId = cityId.trim()
        if (hasActiveServiceSearch(query, operationType, normalizedCityId, isAirport, routeNumberOnly)) return
        searchServices(query, operationType, normalizedCityId, isAirport, routeNumberOnly)
    }

    fun ensureVehicleSearch(
        query: String,
        operationType: Int,
        cityId: String = "",
        isAirport: Boolean = false
    ) {
        val normalizedCityId = cityId.trim()
        if (hasActiveVehicleSearch(query, operationType, normalizedCityId, isAirport)) return
        searchVehicles(query, operationType, normalizedCityId, isAirport)
    }

    private fun hasActiveServiceScope(
        operationType: Int,
        cityId: String,
        isAirport: Boolean,
        routeNumberOnly: Boolean
    ): Boolean =
        _serviceState.value !is ServiceSearchState.Idle &&
            activeServiceOpType == operationType &&
            activeServiceCityId == cityId.trim() &&
            activeServiceIsAirport == isAirport &&
            activeServiceRouteNumberOnly == routeNumberOnly

    private fun hasActiveServiceSearch(
        query: String,
        operationType: Int,
        cityId: String,
        isAirport: Boolean,
        routeNumberOnly: Boolean
    ): Boolean =
        hasActiveServiceScope(operationType, cityId, isAirport, routeNumberOnly) &&
            activeServiceQuery == query.trim()

    private fun hasActiveVehicleScope(
        operationType: Int,
        cityId: String,
        isAirport: Boolean
    ): Boolean =
        _vehicleState.value !is VehicleSearchState.Idle &&
            activeVehicleOpType == operationType &&
            activeVehicleCityId == cityId.trim() &&
            activeVehicleIsAirport == isAirport

    private fun hasActiveVehicleSearch(
        query: String,
        operationType: Int,
        cityId: String,
        isAirport: Boolean
    ): Boolean =
        hasActiveVehicleScope(operationType, cityId, isAirport) &&
            activeVehicleQuery == query.trim()

    fun searchServices(
        query: String,
        operationType: Int,
        cityId: String = "",
        isAirport: Boolean = false,
        routeNumberOnly: Boolean = false
    ) {
        val trimmedQuery = query.trim()
        val normalizedCityId = cityId.trim()
        if (operationType == 2 && normalizedCityId.isBlank() && !isAirport) {
            resetServiceSearch("Select a city first")
            return
        }

        val requestToken = ++serviceRequestToken
        activeServiceQuery = trimmedQuery
        activeServiceOpType = operationType
        activeServiceCityId = normalizedCityId
        activeServiceIsAirport = isAirport
        activeServiceRouteNumberOnly = routeNumberOnly
        viewModelScope.launch {
            _serviceState.value = ServiceSearchState.Loading
            try {
                val needsFetch = cachedServices.isEmpty()
                    || lastServiceOpType != operationType
                    || lastServiceCityId != normalizedCityId
                    || lastServiceIsAirport != isAirport

                if (needsFetch) {
                    val services = authManager.authenticatedCall { auth ->
                        ApiClient.tgsrtcApiService.getDistinctServices(
                            auth,
                            operationType,
                            normalizedCityId,
                            isAirport
                        )
                    }
                    if (requestToken != serviceRequestToken) return@launch
                    cachedServices = services
                    lastServiceOpType = operationType
                    lastServiceCityId = normalizedCityId
                    lastServiceIsAirport = isAirport
                }

                val scopedServices = if (routeNumberOnly) {
                    cachedServices.filter {
                        it.routeNumber.safeText().isNotBlank() || it.name.safeText().isNotBlank()
                    }
                } else {
                    cachedServices
                }
                if (requestToken != serviceRequestToken) return@launch
                _serviceState.value = ServiceSearchState.Success(
                    filterServices(scopedServices, trimmedQuery, routeNumberOnly)
                )
            } catch (e: Exception) {
                if (requestToken != serviceRequestToken) return@launch
                _serviceState.value = ServiceSearchState.Error(e.message ?: "Search failed")
            }
        }
    }

    fun searchVehicles(
        query: String,
        operationType: Int,
        cityId: String = "",
        isAirport: Boolean = false
    ) {
        val trimmedQuery = query.trim()
        val normalizedCityId = cityId.trim()
        if (operationType == 2 && normalizedCityId.isBlank() && !isAirport) {
            resetVehicleSearch("Select a city first")
            return
        }

        val requestToken = ++vehicleRequestToken
        activeVehicleQuery = trimmedQuery
        activeVehicleOpType = operationType
        activeVehicleCityId = normalizedCityId
        activeVehicleIsAirport = isAirport
        viewModelScope.launch {
            _vehicleState.value = VehicleSearchState.Loading
            try {
                val needsFetch = cachedVehicles.isEmpty()
                    || lastVehicleOpType != operationType
                    || lastVehicleCityId != normalizedCityId
                    || lastVehicleIsAirport != isAirport

                if (needsFetch) {
                    val vehicles = authManager.authenticatedCall { auth ->
                        ApiClient.tgsrtcApiService.getVehicles(
                            auth,
                            operationType,
                            normalizedCityId,
                            isAirport
                        )
                    }
                    if (requestToken != vehicleRequestToken) return@launch
                    cachedVehicles = vehicles
                    lastVehicleOpType = operationType
                    lastVehicleCityId = normalizedCityId
                    lastVehicleIsAirport = isAirport
                }

                if (requestToken != vehicleRequestToken) return@launch
                _vehicleState.value = VehicleSearchState.Success(filterVehicles(cachedVehicles, trimmedQuery))
            } catch (e: Exception) {
                if (requestToken != vehicleRequestToken) return@launch
                _vehicleState.value = VehicleSearchState.Error(e.message ?: "Search failed")
            }
        }
    }

    fun searchServiceId(serviceId: String) {
        val id = serviceId.trim()
        if (id.isBlank()) {
            resetServiceIdSearch()
            return
        }

        val requestToken = ++serviceIdRequestToken
        viewModelScope.launch {
            _serviceIdState.value = ServiceIdSearchState.Loading
            try {
                val services = authManager.authenticatedCall { auth ->
                    ApiClient.tgsrtcApiService.getPlannedServiceById(auth, id)
                }
                if (requestToken != serviceIdRequestToken) return@launch
                _serviceIdState.value = ServiceIdSearchState.Success(services)
            } catch (e: Exception) {
                if (requestToken != serviceIdRequestToken) return@launch
                _serviceIdState.value = ServiceIdSearchState.Error(e.message ?: "Search failed")
            }
        }
    }

    private fun filterServices(all: List<ServiceDto>, q: String, routeNumberOnly: Boolean): List<ServiceDto> {
        if (q.isBlank()) return all.distinctBy { it.stableKey(routeNumberOnly) }
        val query = q.normalizedSearch()
        fun ServiceDto.searchValues(): List<String> =
            (if (routeNumberOnly) listOf(routeNumber.safeText(), name.safeText()) else listOf(name.safeText()))
                .filter { it.isNotBlank() }

        val starts = all.filter {
            it.searchValues().any { value ->
                value.startsWith(q, true) || value.normalizedSearch().startsWith(query, true)
            }
        }
        val contains = all.filter {
            val values = it.searchValues()
            values.any { value ->
                value.contains(q, true) || value.normalizedSearch().contains(query, true)
            } && values.none { value ->
                value.startsWith(q, true) || value.normalizedSearch().startsWith(query, true)
            }
        }
        return (starts + contains).distinctBy { it.stableKey(routeNumberOnly) }
    }

    private fun filterVehicles(all: List<VehicleDto>, q: String): List<VehicleDto> {
        if (q.isBlank()) return all.distinctBy { it.stableKey() }
        fun VehicleDto.searchValues(): List<String> =
            listOf(registrationNumber.safeText(), name.safeText()).filter { it.isNotBlank() }

        val starts = all.filter {
            it.searchValues().any { value -> value.startsWith(q, true) }
        }
        val contains = all.filter {
            val values = it.searchValues()
            values.any { value -> value.contains(q, true) } &&
                values.none { value -> value.startsWith(q, true) }
        }
        return (starts + contains).distinctBy { it.stableKey() }
    }

    private fun String?.safeText(): String = this?.trim().orEmpty()

    private fun ServiceDto.stableKey(routeNumberOnly: Boolean): String =
        if (routeNumberOnly) {
            listOf(routeNumber.safeText(), name.safeText(), serviceType.safeText())
                .joinToString("|")
                .ifBlank { id.toString() }
        } else {
            id.takeIf { it > 0 }?.toString()
                ?: listOf(name.safeText(), serviceType.safeText()).joinToString("|")
        }

    private fun VehicleDto.stableKey(): String =
        id.takeIf { it > 0 }?.toString()
            ?: listOf(registrationNumber.safeText(), name.safeText(), serviceType.safeText()).joinToString("|")

    private fun String.normalizedSearch(): String =
        trim().filter { it.isLetterOrDigit() }.uppercase()
}

class ServiceSearchViewModelFactory(
    private val authManager: AuthManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ServiceSearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ServiceSearchViewModel(authManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
