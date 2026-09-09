<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class MatchPlayer extends Model
{
    use HasFactory;

    protected $fillable = [
        'match_id',
        'team_id',
        'tournament_player_id',
        'player_profile_id',
        'draft_pick_id',
        'player_name_snapshot',
        'player_role_snapshot',
        'selection_type',
        'batting_order',
        'is_captain',
        'is_wicketkeeper',
        'approved_at',
        'approved_by',
    ];

    protected function casts(): array
    {
        return [
            'is_captain' => 'boolean',
            'is_wicketkeeper' => 'boolean',
            'approved_at' => 'datetime',
        ];
    }

    public function match(): BelongsTo
    {
        return $this->belongsTo(CricketMatch::class, 'match_id');
    }

    public function team(): BelongsTo
    {
        return $this->belongsTo(Team::class);
    }

    public function tournamentPlayer(): BelongsTo
    {
        return $this->belongsTo(TournamentPlayer::class);
    }

    public function playerProfile(): BelongsTo
    {
        return $this->belongsTo(PlayerProfile::class);
    }

    public function draftPick(): BelongsTo
    {
        return $this->belongsTo(DraftPick::class);
    }

    public function approver(): BelongsTo
    {
        return $this->belongsTo(User::class, 'approved_by');
    }

    public function inningsBattingStats(): HasMany
    {
        return $this->hasMany(InningsBattingStat::class);
    }

    public function inningsBowlingStats(): HasMany
    {
        return $this->hasMany(InningsBowlingStat::class);
    }

    public function scopePlayingXi($query)
    {
        return $query->where('selection_type', 'playing_xi');
    }
}
