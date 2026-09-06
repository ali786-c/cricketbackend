<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasOne;

class Fixture extends Model
{
    use HasFactory;

    protected $fillable = [
        'tournament_id', 'home_team_id', 'away_team_id', 'round_number', 'round_name',
        'match_number', 'scheduled_at', 'venue', 'city', 'timezone', 'status',
        'notes', 'created_by', 'updated_by',
        'stage_id', 'stage_name', 'match_type', 'umpire1', 'umpire2',
    ];

    protected function casts(): array
    {
        return ['scheduled_at' => 'datetime'];
    }

    public function tournament(): BelongsTo { return $this->belongsTo(Tournament::class); }
    public function homeTeam(): BelongsTo { return $this->belongsTo(Team::class, 'home_team_id'); }
    public function awayTeam(): BelongsTo { return $this->belongsTo(Team::class, 'away_team_id'); }
    public function creator(): BelongsTo { return $this->belongsTo(User::class, 'created_by'); }
    public function updater(): BelongsTo { return $this->belongsTo(User::class, 'updated_by'); }
    public function match(): HasOne { return $this->hasOne(CricketMatch::class, 'fixture_id'); }

    public function getTitleAttribute(): string
    {
        return ($this->homeTeam?->short_name ?: $this->homeTeam?->name ?: 'TBD').' vs '.($this->awayTeam?->short_name ?: $this->awayTeam?->name ?: 'TBD');
    }
}
