package com.arnavgames.cloakrun.feature.lobby

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.InputTransformation.Companion as Transformations
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.then
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arnavgames.cloakrun.core.designsystem.component.CloakButton
import com.arnavgames.cloakrun.core.designsystem.component.CloakButtonStyle
import com.arnavgames.cloakrun.core.designsystem.component.NightBackdrop
import com.arnavgames.cloakrun.core.designsystem.glass.GlassScene
import com.arnavgames.cloakrun.core.designsystem.haptics.HapticCue
import com.arnavgames.cloakrun.core.designsystem.shape.SquircleShape
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.game.lobby.JoinCode
import com.arnavgames.cloakrun.core.icons.CloakIcons
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun JoinRoute(
    initialInvite: String?,
    onBack: () -> Unit,
    onJoin: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JoinViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val joined by rememberUpdatedState(onJoin)
    LaunchedEffect(viewModel) { viewModel.lobbyJoined.collect { joined(it) } }
    LaunchedEffect(initialInvite) { initialInvite?.let(viewModel::joinFromInvite) }
    JoinScreen(
        state = state,
        code = viewModel.code,
        onBack = onBack,
        onJoin = viewModel::join,
        onScan = {
            scope.launch {
                when (val text = scanQr(context)) {
                    ScanResult.Unavailable -> viewModel.scannerUnavailable()
                    ScanResult.Cancelled -> Unit
                    is ScanResult.Text -> viewModel.joinFromInvite(text.value)
                }
            }
        },
        modifier = modifier,
    )
}

private sealed interface ScanResult {
    data class Text(val value: String) : ScanResult

    data object Cancelled : ScanResult

    data object Unavailable : ScanResult
}

/** Google's code scanner runs in Play services, so CloakRun never needs the camera permission. */
private suspend fun scanQr(context: Context): ScanResult {
    val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).enableAutoZoom().build()
    return try {
        val barcode = GmsBarcodeScanning.getClient(context, options).startScan().await()
        barcode.rawValue?.let { ScanResult.Text(it) } ?: ScanResult.Cancelled
    } catch (e: com.google.mlkit.common.MlKitException) {
        if (e.errorCode == com.google.mlkit.common.MlKitException.CODE_SCANNER_CANCELLED) ScanResult.Cancelled else ScanResult.Unavailable
    } catch (_: kotlinx.coroutines.CancellationException) {
        ScanResult.Cancelled
    }
}

@Composable
fun JoinScreen(
    state: JoinUiState,
    code: TextFieldState,
    onBack: () -> Unit,
    onJoin: () -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = CloakTheme.spacing
    val colors = CloakTheme.colors
    GlassScene(backdrop = { NightBackdrop(focus = Offset(0.2f, 0.1f)) }, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .padding(horizontal = spacing.gutter, vertical = spacing.m)
                .align(Alignment.TopCenter)
                .widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.l),
        ) {
            ScreenTitle(R.string.join_title, onBack)
            Text(stringResource(R.string.join_body), style = CloakTheme.typography.body, color = colors.textSecondary)
            CodeInput(code, onDone = onJoin, error = state.message != null)
            state.message?.let { Text(stringResource(it), style = CloakTheme.typography.bodyStrong, color = colors.danger) }
            Spacer(Modifier.weight(1f))
            CloakButton(
                stringResource(R.string.join_scan),
                onClick = onScan,
                style = CloakButtonStyle.Glass,
                icon = CloakIcons.Qr,
                modifier = Modifier.fillMaxWidth(),
            )
            CloakButton(
                stringResource(R.string.join_action),
                onClick = onJoin,
                icon = CloakIcons.Lobby,
                large = true,
                enabled = state.complete && !state.joining,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(spacing.s))
        }
    }
}

/**
 * Six glass tiles over one hidden text field. Typing fills the tiles left to right; each new
 * character drops in with a small spring, and impossible characters are never accepted.
 */
@Composable
private fun CodeInput(
    state: TextFieldState,
    onDone: () -> Unit,
    error: Boolean,
) {
    val colors = CloakTheme.colors
    val haptics = CloakTheme.haptics
    val focus = remember { FocusRequester() }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(Unit) { focus.requestFocus() }
    val label = stringResource(R.string.join_code_cd, state.text.length)
    val filter = remember { InputTransformation { replace(0, length, JoinCode.filterTyping(asCharSequence().toString())) } }
    BasicTextField(
        state = state,
        inputTransformation = filter.then(Transformations.maxLength(JoinCode.LENGTH)),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            // Password type turns off suggestions and composing, which would fight the live filtering.
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Go,
            autoCorrectEnabled = false,
        ),
        onKeyboardAction = { onDone() },
        lineLimits = TextFieldLineLimits.SingleLine,
        interactionSource = interaction,
        cursorBrush = SolidColor(colors.primary.copy(alpha = 0f)),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus)
            .semantics { contentDescription = label },
        decorator = { innerField ->
            Box {
                // The real field stays invisible; the tiles show its content.
                Box(Modifier.size(1.dp).graphicsLayer { alpha = 0f }) { innerField() }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    val text = state.text
                    repeat(JoinCode.LENGTH) { i ->
                        val char = text.getOrNull(i)
                        val active = focused && i == text.length.coerceAtMost(JoinCode.LENGTH - 1)
                        CodeTile(char, active, error)
                    }
                }
            }
        },
    )
    LaunchedEffect(state.text.length) { if (state.text.isNotEmpty()) haptics.play(HapticCue.Tick) }
}

@Composable
private fun CodeTile(
    char: Char?,
    active: Boolean,
    error: Boolean,
) {
    val colors = CloakTheme.colors
    val motion = CloakTheme.motion
    val drop = remember { Animatable(0f) }
    LaunchedEffect(char) {
        if (char != null && !motion.reduced) {
            drop.snapTo(-14f)
            drop.animateTo(0f, motion.bouncy())
        }
    }
    val shape = SquircleShape(CloakTheme.radii.m)
    val edge = when {
        error -> colors.danger
        active -> colors.primary
        else -> colors.outline
    }
    Box(
        Modifier
            .size(width = 48.dp, height = 64.dp)
            .background(colors.glassTintStrong, shape)
            .border(1.5.dp, edge, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            char?.toString().orEmpty(),
            style = CloakTheme.typography.countdown.copy(fontSize = CloakTheme.typography.headline.fontSize),
            color = colors.textPrimary,
            modifier = Modifier.graphicsLayer { translationY = drop.value * density },
        )
    }
}
