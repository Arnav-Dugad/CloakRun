package com.arnavgames.cloakrun.core.game.profile

import kotlin.random.Random

/** Player names: short, plain and safe to show to a lobby of friends. */
object DisplayName {
    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 20

    enum class Problem { TooShort, TooLong, BadCharacters }

    /** Letters (any script), digits, spaces, dots, dashes and underscores; no leading or trailing space. */
    fun problem(name: String): Problem? = when {
        name.trim().length < MIN_LENGTH -> Problem.TooShort
        name.length > MAX_LENGTH -> Problem.TooLong
        name != name.trim() || "  " in name -> Problem.BadCharacters
        !name.all { it.isLetterOrDigit() || it in " ._-" } -> Problem.BadCharacters
        else -> null
    }

    fun clean(name: String): String = name.trim().replace(Regex("\\s+"), " ").take(MAX_LENGTH)

    /** A friendly generated name, so nobody has to type anything to start playing. */
    fun generate(random: Random = Random.Default): String =
        "${ADJECTIVES.random(random)} ${ANIMALS.random(random)} ${random.nextInt(10, 100)}"

    private val ADJECTIVES = listOf(
        "Silent", "Swift", "Hidden", "Neon", "Shadow", "Velvet", "Quiet", "Lunar", "Misty", "Electric",
        "Frosted", "Clever", "Midnight", "Phantom", "Nimble", "Cosmic",
    )
    private val ANIMALS = listOf(
        "Fox", "Lynx", "Owl", "Otter", "Heron", "Panther", "Hare", "Raven", "Gecko", "Moth", "Wolf", "Marten",
        "Falcon", "Viper", "Badger", "Kestrel",
    )

    /** A random avatar seed. Seeds are opaque; the avatar is drawn from a hash of the seed. */
    fun avatarSeed(random: Random = Random.Default): String = buildString {
        repeat(SEED_LENGTH) { append(SEED_ALPHABET[random.nextInt(SEED_ALPHABET.length)]) }
    }

    private const val SEED_LENGTH = 12
    private const val SEED_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"
}
