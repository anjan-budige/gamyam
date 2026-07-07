package com.gamyam.app.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@Serializable
data class Stop(
    val id: String?,
    val name: String?,
    val searchName: String?,
    val geozoneType: String?
)

@Serializable
data class StopListResponse(
    val stops: List<Stop>?
)

@Serializable
data class City(
    val id: String?,
    val name: String?,
    val count: Int? = 0,
    val file: String? = null
)

@Serializable
data class CityIndexResponse(
    val cities: List<City>?
)

class StopRepository(private val context: Context) {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    suspend fun getDistrictStops(): List<Stop> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("district_stops.json").bufferedReader().use { it.readText() }
            val response = jsonParser.decodeFromString<StopListResponse>(jsonString)
            response.stops ?: emptyList()
        } catch (e: IOException) {
            emptyList()
        }
    }

    suspend fun getAirportStops(): List<Stop> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("airport_stops.json").bufferedReader().use { it.readText() }
            val response = jsonParser.decodeFromString<StopListResponse>(jsonString)
            response.stops ?: emptyList()
        } catch (e: IOException) {
            emptyList()
        }
    }

    suspend fun getCities(): List<City> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("city_index.json").bufferedReader().use { it.readText() }
            val response = jsonParser.decodeFromString<CityIndexResponse>(jsonString)
            response.cities?.filter { (it.count ?: 0) > 0 } ?: emptyList()
        } catch (e: IOException) {
            emptyList()
        }
    }

    suspend fun getCityStops(cityFile: String): List<Stop> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open(cityFile).bufferedReader().use { it.readText() }
            val response = jsonParser.decodeFromString<StopListResponse>(jsonString)
            response.stops ?: emptyList()
        } catch (e: IOException) {
            emptyList()
        }
    }
}
