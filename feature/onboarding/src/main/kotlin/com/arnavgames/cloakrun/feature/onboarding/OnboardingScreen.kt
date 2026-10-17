package com.arnavgames.cloakrun.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arnavgames.cloakrun.core.data.AgeBand
import com.arnavgames.cloakrun.core.designsystem.component.CloakAvatar
import com.arnavgames.cloakrun.core.designsystem.component.CloakButton
import com.arnavgames.cloakrun.core.designsystem.component.CloakButtonStyle
import com.arnavgames.cloakrun.core.designsystem.component.CloakTextField
import com.arnavgames.cloakrun.core.designsystem.component.CloakToggle
import com.arnavgames.cloakrun.core.designsystem.component.GlassCard
import com.arnavgames.cloakrun.core.designsystem.component.NightBackdrop
import com.arnavgames.cloakrun.core.designsystem.component.StateMessage
import com.arnavgames.cloakrun.core.designsystem.glass.GlassScene
import com.arnavgames.cloakrun.core.designsystem.shape.SquircleShape
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.game.profile.DisplayName
import com.arnavgames.cloakrun.core.icons.CloakIcon
import com.arnavgames.cloakrun.core.icons.CloakIcons

@Composable
fun OnboardingRoute(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val finish by rememberUpdatedState(onFinish)
    LaunchedEffect(viewModel) { viewModel.finished.collect { finish() } }
    BackHandler(enabled = state.step in setOf(OnboardingStep.Consent, OnboardingStep.Profile)) { viewModel.back() }
    OnboardingScreen(
        state = state,
        name = viewModel.name,
        actions = OnboardingActions(
            chooseAge = viewModel::chooseAge,
            acceptRules = { viewModel.acceptRules() },
            setDiagnostics = viewModel::setDiagnostics,
            acceptConsent = { viewModel.acceptConsent() },
            shuffleAvatar = viewModel::shuffleAvatar,
            suggestName = viewModel::suggestName,
            finish = viewModel::finish,
        ),
        modifier = modifier,
    )
}

class OnboardingActions(
    val chooseAge: (AgeBand) -> Unit = {},
    val acceptRules: () -> Unit = {},
    val setDiagnostics: (Boolean) -> Unit = {},
    val acceptConsent: () -> Unit = {},
    val shuffleAvatar: () -> Unit = {},
    val suggestName: () -> Unit = {},
    val finish: () -> Unit = {},
)

private val STEPS = listOf(OnboardingStep.Age, OnboardingStep.Rules, OnboardingStep.Consent, OnboardingStep.Profile)

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    name: TextFieldState,
    actions: OnboardingActions,
    modifier: Modifier = Modifier,
) {
    val spacing = CloakTheme.spacing
    val reduced = CloakTheme.motion.reduced
    GlassScene(backdrop = { NightBackdrop(focus = Offset(0.5f, 0.12f)) }, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .padding(horizontal = spacing.gutter, vertical = spacing.l)
                .align(Alignment.TopCenter)
                .widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.l),
        ) {
            StepDots(state.step)
            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    if (reduced) {
                        fadeIn() togetherWith fadeOut()
                    } else {
                        val dir = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                        (slideInHorizontally { it / 4 * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 * dir } + fadeOut())
                    }
                },
                label = "onboardingStep",
                modifier = Modifier.weight(1f),
            ) { step ->
                // A minimum height of the viewport lets the weighted spacers push actions to the bottom
                // while long text still scrolls on small screens.
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val viewport = maxHeight
                    Column(
                        Modifier.verticalScroll(rememberScrollState()).heightIn(min = viewport),
                        verticalArrangement = Arrangement.spacedBy(spacing.l),
                    ) {
                        when (step) {
                            OnboardingStep.Loading -> Unit

                            OnboardingStep.Age -> AgeStep(actions.chooseAge)

                            OnboardingStep.Blocked -> StateMessage(
                                icon = CloakIcons.Lock,
                                title = stringResource(R.string.onboarding_blocked_title),
                                body = stringResource(R.string.onboarding_blocked_body),
                            )

                            OnboardingStep.Rules -> RulesStep(state.teen, actions.acceptRules)

                            OnboardingStep.Consent -> ConsentStep(state.diagnostics, actions.setDiagnostics, actions.acceptConsent)

                            OnboardingStep.Profile -> ProfileStep(state, name, actions)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepDots(step: OnboardingStep) {
    val index = STEPS.indexOf(step)
    if (index < 0) return
    val colors = CloakTheme.colors
    val label = stringResource(R.string.onboarding_progress, index + 1, STEPS.size)
    Row(
        Modifier.fillMaxWidth().semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        STEPS.forEachIndexed { i, _ ->
            val width by animateDpAsState(if (i == index) 28.dp else 8.dp, CloakTheme.motion.bouncy(), label = "dot$i")
            Box(
                Modifier
                    .size(width = width, height = 8.dp)
                    .background(if (i <= index) colors.primary else colors.outline, SquircleShape.Pill),
            )
        }
    }
}

@Composable
private fun ColumnScope.Header(title: Int, body: Int?) {
    Spacer(Modifier.height(CloakTheme.spacing.l))
    Text(
        stringResource(title),
        style = CloakTheme.typography.headline,
        color = CloakTheme.colors.textPrimary,
        modifier = Modifier.semantics { heading() },
    )
    if (body != null) Text(stringResource(body), style = CloakTheme.typography.body, color = CloakTheme.colors.textSecondary)
}

@Composable
private fun ColumnScope.AgeStep(onChoose: (AgeBand) -> Unit) {
    Header(R.string.onboarding_age_title, R.string.onboarding_age_body)
    Spacer(Modifier.weight(1f))
    // Neutral age screen: every option looks the same, none is pre-selected.
    listOf(
        AgeBand.Under13 to R.string.onboarding_age_under13,
        AgeBand.Teen to R.string.onboarding_age_teen,
        AgeBand.Adult to R.string.onboarding_age_adult,
    ).forEach { (band, label) ->
        CloakButton(
            stringResource(label),
            onClick = { onChoose(band) },
            style = CloakButtonStyle.Glass,
            large = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ColumnScope.RulesStep(teen: Boolean, onAccept: () -> Unit) {
    Header(R.string.onboarding_rules_title, R.string.onboarding_rules_body)
    GlassCard(Modifier.fillMaxWidth()) {
        Bullet(CloakIcons.Lock, R.string.onboarding_rule_trespass)
        Bullet(CloakIcons.Warning, R.string.onboarding_rule_roads)
        Bullet(CloakIcons.Team, R.string.onboarding_rule_groups)
        Bullet(CloakIcons.Sun, R.string.onboarding_rule_daytime)
        Bullet(CloakIcons.Sos, R.string.onboarding_rule_stop)
    }
    if (teen) {
        Text(stringResource(R.string.onboarding_teen_note), style = CloakTheme.typography.bodyStrong, color = CloakTheme.colors.warning)
    }
    Spacer(Modifier.weight(1f))
    CloakButton(
        stringResource(R.string.onboarding_rules_accept),
        onClick = onAccept,
        icon = CloakIcons.Shield,
        large = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ColumnScope.ConsentStep(
    diagnostics: Boolean,
    onDiagnostics: (Boolean) -> Unit,
    onAccept: () -> Unit,
) {
    val colors = CloakTheme.colors
    Header(R.string.onboarding_consent_title, null)
    GlassCard(Modifier.fillMaxWidth()) {
        Bullet(CloakIcons.EyeOff, R.string.onboarding_consent_exact)
        Bullet(CloakIcons.Zone, R.string.onboarding_consent_zone)
        Bullet(CloakIcons.Location, R.string.onboarding_consent_when)
        Bullet(CloakIcons.Profile, R.string.onboarding_consent_name)
        Bullet(CloakIcons.Shield, R.string.onboarding_consent_cleanup)
    }
    GlassCard(Modifier.fillMaxWidth()) {
        val label = stringResource(R.string.onboarding_consent_diagnostics)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CloakTheme.spacing.m)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = CloakTheme.typography.bodyStrong, color = colors.textPrimary)
                Text(
                    stringResource(R.string.onboarding_consent_diagnostics_body),
                    style = CloakTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            CloakToggle(checked = diagnostics, onCheckedChange = onDiagnostics, contentDescription = label)
        }
    }
    Spacer(Modifier.weight(1f))
    CloakButton(
        stringResource(R.string.onboarding_consent_accept),
        onClick = onAccept,
        icon = CloakIcons.Check,
        large = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ColumnScope.ProfileStep(
    state: OnboardingUiState,
    name: TextFieldState,
    actions: OnboardingActions,
) {
    val spacing = CloakTheme.spacing
    Header(R.string.onboarding_profile_title, R.string.onboarding_profile_body)
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.m),
    ) {
        CloakAvatar(seed = state.avatarSeed, contentDescription = stringResource(R.string.onboarding_profile_avatar_cd), size = 128.dp)
        CloakButton(
            stringResource(R.string.onboarding_profile_shuffle),
            onClick = actions.shuffleAvatar,
            style = CloakButtonStyle.Glass,
            icon = CloakIcons.Replay,
        )
    }
    CloakTextField(
        state = name,
        label = stringResource(R.string.onboarding_profile_name),
        supportingText = stringResource(nameMessage(state.nameProblem)),
        isError = state.nameProblem != null,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
    CloakButton(
        stringResource(R.string.onboarding_profile_suggest),
        onClick = actions.suggestName,
        style = CloakButtonStyle.Ghost,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.weight(1f))
    CloakButton(
        stringResource(R.string.onboarding_profile_finish),
        onClick = actions.finish,
        icon = CloakIcons.Play,
        large = true,
        enabled = !state.saving,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun nameMessage(problem: DisplayName.Problem?): Int = when (problem) {
    null -> R.string.onboarding_name_hint
    DisplayName.Problem.TooShort -> R.string.onboarding_name_too_short
    DisplayName.Problem.TooLong -> R.string.onboarding_name_too_long
    DisplayName.Problem.BadCharacters -> R.string.onboarding_name_bad
}

@Composable
private fun Bullet(icon: ImageVector, text: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(CloakTheme.spacing.m), verticalAlignment = Alignment.Top) {
        CloakIcon(icon, contentDescription = null, tint = CloakTheme.colors.accent, size = 22.dp)
        Text(
            stringResource(text),
            style = CloakTheme.typography.body,
            color = CloakTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}
