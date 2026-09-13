<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasOne;

class MatchDelivery extends Model
{
    use HasFactory;

    protected $table = 'match_deliveries';

    protected $fillable = [
        'match_id', 'innings_id', 'over_number', 'ball_number', 'sequence_number',
        'striker_id', 'non_striker_id', 'bowler_id', 'runs_off_bat', 'wides',
        'no_balls', 'byes', 'leg_byes', 'penalty_runs', 'total_runs',
        'is_legal_delivery', 'wicket_id', 'commentary', 'recorded_by',
        'recorded_at', 'revision', 'voided_at', 'void_reason', 'undo_uuid',
        'local_uuid', 'device_timestamp', 'wagon_x', 'wagon_y',
    ];

    protected function casts(): array
    {
        return [
            'is_legal_delivery' => 'boolean',
            'recorded_at' => 'datetime',
            'voided_at' => 'datetime',
            'device_timestamp' => 'datetime',
            'wagon_x' => 'float',
            'wagon_y' => 'float',
        ];
    }

    public function match(): BelongsTo { return $this->belongsTo(CricketMatch::class, 'match_id'); }
    public function innings(): BelongsTo { return $this->belongsTo(MatchInnings::class, 'innings_id'); }
    public function striker(): BelongsTo { return $this->belongsTo(MatchPlayer::class, 'striker_id'); }
    public function nonStriker(): BelongsTo { return $this->belongsTo(MatchPlayer::class, 'non_striker_id'); }
    public function bowler(): BelongsTo { return $this->belongsTo(MatchPlayer::class, 'bowler_id'); }
    public function wicket(): HasOne { return $this->hasOne(MatchWicket::class, 'delivery_id'); }

    public function notation(): string
    {
        if ($this->voided_at) return 'void';
        if ($this->wides > 0) return $this->wides === 1 ? 'Wd' : $this->wides.'Wd';
        if ($this->no_balls > 0) return ($this->runs_off_bat > 0 ? $this->runs_off_bat : '').'Nb';
        if ($this->wicket) return 'W';
        if ($this->byes > 0) return 'B'.$this->byes;
        if ($this->leg_byes > 0) return 'Lb'.$this->leg_byes;
        return (string) $this->runs_off_bat;
    }

    public function ttsCommentary(): string
    {
        if ($this->voided_at) {
            return "This delivery was voided.";
        }

        $strikerName = $this->striker?->player_name_snapshot ?? 'Batsman';
        $bowlerName = $this->bowler?->player_name_snapshot ?? 'Bowler';
        $runs = (int) $this->runs_off_bat;
        $over = $this->over_number;
        $ball = $this->ball_number;

        if ($this->wicket) {
            $type = str_replace('_', ' ', $this->wicket->dismissal_type);
            return "Over {$over}.{$ball}: {$bowlerName} to {$strikerName}, OUT! Dismissed by {$type}.";
        }

        if ($this->wides > 0) {
            return "Over {$over}.{$ball}: {$bowlerName} to {$strikerName}, Wide ball. {$this->wides} runs.";
        }

        if ($this->no_balls > 0) {
            return "Over {$over}.{$ball}: {$bowlerName} to {$strikerName}, No Ball! {$this->total_runs} runs.";
        }

        if ($runs === 6) {
            return "Over {$over}.{$ball}: {$bowlerName} to {$strikerName}, SIX runs! Massive shot over the boundary.";
        }

        if ($runs === 4) {
            return "Over {$over}.{$ball}: {$bowlerName} to {$strikerName}, FOUR runs! Beautifully timed boundary.";
        }

        if ($runs === 0) {
            return "Over {$over}.{$ball}: {$bowlerName} to {$strikerName}, dot ball. No run scored.";
        }

        return "Over {$over}.{$ball}: {$bowlerName} to {$strikerName}, {$runs} run" . ($runs > 1 ? "s." : ".");
    }
}
