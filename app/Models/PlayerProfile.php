<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Support\Str;

class PlayerProfile extends Model
{
    use HasFactory;

    protected $fillable = [
        'user_id',
        'full_name',
        'unique_code',
        'phone',
        'city',
        'playing_role',
        'batting_style',
        'bowling_style',
        'photo_path',
        'bio',
        'is_active',
        'is_guest',
    ];

    protected function casts(): array
    {
        return ['is_active' => 'boolean'];
    }

    protected static function booted(): void
    {
        static::creating(function (PlayerProfile $profile) {
            if (empty($profile->unique_code)) {
                $profile->unique_code = self::generateUniqueCode();
            }
        });
    }

    public static function generateUniqueCode(): string
    {
        do {
            $code = (string) random_int(100000, 999999);
        } while (static::where('unique_code', $code)->exists());

        return $code;
    }

    /**
     * Find a player by their unique code.
     */
    public static function findByCode(string $code): ?static
    {
        return static::where('unique_code', $code)->first();
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public function tournamentRegistrations(): HasMany
    {
        return $this->hasMany(TournamentPlayer::class);
    }
}
