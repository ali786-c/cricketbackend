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
    @Provides fun provideAdminTeamDao(db: CricketDatabase): com.devwithguru.cricket.data.db.dao.AdminTeamDao = db.adminTeamDao()
    @Provides fun provideAdminPlayerDao(db: CricketDatabase): com.devwithguru.cricket.data.db.dao.AdminPlayerDao = db.adminPlayerDao()
    @Provides fun provideAdminFixtureDao(db: CricketDatabase): com.devwithguru.cricket.data.db.dao.AdminFixtureDao = db.adminFixtureDao()
    @Provides fun provideAdminDraftSetupDao(db: CricketDatabase): com.devwithguru.cricket.data.db.dao.AdminDraftSetupDao = db.adminDraftSetupDao()
    @Provides fun provideStageDao(db: CricketDatabase): com.devwithguru.cricket.data.db.dao.StageDao = db.stageDao()

    @Provides
    @Named("auth_token")
    fun provideAuthToken(authRepository: com.devwithguru.cricket.data.repository.AuthRepository): () -> String? = { authRepository.getToken() }

    private fun defaultPlayers(): List<PlayerEntity> = listOf(
        PlayerEntity("h1", "Ahmed Ali", "Batter", true),
        PlayerEntity("h2", "Bilal Butt", "Batter", true),
        PlayerEntity("h3", "Salman Ahmed", "Wicketkeeper", true),
        PlayerEntity("h4", "Usman Shinwari", "Bowler", true),
        PlayerEntity("h5", "Zain Abbas", "Batter", true),
        PlayerEntity("h6", "Imran Khan", "All-rounder", true),
        PlayerEntity("h7", "Farhan Saeed", "Bowler", true),
        PlayerEntity("h8", "Riaz Afridi", "Bowler", true),
        PlayerEntity("h9", "Asif Iqbal", "Batter", true),
        PlayerEntity("h10", "Shoaib Malik", "All-rounder", true),
        PlayerEntity("h11", "Wahab Riaz", "Bowler", true),
        PlayerEntity("a1", "Yasir Khan", "Bowler", true),
        PlayerEntity("a2", "Babar Azam", "Batter", true),
        PlayerEntity("a3", "Mohammad Rizwan", "Wicketkeeper", true),
        PlayerEntity("a4", "Shaheen Afridi", "Bowler", true),
        PlayerEntity("a5", "Shadab Khan", "All-rounder", true),
        PlayerEntity("a6", "Fakhar Zaman", "Batter", true),
        PlayerEntity("a7", "Haris Rauf", "Bowler", true),
        PlayerEntity("a8", "Naseem Shah", "Bowler", true),
        PlayerEntity("a9", "Iftikhar Ahmed", "All-rounder", true),
        PlayerEntity("a10", "Saim Ayub", "Batter", true),
        PlayerEntity("a11", "Imad Wasim", "All-rounder", true)
    )
}
