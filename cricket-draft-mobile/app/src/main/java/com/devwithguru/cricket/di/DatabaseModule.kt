package com.devwithguru.cricket.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.devwithguru.cricket.data.db.CricketDatabase
import com.devwithguru.cricket.data.db.dao.BatterStatsDao
import com.devwithguru.cricket.data.db.dao.BowlerStatsDao
import com.devwithguru.cricket.data.db.dao.FixtureDao
import com.devwithguru.cricket.data.db.dao.InningsDao
import com.devwithguru.cricket.data.db.dao.PartnershipEventDao
import com.devwithguru.cricket.data.db.dao.TournamentDao
import com.devwithguru.cricket.data.db.dao.TeamDao
import com.devwithguru.cricket.data.db.dao.PlayerDao
import com.devwithguru.cricket.data.db.dao.WicketEventDao
import com.devwithguru.cricket.data.db.entity.PlayerEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        playerDaoProvider: Provider<PlayerDao>
    ): CricketDatabase {
        return Room.databaseBuilder(
            context,
            CricketDatabase::class.java,
            "cricket.db"
        )
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Seed default players on first install
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        val dao = playerDaoProvider.get()
                        if (dao.count() == 0) {
                            dao.insertAll(defaultPlayers())
                        }
                    }
                }
            })
            .build()
    }

    @Provides fun providePlayerDao(db: CricketDatabase): PlayerDao = db.playerDao()
    @Provides fun provideFixtureDao(db: CricketDatabase): FixtureDao = db.fixtureDao()
    @Provides fun provideInningsDao(db: CricketDatabase): InningsDao = db.inningsDao()
    @Provides fun provideBatterStatsDao(db: CricketDatabase): BatterStatsDao = db.batterStatsDao()
    @Provides fun provideBowlerStatsDao(db: CricketDatabase): BowlerStatsDao = db.bowlerStatsDao()
    @Provides fun provideWicketEventDao(db: CricketDatabase): WicketEventDao = db.wicketEventDao()
    @Provides fun providePartnershipEventDao(db: CricketDatabase): PartnershipEventDao = db.partnershipEventDao()
    @Provides fun provideTournamentDao(db: CricketDatabase): TournamentDao = db.tournamentDao()
    @Provides fun provideTeamDao(db: CricketDatabase): TeamDao = db.teamDao()
    @Provides fun provideSyncStatusDao(db: CricketDatabase): com.devwithguru.cricket.data.db.dao.SyncStatusDao = db.syncStatusDao()
    @Provides fun providePendingChangeDao(db: CricketDatabase): com.devwithguru.cricket.data.db.dao.PendingChangeDao = db.pendingChangeDao()
    @Provides fun providePendingDeliveryDao(db: CricketDatabase): com.devwithguru.cricket.data.db.dao.PendingDeliveryDao = db.pendingDeliveryDao()
    @Provides fun provideAdminTeamDao(db: CricketDatabase): com.devwithguru.cricket.data.db.dao.AdminTeamDao = db.adminTeamDao()
    @Provides fun provideAdminPlayerDao(db: CricketDatabase): com.devwithguru.cricket.data.db.dao.AdminPlayerDao = db.adminPlayerDao()
    @Provides fun provideAdminFixtureDao(db: CricketDatabase): com.devwithguru.cricket.data.db.dao.AdminFixtureDao = db.adminFixtureDao()
    @Provides fun provideAdminDraftSetupDao(db: CricketDatabase): com.devwithguru.cricket.data.db.dao.AdminDraftSetupDao = db.adminDraftSetupDao()
    @Provides fun provideStageDao(db: CricketDatabase): com.devwithguru.cricket.data.db.dao.StageDao = db.stageDao()
    @Provides fun provideUserProfileDao(db: CricketDatabase): com.devwithguru.cricket.data.db.dao.UserProfileDao = db.userProfileDao()
    @Provides fun providePlayerStatsDao(db: CricketDatabase): com.devwithguru.cricket.data.db.dao.PlayerStatsDao = db.playerStatsDao()

    @Provides
    @Named("auth_token")
    fun provideAuthToken(authRepository: com.devwithguru.cricket.data.repository.AuthRepository): () -> String? = { authRepository.getToken() }

    private fun defaultPlayers(): List<PlayerEntity> = listOf(
        PlayerEntity(id = "h1", playerProfileId = null, name = "Ahmed Ali", role = "Batter", battingStyle = null, bowlingStyle = null, city = null, photoPath = null, isRegistered = true, teamId = null, status = "approved"),
        PlayerEntity(id = "h2", playerProfileId = null, name = "Bilal Butt", role = "Batter", battingStyle = null, bowlingStyle = null, city = null, photoPath = null, isRegistered = true, teamId = null, status = "approved"),
        PlayerEntity(id = "h3", playerProfileId = null, name = "Salman Ahmed", role = "Wicketkeeper", battingStyle = null, bowlingStyle = null, city = null, photoPath = null, isRegistered = true, teamId = null, status = "approved"),
        PlayerEntity(id = "h4", playerProfileId = null, name = "Usman Shinwari", role = "Bowler", battingStyle = null, bowlingStyle = null, city = null, photoPath = null, isRegistered = true, teamId = null, status = "approved"),
        PlayerEntity(id = "h5", playerProfileId = null, name = "Zain Abbas", role = "Batter", battingStyle = null, bowlingStyle = null, city = null, photoPath = null, isRegistered = true, teamId = null, status = "approved"),
        PlayerEntity(id = "h6", playerProfileId = null, name = "Imran Khan", role = "All-rounder", battingStyle = null, bowlingStyle = null, city = null, photoPath = null, isRegistered = true, teamId = null, status = "approved"),
        PlayerEntity(id = "h7", playerProfileId = null, name = "Farhan Saeed", role = "Bowler", battingStyle = null, bowlingStyle = null, city = null, photoPath = null, isRegistered = true, teamId = null, status = "approved"),
        PlayerEntity(id = "h8", playerProfileId = null, name = "Riaz Afridi", role = "Bowler", battingStyle = null, bowlingStyle = null, city = null, photoPath = null, isRegistered = true, teamId = null, status = "approved"),
        PlayerEntity(id = "h9", playerProfileId = null, name = "Asif Iqbal", role = "Batter", battingStyle = null, bowlingStyle = null, city = null, photoPath = null, isRegistered = true, teamId = null, status = "approved"),
        PlayerEntity(id = "h10", playerProfileId = null, name = "Shoaib Malik", role = "All-rounder", battingStyle = null, bowlingStyle = null, city = null, photoPath = null, isRegistered = true, teamId = null, status = "approved"),
        PlayerEntity(id = "h11", playerProfileId = null, name = "Wahab Riaz", role = "Bowler", battingStyle = null, bowlingStyle = null, city = null, photoPath = null, isRegistered = true, teamId = null, status = "approved"),
        PlayerEntity(id = "a1", playerProfileId = null, name = "Yasir Khan", role = "Bowler", battingStyle = null, bowlingStyle = null, city = null, photoPath = null, isRegistered = true, teamId = null, status = "approved"),
        PlayerEntity(id = "a2", playerProfileId = null, name = "Babar Azam", role = "Batter", battingStyle = null, bowlingStyle = null, city = null, photoPath = null, isRegistered = true, teamId = null, status = "approved"),
        PlayerEntity(id = "a3", playerProfileId = null, name = "Mohammad Rizwan", role = "Wicketkeeper", battingStyle = null, bowlingStyle = null, city = null, photoPath = null, isRegistered = true, teamId = null, status = "approved"),
        PlayerEntity(id = "a4", playerProfileId = null, name = "Shaheen Afridi", role = "Bowler", battingStyle = null, bowlingStyle = null, city = null, photoPath = null, isRegistered = true, teamId = null, status = "approved"),
        PlayerEntity(id = "a5", playerProfileId = null, name = "Shadab Khan", role = "All-rounder", battingStyle = null, bowlingStyle = null, city = null, photoPath = null, isRegistered = true, teamId = null, status = "approved"),
        PlayerEntity(id = "a6", playerProfileId = null, name = "Fakhar Zaman", role = "Batter", battingStyle = null, bowlingStyle = null, city = null, photoPath = null, isRegistered = true, teamId = null, status = "approved"),
        PlayerEntity(id = "a7", playerProfileId = null, name = "Haris Rauf", role = "Bowler", battingStyle = null, bowlingStyle = null, city = null, photoPath = null, isRegistered = true, teamId = null, status = "approved"),
        PlayerEntity(id = "a8", playerProfileId = null, name = "Naseem Shah", role = "Bowler", battingStyle = null, bowlingStyle = null, city = null, photoPath = null, isRegistered = true, teamId = null, status = "approved"),
        PlayerEntity(id = "a9", playerProfileId = null, name = "Iftikhar Ahmed", role = "All-rounder", battingStyle = null, bowlingStyle = null, city = null, photoPath = null, isRegistered = true, teamId = null, status = "approved"),
        PlayerEntity(id = "a10", playerProfileId = null, name = "Saim Ayub", role = "Batter", battingStyle = null, bowlingStyle = null, city = null, photoPath = null, isRegistered = true, teamId = null, status = "approved"),
        PlayerEntity(id = "a11", playerProfileId = null, name = "Imad Wasim", role = "All-rounder", battingStyle = null, bowlingStyle = null, city = null, photoPath = null, isRegistered = true, teamId = null, status = "approved")
    )
}
