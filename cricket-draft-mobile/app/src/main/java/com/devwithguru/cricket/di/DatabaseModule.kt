package com.devwithguru.cricket.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE admin_fixtures ADD COLUMN serverMatchId INTEGER")
            db.execSQL("ALTER TABLE admin_fixtures ADD COLUMN serverRevision INTEGER")
            db.execSQL("ALTER TABLE admin_fixtures ADD COLUMN syncError TEXT")
            db.execSQL("ALTER TABLE fixtures ADD COLUMN playerServerIds TEXT NOT NULL DEFAULT '{}'")
            db.execSQL("ALTER TABLE fixtures ADD COLUMN ballsPerOver INTEGER NOT NULL DEFAULT 6")
            db.execSQL("ALTER TABLE pending_deliveries ADD COLUMN strikerName TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE pending_deliveries ADD COLUMN nonStrikerName TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE pending_deliveries ADD COLUMN bowlerName TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE pending_deliveries ADD COLUMN wicketDismissedPlayerName TEXT")
            db.execSQL("ALTER TABLE pending_deliveries ADD COLUMN wicketFielderName TEXT")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CricketDatabase {
        return Room.databaseBuilder(
            context,
            CricketDatabase::class.java,
            "cricket.db"
        )
            .addMigrations(MIGRATION_16_17)
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

}
