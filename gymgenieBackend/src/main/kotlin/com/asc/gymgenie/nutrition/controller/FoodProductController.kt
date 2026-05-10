package com.asc.gymgenie.nutrition.controller

import com.asc.gymgenie.nutrition.dto.FoodProductResponse
import com.asc.gymgenie.nutrition.entity.FoodCategory
import com.asc.gymgenie.nutrition.service.FoodProductService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

/**
 * Read-only HTTP API for the global food product catalog.
 *
 * Endpoints require an authenticated user (enforced by SecurityConfig — anyRequest().authenticated()),
 * but the data itself is not user-scoped. The [Authentication] parameter is kept for consistency
 * with other authenticated endpoints and to make the auth requirement explicit.
 */
@RestController
@RequestMapping("/api/v1/products")
class FoodProductController(
    private val foodProductService: FoodProductService
) {

    @GetMapping
    fun search(
        @Suppress("unused") authentication: Authentication,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) category: FoodCategory?
    ): ResponseEntity<List<FoodProductResponse>> {
        return ResponseEntity.ok(foodProductService.search(search, category))
    }

    @GetMapping("/{id}")
    fun getById(
        @Suppress("unused") authentication: Authentication,
        @PathVariable id: UUID
    ): ResponseEntity<FoodProductResponse> {
        return ResponseEntity.ok(foodProductService.getById(id))
    }
}
