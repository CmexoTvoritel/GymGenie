package com.asc.gymgenie.activity.controller

import com.asc.gymgenie.activity.dto.ActivityCatalogResponse
import com.asc.gymgenie.activity.dto.ActivityCheckinRequest
import com.asc.gymgenie.activity.dto.ActivityHistoryDayResponse
import com.asc.gymgenie.activity.dto.ActivityLogResponse
import com.asc.gymgenie.activity.dto.ActivityTodayResponse
import com.asc.gymgenie.activity.dto.AddActivityToPlanRequest
import com.asc.gymgenie.activity.dto.UpdateActivityScheduleRequest
import com.asc.gymgenie.activity.service.ActivityService
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/v1/activities")
class ActivityController(
    private val activityService: ActivityService
) {

    @GetMapping("/catalog")
    fun getCatalog(): ResponseEntity<List<ActivityCatalogResponse>> {
        return ResponseEntity.ok(activityService.getCatalog())
    }

    @GetMapping("/today")
    fun getToday(
        authentication: Authentication,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?
    ): ResponseEntity<List<ActivityTodayResponse>> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(activityService.getTodayActivities(userId, date ?: LocalDate.now()))
    }

    @PostMapping("/{activityId}/checkin")
    fun checkin(
        authentication: Authentication,
        @PathVariable activityId: UUID,
        @Valid @RequestBody request: ActivityCheckinRequest
    ): ResponseEntity<ActivityLogResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(activityService.checkin(userId, activityId, request))
    }

    @PostMapping("/{activityId}/plan")
    fun addToPlan(
        authentication: Authentication,
        @PathVariable activityId: UUID,
        @Valid @RequestBody(required = false) request: AddActivityToPlanRequest?
    ): ResponseEntity<Void> {
        val userId = UUID.fromString(authentication.name)
        activityService.addToPlan(userId, activityId, request ?: AddActivityToPlanRequest())
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{activityId}/plan/schedule")
    fun updateSchedule(
        authentication: Authentication,
        @PathVariable activityId: UUID,
        @Valid @RequestBody request: UpdateActivityScheduleRequest
    ): ResponseEntity<ActivityTodayResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(activityService.updateSchedule(userId, activityId, request))
    }

    @DeleteMapping("/{activityId}/plan")
    fun removeFromPlan(
        authentication: Authentication,
        @PathVariable activityId: UUID
    ): ResponseEntity<Void> {
        val userId = UUID.fromString(authentication.name)
        activityService.removeFromPlan(userId, activityId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/history")
    fun getHistory(
        authentication: Authentication,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<List<ActivityHistoryDayResponse>> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(activityService.getHistory(userId, startDate, endDate))
    }
}
