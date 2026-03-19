package com.asc.gymgenie.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val IconEmail: ImageVector by lazy {
    ImageVector.Builder(
        name = "Email",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.EvenOdd,
        ) {
            // Simple envelope shape using lines
            moveTo(2f, 6f)
            lineTo(2f, 18f)
            lineTo(22f, 18f)
            lineTo(22f, 6f)
            lineTo(2f, 6f)
            close()
            // Inner cutout for envelope flap
            moveTo(4f, 8f)
            lineTo(12f, 13f)
            lineTo(20f, 8f)
            lineTo(20f, 16f)
            lineTo(4f, 16f)
            lineTo(4f, 8f)
            close()
        }
    }.build()
}

val IconLock: ImageVector by lazy {
    ImageVector.Builder(
        name = "Lock",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // Lock body
        path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 10f)
            lineTo(6f, 20f)
            lineTo(18f, 20f)
            lineTo(18f, 10f)
            lineTo(6f, 10f)
            close()
            // Keyhole
            moveTo(11f, 14f)
            lineTo(13f, 14f)
            lineTo(13f, 17f)
            lineTo(11f, 17f)
            close()
        }
        // Lock shackle
        path(fill = SolidColor(Color.Black)) {
            moveTo(8f, 10f)
            lineTo(8f, 7f)
            lineTo(8f, 5f)
            lineTo(10f, 3f)
            lineTo(14f, 3f)
            lineTo(16f, 5f)
            lineTo(16f, 10f)
            lineTo(14.5f, 10f)
            lineTo(14.5f, 5.5f)
            lineTo(13.5f, 4.5f)
            lineTo(10.5f, 4.5f)
            lineTo(9.5f, 5.5f)
            lineTo(9.5f, 10f)
            close()
        }
    }.build()
}

val IconPerson: ImageVector by lazy {
    ImageVector.Builder(
        name = "Person",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // Head (circle approximation using lines)
        path(fill = SolidColor(Color.Black)) {
            // Head - simple oval
            moveTo(12f, 4f)
            lineTo(14.5f, 4.5f)
            lineTo(16f, 6.5f)
            lineTo(16f, 8.5f)
            lineTo(14.5f, 10.5f)
            lineTo(12f, 11f)
            lineTo(9.5f, 10.5f)
            lineTo(8f, 8.5f)
            lineTo(8f, 6.5f)
            lineTo(9.5f, 4.5f)
            close()
        }
        // Body
        path(fill = SolidColor(Color.Black)) {
            moveTo(4f, 20f)
            lineTo(4f, 18f)
            lineTo(6f, 15f)
            lineTo(9f, 13.5f)
            lineTo(12f, 13f)
            lineTo(15f, 13.5f)
            lineTo(18f, 15f)
            lineTo(20f, 18f)
            lineTo(20f, 20f)
            close()
        }
    }.build()
}
