<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Stage extends Model
{
    use HasFactory;

    protected $fillable = [
        'tournament_id',
        'name',
        'type',
        'order',
        'number_of_teams',
        'matches_per_team',
        'points_for_win',
        'points_for_tie',
        'points_for_no_result',
        'points_for_loss',
        'qualification_rule',
        'qualification_count',
        'status',
        'teams_count',
        'matches_count',
        'completed_matches',
    ];

    protected function casts(): array
    {
        return [
            'order' => 'integer',
            'number_of_teams' => 'integer',
            'matches_per_team' => 'integer',
            'points_for_win' => 'integer',
            'points_for_tie' => 'integer',
            'points_for_no_result' => 'integer',
            'points_for_loss' => 'integer',
            'qualification_count' => 'integer',
            'teams_count' => 'integer',
            'matches_count' => 'integer',
            'completed_matches' => 'integer',
        ];
    }

    public function tournament(): BelongsTo
    {
        return $this->belongsTo(Tournament::class);
    }

    public function fixtures(): HasMany
    {
        return $this->hasMany(Fixture::class, 'stage_id');
    }
}
