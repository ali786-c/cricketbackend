<?php

namespace Tests\Unit;

use App\Modules\Scoring\Contracts\MatchConfiguration;
use InvalidArgumentException;
use PHPUnit\Framework\TestCase;

class MatchConfigurationContractTest extends TestCase
{
    public function test_configuration_serializes_to_canonical_wire_contract(): void
    {
        $contract = MatchConfiguration::fromArray($this->validConfiguration());

        $this->assertSame('custom', $contract->format);
        $this->assertSame('tennis', $contract->ballType);
        $this->assertSame(11, $contract->toArray()['playing_xi_size']);
        $this->assertArrayHasKey('locked_at', $contract->toArray());
    }

    public function test_cross_field_invariants_are_enforced(): void
    {
        $this->expectException(InvalidArgumentException::class);
        MatchConfiguration::fromArray([...$this->validConfiguration(), 'maximum_wickets' => 11]);
    }

    private function validConfiguration(): array
    {
        return [
            'format' => 'custom', 'innings_per_side' => 1, 'overs_per_innings' => 10,
            'squad_size' => 15, 'playing_xi_size' => 11, 'maximum_wickets' => 10,
            'legal_balls_per_over' => 6, 'max_overs_per_bowler' => 2, 'ball_type' => 'tennis',
            'version' => 1,
        ];
    }
}
