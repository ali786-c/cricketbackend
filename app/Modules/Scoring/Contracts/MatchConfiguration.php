<?php

namespace App\Modules\Scoring\Contracts;

use App\Models\CricketRuleProfile;
use InvalidArgumentException;

final readonly class MatchConfiguration
{
    public function __construct(
        public string $format,
        public int $inningsPerSide,
        public int $oversPerInnings,
        public ?int $squadSize,
        public int $playingXiSize,
        public int $maximumWickets,
        public int $legalBallsPerOver,
        public ?int $maxOversPerBowler,
        public string $ballType,
        public int $noBallRuns = 1,
        public int $wideRuns = 1,
        public bool $wideRunsToBatsman = false,
        public bool $noBallRunsToBatsman = false,
        public bool $lastManStanding = false,
        public ?int $maxBallsPerOver = null,
        public ?int $maxRunsPerOver = null,
        public string $origin = 'custom',
        public int $version = 1,
        public ?string $lockedAt = null,
    ) {
        $errors = $this->errors();
        if ($errors !== []) {
            throw new InvalidArgumentException(implode('; ', array_map(fn ($key, $value) => "$key: $value", array_keys($errors), $errors)));
        }
    }

    public static function fromArray(array $data, string $origin = 'custom'): self
    {
        return new self(
            strtolower((string) $data['format']), (int) $data['innings_per_side'], (int) $data['overs_per_innings'],
            isset($data['squad_size']) ? (int) $data['squad_size'] : null, (int) $data['playing_xi_size'],
            (int) $data['maximum_wickets'], (int) $data['legal_balls_per_over'],
            isset($data['max_overs_per_bowler']) ? (int) $data['max_overs_per_bowler'] : null,
            strtolower((string) $data['ball_type']), (int) ($data['no_ball_runs'] ?? 1), (int) ($data['wide_runs'] ?? 1),
            (bool) ($data['wide_runs_to_batsman'] ?? false), (bool) ($data['noball_runs_to_batsman'] ?? false),
            (bool) ($data['last_man_standing'] ?? false), isset($data['max_balls_per_over']) ? (int) $data['max_balls_per_over'] : null,
            isset($data['max_runs_per_over']) ? (int) $data['max_runs_per_over'] : null, $origin,
            (int) ($data['version'] ?? 1), $data['locked_at'] ?? null,
        );
    }

    public static function fromProfile(CricketRuleProfile $profile, ?string $ballType, string $origin, ?string $lockedAt = null): self
    {
        return self::fromArray([...$profile->toArray(), 'ball_type' => $ballType ?? 'leather', 'locked_at' => $lockedAt], $origin);
    }

    public function toArray(): array
    {
        return [
            'format' => $this->format, 'innings_per_side' => $this->inningsPerSide, 'overs_per_innings' => $this->oversPerInnings,
            'squad_size' => $this->squadSize, 'playing_xi_size' => $this->playingXiSize, 'maximum_wickets' => $this->maximumWickets,
            'legal_balls_per_over' => $this->legalBallsPerOver, 'max_overs_per_bowler' => $this->maxOversPerBowler,
            'ball_type' => $this->ballType, 'no_ball_runs' => $this->noBallRuns, 'wide_runs' => $this->wideRuns,
            'wide_runs_to_batsman' => $this->wideRunsToBatsman, 'noball_runs_to_batsman' => $this->noBallRunsToBatsman,
            'last_man_standing' => $this->lastManStanding, 'max_balls_per_over' => $this->maxBallsPerOver,
            'max_runs_per_over' => $this->maxRunsPerOver, 'origin' => $this->origin, 'version' => $this->version, 'locked_at' => $this->lockedAt,
        ];
    }

    private function errors(): array
    {
        $errors = [];
        if (! in_array($this->format, ['t10', 't20', 'odi', 'test', 'limited_overs', 'custom'], true)) $errors['format'] = 'Unsupported format';
        if (! in_array($this->ballType, ['leather', 'tennis', 'hard_ball', 'tape_ball', 'indoor'], true)) $errors['ball_type'] = 'Unsupported ball type';
        if ($this->inningsPerSide < 1 || $this->inningsPerSide > 4) $errors['innings_per_side'] = 'Must be between 1 and 4';
        if ($this->oversPerInnings < 1 || $this->oversPerInnings > 100) $errors['overs_per_innings'] = 'Must be between 1 and 100';
        if ($this->playingXiSize < 2 || $this->playingXiSize > 99) $errors['playing_xi_size'] = 'Must be between 2 and 99';
        if ($this->maximumWickets < 1 || $this->maximumWickets >= $this->playingXiSize) $errors['maximum_wickets'] = 'Must be below playing XI size';
        if ($this->squadSize !== null && $this->squadSize < $this->playingXiSize) $errors['squad_size'] = 'Must be at least playing XI size';
        if ($this->legalBallsPerOver < 1 || $this->legalBallsPerOver > 12) $errors['legal_balls_per_over'] = 'Must be between 1 and 12';
        if ($this->maxOversPerBowler !== null && ($this->maxOversPerBowler < 1 || $this->maxOversPerBowler > $this->oversPerInnings)) $errors['max_overs_per_bowler'] = 'Must not exceed innings overs';
        if ($this->noBallRuns < 0 || $this->noBallRuns > 10) $errors['no_ball_runs'] = 'Must be between 0 and 10';
        if ($this->wideRuns < 0 || $this->wideRuns > 10) $errors['wide_runs'] = 'Must be between 0 and 10';
        if ($this->maxBallsPerOver !== null && ($this->maxBallsPerOver < $this->legalBallsPerOver || $this->maxBallsPerOver > 24)) $errors['max_balls_per_over'] = 'Must be at least legal balls and at most 24';
        if ($this->maxRunsPerOver !== null && ($this->maxRunsPerOver < 1 || $this->maxRunsPerOver > 100)) $errors['max_runs_per_over'] = 'Must be between 1 and 100';
        if ($this->version < 1) $errors['version'] = 'Must be positive';
        return $errors;
    }
}
