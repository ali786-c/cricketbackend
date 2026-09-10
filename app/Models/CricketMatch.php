<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class CricketMatch extends Model
{
    use HasFactory;

    protected $table = 'matches';

    protected $fillable = [
        'fixture_id',
        'client_uuid',
        'tournament_id',
        'rule_profile_id',
        'rule_profile_version',
        'rule_snapshot',
        'overs_per_innings',
        'status',
        'toss_winner_team_id',
        'toss_decision',
        'winner_team_id',
        'result_type',
        'result_summary',
        'result_submitted_at',
        'result_submitted_by',
        'result_approved_by',
        'toss_recorded_at',
        'started_at',
        'completed_at',
        'approved_at',
        'current_innings_id',
        'revision',
        'last_event_at',
        'created_by',
        'updated_by',
    ];

    protected function casts(): array
    {
        return [
            'overs_per_innings' => 'integer',
            'rule_snapshot' => 'array',
            'toss_recorded_at' => 'datetime',
            'started_at' => 'datetime',
            'completed_at' => 'datetime',
            'approved_at' => 'datetime',
            'result_submitted_at' => 'datetime',
            'last_event_at' => 'datetime',
        ];
    }

    public function tournament(): BelongsTo
    {
        return $this->belongsTo(Tournament::class);
    }

    public function fixture(): BelongsTo
    {
        return $this->belongsTo(Fixture::class, 'fixture_id');
    }

    public function ruleProfile(): BelongsTo
    {
        return $this->belongsTo(CricketRuleProfile::class, 'rule_profile_id');
    }

    public function tossWinner(): BelongsTo
    {
        return $this->belongsTo(Team::class, 'toss_winner_team_id');
    }

    public function winner(): BelongsTo
    {
        return $this->belongsTo(Team::class, 'winner_team_id');
    }

    public function resultSubmitter(): BelongsTo
    {
        return $this->belongsTo(User::class, 'result_submitted_by');
    }

    public function resultApprover(): BelongsTo
    {
        return $this->belongsTo(User::class, 'result_approved_by');
    }

    public function creator(): BelongsTo
    {
        return $this->belongsTo(User::class, 'created_by');
    }

    public function updater(): BelongsTo
    {
        return $this->belongsTo(User::class, 'updated_by');
    }

    public function players(): HasMany
    {
        return $this->hasMany(MatchPlayer::class, 'match_id');
    }

    public function innings(): HasMany
    {
        return $this->hasMany(MatchInnings::class, 'match_id')->orderBy('innings_number');
    }

    public function deliveries(): HasMany
    {
        return $this->hasMany(MatchDelivery::class, 'match_id')->orderBy('sequence_number');
    }

    public function getTeamIdsAttribute(): array
    {
        return $this->players()->distinct()->pluck('team_id')->map(fn ($id) => (int) $id)->values()->all();
    }
}
