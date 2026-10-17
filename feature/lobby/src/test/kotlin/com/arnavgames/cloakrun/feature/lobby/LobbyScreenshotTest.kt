package com.arnavgames.cloakrun.feature.lobby

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import com.arnavgames.cloakrun.core.data.LobbyUpdate
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.designsystem.theme.ThemePreference
import com.arnavgames.cloakrun.core.game.area.PlayArea
import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import com.arnavgames.cloakrun.core.game.lobby.Lobby
import com.arnavgames.cloakrun.core.game.lobby.LobbyPlayer
import com.arnavgames.cloakrun.core.game.lobby.MatchConfig
import com.arnavgames.cloakrun.core.game.lobby.MatchPhase
import com.arnavgames.cloakrun.core.game.lobby.Role
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class LobbyScreenshotTest {
    private val players = listOf(
        LobbyPlayer("h", "Silent Lynx 37", "hostseed", Role.Hider, ready = false, joinedAtMillis = 1),
        LobbyPlayer("a", "Midnight Viper 72", "guestseed", Role.Seeker, ready = true, joinedAtMillis = 2),
        LobbyPlayer("b", "Neon Otter 15", "thirdseed", Role.Hider, ready = false, joinedAtMillis = 3),
    )

    private fun lobby(phase: MatchPhase = MatchPhase.Lobby) = Lobby(
        matchId = "m",
        code = "K7Q2XM",
        hostUid = "h",
        phase = phase,
        config = MatchConfig(seekers = 1),
        area = PlayArea.Circle(GeoPoint(12.9763, 77.5929), 400.0),
        players = players,
    )

    private fun shot(name: String, content: @Composable () -> Unit) = captureRoboImage(
        filePath = "src/test/screenshots/$name.png",
        roborazziOptions = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f)),
    ) {
        CloakTheme(preference = ThemePreference.Night, reduceMotion = true) { content() }
    }

    @Test
    fun lobby_host() = shot("lobby_host") {
        LobbyScreen(
            state = LobbyUiState(update = LobbyUpdate.Live(lobby(), online = setOf("h", "a")), myUid = "h"),
            notice = null,
            actions = LobbyActions(),
        )
    }

    @Test
    fun lobby_guest() = shot("lobby_guest") {
        LobbyScreen(
            state = LobbyUiState(update = LobbyUpdate.Live(lobby(), online = setOf("h", "a", "b")), myUid = "a"),
            notice = null,
            actions = LobbyActions(),
        )
    }

    @Test
    fun briefing_seeker() = shot("briefing_seeker") {
        LobbyScreen(
            state = LobbyUiState(update = LobbyUpdate.Live(lobby(MatchPhase.Briefing), online = setOf("h", "a")), myUid = "a"),
            notice = null,
            actions = LobbyActions(),
        )
    }

    @Test
    fun lobby_kicked() = shot("lobby_kicked") {
        LobbyScreen(state = LobbyUiState(update = LobbyUpdate.Kicked, myUid = "a"), notice = null, actions = LobbyActions())
    }

    @Test
    fun join() = shot("join") {
        JoinScreen(
            state = JoinUiState(complete = false),
            code = TextFieldState("K7Q"),
            onBack = {},
            onJoin = {},
            onScan = {},
        )
    }
}
