package com.gamyam.app.data

import com.google.gson.annotations.SerializedName
import com.google.gson.JsonElement
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// ── OAuth ─────────────────────────────────────────────────────────────────────

data class MapAccessTokenModel(
    val access_token: String,
    val token_type:   String,
    val expires_in:   Long,
    val project_code: String,
    val client_id:    String
)

interface OAuthApiService {
    @FormUrlEncoded
    @POST("api/security/oauth/token")
    suspend fun getAccessToken(
        @Field("grant_type")    grantType:    String = "client_credentials",
        @Field("client_id")     clientId:     String,
        @Field("client_secret") clientSecret: String
    ): MapAccessTokenModel
}

// ── TGSRTC DTOs ───────────────────────────────────────────────────────────────

data class CityDto(
    @SerializedName("id")          val id:   String = "",
    @SerializedName("name")        val name: String = ""
)

data class ServiceDto(
    @SerializedName("id")          val id:          Int    = 0,
    @SerializedName("name")        val name:        String? = null,
    @SerializedName("routeNumber") val routeNumber: String? = null,
    @SerializedName("serviceType") val serviceType: String? = null
)

data class VehicleDto(
    @SerializedName("id")                 val id:                 Int    = 0,
    @SerializedName("name")               val name:               String? = null,
    @SerializedName("registrationNumber") val registrationNumber: String? = null,
    @SerializedName("serviceType")        val serviceType:        String? = null
)

data class PlannedPointDto(
    @SerializedName("id")          val id:          String = "",
    @SerializedName("name")        val name:        String? = null,
    @SerializedName("plannedTime") val plannedTime: Long?  = null
)

data class PlannedServiceDto(
    @SerializedName("serviceId")   val serviceId:   Int    = 0,
    @SerializedName("serviceName") val serviceName: String? = null,
    @SerializedName("serviceType") val serviceType: String? = null,
    @SerializedName("duration")    val duration:    Int    = 0,
    @SerializedName("depot")       val depot:       String? = null,
    @SerializedName("from")        val from:        PlannedPointDto? = null,
    @SerializedName("to")          val to:          PlannedPointDto? = null
)

data class GeometryDto(
    @SerializedName("type")        val type:        String? = null,
    @SerializedName("coordinates") val coordinates: JsonElement? = null
)

data class TripPointDto(
    @SerializedName("id")              val id:              String = "",
    @SerializedName("name")            val name:            String? = null,
    @SerializedName("sequence")        val sequence:        Int?   = null,
    @SerializedName("plannedTime")     val plannedTime:     Long?  = null,
    @SerializedName("plannedExitTime") val plannedExitTime: Long?  = null,
    @SerializedName("eta")             val eta:             Long?  = null,
    @SerializedName("etd")             val etd:             Long?  = null,
    @SerializedName("entryTime")       val entryTime:       Long?  = null,
    @SerializedName("exitTime")        val exitTime:        Long?  = null,
    @SerializedName("geometry")        val geometry:        GeometryDto? = null
)

data class PolylinePointDto(
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lng") val lng: Double? = null
)

data class BusTripDto(
    @SerializedName("id")            val id:            Int    = 0,
    @SerializedName("tripId")        val tripId:        Int?   = null,
    @SerializedName("serviceId")     val serviceId:     Int    = 0,
    @SerializedName("serviceName")   val serviceName:   String? = null,
    @SerializedName("serviceType")   val serviceType:   String? = null,
    @SerializedName("vehicleId")     val vehicleId:     Int    = 0,
    @SerializedName("vehicleNumber") val vehicleNumber: String? = null,
    @SerializedName("depot")         val depot:         String? = null,
    @SerializedName("duration")      val duration:      Int    = 0,
    @SerializedName("totalStops")    val totalStops:    Int    = 0,
    @SerializedName("statusStr")     val statusStr:     String? = null,
    @SerializedName("from")          val from:          TripPointDto? = null,
    @SerializedName("to")            val to:            TripPointDto? = null,
    @SerializedName("myPoint")       val myPoint:       TripPointDto? = null
)

data class TripDetailDto(
    @SerializedName("id")               val id:               Int    = 0,
    @SerializedName("name")             val name:             String? = null,
    @SerializedName("serviceId")        val serviceId:        Int    = 0,
    @SerializedName("serviceName")      val serviceName:      String? = null,
    @SerializedName("vehicleId")        val vehicleId:        Int    = 0,
    @SerializedName("vehicleNumber")    val vehicleNumber:    String? = null,
    @SerializedName("serviceType")      val serviceType:      String? = null,
    @SerializedName("statusStr")        val statusStr:        String? = null,
    @SerializedName("status")           val status:           Int    = 0,
    @SerializedName("depotName")        val depotName:        String? = null,
    @SerializedName("driverName")       val driverName:       String? = null,
    @SerializedName("driverMobile")     val driverMobile:     String? = null,
    @SerializedName("helpline")         val helpline:         String? = null,
    @SerializedName("isDelayed")        val isDelayed:        Boolean = false,
    @SerializedName("duration")         val duration:         Int    = 0,
    @SerializedName("plannedStartTime") val plannedStartTime: Long?  = null,
    @SerializedName("plannedEndTime")   val plannedEndTime:   Long?  = null,
    @SerializedName("points")           val points:           List<TripPointDto> = emptyList(),
    @SerializedName("polylinePoints")   val polylinePoints:   List<PolylinePointDto> = emptyList()
)

data class LiveVehicleDto(
    @SerializedName("id")            val id:            Int    = 0,
    @SerializedName("vehicleNumber") val vehicleNumber: String? = null,
    @SerializedName("tripId")        val tripId:        Int    = 0,
    @SerializedName("latitude")      val latitude:      Double? = null,
    @SerializedName("longitude")     val longitude:     Double? = null,
    @SerializedName("heading")       val heading:       Double? = null,
    @SerializedName("speedKph")      val speedKph:      Double? = null,
    @SerializedName("gpsTime")       val gpsTime:       Long?  = null,
    @SerializedName("gprsTime")      val gprsTime:      Long?  = null,
    @SerializedName("address")       val address:       String? = null,
    @SerializedName("status")        val status:        Int    = 0,
    @SerializedName("statusStr")     val statusStr:     String? = null,
    @SerializedName("currentPointId") val currentPointId: Int = 0,
    @SerializedName("previousPointId") val previousPointId: Int = 0,
    @SerializedName("nextPointId")    val nextPointId:    Int = 0,
    @SerializedName("distanceFromPrevious") val distanceFromPrevious: Double? = null
)

data class RunningTripsResponseDto(
    @SerializedName("trips") val trips: List<BusTripDto> = emptyList()
)

data class NearbyStopDto(
    @SerializedName("id")          val id:          String = "",
    @SerializedName("name")        val name:        String? = null,
    @SerializedName("distance")    val distance:    Double? = null,
    @SerializedName("geozoneType") val geozoneType: String? = null,
    @SerializedName("metadata")    val metadata:    Map<String, String> = emptyMap()
)

// ── TGSRTC API service ────────────────────────────────────────────────────────

interface TgsrtcApiService {

    @GET("tsrtc/api/v3.0/city")
    suspend fun getCities(
        @Header("Authorization") auth: String
    ): List<CityDto>

    /** District: operationType=1, City: operationType=2, Airport: operationType=3 */
    @GET("tsrtc/api/v3.0/distinctservices")
    suspend fun getDistinctServices(
        @Header("Authorization") auth:          String,
        @Query("operationType")  operationType: Int,
        @Query("cityId")         cityId:        String  = "",
        @Query("isAirport")      isAirport:     Boolean = false
    ): List<ServiceDto>

    @GET("tsrtc/api/v2.0/vehicles")
    suspend fun getVehicles(
        @Header("Authorization") auth:          String,
        @Query("operationType")  operationType: Int,
        @Query("cityId")         cityId:        String  = "",
        @Query("isAirport")      isAirport:     Boolean = false
    ): List<VehicleDto>

    @GET("tsrtc/api/v3.0/trips/planned")
    suspend fun getPlannedTripsBetweenLocations(
        @Header("Authorization") auth: String,
        @Query("formGeozoneType") fromGeozoneType: String,
        @Query("toGeozoneType") toGeozoneType: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("cityId") cityId: String = "",
        @Query("operationType") operationType: Int,
        @Query("startTime") startTime: String = "",
        @Query("serviceType") serviceType: String = "",
        @Query("endTime") endTime: String = "",
        @Query("isAirport") isAirport: Boolean = false
    ): List<BusTripDto>

    @GET("tsrtc/api/v3.0/trips/betweenTwoLocations")
    suspend fun getRunningTripsBetweenLocations(
        @Header("Authorization") auth: String,
        @Query("formGeozoneType") fromGeozoneType: String,
        @Query("toGeozoneType") toGeozoneType: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("cityId") cityId: String = "",
        @Query("operationType") operationType: Int,
        @Query("longitude") longitude: String = "",
        @Query("latitude") latitude: String = "",
        @Query("isAirport") isAirport: Boolean = false,
        @Query("serviceType") serviceType: String = ""
    ): RunningTripsResponseDto

    @GET("tsrtc/api/v3.0/trips/planned/routeNumber")
    suspend fun getPlannedTripsByRouteNumber(
        @Header("Authorization") auth: String,
        @Query("routeNumber") routeNumber: String,
        @Query("cityId") cityId: String = "",
        @Query("operationType") operationType: Int,
        @Query("isAirport") isAirport: Boolean = false
    ): List<BusTripDto>

    @GET("tsrtc/api/v3.0/trips/routeNumber")
    suspend fun getRunningTripsByRouteNumber(
        @Header("Authorization") auth: String,
        @Query("routeNumber") routeNumber: String,
        @Query("operationType") operationType: Int,
        @Query("cityId") cityId: String = "",
        @Query("isAirport") isAirport: Boolean = false
    ): List<BusTripDto>

    @GET("tsrtc/api/v3.0/trips/planned/{serviceId}")
    suspend fun getPlannedServiceById(
        @Header("Authorization") auth: String,
        @Path("serviceId") serviceId: String
    ): List<PlannedServiceDto>

    @GET("tsrtc/api/v2.0/trips/service/{serviceId}")
    suspend fun getRunningTripsByService(
        @Header("Authorization") auth:          String,
        @Path("serviceId")       serviceId:     String,
        @Query("operationType")  operationType: Int,
        @Query("routeType")      routeType:     Int = 2
    ): List<BusTripDto>

    @GET("tsrtc/api/v2.0/services/{serviceId}")
    suspend fun getServiceDetails(
        @Header("Authorization") auth:      String,
        @Path("serviceId")       serviceId: String
    ): TripDetailDto

    @GET("tsrtc/api/v2.0/trips/vehicleNumber/{vehicleId}")
    suspend fun getTripsByVehicle(
        @Header("Authorization") auth:          String,
        @Path("vehicleId")       vehicleId:     String,
        @Query("operationType")  operationType: Int,
        @Query("routeType")      routeType:     Int = 2
    ): List<BusTripDto>

    @GET("tsrtc/api/v2.0/vehicles/{vehicleId}")
    suspend fun getLiveVehicle(
        @Header("Authorization") auth:      String,
        @Path("vehicleId")       vehicleId: String
    ): LiveVehicleDto

    @GET("tsrtc/api/v2.0/stops/nearby")
    suspend fun getNearbyStops(
        @Header("Authorization") auth: String,
        @Query("longitude") longitude: Double,
        @Query("latitude") latitude: Double,
        @Query("maxDistance") maxDistance: Int = 20,
        @Query("limit") limit: Int = 20
    ): List<NearbyStopDto>

    @GET("tsrtc/api/v3.0/stops")
    suspend fun getNearestStops(
        @Header("Authorization") auth: String,
        @Query("operationType") operationType: Int,
        @Query("cityId") cityId: String = "",
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("isAirport") isAirport: Boolean = false
    ): List<NearbyStopDto>

    @GET("tsrtc/api/v3.0/stops")
    suspend fun getStops(
        @Header("Authorization") auth: String,
        @Query("operationType") operationType: Int,
        @Query("cityId") cityId: String = "",
        @Query("isAirport") isAirport: Boolean = false
    ): List<NearbyStopDto>

}

// ── Retrofit singletons ───────────────────────────────────────────────────────

object ApiClient {
    private const val OAUTH_BASE_URL  = "https://outpost.mapmyindia.com/"
    private const val TGSRTC_BASE_URL = "https://intouch.mapmyindia.com/"

    val oauthApiService: OAuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(OAUTH_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OAuthApiService::class.java)
    }

    val tgsrtcApiService: TgsrtcApiService by lazy {
        Retrofit.Builder()
            .baseUrl(TGSRTC_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TgsrtcApiService::class.java)
    }
}
