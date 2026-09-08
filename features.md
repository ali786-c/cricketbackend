# Recent Features & Sync Implementation Log

This document tracks the recent major architectural changes and feature developments implemented across the Laravel Backend and the Mobile App.

## 1. 2-Way Sync Pipeline (Player Profiles & Stats)
We established a strict, robust 2-way sync pipeline to keep the mobile offline database and the remote backend perfectly aligned.
* **Strict DB Mapping:** Replaced SharedPreferences for stats with proper SQLite tables (`UserProfileEntity`, `PlayerStatsEntity`). The Room DB schema was mapped 1:1 with the Laravel backend tables (`player_profiles`, `player_stats`).
* **Multipart Photo Uploads:** Updated the `ProfileController` on the backend and Retrofit `ApiService` on mobile to support `@Multipart` form data. This ensures profile photos and text data sync seamlessly in a single request.
* **Immediate Local Persistence:** Configured `AuthRepository` to immediately save synced profile data and stats into Room DB upon successful API responses.

## 2. Guest Player (Manual Player) Architecture
To support scenarios where Admins/Scorers need to add players directly in the Match Center who don't have an app account:
* **Nullable User Accounts:** Updated the `player_profiles` database table schema by making `user_id` nullable and adding an `is_guest` boolean flag.
* **Auto-Profile Generation:** Created a new endpoint (`POST /api/v1/tournaments/{tournament}/players/manual`) in `AdminPlayerController`. When an admin adds a player offline, the sync pipeline hits this endpoint, which automatically generates a "Guest Profile" with a globally unique code (e.g., `PLR-XXXXX`).
* **Global Access:** Manual/Guest players are no longer isolated to a single match. They have real profiles and are globally searchable across the app, ensuring their stats accumulate correctly across multiple tournaments.
* **Future Claiming Support:** Because guest players receive an official `PLR-XXXXX` code, real users can later "Claim" these profiles when they download the app and register, permanently linking their new `user_id` to the existing guest profile.

## 3. API & Controller Updates
* `TournamentController::players` now accurately returns required profile fields (`photo_path`, `batting_style`, `bowling_style`).
* Created `database/migrations/xxxx_create_player_stats_table.php` and `App\Models\PlayerStat`.
* Refactored `routes/api.php` to securely route profile updates and guest player creation (`api.v1.admin.players.manual.store`).
