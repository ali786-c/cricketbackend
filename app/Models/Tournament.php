<?php

namespace App\Models;

use Illuminate\Support\Carbon;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Database\Eloquent\Relations\HasOne;

class Tournament extends Model
{
    use HasFactory;

    protected $fillable = [
        'name',
        'season_name',
        'slug',
        'description',
        'location',
        'venue',
        'city',
        'timezone',
        'starts_on',
        'ends_on',
        'registration_opens_at',
        'registration_closes_at',
        'status',
        'is_public',
        'logo_path',
        'banner_path',
        'squad_size',
        'has_draft',
        'default_pick_duration',
        'cricket_rule_profile_id',
        'default_overs_per_innings',
        'published_at',
        'ball_type',
        'data_source',
        'organizer_name',
        'contact_info',
        'competition_structure',
        'tournament_code',
    ];

    protected function casts(): array
    {
        return [
            'starts_on' => 'date',
            'ends_on' => 'date',
            'registration_opens_at' => 'datetime',
            'registration_closes_at' => 'datetime',
            'is_public' => 'boolean',
            'has_draft' => 'boolean',
            'default_overs_per_innings' => 'integer',
            'published_at' => 'datetime',
        ];
    }

    public function getRouteKeyName(): string
    {
        return 'slug';
    }

    public function registrationIsOpen(?Carbon $at = null): bool
    {
        $at ??= now();

        if ($this->registration_opens_at && $at->lt($this->registration_opens_at)) {
            return false;
        }

        if ($this->registration_closes_at && $at->gt($this->registration_closes_at)) {
            return false;
        }

        return true;
    }

    public function publiclyVisibleNow(): bool
    {
        if (! $this->is_public) {
            return false;
        }

        if (in_array($this->status, ['registration', 'ready'], true)) {
            return $this->registrationIsOpen();
        }

        return in_array($this->status, ['live', 'completed'], true);
    }

    public function cricketRuleProfile(): BelongsTo
    {
        return $this->belongsTo(CricketRuleProfile::class);
    }

    public function teams(): HasMany
    {
        return $this->hasMany(Team::class);
    }

    public function tournamentPlayers(): HasMany
    {
        return $this->hasMany(TournamentPlayer::class);
    }

    public function draft(): HasOne
    {
        return $this->hasOne(Draft::class);
    }

    public function auditLogs(): HasMany
    {
        return $this->hasMany(AuditLog::class);
    }

    public function matches(): HasMany
    {
        return $this->hasMany(CricketMatch::class);
    }

    public function fixtures(): HasMany
    {
        return $this->hasMany(Fixture::class)->orderBy('scheduled_at');
    }

    public function standings(): HasMany
    {
        return $this->hasMany(TournamentStanding::class);
    }

    public function stages(): HasMany
    {
        return $this->hasMany(Stage::class)->orderBy('order');
    }
}
