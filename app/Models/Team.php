<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Database\Eloquent\Relations\HasOne;
use Illuminate\Support\Str;

class Team extends Model
{
    use HasFactory;

    protected $fillable = [
        'tournament_id',
        'name',
        'short_name',
        'unique_code',
        'logo_path',
        'display_order',
        'is_active',
        'creator_id',
        'vice_captain_name',
        'manager_name',
        'wicketkeeper_name',
        'status',
    ];

    protected function casts(): array
    {
        return ['is_active' => 'boolean'];
    }

    protected static function booted(): void
    {
        static::creating(function (Team $team) {
            if (empty($team->unique_code)) {
                $team->unique_code = self::generateUniqueCode();
            }
        });
    }

    public static function generateUniqueCode(): string
    {
        do {
            $code = 'TEAM-' . strtoupper(Str::random(5));
        } while (static::where('unique_code', $code)->exists());

        return $code;
    }

    /**
     * Find a team by its unique code.
     */
    public static function findByCode(string $code): ?static
    {
        return static::where('unique_code', $code)->first();
    }

    public function tournament(): BelongsTo
    {
        return $this->belongsTo(Tournament::class);
    }

    public function captainAssignments(): HasMany
    {
        return $this->hasMany(TeamCaptain::class);
    }

    public function activeCaptain(): HasOne
    {
        return $this->hasOne(TeamCaptain::class)->whereNull('revoked_at')->latestOfMany('assigned_at');
    }

    public function draftPicks(): HasMany
    {
        return $this->hasMany(DraftPick::class);
    }

    public function matchPlayers(): HasMany
    {
        return $this->hasMany(MatchPlayer::class);
    }

    public function homeFixtures(): HasMany
    {
        return $this->hasMany(Fixture::class, 'home_team_id');
    }

    public function awayFixtures(): HasMany
    {
        return $this->hasMany(Fixture::class, 'away_team_id');
    }

    public function creator(): BelongsTo
    {
        return $this->belongsTo(User::class, 'creator_id');
    }
}
