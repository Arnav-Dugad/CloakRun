package com.arnavgames.cloakrun.core.game.lobby

/**
 * Invite links are verified Android App Links on the GitHub Pages domain. With CloakRun installed
 * they open the app straight into the lobby; without it, the page shows the code and a download
 * button. The same URL is what the QR code encodes.
 */
object InviteLink {
    const val HOST = "arnav-dugad.github.io"
    const val PATH = "/join/"
    private const val PARAM = "code"

    fun forCode(code: String): String {
        require(JoinCode.isValid(code)) { "not a join code" }
        return "https://$HOST$PATH?$PARAM=$code"
    }

    /**
     * Extracts a join code from an invite link, or from a bare code (what a person might paste or
     * what an older QR might hold). Returns null for anything else, including links to other hosts.
     */
    fun parse(text: String): String? {
        val trimmed = text.trim()
        JoinCode.normalize(trimmed)?.let { return it }
        val match = LINK.matchEntire(trimmed) ?: return null
        val query = match.groupValues[1]
        val value = query.split('&')
            .map { it.split('=', limit = 2) }
            .firstOrNull { it.size == 2 && it[0] == PARAM }
            ?.get(1)
        return value?.let(JoinCode::normalize)
    }

    private val LINK = Regex("""https://${Regex.escape(HOST)}${Regex.escape(PATH)}?\?([^#]*)(#.*)?""", RegexOption.IGNORE_CASE)
}
