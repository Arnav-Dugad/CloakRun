package com.arnavgames.cloakrun.feature.lobby

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arnavgames.cloakrun.core.data.LobbyFailure
import com.arnavgames.cloakrun.core.designsystem.token.CloakPalette
import com.arnavgames.cloakrun.core.game.lobby.Role
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

internal fun LobbyFailure.message(): Int = when (this) {
    LobbyFailure.NotConfigured -> R.string.failure_not_configured
    LobbyFailure.Disabled -> R.string.failure_disabled
    LobbyFailure.CodeNotFound -> R.string.failure_code
    LobbyFailure.Full -> R.string.failure_full
    LobbyFailure.AlreadyStarted -> R.string.failure_started
    LobbyFailure.Expired -> R.string.failure_expired
    LobbyFailure.Kicked -> R.string.failure_kicked
    LobbyFailure.Offline -> R.string.failure_offline
    LobbyFailure.Denied -> R.string.failure_denied
    LobbyFailure.Unknown -> R.string.failure_unknown
}

internal fun Role.label(): Int = when (this) {
    Role.Unassigned -> R.string.lobby_role_unassigned
    Role.Hider -> R.string.lobby_role_hider
    Role.Seeker -> R.string.lobby_role_seeker
    Role.Spectator -> R.string.lobby_role_spectator
}

/**
 * A QR code drawn as rounded dark modules on a light tile, with a quiet zone. Inverted codes (light
 * on dark) fail on some scanners, so the tile stays light in every theme. The three finder patterns
 * stay square blocks so every scanner recognises them. Medium error correction survives a cracked
 * or glare-covered screen.
 */
@Composable
internal fun QrCode(
    text: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    color: Color = CloakPalette.Midnight800,
    tile: Color = CloakPalette.Frost50,
    size: Dp = 240.dp,
) {
    val matrix = remember(text) { encodeQr(text) }
    Canvas(modifier.size(size).semantics { this.contentDescription = contentDescription }) {
        val quiet = 2
        val cells = matrix.width + quiet * 2
        val cell = this.size.minDimension / cells
        drawRoundRect(tile, cornerRadius = CornerRadius(cell * 2.5f))
        val module = Size(cell * 0.86f, cell * 0.86f)
        val inset = cell * 0.07f
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (!matrix[x, y]) continue
                val finder = isFinder(x, y, matrix.width)
                drawRoundRect(
                    color = color,
                    topLeft = Offset((x + quiet) * cell + if (finder) 0f else inset, (y + quiet) * cell + if (finder) 0f else inset),
                    size = if (finder) Size(cell, cell) else module,
                    cornerRadius = if (finder) CornerRadius.Zero else CornerRadius(cell * 0.3f),
                )
            }
        }
    }
}

private fun isFinder(
    x: Int,
    y: Int,
    n: Int,
): Boolean {
    val f = 7
    return (x < f && y < f) || (x >= n - f && y < f) || (x < f && y >= n - f)
}

private fun encodeQr(text: String): BitMatrix = QRCodeWriter().encode(
    text,
    BarcodeFormat.QR_CODE,
    0,
    0,
    mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 0),
)
