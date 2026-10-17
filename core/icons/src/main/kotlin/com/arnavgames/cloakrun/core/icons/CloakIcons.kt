package com.arnavgames.cloakrun.core.icons

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The CloakRun icon set. Drawn in-house on a 24dp grid with a 2dp stroke and round caps and
 * joins; no Material Icons and no stock packs. Directional icons mirror automatically in RTL.
 *
 * Every icon is listed in [all], which the Design Gallery and the icon tests iterate.
 */
object CloakIcons {
    // Game: core
    val Radar: ImageVector by lazy {
        icon("Radar") {
            stroke("M21 12a9 9 0 1 1-9-9")
            stroke("M17 12a5 5 0 1 1-5-5")
            stroke("M12 12l6.4-6.4")
            dot(12f, 12f, 1.6f)
        }
    }

    val Cloak: ImageVector by lazy {
        icon("Cloak") {
            stroke("M12 21.5C9 18.6 5 15.4 5 10.2a7 7 0 0 1 14 0c0 5.2-4 8.4-7 11.3Z")
            stroke("M12 15.2c-1.7-1.3-3.2-2.6-3.2-4.6a3.2 3.2 0 0 1 6.4 0c0 2-1.5 3.3-3.2 4.6Z")
            dot(12f, 10.8f, 1.1f)
        }
    }

    val Decoy: ImageVector by lazy {
        icon("Decoy") {
            circle(9f, 14.5f, 5.5f)
            dashedCircle(15.5f, 8.5f, 4.5f, count = 6, duty = 0.55f, rotationDeg = -20f)
            dot(9f, 14.5f, 1.4f)
        }
    }

    val Sonar: ImageVector by lazy {
        icon("Sonar") {
            dot(12f, 12f, 1.8f)
            stroke("M8.5 8.5a5 5 0 0 0 0 7")
            stroke("M15.5 8.5a5 5 0 0 1 0 7")
            stroke("M5.6 5.6a9 9 0 0 0 0 12.8")
            stroke("M18.4 5.6a9 9 0 0 1 0 12.8")
        }
    }

    val Echo: ImageVector by lazy {
        icon("Echo") {
            stroke("M2.5 12.5h4l2.2-5.5 3.4 11 2.4-7 1.5 1.5h5.5")
        }
    }

    val Freeze: ImageVector by lazy {
        icon("Freeze") {
            stroke("M12 2.5v19")
            stroke("M3.8 7.25l16.4 9.5")
            stroke("M3.8 16.75l16.4-9.5")
            stroke("M9.5 4.5L12 7l2.5-2.5")
            stroke("M9.5 19.5L12 17l2.5 2.5")
        }
    }

    val Trap: ImageVector by lazy {
        icon("Trap") {
            stroke("M3 20h18")
            stroke("M4.5 16a7.5 7.5 0 0 1 15 0")
            stroke("M7.5 16l1.1-2.6 1.1 2.6 1.15-2.6 1.15 2.6 1.15-2.6 1.15 2.6 1.1-2.6 1.1 2.6")
            stroke("M12 16v4")
        }
    }

    val Blackout: ImageVector by lazy {
        icon("Blackout") {
            stroke("M3 8h12")
            stroke("M18.5 8H21")
            stroke("M6 12h15")
            stroke("M3 16h9")
            stroke("M15.5 16H18")
        }
    }

    val PhantomSwap: ImageVector by lazy {
        icon("PhantomSwap") {
            stroke("M7 3.5L3 7.5l4 4")
            stroke("M3 7.5h13.5")
            stroke("M17 12.5l4 4-4 4")
            stroke("M21 16.5H7.5")
        }
    }

    val Cache: ImageVector by lazy {
        icon("Cache") {
            stroke("M12 3l7 6.5-7 11.5-7-11.5Z")
            stroke("M5 9.5h14")
            stroke("M9.2 9.5L12 3l2.8 6.5L12 21Z")
        }
    }

    val Zone: ImageVector by lazy {
        icon("Zone") {
            dashedCircle(12f, 12f, 9f, count = 10, duty = 0.55f, rotationDeg = -90f)
            circle(12f, 12f, 3f)
        }
    }

    val Tag: ImageVector by lazy {
        icon("Tag") {
            stroke("M3.5 8V5.5a2 2 0 0 1 2-2H8")
            stroke("M16 3.5h2.5a2 2 0 0 1 2 2V8")
            stroke("M20.5 16v2.5a2 2 0 0 1-2 2H16")
            stroke("M8 20.5H5.5a2 2 0 0 1-2-2V16")
            circle(12f, 12f, 3.2f)
        }
    }

    val Hider: ImageVector by lazy {
        icon("Hider") {
            stroke("M5.5 12.5a6.5 6.5 0 0 1 13 0")
            stroke("M5.5 12.5c0 1.5.4 2.6 1 3.5")
            stroke("M18.5 12.5c0 1.5-.4 2.6-1 3.5")
            circle(12f, 12f, 2.6f)
            stroke("M5 21c1.6-2.6 4.1-4 7-4s5.4 1.4 7 4")
        }
    }

    val Seeker: ImageVector by lazy {
        icon("Seeker") {
            stroke("M12 20.5L5.1 6.4A9 9 0 0 1 18.9 6.4Z")
            stroke("M8.9 14.2a5 5 0 0 1 6.2 0")
            dot(12f, 20.5f, 1.4f)
        }
    }

    val Ghost: ImageVector by lazy {
        icon("Ghost") {
            stroke("M5 20.5V11a7 7 0 0 1 14 0v9.5l-2.33-1.8-2.34 1.8L12 18.7l-2.33 1.8-2.34-1.8Z")
            dot(9.5f, 11f, 1.2f)
            dot(14.5f, 11f, 1.2f)
        }
    }

    val Trophy: ImageVector by lazy {
        icon("Trophy") {
            stroke("M7.5 3.5h9V9a4.5 4.5 0 0 1-9 0Z")
            stroke("M7.5 5.5H4.5v1.5a3.5 3.5 0 0 0 3.3 3.5")
            stroke("M16.5 5.5h3v1.5a3.5 3.5 0 0 1-3.3 3.5")
            stroke("M12 13.5v4")
            stroke("M8 20.5h8")
            stroke("M9.5 17.5h5v3h-5Z")
        }
    }

    val Lobby: ImageVector by lazy {
        icon("Lobby") {
            circle(12f, 12f, 3.2f)
            stroke("M3.6 15.1a9 4 -25 1 0 16.8-6.2a9 4 -25 1 0-16.8 6.2Z")
            dot(19.6f, 6.6f, 1.5f)
        }
    }

    val Invite: ImageVector by lazy {
        icon("Invite") {
            circle(10f, 8f, 4f)
            stroke("M3 20.5v-.5a6 6 0 0 1 6-6h2")
            stroke("M18 13v7")
            stroke("M14.5 16.5h7")
        }
    }

    val Qr: ImageVector by lazy {
        icon("Qr") {
            roundRect(3.5f, 3.5f, 6.5f, 6.5f, 1.5f)
            roundRect(14f, 3.5f, 6.5f, 6.5f, 1.5f)
            roundRect(3.5f, 14f, 6.5f, 6.5f, 1.5f)
            stroke("M14 14h2.5")
            stroke("M20.5 14v2.5")
            stroke("M14 17.5v3h3")
            stroke("M19.5 19.5h1")
        }
    }

    val Sos: ImageVector by lazy {
        icon("Sos") {
            circle(12f, 12f, 9f)
            circle(12f, 12f, 3.8f)
            stroke("M5.6 5.6l3.7 3.7")
            stroke("M18.4 5.6l-3.7 3.7")
            stroke("M18.4 18.4l-3.7-3.7")
            stroke("M5.6 18.4l3.7-3.7")
        }
    }

    val Battery: ImageVector by lazy {
        icon("Battery") {
            roundRect(2.5f, 7f, 16.5f, 10f, 2.5f)
            stroke("M21.5 10.5v3")
            stroke("M6.5 10.5v3")
            stroke("M10 10.5v3")
        }
    }

    val Signal: ImageVector by lazy {
        icon("Signal") {
            stroke("M4.5 20v-3")
            stroke("M9.5 20v-7")
            stroke("M14.5 20V9")
            stroke("M19.5 20V4.5")
        }
    }

    val Compass: ImageVector by lazy {
        icon("Compass") {
            circle(12f, 12f, 9f)
            stroke("M15.8 8.2l-2 5.6-5.6 2 2-5.6Z")
        }
    }

    val Settings: ImageVector by lazy {
        icon("Settings") {
            stroke("M3.5 7h9")
            stroke("M19.5 7h1")
            circle(16f, 7f, 2.5f)
            stroke("M3.5 17h2")
            stroke("M12.5 17h8")
            circle(9f, 17f, 2.5f)
        }
    }

    val Profile: ImageVector by lazy {
        icon("Profile") {
            circle(12f, 8f, 4f)
            stroke("M4.5 20.5a7.5 7.5 0 0 1 15 0")
        }
    }

    val Replay: ImageVector by lazy {
        icon("Replay", autoMirror = true) {
            stroke("M4 12a8 8 0 1 0 2.4-5.7")
            stroke("M4 3.5v4.5h4.5")
            stroke("M10.5 9.3v5.4l4.3-2.7Z")
        }
    }

    val Shield: ImageVector by lazy {
        icon("Shield") {
            stroke("M12 3l7 2.8v5.4c0 4.7-3 8.3-7 9.8-4-1.5-7-5.1-7-9.8V5.8Z")
            stroke("M9 12l2.1 2.1L15.2 10")
        }
    }

    val Eye: ImageVector by lazy {
        icon("Eye") {
            stroke("M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z")
            circle(12f, 12f, 3f)
        }
    }

    val EyeOff: ImageVector by lazy {
        icon("EyeOff") {
            stroke("M9.9 5.7A9.9 9.9 0 0 1 12 5.5c6 0 9.5 6.5 9.5 6.5a17 17 0 0 1-2.4 3.2")
            stroke("M6.5 7.6C4 9.4 2.5 12 2.5 12S6 18.5 12 18.5c1.7 0 3.2-.5 4.5-1.2")
            stroke("M9.9 9.9a3 3 0 0 0 4.2 4.2")
            stroke("M3.5 3.5l17 17")
        }
    }

    val Timer: ImageVector by lazy {
        icon("Timer") {
            circle(12f, 13.5f, 7.5f)
            stroke("M12 13.5V10")
            stroke("M9.5 2.5h5")
            stroke("M18.5 6.5l1.3-1.3")
        }
    }

    val Team: ImageVector by lazy {
        icon("Team") {
            circle(9f, 8f, 3.5f)
            stroke("M2.5 20a6.5 6.5 0 0 1 13 0")
            stroke("M15.5 4.8a3.2 3.2 0 0 1 0 6.4")
            stroke("M17.5 14.3a6.5 6.5 0 0 1 4 5.7")
        }
    }

    // Interface
    val Close: ImageVector by lazy {
        icon("Close") {
            stroke("M6 6l12 12")
            stroke("M18 6L6 18")
        }
    }

    val Back: ImageVector by lazy {
        icon("Back", autoMirror = true) {
            stroke("M15 5l-7 7 7 7")
        }
    }

    val ChevronRight: ImageVector by lazy {
        icon("ChevronRight", autoMirror = true) {
            stroke("M9 5l7 7-7 7")
        }
    }

    val Check: ImageVector by lazy {
        icon("Check") {
            stroke("M4.5 12.5l5 5L19.5 7")
        }
    }

    val Plus: ImageVector by lazy {
        icon("Plus") {
            stroke("M12 5v14")
            stroke("M5 12h14")
        }
    }

    val Copy: ImageVector by lazy {
        icon("Copy") {
            roundRect(8.5f, 8.5f, 12f, 12f, 2.5f)
            stroke("M15.5 8.5V6a2.5 2.5 0 0 0-2.5-2.5H6A2.5 2.5 0 0 0 3.5 6v7A2.5 2.5 0 0 0 6 15.5h2.5")
        }
    }

    val Share: ImageVector by lazy {
        icon("Share") {
            circle(18f, 5.5f, 2.5f)
            circle(6f, 12f, 2.5f)
            circle(18f, 18.5f, 2.5f)
            stroke("M8.2 10.8l7.6-4.1")
            stroke("M8.2 13.2l7.6 4.1")
        }
    }

    val Location: ImageVector by lazy {
        icon("Location") {
            stroke("M12 21.5s-7-6.2-7-11.7a7 7 0 0 1 14 0c0 5.5-7 11.7-7 11.7Z")
            circle(12f, 9.8f, 2.6f)
        }
    }

    val Map: ImageVector by lazy {
        icon("Map") {
            stroke("M3 6.5l6-3 6 3 6-3v14l-6 3-6-3-6 3Z")
            stroke("M9 3.5v14")
            stroke("M15 6.5v14")
        }
    }

    val Lock: ImageVector by lazy {
        icon("Lock") {
            roundRect(4.5f, 10.5f, 15f, 10.5f, 2.5f)
            stroke("M8 10.5V7a4 4 0 0 1 8 0v3.5")
            stroke("M12 14.5v2.5")
        }
    }

    val Warning: ImageVector by lazy {
        icon("Warning") {
            stroke("M10.3 4.2L2.7 17.6a2 2 0 0 0 1.7 3h15.2a2 2 0 0 0 1.7-3L13.7 4.2a2 2 0 0 0-3.4 0Z")
            stroke("M12 9.5v4")
            dot(12f, 17f, 1.2f)
        }
    }

    val Info: ImageVector by lazy {
        icon("Info") {
            circle(12f, 12f, 9f)
            stroke("M12 11v5.5")
            dot(12f, 7.8f, 1.2f)
        }
    }

    val Play: ImageVector by lazy {
        icon("Play", autoMirror = true) {
            stroke("M7 4.8v14.4a.8.8 0 0 0 1.2.7l11.5-7.2a.8.8 0 0 0 0-1.4L8.2 4.1a.8.8 0 0 0-1.2.7Z")
        }
    }

    val Pause: ImageVector by lazy {
        icon("Pause") {
            stroke("M8 5v14")
            stroke("M16 5v14")
        }
    }

    val Sound: ImageVector by lazy {
        icon("Sound") {
            stroke("M3.5 9.5h3.5l5-4v13l-5-4H3.5Z")
            stroke("M15.5 9a4 4 0 0 1 0 6")
            stroke("M18.5 6a8 8 0 0 1 0 12")
        }
    }

    val SoundOff: ImageVector by lazy {
        icon("SoundOff") {
            stroke("M3.5 9.5h3.5l5-4v13l-5-4H3.5Z")
            stroke("M16 9.5l5 5")
            stroke("M21 9.5l-5 5")
        }
    }

    val Haptic: ImageVector by lazy {
        icon("Haptic") {
            roundRect(8f, 3.5f, 8f, 17f, 2.5f)
            stroke("M4.5 9v6")
            stroke("M19.5 9v6")
            stroke("M2 10.5v3")
            stroke("M22 10.5v3")
        }
    }

    val Bluetooth: ImageVector by lazy {
        icon("Bluetooth") {
            stroke("M6.5 7.5l11 9-5.5 4.5V3l5.5 4.5-11 9")
        }
    }

    val Sun: ImageVector by lazy {
        icon("Sun") {
            circle(12f, 12f, 4f)
            stroke("M12 2.5v2")
            stroke("M12 19.5v2")
            stroke("M2.5 12h2")
            stroke("M19.5 12h2")
            stroke("M5.3 5.3l1.4 1.4")
            stroke("M17.3 17.3l1.4 1.4")
            stroke("M5.3 18.7l1.4-1.4")
            stroke("M17.3 6.7l1.4-1.4")
        }
    }

    val Moon: ImageVector by lazy {
        icon("Moon") {
            stroke("M20.5 14.2A8.5 8.5 0 1 1 9.8 3.5a6.8 6.8 0 0 0 10.7 10.7Z")
        }
    }

    val Star: ImageVector by lazy {
        icon("Star") {
            stroke("M12 3.3l2.7 5.5 6 .9-4.35 4.25 1 6L12 17.1l-5.35 2.85 1-6L3.3 9.7l6-.9Z")
        }
    }

    /** Every icon with its name, in display order. */
    val all: List<Pair<String, ImageVector>> by lazy {
        listOf(
            "radar" to Radar, "cloak" to Cloak, "decoy" to Decoy, "sonar" to Sonar, "echo" to Echo,
            "freeze" to Freeze, "trap" to Trap, "blackout" to Blackout, "swap" to PhantomSwap,
            "cache" to Cache, "zone" to Zone, "tag" to Tag, "hider" to Hider, "seeker" to Seeker,
            "ghost" to Ghost, "trophy" to Trophy, "lobby" to Lobby, "invite" to Invite, "qr" to Qr,
            "sos" to Sos, "battery" to Battery, "signal" to Signal, "compass" to Compass,
            "settings" to Settings, "profile" to Profile, "replay" to Replay, "shield" to Shield,
            "eye" to Eye, "eye off" to EyeOff, "timer" to Timer, "team" to Team,
            "close" to Close, "back" to Back, "chevron" to ChevronRight, "check" to Check, "plus" to Plus,
            "copy" to Copy, "share" to Share, "location" to Location, "map" to Map, "lock" to Lock,
            "warning" to Warning, "info" to Info, "play" to Play, "pause" to Pause, "sound" to Sound,
            "sound off" to SoundOff, "haptic" to Haptic, "bluetooth" to Bluetooth, "sun" to Sun,
            "moon" to Moon, "star" to Star,
        )
    }
}
