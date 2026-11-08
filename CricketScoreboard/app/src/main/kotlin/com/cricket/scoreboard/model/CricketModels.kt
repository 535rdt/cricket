package com.cricket.scoreboard.model

enum class PlayerRole(val label: String) {
    BATSMAN("Batsman"),
    BOWLER("Bowler"),
    ALL_ROUNDER("All-Rounder"),
    WICKET_KEEPER("Wicketkeeper")
}

data class Player(
    val id: String,
    val name: String,
    val role: PlayerRole = PlayerRole.ALL_ROUNDER,
    val isCaptain: Boolean = false,
    val isWicketKeeper: Boolean = false,
    val photoUri: String? = null
)

data class Team(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val players: List<Player> = emptyList()
)

enum class TossChoice(val label: String) {
    BAT("Bat First"),
    BOWL("Bowl First")
}

data class TossResult(
    val winnerTeamId: String,
    val choice: TossChoice
)

enum class ExtraType(val shortName: String, val displayName: String) {
    NONE("", "None"),
    WIDE("wd", "Wide"),
    NO_BALL("nb", "No Ball"),
    LEG_BYE("lb", "Leg Bye"),
    BYE("b", "Bye")
}

enum class WicketType(val label: String) {
    BOWLED("Bowled"),
    CAUGHT("Caught"),
    RUN_OUT("Run Out"),
    LBW("LBW"),
    STUMPED("Stumped"),
    HIT_WICKET("Hit Wicket"),
    RETIRED_HURT("Retired Hurt"),
    RETIRED_OUT("Retired Out")
}

data class Delivery(
    val id: String,
    val overIndex: Int,
    val ballInOver: Int,
    val legalBallNumber: Int,
    val bowlerId: String,
    val bowlerName: String,
    val strikerId: String,
    val strikerName: String,
    val nonStrikerId: String,
    val nonStrikerName: String,
    val runsBat: Int,
    val extraRuns: Int,
    val extraType: ExtraType,
    val isWicket: Boolean,
    val wicketType: WicketType? = null,
    val dismissedPlayerId: String? = null,
    val dismissedPlayerName: String? = null,
    val fielderName: String? = null,
    val description: String = "",
    val isLegalDelivery: Boolean = (extraType != ExtraType.WIDE && extraType != ExtraType.NO_BALL),
    val isFreeHit: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalRunsThisBall: Int get() = runsBat + extraRuns

    val badgeText: String get() {
        return when {
            isWicket -> if (runsBat > 0) "W+$runsBat" else "W"
            extraType == ExtraType.WIDE -> if (extraRuns > 1) "${extraRuns}wd" else "wd"
            extraType == ExtraType.NO_BALL -> if (runsBat > 0) "${runsBat}nb" else "nb"
            extraType == ExtraType.LEG_BYE -> "${extraRuns}lb"
            extraType == ExtraType.BYE -> "${extraRuns}b"
            runsBat == 0 -> "•"
            runsBat == 4 -> "4"
            runsBat == 6 -> "6"
            else -> "$runsBat"
        }
    }
}

data class FallOfWicket(
    val wicketNum: Int,
    val teamRuns: Int,
    val overText: String,
    val batsmanName: String
)

data class BatsmanScore(
    val playerId: String,
    val name: String,
    val runs: Int = 0,
    val balls: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val dismissalText: String = "not out",
    val isOut: Boolean = false,
    val isBatting: Boolean = false,
    val photoUri: String? = null
) {
    val strikeRate: Double
        get() = if (balls > 0) (runs.toDouble() * 100.0) / balls else 0.0

    val formattedStrikeRate: String
        get() = String.format(java.util.Locale.US, "%.1f", strikeRate)
}

data class BowlerScore(
    val playerId: String,
    val name: String,
    val legalBalls: Int = 0,
    val maidens: Int = 0,
    val runsConceded: Int = 0,
    val wickets: Int = 0,
    val photoUri: String? = null
) {
    val oversText: String
        get() = "${legalBalls / 6}.${legalBalls % 6}"

    val oversDouble: Double
        get() = (legalBalls / 6) + (legalBalls % 6) / 6.0

    val economy: Double
        get() = if (legalBalls > 0) (runsConceded.toDouble() * 6.0) / legalBalls else 0.0

    val formattedEconomy: String
        get() = String.format(java.util.Locale.US, "%.2f", economy)
}

data class Innings(
    val inningsNumber: Int,
    val battingTeamId: String,
    val battingTeamName: String,
    val bowlingTeamId: String,
    val bowlingTeamName: String,
    val targetScore: Int? = null,
    val totalRuns: Int = 0,
    val wickets: Int = 0,
    val legalBalls: Int = 0,
    val maxOvers: Int = 20,
    val wides: Int = 0,
    val noBalls: Int = 0,
    val legByes: Int = 0,
    val byes: Int = 0,
    val penaltyRuns: Int = 0,
    val batsmen: List<BatsmanScore> = emptyList(),
    val bowlers: List<BowlerScore> = emptyList(),
    val deliveries: List<Delivery> = emptyList(),
    val fallOfWickets: List<FallOfWicket> = emptyList(),
    val isCompleted: Boolean = false
) {
    val totalExtras: Int
        get() = wides + noBalls + legByes + byes + penaltyRuns

    val oversText: String
        get() = "${legalBalls / 6}.${legalBalls % 6}"

    val crr: Double
        get() = if (legalBalls > 0) (totalRuns.toDouble() * 6.0) / legalBalls else 0.0

    val formattedCrr: String
        get() = String.format(java.util.Locale.US, "%.2f", crr)

    val projectedScore: Int
        get() = if (legalBalls > 0) ((totalRuns.toDouble() / legalBalls) * (maxOvers * 6)).toInt() else 0

    val currentOverDeliveries: List<Delivery>
        get() {
            if (deliveries.isEmpty()) return emptyList()
            val currentOverIndex = legalBalls / 6
            return deliveries.filter { it.overIndex == currentOverIndex }
        }
}

data class Match(
    val id: String = java.util.UUID.randomUUID().toString(),
    val date: String = "",
    val teamA: Team,
    val teamB: Team,
    val totalOvers: Int = 20,
    val maxOversPerBowler: Int = 4,
    val playersPerSide: Int = 11,
    val wideRuns: Int = 1,
    val noBallRuns: Int = 1,
    val rebowlWide: Boolean = true,
    val rebowlNoBall: Boolean = true,
    val freeHitEnabled: Boolean = true,
    val tossResult: TossResult,
    val innings1: Innings,
    val innings2: Innings? = null,
    val currentInningsIndex: Int = 1,
    val currentStrikerId: String = "",
    val currentNonStrikerId: String = "",
    val currentBowlerId: String = "",
    val lastBowlerId: String? = null,
    val isFinished: Boolean = false,
    val resultMessage: String? = null,
    val manOfTheMatch: String? = null
) {
    val currentInnings: Innings
        get() = if (currentInningsIndex == 1) innings1 else (innings2 ?: innings1)

    val battingTeam: Team
        get() = if (currentInnings.battingTeamId == teamA.id) teamA else teamB

    val bowlingTeam: Team
        get() = if (currentInnings.bowlingTeamId == teamA.id) teamA else teamB

    val requiredRuns: Int?
        get() {
            if (currentInningsIndex != 2 || currentInnings.targetScore == null) return null
            return currentInnings.targetScore!! - currentInnings.totalRuns
        }

    val remainingBalls: Int?
        get() {
            if (currentInningsIndex != 2) return null
            val totalMaxBalls = totalOvers * 6
            return (totalMaxBalls - currentInnings.legalBalls).coerceAtLeast(0)
        }

    val rrr: Double?
        get() {
            val req = requiredRuns ?: return null
            val balls = remainingBalls ?: return null
            if (balls <= 0) return if (req <= 0) 0.0 else Double.POSITIVE_INFINITY
            return (req.toDouble() * 6.0) / balls
        }

    val formattedRrr: String?
        get() = rrr?.let { String.format(java.util.Locale.US, "%.2f", it) }
}

data class SavedSharedMatch(
    val matchCode: String,
    val teamAName: String,
    val teamBName: String,
    val scoreSummary: String,
    val lastUpdated: Long = System.currentTimeMillis(),
    val match: Match
)
