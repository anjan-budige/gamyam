package com.gamyam.app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main   : NavKey
@Serializable data object Splash : NavKey

@Serializable data class LiveBusTracker(val serviceIdOrBusNumber: String) : NavKey
@Serializable data class ServiceIdSearchList(
    val query: String = "",
    val tab: String = "DISTRICT",
    val forceList: Boolean = false
) : NavKey
@Serializable data class ReservationSearchList(val query: String) : NavKey
@Serializable data class ReservationServicesResult(
    val reservationServiceId: Int,
    val displayNumber: String = "",
    val operationType: Int = 1,
    val requestId: Long = 0L
) : NavKey
@Serializable data class RouteNumberSearchList(
    val routeNumber: String,
    val cityId: String,
    val operationType: Int = 1,
    val isAirport: Boolean = false
) : NavKey
@Serializable data class VehicleSearchList(
    val query: String,
    val operationType: Int = 1,
    val cityId: String = "",
    val isAirport: Boolean = false
) : NavKey
@Serializable data class FromToServicesResult(
    val fromId: String,
    val fromName: String,
    val fromGeozoneType: String,
    val toId: String,
    val toName: String,
    val toGeozoneType: String,
    val operationType: Int = 1,
    val cityId: String = "",
    val isAirport: Boolean = false,
    val requestId: Long = 0L
) : NavKey
@Serializable data class RouteServicesResult(
    val routeNumber: String,
    val cityId: String = "",
    val operationType: Int = 1,
    val isAirport: Boolean = false,
    val requestId: Long = 0L
) : NavKey
@Serializable data class ServiceTripDetail(
    val serviceId: Int,
    val displayNumber: String = "",
    val operationType: Int = 1,
    val fromName: String = "",
    val toName: String = ""
) : NavKey
@Serializable data class VehicleTripDetail(
    val vehicleId: Int,
    val registrationNumber: String,
    val serviceType: String = "",
    val operationType: Int = 1,
    val requestId: Long = 0L
) : NavKey
