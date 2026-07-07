package com.gamyam.app.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gamyam.app.data.ApiClient
import com.gamyam.app.data.AppSettingsRepository
import com.gamyam.app.data.AppThemeMode
import com.gamyam.app.data.AuthManager
import com.gamyam.app.data.SavedHomeSearchState
import com.gamyam.app.data.SearchHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

// ── Data models (JSON assets for stops) ──────────────────────────────────────

@Serializable
data class Stop(
    val id:           String = "",
    val name:         String = "",
    val searchName:   String = "",
    val geozoneType:  String = "",
    val searchGeozoneType: String = ""
)

@Serializable
data class StopsResponse(
    val count: Int          = 0,
    val stops: List<Stop>   = emptyList()
)

@Serializable
data class City(
    val id:    String = "",
    val name:  String = "",
    val count: Int    = 0,
    val file:  String = ""
)

@Serializable
data class CityIndexResponse(
    val cities: List<City> = emptyList()
)

data class NearbyStopHint(
    val id: String = "",
    val name: String = "",
    val linkedStopIds: List<String> = emptyList()
)

private data class StopMergeResult(
    val stops: List<Stop>,
    val added: Int,
    val removed: Int
)

const val HISTORY_TYPE_FROM_TO = "FROM_TO"
const val HISTORY_TYPE_SERVICE_ID = "SERVICE_ID"

/** A single entry stored in the search-history cache. */
@Serializable
data class SearchHistoryItem(
    val type: String = HISTORY_TYPE_FROM_TO,
    val fromName: String = "",
    val toName:   String = "",
    val fromCode: String = "",
    val toCode:   String = "",
    val tab:      String = ServiceTab.DISTRICT.name,
    val fromId: String = "",
    val toId: String = "",
    val fromGeozoneType: String = "",
    val toGeozoneType: String = "",
    val operationType: Int = 1,
    val cityId: String = "",
    val isAirport: Boolean = false,
    val serviceId: Int = 0,
    val serviceLabel: String = ""
) {
    val isServiceIdSearch: Boolean
        get() = type == HISTORY_TYPE_SERVICE_ID || serviceId > 0 && fromId.isBlank() && toId.isBlank()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val jsonParser   = Json { ignoreUnknownKeys = true }
    private val historyRepo  = SearchHistoryRepository(application.applicationContext)
    private val settingsRepo = AppSettingsRepository(application.applicationContext)

    // AuthManager – needed to call cities API with a bearer token
    // ── Stop lists (from JSON assets) ────────────────────────────────────────
    private val _allDistrictStops = MutableStateFlow<List<Stop>>(emptyList())
    private val _allAirportStops  = MutableStateFlow<List<Stop>>(emptyList())
    private val _currentCityStops = MutableStateFlow<List<Stop>>(emptyList())
    private val nearbyStopsByTab = mutableMapOf<ServiceTab, List<Stop>>()

    private val _filteredFromStops = MutableStateFlow<List<Stop>>(emptyList())
    val filteredFromStops: StateFlow<List<Stop>> = _filteredFromStops.asStateFlow()

    private val _filteredToStops = MutableStateFlow<List<Stop>>(emptyList())
    val filteredToStops: StateFlow<List<Stop>> = _filteredToStops.asStateFlow()

    // ── City list (from live API) ─────────────────────────────────────────────
    private val _allCities     = MutableStateFlow<List<City>>(emptyList())
    private val _filteredCities = MutableStateFlow<List<City>>(emptyList())
    val filteredCities: StateFlow<List<City>> = _filteredCities.asStateFlow()

    private val _selectedCityId = MutableStateFlow("")
    val selectedCityId: StateFlow<String> = _selectedCityId.asStateFlow()

    private val _selectedCityName = MutableStateFlow("")
    val selectedCityName: StateFlow<String> = _selectedCityName.asStateFlow()

    private val _selectedTab = MutableStateFlow(ServiceTab.DISTRICT)
    val selectedTab: StateFlow<ServiceTab> = _selectedTab.asStateFlow()
    private var restoredHomeStateOnce = false
    private var restoredSavedCityOnce = false

    private val _updateStopsStatus = MutableStateFlow("")
    val updateStopsStatus: StateFlow<String> = _updateStopsStatus.asStateFlow()

    val savedHomeSearchState: StateFlow<SavedHomeSearchState> = settingsRepo.homeSearchState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedHomeSearchState())

    val themeMode: StateFlow<AppThemeMode> = settingsRepo.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppThemeMode.SYSTEM)

    // ── Search history (DataStore) ────────────────────────────────────────────
    val recentSearches: StateFlow<List<SearchHistoryItem>> = historyRepo.flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ─────────────────────────────────────────────────────────────────────────
    //  Load data  (idempotent — only runs once)
    //  Stops: JSON assets  |  Cities: live API
    // ─────────────────────────────────────────────────────────────────────────
    fun loadData(force: Boolean = false) {
        if (!force && _allDistrictStops.value.isNotEmpty()) return
        val ctx = getApplication<Application>().applicationContext
        viewModelScope.launch {
            // ── Stops from JSON assets ────────────────────────────────────
            withContext(Dispatchers.IO) {
                try {
                    val district = jsonParser.decodeFromString<StopsResponse>(readStopsJson("district_stops.json"))
                    _allDistrictStops.value   = district.stops
                    _filteredFromStops.value  = district.stops.take(50)
                    _filteredToStops.value    = district.stops.take(50)

                    val airport = jsonParser.decodeFromString<StopsResponse>(readStopsJson("airport_stops.json"))
                    _allAirportStops.value = airport.stops
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // ── Cities from live API ──────────────────────────────────────
            withContext(Dispatchers.IO) {
                try {
                    val cityIndex = jsonParser.decodeFromString<CityIndexResponse>(readStopsJson("city_index.json"))
                    val cities = cityIndex.cities
                        .filter { it.count > 0 && it.file.isNotBlank() }
                        .sortedBy { it.name }
                    _allCities.value      = cities
                    _filteredCities.value = cities
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  City selection – loads city stops from JSON asset keyed by city name
    //  (city stops are still in assets; only the city *list* comes from API)
    // ─────────────────────────────────────────────────────────────────────────
    fun selectCity(cityName: String) {
        _allCities.value.firstOrNull { it.name.equals(cityName, ignoreCase = true) }?.let {
            selectCity(it)
            return
        }
        clearCityStops()
    }

    fun selectTab(tab: ServiceTab) {
        restoredHomeStateOnce = true
        _selectedTab.value = tab
        saveHomeSearchState(savedHomeSearchState.value.copy(tab = tab.name))
    }

    fun shouldRestoreHomeState(state: SavedHomeSearchState): Boolean {
        if (restoredHomeStateOnce || state == SavedHomeSearchState()) return false
        restoredHomeStateOnce = true
        return true
    }

    fun shouldRestoreSavedCity(state: SavedHomeSearchState): Boolean {
        if (
            restoredSavedCityOnce ||
            state.tab != ServiceTab.CITY.name ||
            state.cityId.isBlank() && state.cityName.isBlank()
        ) {
            return false
        }
        restoredSavedCityOnce = true
        return true
    }

    fun selectCity(city: City) {
        restoredSavedCityOnce = true
        nearbyStopsByTab.remove(ServiceTab.CITY)
        _selectedCityId.value = city.id
        _selectedCityName.value = city.name
        if (_selectedTab.value == ServiceTab.CITY) {
            saveHomeSearchState(savedHomeSearchState.value.copy(tab = ServiceTab.CITY.name, cityId = city.id, cityName = city.name))
        }
        // Derive a safe filename from the city name: lowercase, spaces → underscores
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    if (_selectedTab.value == ServiceTab.CITY && _selectedCityId.value == city.id) {
                        saveHomeSearchState(savedHomeSearchState.value.copy(tab = ServiceTab.CITY.name, cityId = city.id, cityName = city.name))
                    }
                    val cityStops = jsonParser.decodeFromString<StopsResponse>(readStopsJson(city.file))
                    if (_selectedCityId.value == city.id) {
                        _currentCityStops.value  = cityStops.stops
                    }
                    if (_selectedTab.value == ServiceTab.CITY && _selectedCityId.value == city.id) {
                        _filteredFromStops.value = cityStops.stops.take(50)
                        _filteredToStops.value   = cityStops.stops.take(50)
                    }
                } catch (e: Exception) {
                    // Asset may not exist for this city – leave stops empty
                    if (_selectedTab.value == ServiceTab.CITY && _selectedCityId.value == city.id) {
                        clearCityStops()
                    } else if (_selectedCityId.value == city.id) {
                        _currentCityStops.value = emptyList()
                    }
                    e.printStackTrace()
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Search helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun getStopsForTab(tab: ServiceTab) = when (tab) {
        ServiceTab.DISTRICT -> _allDistrictStops.value
        ServiceTab.CITY     -> _currentCityStops.value
        ServiceTab.AIRPORT  -> _allAirportStops.value
    }

    fun searchFromStops(query: String, tab: ServiceTab) {
        _filteredFromStops.value = filterStops(getStopsForTab(tab), query, tab)
    }

    fun searchToStops(query: String, tab: ServiceTab) {
        _filteredToStops.value = filterStops(getStopsForTab(tab), query, tab)
    }

    fun prioritizeNearbyStops(tab: ServiceTab, nearbyStops: List<NearbyStopHint>): Int {
        val matches = matchExistingNearbyStops(getStopsForTab(tab), nearbyStops)
        if (matches.isEmpty()) {
            nearbyStopsByTab.remove(tab)
            return 0
        }
        nearbyStopsByTab[tab] = matches
        _filteredFromStops.value = matches
        _filteredToStops.value = matches
        return matches.size
    }

    fun hasNearbyStops(tab: ServiceTab): Boolean =
        nearbyStopsByTab[tab].orEmpty().isNotEmpty()

    fun showNearbyFromStops(tab: ServiceTab): Boolean {
        val stops = nearbyStopsByTab[tab].orEmpty()
        if (stops.isEmpty()) return false
        _filteredFromStops.value = stops
        return true
    }

    fun showNearbyToStops(tab: ServiceTab): Boolean {
        val stops = nearbyStopsByTab[tab].orEmpty()
        if (stops.isEmpty()) return false
        _filteredToStops.value = stops
        return true
    }

    fun searchCities(query: String) {
        val all = _allCities.value
        _filteredCities.value = if (query.isBlank()) all else {
            val exact   = all.filter { it.name.startsWith(query, ignoreCase = true) }
            val partial = all.filter { it.name.contains(query, ignoreCase = true) && !it.name.startsWith(query, ignoreCase = true) }
            (exact + partial)
        }
    }

    fun saveHomeSearchState(state: SavedHomeSearchState) {
        viewModelScope.launch { settingsRepo.saveHomeSearchState(state) }
    }

    fun saveThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    }

    fun clearUpdateStopsStatus() {
        _updateStopsStatus.value = ""
    }

    fun updateStops(authManager: AuthManager?) {
        val manager = authManager ?: run {
            _updateStopsStatus.value = "Unable to update stops"
            return
        }
        val ctx = getApplication<Application>().applicationContext
        viewModelScope.launch {
            _updateStopsStatus.value = "Updating stops"
            try {
                val currentDistrictStops = readStopsList("district_stops.json")
                val currentAirportStops = readStopsList("airport_stops.json")
                val currentCities = _allCities.value
                val currentCitiesByKey = currentCities.associateBy { it.cityMergeKey() }

                val districtStops = manager.authenticatedCall { auth ->
                    ApiClient.tgsrtcApiService.getStops(auth, operationType = 1)
                }
                    .map { it.toStop() }
                    .distinctBy { it.mergeKey() }
                val districtMerge = mergeStopsKeepingExisting(currentDistrictStops, districtStops)
                if (districtStops.isNotEmpty() && districtMerge.added > 0) {
                    writeStopsJson(ctx, "district_stops.json", districtMerge.stops)
                }

                val airportStops = manager.authenticatedCall { auth ->
                    ApiClient.tgsrtcApiService.getStops(auth, operationType = 3, isAirport = true)
                }
                    .map { it.toStop() }
                    .distinctBy { it.mergeKey() }
                val airportMerge = mergeStopsKeepingExisting(currentAirportStops, airportStops)
                if (airportStops.isNotEmpty() && airportMerge.added > 0) {
                    writeStopsJson(ctx, "airport_stops.json", airportMerge.stops)
                }

                val apiCities = manager.authenticatedCall { auth ->
                    ApiClient.tgsrtcApiService.getCities(auth)
                }
                val apiCityModels = apiCities.map { city ->
                    val file = cityFileName(city.name, city.id)
                    City(id = city.id, name = city.name, count = 0, file = file)
                }
                val apiCitiesByKey = apiCityModels.associateBy { it.cityMergeKey() }
                val mergedCitySeeds = (currentCitiesByKey.keys + apiCitiesByKey.keys)
                    .mapNotNull { key ->
                        val current = currentCitiesByKey[key]
                        val fetched = apiCitiesByKey[key]
                        when {
                            current != null && fetched != null -> current.copy(
                                id = fetched.id.ifBlank { current.id },
                                name = fetched.name.ifBlank { current.name }
                            )
                            current != null -> current
                            else -> fetched
                        }
                    }
                    .ifEmpty { currentCities }

                var cityStopsAdded = 0
                var cityStopsRemoved = 0
                val updatedCities = mergedCitySeeds.map { city ->
                    val latestStops = if (apiCitiesByKey.containsKey(city.cityMergeKey())) {
                        runCatching {
                            manager.authenticatedCall { auth ->
                                ApiClient.tgsrtcApiService.getStops(auth, operationType = 2, cityId = city.id)
                            }
                                .map { it.toStop() }
                                .distinctBy { it.mergeKey() }
                        }.getOrDefault(emptyList())
                    } else {
                        emptyList()
                    }
                    val existingStops = readStopsList(city.file)
                    val mergedStops = mergeStopsKeepingExisting(existingStops, latestStops)
                    if (latestStops.isNotEmpty()) {
                        cityStopsAdded += mergedStops.added
                        cityStopsRemoved += mergedStops.removed
                    }
                    if (latestStops.isNotEmpty() && mergedStops.added > 0) {
                        writeStopsJson(ctx, city.file, mergedStops.stops)
                    }
                    val count = when {
                        latestStops.isNotEmpty() -> mergedStops.stops.size
                        existingStops.isNotEmpty() -> existingStops.size
                        else -> countStopsFromJson(city.file)
                    }
                    city.copy(count = count)
                }.filter { it.count > 0 }
                    .sortedBy { it.name }
                writeCityIndexJson(ctx, updatedCities)

                loadData(force = true)
                val added = districtMerge.added + airportMerge.added + cityStopsAdded
                val removed = districtMerge.removed + airportMerge.removed + cityStopsRemoved
                _updateStopsStatus.value = when {
                    added > 0 && removed > 0 -> "$added new stations added. $removed missing in latest fetch, kept locally."
                    added > 0 -> "$added new stations added"
                    removed > 0 -> "No new stations. $removed missing in latest fetch, kept locally."
                    else -> "Stops are up to date"
                }
            } catch (e: Exception) {
                _updateStopsStatus.value = e.message ?: "Unable to update stops"
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Save a completed search to history
    // ─────────────────────────────────────────────────────────────────────────

    fun saveSearch(from: Stop, to: Stop, fromCode: String, toCode: String, tab: ServiceTab) {
        val operationType = when (tab) {
            ServiceTab.DISTRICT -> 1
            ServiceTab.CITY -> 2
            ServiceTab.AIRPORT -> 3
        }
        fun Stop.requestGeozoneType(): String =
            (if (tab == ServiceTab.AIRPORT) searchGeozoneType.ifBlank { geozoneType } else geozoneType)
                .ifBlank { "stage" }

        viewModelScope.launch {
            historyRepo.save(
                SearchHistoryItem(
                    type = HISTORY_TYPE_FROM_TO,
                    fromName = from.name, toName = to.name,
                    fromCode = fromCode,  toCode = toCode,
                    tab      = tab.name,
                    fromId = from.id,
                    toId = to.id,
                    fromGeozoneType = from.requestGeozoneType(),
                    toGeozoneType = to.requestGeozoneType(),
                    operationType = operationType,
                    cityId = if (tab == ServiceTab.CITY) _selectedCityId.value else "",
                    isAirport = tab == ServiceTab.AIRPORT
                )
            )
        }
    }

    fun saveServiceIdSearch(serviceId: Int, displayLabel: String, tab: ServiceTab) {
        if (serviceId <= 0) return
        viewModelScope.launch {
            historyRepo.save(
                SearchHistoryItem(
                    type = HISTORY_TYPE_SERVICE_ID,
                    fromName = "Service ID",
                    toName = serviceId.toString(),
                    tab = tab.name,
                    operationType = when (tab) {
                        ServiceTab.DISTRICT -> 1
                        ServiceTab.CITY -> 2
                        ServiceTab.AIRPORT -> 3
                    },
                    cityId = if (tab == ServiceTab.CITY) _selectedCityId.value else "",
                    isAirport = tab == ServiceTab.AIRPORT,
                    serviceId = serviceId,
                    serviceLabel = displayLabel.ifBlank { serviceId.toString() }
                )
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Internal
    // ─────────────────────────────────────────────────────────────────────────

    private fun filterStops(stops: List<Stop>, query: String, tab: ServiceTab): List<Stop> {
        if (query.isBlank()) return nearbyStopsByTab[tab]?.take(20) ?: stops.take(50)
        val q       = query.trim()
        val exact   = stops.filter { it.name.startsWith(q, ignoreCase = true) || it.searchName.startsWith(q, ignoreCase = true) }
        val partial = stops.filter {
            (it.name.contains(q, ignoreCase = true) || it.searchName.contains(q, ignoreCase = true))
                && !it.name.startsWith(q, ignoreCase = true) && !it.searchName.startsWith(q, ignoreCase = true)
        }
        return (exact + partial).take(50)
    }

    private fun matchExistingNearbyStops(stops: List<Stop>, nearbyStops: List<NearbyStopHint>): List<Stop> {
        if (nearbyStops.isEmpty()) return emptyList()
        val byId = stops.associateBy { it.id }
        val byName = stops.associateBy { it.name.normalizedStopName() }
        val normalizedStops = stops.map { it to it.name.normalizedStopName() }
        return nearbyStops.mapNotNull { hint ->
            hint.linkedStopIds.firstNotNullOfOrNull { byId[it] }
                ?: byId[hint.id]
                ?: byName[hint.name.normalizedStopName()]
                ?: normalizedStops.firstOrNull { (_, name) -> name.isNearbyNameMatch(hint.name.normalizedStopName()) }?.first
        }.distinctBy { it.id }
    }

    private fun String.normalizedStopName(): String =
        trim().lowercase().replace(Regex("""[^a-z0-9]+"""), " ")
            .trim()

    private fun String.isNearbyNameMatch(other: String): Boolean {
        if (isBlank() || other.isBlank()) return false
        if (this == other) return true
        val thisTokens = stopNameTokens()
        val otherTokens = other.stopNameTokens()
        if (thisTokens.isEmpty() || otherTokens.isEmpty()) return false
        val common = thisTokens.intersect(otherTokens)
        return common.size >= 2 || common.sumOf { it.length } >= 10
    }

    private fun String.stopNameTokens(): Set<String> =
        split(" ")
            .map { token ->
                when (token) {
                    "chowk", "chourastha", "chowrastha" -> "xroad"
                    "x", "rd", "road", "roads" -> "road"
                    "geeta", "gita", "geethabhavan", "gitabhavan" -> "geethabhavan"
                    "karimnagar" -> "karimnagar"
                    else -> token
                }
            }
            .filter { it.length > 2 && it !in setOf("twd", "towards", "bus", "stop", "depot") }
            .toSet()

    private fun clearCityStops() {
        nearbyStopsByTab.remove(ServiceTab.CITY)
        _selectedCityId.value = ""
        _selectedCityName.value = ""
        _currentCityStops.value  = emptyList()
        _filteredFromStops.value = emptyList()
        _filteredToStops.value   = emptyList()
        saveHomeSearchState(savedHomeSearchState.value.copy(cityId = "", cityName = ""))
    }

    private fun readStopsJson(fileName: String): String {
        val ctx = getApplication<Application>().applicationContext
        val updated = File(ctx.filesDir, "stops/$fileName")
        return if (updated.exists()) {
            updated.readText()
        } else {
            ctx.assets.open(fileName).bufferedReader().use { it.readText() }
        }
    }

    private fun writeStopsJson(ctx: android.content.Context, fileName: String, stops: List<Stop>) {
        val dir = File(ctx.filesDir, "stops").apply { mkdirs() }
        File(dir, fileName).writeText(jsonParser.encodeToString(StopsResponse(count = stops.size, stops = stops)))
    }

    private fun writeCityIndexJson(ctx: android.content.Context, cities: List<City>) {
        val dir = File(ctx.filesDir, "stops").apply { mkdirs() }
        File(dir, "city_index.json").writeText(jsonParser.encodeToString(CityIndexResponse(cities)))
    }

    private fun readStopsList(fileName: String): List<Stop> =
        runCatching { jsonParser.decodeFromString<StopsResponse>(readStopsJson(fileName)).stops }
            .getOrDefault(emptyList())

    private fun countStopsFromJson(fileName: String): Int =
        readStopsList(fileName).size

    private fun mergeStopsKeepingExisting(existing: List<Stop>, fetched: List<Stop>): StopMergeResult {
        if (fetched.isEmpty()) return StopMergeResult(existing, added = 0, removed = 0)
        val existingByKey = existing.associateBy { it.mergeKey() }
        val fetchedByKey = fetched.associateBy { it.mergeKey() }
        val additions = fetched.filter { it.mergeKey() !in existingByKey }
        val removed = existing.count { it.mergeKey() !in fetchedByKey }
        val merged = (existing + additions)
            .filter { it.mergeKey().isNotBlank() }
            .distinctBy { it.mergeKey() }
            .sortedBy { it.name }
        return StopMergeResult(merged, added = additions.size, removed = removed)
    }

    private fun Stop.mergeKey(): String =
        id.ifBlank { name }.trim().lowercase()

    private fun City.cityMergeKey(): String =
        id.ifBlank { name }.trim().lowercase()

    private fun com.gamyam.app.data.NearbyStopDto.toStop(): Stop =
        Stop(
            id = id,
            name = name.orEmpty(),
            searchName = name.orEmpty(),
            geozoneType = geozoneType.orEmpty().ifBlank { "stage" },
            searchGeozoneType = geozoneType.orEmpty()
        )

    private fun cityFileName(name: String, id: String): String {
        val safeName = name.lowercase().replace(Regex("""[^a-z0-9]+"""), "_").trim('_')
        val safeId = id.lowercase().replace(Regex("""[^a-z0-9]+"""), "_").trim('_')
        return "city_${safeName}_${safeId}_stops.json"
    }
}
