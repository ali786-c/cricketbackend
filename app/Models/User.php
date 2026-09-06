<?php

namespace App\Models;

use Database\Factories\UserFactory;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Laravel\Sanctum\HasApiTokens;
use Spatie\Permission\Traits\HasRoles;

class User extends Authenticatable
{
    /** @use HasFactory<UserFactory> */
    use HasApiTokens, HasFactory, Notifiable, HasRoles;

    protected $fillable = [
        'name',
        'email',
        'password',
    ];

    protected $hidden = [
        'password',
        'remember_token',
    ];

    public function playerProfile()
    {
        return $this->hasOne(PlayerProfile::class);
    }

    public function teamCaptainAssignments()
    {
        return $this->hasMany(TeamCaptain::class);
    }

    public function reviewedTournamentPlayers()
    {
        return $this->hasMany(TournamentPlayer::class, 'reviewed_by');
    }

    public function selectedDraftPicks()
    {
        return $this->hasMany(DraftPick::class, 'selected_by');
    }

    public function auditLogs()
    {
        return $this->hasMany(AuditLog::class);
    }

    protected static function booted(): void
    {
        static::created(function (User $user) {
            $user->playerProfile()->create([
                'full_name' => $user->name,
                'is_active' => true,
            ]);
        });
    }

    protected function casts(): array
    {
        return [
            'email_verified_at' => 'datetime',
            'password' => 'hashed',
        ];
    }
}
