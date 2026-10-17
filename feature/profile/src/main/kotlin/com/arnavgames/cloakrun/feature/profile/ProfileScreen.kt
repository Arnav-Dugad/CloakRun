package com.arnavgames.cloakrun.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arnavgames.cloakrun.core.designsystem.component.CloakAvatar
import com.arnavgames.cloakrun.core.designsystem.component.CloakButton
import com.arnavgames.cloakrun.core.designsystem.component.CloakButtonStyle
import com.arnavgames.cloakrun.core.designsystem.component.CloakTextField
import com.arnavgames.cloakrun.core.designsystem.component.CloakToggle
import com.arnavgames.cloakrun.core.designsystem.component.GlassCard
import com.arnavgames.cloakrun.core.designsystem.component.GlassModal
import com.arnavgames.cloakrun.core.designsystem.component.HoldToConfirmButton
import com.arnavgames.cloakrun.core.designsystem.component.NightBackdrop
import com.arnavgames.cloakrun.core.designsystem.glass.GlassScene
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.icons.CloakIcon
import com.arnavgames.cloakrun.core.icons.CloakIcons
import kotlinx.coroutines.delay

@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    onAccountDelete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val deleted by rememberUpdatedState(onAccountDelete)
    LaunchedEffect(viewModel) { viewModel.accountDeleted.collect { deleted() } }
    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(MESSAGE_MS)
            viewModel.dismissMessage()
        }
    }
    ProfileScreen(
        state = state,
        name = viewModel.name,
        onBack = onBack,
        onShuffle = viewModel::shuffle,
        onSave = viewModel::save,
        onLink = { viewModel.linkGoogle(context) },
        onDiagnostics = { viewModel.setDiagnostics(it) },
        onDelete = { viewModel.deleteAccount(context) },
        modifier = modifier,
    )
    GlassModal(
        visible = state.pendingSwitch != null,
        onDismiss = viewModel::cancelSwitch,
        title = stringResource(R.string.profile_switch_title),
    ) {
        Text(stringResource(R.string.profile_switch_body), style = CloakTheme.typography.body, color = CloakTheme.colors.textSecondary)
        CloakButton(stringResource(R.string.profile_switch_confirm), onClick = viewModel::confirmSwitch, modifier = Modifier.fillMaxWidth())
    }
}

private const val MESSAGE_MS = 3_000L

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    name: TextFieldState,
    onBack: () -> Unit,
    onShuffle: () -> Unit,
    onSave: () -> Unit,
    onLink: () -> Unit,
    onDiagnostics: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = CloakTheme.spacing
    val colors = CloakTheme.colors
    GlassScene(backdrop = { NightBackdrop(focus = Offset(0.5f, 0.15f)) }, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.gutter, vertical = spacing.m)
                .align(Alignment.TopCenter)
                .widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.l),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                CloakButton(stringResource(R.string.profile_back), onClick = onBack, style = CloakButtonStyle.Ghost, icon = CloakIcons.Back)
                Text(
                    stringResource(R.string.profile_title),
                    style = CloakTheme.typography.headline,
                    color = colors.textPrimary,
                    modifier = Modifier.semantics { heading() },
                )
            }
            state.message?.let { Text(stringResource(it), style = CloakTheme.typography.bodyStrong, color = colors.accent) }
            IdentityCard(state, name, onShuffle, onSave)
            AccountCard(state, onLink)
            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_privacy), style = CloakTheme.typography.overline, color = colors.textSecondary)
                val label = stringResource(R.string.profile_diagnostics)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
                    Column(Modifier.weight(1f)) {
                        Text(label, style = CloakTheme.typography.bodyStrong, color = colors.textPrimary)
                        Text(
                            stringResource(R.string.profile_diagnostics_body),
                            style = CloakTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    CloakToggle(checked = state.diagnostics, onCheckedChange = onDiagnostics, contentDescription = label)
                }
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_delete_title), style = CloakTheme.typography.titleSmall, color = colors.danger)
                Text(stringResource(R.string.profile_delete_body), style = CloakTheme.typography.bodySmall, color = colors.textSecondary)
                HoldToConfirmButton(
                    text = stringResource(R.string.profile_delete_confirm),
                    icon = CloakIcons.Warning,
                    onConfirm = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun IdentityCard(
    state: ProfileUiState,
    name: TextFieldState,
    onShuffle: () -> Unit,
    onSave: () -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (state.avatarSeed.isNotEmpty()) {
                CloakAvatar(seed = state.avatarSeed, contentDescription = stringResource(R.string.profile_avatar_cd), size = 112.dp)
            }
        }
        CloakButton(
            stringResource(R.string.profile_shuffle),
            onClick = onShuffle,
            style = CloakButtonStyle.Glass,
            icon = CloakIcons.Replay,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        CloakTextField(
            state = name,
            label = stringResource(R.string.profile_name),
            supportingText = stringResource(if (state.nameInvalid) R.string.profile_name_invalid else R.string.profile_name_hint),
            isError = state.nameInvalid,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        CloakButton(stringResource(R.string.profile_save), onClick = onSave, icon = CloakIcons.Check, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AccountCard(state: ProfileUiState, onLink: () -> Unit) {
    val colors = CloakTheme.colors
    val user = state.user
    GlassCard(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.profile_account), style = CloakTheme.typography.overline, color = colors.textSecondary)
        when {
            !state.online -> Text(
                stringResource(R.string.profile_offline_build),
                style = CloakTheme.typography.body,
                color = colors.textSecondary,
            )

            user != null && !user.isAnonymous -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CloakTheme.spacing.m),
            ) {
                CloakIcon(CloakIcons.Shield, contentDescription = null, tint = colors.success)
                Text(
                    user.email?.let { stringResource(R.string.profile_google_email, it) } ?: stringResource(R.string.profile_google),
                    style = CloakTheme.typography.bodyStrong,
                    color = colors.textPrimary,
                )
            }

            else -> {
                Text(stringResource(R.string.profile_guest), style = CloakTheme.typography.bodyStrong, color = colors.textPrimary)
                Text(stringResource(R.string.profile_guest_body), style = CloakTheme.typography.bodySmall, color = colors.textSecondary)
                CloakButton(
                    stringResource(R.string.profile_link),
                    onClick = onLink,
                    style = CloakButtonStyle.Glass,
                    icon = CloakIcons.Profile,
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
