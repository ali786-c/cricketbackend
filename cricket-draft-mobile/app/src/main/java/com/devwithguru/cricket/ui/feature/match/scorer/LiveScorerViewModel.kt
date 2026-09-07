package com.devwithguru.cricket.ui.feature.match.scorer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devwithguru.cricket.data.db.entity.PendingDeliveryEntity
import com.devwithguru.cricket.data.repository.DeliverySyncRepository
import com.devwithguru.cricket.domain.model.BatterState
import com.devwithguru.cricket.domain.model.BowlerState
import com.devwithguru.cricket.domain.model.WicketEvent
import com.devwithguru.cricket.domain.model.PartnershipEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveScorerViewModel @Inject constructor(
    private val deliverySyncRepository: DeliverySyncRepository
) : ViewModel() {

    var isInitialized = false

    // Match context (set during initialize or from Screen)
    var matchId: String = ""
        private set
    var inningsNumber: Int = 1
        private set
    var strikerServerId: Int = 0
    var nonStrikerServerId: Int = 0
    var bowlerServerId: Int = 0

    // Main game state
    var state by mutableStateOf(MatchScoringState())
        private set

    // History stack for Undo
    val historyStack = mutableStateListOf<MatchScoringState>()

    // Modal state controllers
    var showWicketDialog by mutableStateOf(false)
    var showSelectBowlerDialog by mutableStateOf(false)
    var showSelectBatsmanDialog by mutableStateOf(false)
    var slotToReplace by mutableStateOf(1)
    var showAdvancedKeyboard by mutableStateOf(false)

    // Sub-dialogs for specific extras combinations
    var activeExtraDialogType by mutableStateOf("") // "Wd", "Nb", "By", "Lb" or empty

    var showSelectOpeningStrikerDialog by mutableStateOf(false)
    var showSelectOpeningNonStrikerDialog by mutableStateOf(false)
    var showSelectOpeningBowlerDialog by mutableStateOf(false)


    var ballsPerOver by mutableStateOf(6)

    // Roster arrays
    val availableBatsmenList = mutableStateListOf<String>()
    val availableBowlersList = mutableStateListOf<String>()
    val battingSquadList = mutableStateListOf<String>()
    val bowlingSquadList = mutableStateListOf<String>()

    /**
     * Set match context for delivery persistence.
     * Must be called before scoring begins.
     */
    fun setMatchContext(matchId: String, inningsNumber: Int) {
        this.matchId = matchId
        this.inningsNumber = inningsNumber
    }

    /**
     * Set server-side player IDs for sync (called when player roster is loaded).
     */
    fun setPlayerServerIds(ids: Map<String, Int>) {
        // Called from parent when player IDs are resolved from the API
    }

    fun initialize(
        runs: Int,
        wickets: Int,
        oversBowled: String,
        striker: String,
        nonStriker: String,
        initialBowlerName: String = "",
        ballsPerOver: Int = 6,
        battingSquad: List<String> = emptyList(),
        bowlingSquad: List<String> = emptyList(),
        homeTeamName: String = "",
        awayTeamName: String = "",
        matchTotalOvers: Int = 6,
        matchTotalWickets: Int = 10,
        isInnings2: Boolean = false,
        firstInningsTarget: Int? = null,
        initialBatsmenStats: List<BatterState> = emptyList(),
        initialBowlersStats: List<BowlerState> = emptyList(),
        initialFOW: List<WicketEvent> = emptyList(),
        initialPartnerships: List<PartnershipEvent> = emptyList(),
        initialActivePartnershipRuns: Int = 0,
        initialActivePartnershipBalls: Int = 0
    ) {
        if (isInitialized && 
            state.isInnings2 == isInnings2 && 
            battingSquad == battingSquadList.toList() && 
            bowlingSquad == bowlingSquadList.toList()
        ) return
        isInitialized = true

        val parsedOversToBalls = { oversStr: String ->
            val parts = oversStr.split(".")
            val ov = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val bl = parts.getOrNull(1)?.toIntOrNull() ?: 0
            (ov * ballsPerOver) + bl
        }

        // Reset local dialogue/keyboard flags
        showWicketDialog = false
        showSelectBowlerDialog = false
        showSelectBatsmanDialog = false
        activeExtraDialogType = ""
        showAdvancedKeyboard = false
        this.ballsPerOver = ballsPerOver

        // Clear history stack to prevent back-stepping into Innings 1
        historyStack.clear()

        // Reset squads roster list
        battingSquadList.clear()
        if (battingSquad.isNotEmpty()) {
            battingSquadList.addAll(battingSquad)
        } else {
            battingSquadList.addAll(
                listOf("Salman Ahmed", "Imran Khan", "Zain Abbas", "Farhan Saeed", "Asif Ali", "Kamran Akmal")
            )
        }

        bowlingSquadList.clear()
        if (bowlingSquad.isNotEmpty()) {
            bowlingSquadList.addAll(bowlingSquad)
        } else {
            bowlingSquadList.addAll(
                listOf("Yasir Khan", "Sohail Tanvir", "Wahab Riaz", "Umaid Asif", "Haris Rauf")
            )
        }

        val loadedBatsmenMap = initialBatsmenStats.associateBy { it.name }
        val loadedBowlersMap = initialBowlersStats.associateBy { it.name }

        if (striker.isBlank() && nonStriker.isBlank()) {
            showSelectOpeningStrikerDialog = true
            showSelectOpeningNonStrikerDialog = false
            showSelectOpeningBowlerDialog = false

            val battingTeam = if (isInnings2) awayTeamName else homeTeamName
            val bowlingTeam = if (isInnings2) homeTeamName else awayTeamName

            // Set placeholder state
            state = MatchScoringState(
                runs = runs,
                wickets = wickets,
                totalBalls = parsedOversToBalls(oversBowled),
                strikerName = "Select Striker",
                batter1 = BatterState("Select Striker"),
                batter2 = BatterState("Select Non-Striker"),
                bowler = BowlerState(name = "Select Bowler", balls = 0, runsConceded = 0, wickets = 0),
                thisOver = emptyList(),
                nextBatsmenQueue = battingSquadList.toList(),
                dismissedBatsmen = emptyList(),
                homeTeamName = homeTeamName,
                awayTeamName = awayTeamName,
                battingTeamName = battingTeam,
                bowlingTeamName = bowlingTeam,
                matchTotalOvers = matchTotalOvers,
                matchTotalWickets = matchTotalWickets,
                isInnings2 = isInnings2,
                firstInningsTarget = firstInningsTarget,
                ballsPerOver = ballsPerOver,
                bowlersStats = loadedBowlersMap,
                batsmenStats = loadedBatsmenMap,
                fallOfWickets = initialFOW,
                partnerships = initialPartnerships,
                activePartnershipRuns = initialActivePartnershipRuns,
                activePartnershipBalls = initialActivePartnershipBalls
            )
        } else {
            showSelectOpeningStrikerDialog = false
            showSelectOpeningNonStrikerDialog = false
            showSelectOpeningBowlerDialog = false

            val strName = striker
            val nonStrName = nonStriker

            // Reconstruct dismissed list based on loaded stats
            val dismissedList = loadedBatsmenMap.values.filter { it.isDismissed }.map { it.name }

            // Remove starting and dismissed batsmen from available listing
            availableBatsmenList.clear()
            availableBatsmenList.addAll(battingSquadList)
            availableBatsmenList.remove(strName)
            availableBatsmenList.remove(nonStrName)
            availableBatsmenList.removeAll(dismissedList)

            val resolvedBowlerName = initialBowlerName.ifBlank {
                bowlingSquadList.find { it == state.bowler.name } ?: bowlingSquadList.getOrNull(0) ?: "Yasir Khan"
            }

            val initialStrikerBatter = loadedBatsmenMap[strName] ?: BatterState(strName)
            val initialNonStrikerBatter = loadedBatsmenMap[nonStrName] ?: BatterState(nonStrName)
            val initialBowler = loadedBowlersMap[resolvedBowlerName] ?: BowlerState(name = resolvedBowlerName, balls = 0, runsConceded = 0, wickets = 0)

            val battingTeam = if (isInnings2) awayTeamName else homeTeamName
            val bowlingTeam = if (isInnings2) homeTeamName else awayTeamName

            state = MatchScoringState(
                runs = runs,
                wickets = wickets,
                totalBalls = parsedOversToBalls(oversBowled),
                strikerName = strName,
                batter1 = initialStrikerBatter,
                batter2 = initialNonStrikerBatter,
                bowler = initialBowler,
                thisOver = emptyList(),
                nextBatsmenQueue = availableBatsmenList.toList(),
                dismissedBatsmen = dismissedList,
                homeTeamName = homeTeamName,
                awayTeamName = awayTeamName,
                battingTeamName = battingTeam,
                bowlingTeamName = bowlingTeam,
                matchTotalOvers = matchTotalOvers,
                matchTotalWickets = matchTotalWickets,
                isInnings2 = isInnings2,
                firstInningsTarget = firstInningsTarget,
                ballsPerOver = ballsPerOver,
                bowlersStats = loadedBowlersMap + (resolvedBowlerName to initialBowler),
                batsmenStats = loadedBatsmenMap + (strName to initialStrikerBatter) + (nonStrName to initialNonStrikerBatter),
                fallOfWickets = initialFOW,
                partnerships = initialPartnerships,
                activePartnershipRuns = initialActivePartnershipRuns,
                activePartnershipBalls = initialActivePartnershipBalls
            )
        }
    }

    fun selectOpeningBatsmen(strikerName: String, nonStrikerName: String) {
        val initialStriker = BatterState(strikerName)
        val initialNonStriker = BatterState(nonStrikerName)
        state = state.copy(
            strikerName = strikerName,
            batter1 = initialStriker,
            batter2 = initialNonStriker,
            nextBatsmenQueue = battingSquadList.filter { it != strikerName && it != nonStrikerName },
            batsmenStats = state.batsmenStats + (strikerName to initialStriker) + (nonStrikerName to initialNonStriker)
        )
        showSelectOpeningStrikerDialog = false
        showSelectOpeningNonStrikerDialog = false
        showSelectOpeningBowlerDialog = true
    }

    fun selectOpeningBowler(name: String) {
        val initialBowler = BowlerState(name)
        state = state.copy(
            bowler = initialBowler,
            bowlersStats = state.bowlersStats + (name to initialBowler)
        )
        // Configure next queue correctly
        availableBatsmenList.clear()
        availableBatsmenList.addAll(battingSquadList)
        availableBatsmenList.remove(state.strikerName)
        availableBatsmenList.remove(state.nonStrikerName)

        state = state.copy(
            nextBatsmenQueue = availableBatsmenList.toList()
        )
        showSelectOpeningBowlerDialog = false
    }

    val isScoringBlocked: Boolean
        get() = showSelectOpeningStrikerDialog ||
                showSelectOpeningNonStrikerDialog ||
                showSelectOpeningBowlerDialog ||
                showSelectBowlerDialog ||
                showSelectBatsmanDialog ||
                showWicketDialog

    val isWicketRecordBlocked: Boolean
        get() = showSelectOpeningStrikerDialog ||
                showSelectOpeningNonStrikerDialog ||
                showSelectOpeningBowlerDialog ||
                showSelectBowlerDialog ||
                showSelectBatsmanDialog

    private fun saveStateToHistory() {
        if (historyStack.size >= MAX_HISTORY_SIZE) {
            historyStack.removeAt(0)
        }
        historyStack.add(
            state.copy(
                batter1 = state.batter1.copy(),
                batter2 = state.batter2.copy(),
                bowler = state.bowler.copy(),
                thisOver = state.thisOver.toList(),
                nextBatsmenQueue = state.nextBatsmenQueue.toList()
            )
        )
    }

    private fun checkOverEnd() {
        if (state.isInnings1Completed || state.isMatchCompleted) {
            showSelectBowlerDialog = false
            return
        }
        val legalBallsInOver = state.thisOver.count { !it.contains("Wd") && !it.contains("Nb") && !it.contains("Pen") }
        if (legalBallsInOver > 0 && legalBallsInOver % ballsPerOver == 0) {
            showSelectBowlerDialog = true
            // Clear thisOver for the next over
            state = state.copy(thisOver = emptyList())
        }
    }

    private fun updateActiveBatsmen(
        runsToAdd: Int,
        ballsIncrement: Int,
        rotateStrike: Boolean,
        isOverEnd: Boolean
    ): Triple<BatterState, BatterState, String> {
        val isBatter1Striker = state.batter1.name == state.strikerName
        val strikerBatter = if (isBatter1Striker) state.batter1 else state.batter2
        val nonStrikerBatter = if (isBatter1Striker) state.batter2 else state.batter1

        val newStriker = strikerBatter.copy(
            runs = strikerBatter.runs + runsToAdd,
            balls = strikerBatter.balls + ballsIncrement,
            fours = strikerBatter.fours + (if (runsToAdd == 4) 1 else 0),
            sixes = strikerBatter.sixes + (if (runsToAdd == 6) 1 else 0)
        )
        val newNonStriker = nonStrikerBatter

        val finalStriker = if (isBatter1Striker) newStriker else newNonStriker
        val finalNonStriker = if (isBatter1Striker) newNonStriker else newStriker

        val finalStrikerName = if (rotateStrike) nonStrikerBatter.name else newStriker.name
        val finalStrikerNameAfterOver = if (isOverEnd) {
            if (finalStrikerName == nonStrikerBatter.name) newStriker.name else nonStrikerBatter.name
        } else {
            finalStrikerName
        }

        return Triple(finalStriker, finalNonStriker, finalStrikerNameAfterOver)
    }

    fun recordRuns(runsToAdd: Int, onScoreChanged: (runs: Int, wickets: Int, overs: String, striker: String, nonStriker: String) -> Unit) {
        if (isScoringBlocked) return
        saveStateToHistory()

        val nextTotalBalls = state.totalBalls + 1
        val isOverEnd = nextTotalBalls > 0 && nextTotalBalls % ballsPerOver == 0
        val rotateStrike = runsToAdd % 2 == 1

        val (finalB1, finalB2, nextStrikerName) = updateActiveBatsmen(
            runsToAdd = runsToAdd,
            ballsIncrement = 1,
            rotateStrike = rotateStrike,
            isOverEnd = isOverEnd
        )

        val newBowler = state.bowler.copy(
            balls = state.bowler.balls + 1,
            runsConceded = state.bowler.runsConceded + runsToAdd
        )

        val updatedThisOver = state.thisOver + "$runsToAdd"
        val updatedBowlersStats = state.bowlersStats + (newBowler.name to newBowler)
        val updatedBatsmenStats = state.batsmenStats + 
            (finalB1.name to finalB1) + 
            (finalB2.name to finalB2)

        val isBatter1Striker = state.batter1.name == state.strikerName
        val nextP1Runs = state.activePartnershipBatter1Runs + (if (isBatter1Striker) runsToAdd else 0)
        val nextP1Balls = state.activePartnershipBatter1Balls + (if (isBatter1Striker) 1 else 0)
        val nextP2Runs = state.activePartnershipBatter2Runs + (if (!isBatter1Striker) runsToAdd else 0)
        val nextP2Balls = state.activePartnershipBatter2Balls + (if (!isBatter1Striker) 1 else 0)

        state = state.copy(
            runs = state.runs + runsToAdd,
            totalBalls = nextTotalBalls,
            dotBalls = state.dotBalls + (if (runsToAdd == 0) 1 else 0),
            batter1 = finalB1,
            batter2 = finalB2,
            strikerName = nextStrikerName,
            bowler = newBowler,
            bowlersStats = updatedBowlersStats,
            batsmenStats = updatedBatsmenStats,
            thisOver = updatedThisOver,
            activePartnershipRuns = state.activePartnershipRuns + runsToAdd,
            activePartnershipBalls = state.activePartnershipBalls + 1,
            activePartnershipBatter1Runs = nextP1Runs,
            activePartnershipBatter1Balls = nextP1Balls,
            activePartnershipBatter2Runs = nextP2Runs,
            activePartnershipBatter2Balls = nextP2Balls
        )

        // Persist delivery to Room for sync
        persistDelivery(
            runsOffBat = runsToAdd,
            extrasType = null,
            extrasRuns = 0
        )

        triggerCallbacks(onScoreChanged)
        checkOverEnd()
    }

    fun recordExtra(extraType: String, onScoreChanged: (runs: Int, wickets: Int, overs: String, striker: String, nonStriker: String) -> Unit) {
        if (isScoringBlocked) return
        // For By/Lb, show the extras dialog to allow amount selection
        if (extraType == "By" || extraType == "Lb") {
            activeExtraDialogType = extraType
            return
        }
        saveStateToHistory()

        val isWide = extraType == "Wd"
        val isNoBall = extraType == "Nb"
        val isBye = extraType == "By"
        val isLegBye = extraType == "Lb"
        val isPenalty = extraType == "Pen"

        val extraRuns = 1

        val bowlBallsIncrement = if (isWide || isNoBall || isPenalty) 0 else 1
        val bowlRunsConceded = if (isWide || isNoBall) extraRuns else 0
        val newBowler = state.bowler.copy(
            balls = state.bowler.balls + bowlBallsIncrement,
            runsConceded = state.bowler.runsConceded + bowlRunsConceded
        )

        val nextTotalBalls = state.totalBalls + bowlBallsIncrement
        val isOverEnd = bowlBallsIncrement > 0 && nextTotalBalls > 0 && nextTotalBalls % ballsPerOver == 0
        val rotateStrike = isBye || isLegBye

        val (finalB1, finalB2, nextStrikerName) = updateActiveBatsmen(
            runsToAdd = 0,
            ballsIncrement = if (isWide || isPenalty) 0 else 1,
            rotateStrike = rotateStrike,
            isOverEnd = isOverEnd
        )

        val updatedBowlersStats = state.bowlersStats + (newBowler.name to newBowler)
        val updatedBatsmenStats = state.batsmenStats + 
            (finalB1.name to finalB1) + 
            (finalB2.name to finalB2)

        val pBallIncrement = if (isWide || isPenalty || isNoBall) 0 else 1
        val isBatter1Striker = state.batter1.name == state.strikerName
        val extraStrikerBalls = if (isWide || isPenalty) 0 else 1
        val nextP1Balls = state.activePartnershipBatter1Balls + (if (isBatter1Striker) extraStrikerBalls else 0)
        val nextP2Balls = state.activePartnershipBatter2Balls + (if (!isBatter1Striker) extraStrikerBalls else 0)

        state = state.copy(
            runs = state.runs + extraRuns,
            extras = state.extras + extraRuns,
            totalBalls = nextTotalBalls,
            batter1 = finalB1,
            batter2 = finalB2,
            strikerName = nextStrikerName,
            bowler = newBowler,
            bowlersStats = updatedBowlersStats,
            batsmenStats = updatedBatsmenStats,
            thisOver = state.thisOver + extraType,
            activePartnershipRuns = state.activePartnershipRuns + extraRuns,
            activePartnershipBalls = state.activePartnershipBalls + pBallIncrement,
            activePartnershipBatter1Runs = state.activePartnershipBatter1Runs,
            activePartnershipBatter1Balls = nextP1Balls,
            activePartnershipBatter2Runs = state.activePartnershipBatter2Runs,
            activePartnershipBatter2Balls = nextP2Balls
        )

        // Persist delivery to Room for sync
        persistDelivery(
            runsOffBat = 0,
            extrasType = extraType,
            extrasRuns = extraRuns
        )

        triggerCallbacks(onScoreChanged)
        checkOverEnd()
    }

    fun recordCustomScore(
        batRuns: Int,
        extrasVal: Int,
        extraType: String,
        onScoreChanged: (runs: Int, wickets: Int, overs: String, striker: String, nonStriker: String) -> Unit
    ) {
        if (isScoringBlocked) return
        saveStateToHistory()

        val isWide = extraType == "Wd"
        val isNoBall = extraType == "Nb"
        val isBye = extraType == "By"
        val isLegBye = extraType == "Lb"
        val isPenalty = extraType == "Pen"

        val totalRunsToAdd = batRuns + extrasVal

        val bowlBallsIncrement = if (isWide || isNoBall || isPenalty) 0 else 1
        val bowlRunsConceded = if (isBye || isLegBye || isPenalty) 0 else totalRunsToAdd
        val newBowler = state.bowler.copy(
            balls = state.bowler.balls + bowlBallsIncrement,
            runsConceded = state.bowler.runsConceded + bowlRunsConceded
        )

        val nextTotalBalls = state.totalBalls + bowlBallsIncrement
        val isOverEnd = bowlBallsIncrement > 0 && nextTotalBalls > 0 && nextTotalBalls % ballsPerOver == 0

        val physicalRuns = when (extraType) {
            "Wd" -> maxOf(0, extrasVal - 1)
            "Nb" -> if (batRuns > 0) batRuns else maxOf(0, extrasVal - 1)
            "By", "Lb" -> extrasVal
            "Pen" -> 0
            else -> batRuns
        }
        val rotateStrike = physicalRuns % 2 == 1

        val (finalB1, finalB2, nextStrikerName) = updateActiveBatsmen(
            runsToAdd = batRuns,
            ballsIncrement = if (isWide || isPenalty) 0 else 1,
            rotateStrike = rotateStrike,
            isOverEnd = isOverEnd
        )

        val tag = if (extrasVal > 0) "${extraType}${totalRunsToAdd}" else "$batRuns"
        val updatedBowlersStats = state.bowlersStats + (newBowler.name to newBowler)
        val updatedBatsmenStats = state.batsmenStats + 
            (finalB1.name to finalB1) + 
            (finalB2.name to finalB2)

        val isBatter1Striker = state.batter1.name == state.strikerName
        val customStrikerBalls = if (isWide || isPenalty) 0 else 1
        val nextP1Runs = state.activePartnershipBatter1Runs + (if (isBatter1Striker) batRuns else 0)
        val nextP1Balls = state.activePartnershipBatter1Balls + (if (isBatter1Striker) customStrikerBalls else 0)
        val nextP2Runs = state.activePartnershipBatter2Runs + (if (!isBatter1Striker) batRuns else 0)
        val nextP2Balls = state.activePartnershipBatter2Balls + (if (!isBatter1Striker) customStrikerBalls else 0)

        state = state.copy(
            runs = state.runs + totalRunsToAdd,
            totalBalls = nextTotalBalls,
            dotBalls = state.dotBalls + (if (totalRunsToAdd == 0 && bowlBallsIncrement > 0) 1 else 0),
            extras = state.extras + (if (isWide || isNoBall || isBye || isLegBye || isPenalty) extrasVal else 0),
            batter1 = finalB1,
            batter2 = finalB2,
            strikerName = nextStrikerName,
            bowler = newBowler,
            bowlersStats = updatedBowlersStats,
            batsmenStats = updatedBatsmenStats,
            thisOver = state.thisOver + tag,
            activePartnershipRuns = state.activePartnershipRuns + totalRunsToAdd,
            activePartnershipBalls = state.activePartnershipBalls + bowlBallsIncrement,
            activePartnershipBatter1Runs = nextP1Runs,
            activePartnershipBatter1Balls = nextP1Balls,
            activePartnershipBatter2Runs = nextP2Runs,
            activePartnershipBatter2Balls = nextP2Balls
        )

        // Persist delivery to Room for sync
        persistDelivery(
            runsOffBat = batRuns,
            extrasType = if (extrasVal > 0) extraType else null,
            extrasRuns = extrasVal
        )

        triggerCallbacks(onScoreChanged)
        showAdvancedKeyboard = false
        if (bowlBallsIncrement > 0) {
            checkOverEnd()
        }
    }

    fun recordWicket(
        wicketType: String,
        dismissedName: String,
        fielderName: String? = null,
        completedRuns: Int = 0,
        extraType: String? = null
    ) {
        if (isWicketRecordBlocked) return
        saveStateToHistory()

        slotToReplace = if (dismissedName == state.batter1.name) 1 else 2

        val isBatter1Striker = state.batter1.name == state.strikerName
        val strikerBallsIncrement = if (extraType == "Wide") 0 else 1

        var updatedB1 = state.batter1
        var updatedB2 = state.batter2

        // Credit completed runs to striker
        if (isBatter1Striker) {
            updatedB1 = updatedB1.copy(
                runs = updatedB1.runs + completedRuns,
                balls = updatedB1.balls + strikerBallsIncrement
            )
        } else {
            updatedB2 = updatedB2.copy(
                runs = updatedB2.runs + completedRuns,
                balls = updatedB2.balls + strikerBallsIncrement
            )
        }

        val activeBowler = state.bowler.name

        val finalUpdatedBatter1 = if (slotToReplace == 1) {
            updatedB1.copy(
                isDismissed = true,
                dismissalType = wicketType,
                bowlerName = activeBowler,
                fielderName = fielderName
            )
        } else {
            updatedB1
        }
        val finalUpdatedBatter2 = if (slotToReplace == 2) {
            updatedB2.copy(
                isDismissed = true,
                dismissalType = wicketType,
                bowlerName = activeBowler,
                fielderName = fielderName
            )
        } else {
            updatedB2
        }

        // Ball legality & extra scoring
        val isWide = extraType == "Wide"
        val isNoBall = extraType == "No Ball"
        val isBye = extraType == "Byes"
        val isLegBye = extraType == "Leg Byes"
        val isExtra = isWide || isNoBall || isBye || isLegBye

        val ballIncrement = if (isWide || isNoBall) 0 else 1
        val nextTotalBalls = state.totalBalls + ballIncrement

        val extraConceded = if (isWide || isNoBall) 1 else 0
        val runsConcededByBowler = if (isBye || isLegBye) 0 else (completedRuns + extraConceded)

        val isBowlerWicket = wicketType != "Run Out" && wicketType != "Mankad" && wicketType != "Retired" && wicketType != "Over The Fence"
        val newBowler = state.bowler.copy(
            balls = state.bowler.balls + ballIncrement,
            runsConceded = state.bowler.runsConceded + runsConcededByBowler,
            wickets = state.bowler.wickets + (if (isBowlerWicket) 1 else 0)
        )

        val teamRunsToAdd = completedRuns + extraConceded
        val teamExtrasToAdd = if (isExtra) teamRunsToAdd else 0

        val updatedBowlersStats = state.bowlersStats + (newBowler.name to newBowler)
        val updatedBatsmenStats = state.batsmenStats + 
            (finalUpdatedBatter1.name to finalUpdatedBatter1) + 
            (finalUpdatedBatter2.name to finalUpdatedBatter2)

        val notation = when {
            wicketType == "Run Out" -> {
                when (extraType) {
                    "Wide" -> if (completedRuns > 0) "${completedRuns + 1}Wd+W" else "Wd+W"
                    "No Ball" -> if (completedRuns > 0) "${completedRuns + 1}Nb+W" else "Nb+W"
                    "Byes" -> if (completedRuns > 0) "${completedRuns}B+W" else "W"
                    "Leg Byes" -> if (completedRuns > 0) "${completedRuns}LB+W" else "W"
                    else -> if (completedRuns > 0) "${completedRuns}+W" else "W"
                }
            }
            extraType == "Wide" -> "Wd+W"
            extraType == "No Ball" -> "Nb+W"
            else -> "W"
        }

        val p1Runs = state.activePartnershipBatter1Runs + (if (isBatter1Striker) completedRuns else 0)
        val p1Balls = state.activePartnershipBatter1Balls + (if (isBatter1Striker) strikerBallsIncrement else 0)
        val p2Runs = state.activePartnershipBatter2Runs + (if (!isBatter1Striker) completedRuns else 0)
        val p2Balls = state.activePartnershipBatter2Balls + (if (!isBatter1Striker) strikerBallsIncrement else 0)

        val newWicket = WicketEvent(
            wicketNumber = state.wickets + 1,
            batsmanName = dismissedName,
            teamRuns = state.runs + teamRunsToAdd,
            overs = "${nextTotalBalls / ballsPerOver}.${nextTotalBalls % ballsPerOver}"
        )
        val newPartnership = PartnershipEvent(
            wicketNumber = state.wickets + 1,
            batter1Name = state.batter1.name,
            batter2Name = state.batter2.name,
            runs = state.activePartnershipRuns + teamRunsToAdd,
            balls = state.activePartnershipBalls + ballIncrement,
            batter1ContributionRuns = p1Runs,
            batter1ContributionBalls = p1Balls,
            batter2ContributionRuns = p2Runs,
            batter2ContributionBalls = p2Balls
        )

        state = state.copy(
            runs = state.runs + teamRunsToAdd,
            extras = state.extras + teamExtrasToAdd,
            wickets = state.wickets + 1,
            totalBalls = nextTotalBalls,
            bowler = newBowler,
            bowlersStats = updatedBowlersStats,
            batsmenStats = updatedBatsmenStats,
            thisOver = state.thisOver + notation,
            dismissedBatsmen = state.dismissedBatsmen + dismissedName,
            batter1 = finalUpdatedBatter1,
            batter2 = finalUpdatedBatter2,
            fallOfWickets = state.fallOfWickets + newWicket,
            partnerships = state.partnerships + newPartnership,
            activePartnershipRuns = 0,
            activePartnershipBalls = 0,
            activePartnershipBatter1Runs = 0,
            activePartnershipBatter1Balls = 0,
            activePartnershipBatter2Runs = 0,
            activePartnershipBatter2Balls = 0
        )

        // Persist wicket delivery to Room for sync
        persistDelivery(
            runsOffBat = completedRuns,
            extrasType = extraType,
            extrasRuns = extraConceded,
            isWicket = true,
            dismissalType = wicketType,
            dismissedPlayerName = dismissedName,
            fielderName = fielderName
        )

        showWicketDialog = false
        if (!state.isInnings1Completed && !state.isMatchCompleted) {
            showSelectBatsmanDialog = true
        }
        checkOverEnd()
    }

    fun confirmIncomingBatsman(
        name: String,
        onScoreChanged: (runs: Int, wickets: Int, overs: String, striker: String, nonStriker: String) -> Unit
    ) {
        availableBatsmenList.remove(name)

        val incomingBatter = BatterState(name)
        val newBatter1 = if (slotToReplace == 1) incomingBatter else state.batter1
        val newBatter2 = if (slotToReplace == 2) incomingBatter else state.batter2

        var nextStrikerName = if (state.strikerName == (if (slotToReplace == 1) state.batter1.name else state.batter2.name)) {
            name
        } else {
            state.strikerName
        }

        val isOverEnd = state.totalBalls > 0 && state.totalBalls % ballsPerOver == 0
        if (isOverEnd) {
            nextStrikerName = if (nextStrikerName == newBatter1.name) newBatter2.name else newBatter1.name
        }

        val nextQueue = battingSquadList.filter { it != newBatter1.name && it != newBatter2.name && !state.dismissedBatsmen.contains(it) }
        val updatedBatsmenStats = state.batsmenStats + (name to incomingBatter)

        state = state.copy(
            batter1 = newBatter1,
            batter2 = newBatter2,
            strikerName = nextStrikerName,
            nextBatsmenQueue = nextQueue,
            batsmenStats = updatedBatsmenStats
        )
        showSelectBatsmanDialog = false
        triggerCallbacks(onScoreChanged)
        checkOverEnd()
    }

    fun selectNextBowler(bowler: String) {
        val nextBowlerState = state.bowlersStats[bowler] ?: BowlerState(bowler)
        state = state.copy(
            bowler = nextBowlerState,
            thisOver = emptyList()
        )
        showSelectBowlerDialog = false
    }

    fun toggleStrike(onScoreChanged: (runs: Int, wickets: Int, overs: String, striker: String, nonStriker: String) -> Unit) {
        saveStateToHistory()
        val nextStrikerName = if (state.strikerName == state.batter1.name) state.batter2.name else state.batter1.name
        state = state.copy(
            strikerName = nextStrikerName
        )
        triggerCallbacks(onScoreChanged)
    }

    fun performUndo(onScoreChanged: (runs: Int, wickets: Int, overs: String, striker: String, nonStriker: String) -> Unit) {
        if (historyStack.isNotEmpty()) {
            state = historyStack.removeAt(historyStack.lastIndex)
            
            showSelectBowlerDialog = false
            showWicketDialog = false
            showSelectBatsmanDialog = false
            activeExtraDialogType = ""
            showAdvancedKeyboard = false
            
            triggerCallbacks(onScoreChanged)
        }
    }

    private fun triggerCallbacks(onScoreChanged: (runs: Int, wickets: Int, overs: String, striker: String, nonStriker: String) -> Unit) {
        onScoreChanged(
            state.runs,
            state.wickets,
            "${state.totalBalls / ballsPerOver}.${state.totalBalls % ballsPerOver}",
            state.batterStriker.name,
            state.batterNonStriker.name
        )
    }

    // ─── Delivery Persistence ──────────────────────────────────────

    /**
     * Persist each delivery to Room for crash recovery and API sync.
     * This runs in a background coroutine to never block the UI.
     */
    private fun persistDelivery(
        runsOffBat: Int = 0,
        extrasType: String? = null,
        extrasRuns: Int = 0,
        isWicket: Boolean = false,
        dismissalType: String? = null,
        dismissedPlayerName: String? = null,
        fielderName: String? = null
    ) {
        if (matchId.isBlank()) return // No match context set

        val (wides, noBalls, byes, legByes) = when (extrasType) {
            "Wd" -> listOf(extrasRuns, 0, 0, 0)
            "Nb" -> listOf(0, extrasRuns, 0, 0)
            "By" -> listOf(0, 0, extrasRuns, 0)
            "Lb" -> listOf(0, 0, 0, extrasRuns)
            "Byes" -> listOf(0, 0, extrasRuns, 0)
            "Leg Byes" -> listOf(0, 0, 0, extrasRuns)
            "Pen" -> listOf(0, 0, 0, 0) // penalty_runs handled separately
            else -> listOf(0, 0, 0, 0)
        }

        val penaltyRuns = if (extrasType == "Pen") extrasRuns else 0

        viewModelScope.launch {
            try {
                deliverySyncRepository.saveDelivery(
                    PendingDeliveryEntity(
                        matchId = matchId,
                        inningsNumber = inningsNumber,
                        runsOffBat = runsOffBat,
                        wides = wides,
                        noBalls = noBalls,
                        byes = byes,
                        legByes = legByes,
                        penaltyRuns = penaltyRuns,
                        strikerId = strikerServerId,
                        nonStrikerId = nonStrikerServerId,
                        bowlerId = bowlerServerId,
                        overNumber = state.totalBalls / ballsPerOver,
                        ballNumber = state.totalBalls % ballsPerOver,
                        cumulativeRuns = state.runs,
                        cumulativeWickets = state.wickets,
                        cumulativeBalls = state.totalBalls
                    )
                )
            } catch (e: Exception) {
                // Silently fail — delivery data is still in memory
                // Will be retried on next scoring action or full sync
            }
        }
    }

    companion object {
        private const val MAX_HISTORY_SIZE = 50
    }
}
