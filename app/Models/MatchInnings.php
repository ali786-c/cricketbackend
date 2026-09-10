<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class MatchInnings extends Model
{
    use HasFactory;

    protected $table = 'match_innings';

    protected $fillable = [
        'match_id', 'innings_number', 'batting_team_id', 'bowling_team_id',
        'current_striker_id', 'current_non_striker_id', 'current_bowler_id', 'status',
        'target_runs', 'maximum_overs', 'total_runs', 'wickets', 'legal_balls',
        'completed_reason', 'started_at', 'completed_at',
    ];

    protected function casts(): array
    {
        return [
            'started_at' => 'datetime',
            'completed_at' => 'datetime',
        ];
    }

    public function match(): BelongsTo { return $this->belongsTo(CricketMatch::class, 'match_id'); }
    public function battingTeam(): BelongsTo { return $this->belongsTo(Team::class, 'batting_team_id'); }
    public function bowlingTeam(): BelongsTo { return $this->belongsTo(Team::class, 'bowling_team_id'); }
    public function currentStriker(): BelongsTo { return $this->belongsTo(MatchPlayer::class, 'current_striker_id'); }
    public function currentNonStriker(): BelongsTo { return $this->belongsTo(MatchPlayer::class, 'current_non_striker_id'); }
    public function currentBowler(): BelongsTo { return $this->belongsTo(MatchPlayer::class, 'current_bowler_id'); }
    public function deliveries(): HasMany { return $this->hasMany(MatchDelivery::class, 'innings_id')->orderBy('sequence_number'); }
    public function battingStats(): HasMany { return $this->hasMany(InningsBattingStat::class, 'innings_id'); }
    public function bowlingStats(): HasMany { return $this->hasMany(InningsBowlingStat::class, 'innings_id'); }

    public function oversDisplay(?int $legalBallsPerOver = 6): string
    {
        $legalBallsPerOver = max(1, $legalBallsPerOver ?: 6);
        return intdiv((int) $this->legal_balls, $legalBallsPerOver).'.'.((int) $this->legal_balls % $legalBallsPerOver);
    }
}
