<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use App\Http\Requests\Admin\StoreTeamRequest;
use App\Models\Team;
use App\Models\TeamCaptain;
use App\Models\Tournament;
use App\Models\User;
use Illuminate\Support\Facades\DB;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\View\View;

class TeamController extends Controller
{
    public function index(Tournament $tournament): View
    {
        return view('admin.tournaments.teams', [
            'tournament' => $tournament->load('teams.activeCaptain.user'),
            'captainCandidates' => User::query()
                ->where(function ($query) use ($tournament) {
                    $query->whereHas('playerProfile.tournamentRegistrations', function ($q) use ($tournament) {
                        $q->where('tournament_id', $tournament->id)
                          ->where('status', 'approved');
                    })
                    ->orWhere(function ($q) use ($tournament) {
                        $q->whereHas('roles', function ($sub) {
                            $sub->where('name', 'captain');
                        })
                        ->whereDoesntHave('teamCaptainAssignments', function ($sub) use ($tournament) {
                            $sub->whereNull('revoked_at')
                                ->whereHas('team', function ($teamQuery) use ($tournament) {
                                    $teamQuery->where('tournament_id', '<>', $tournament->id);
                                });
                        });
                    });
                })
                ->orderBy('name')
                ->get(),
        ]);
    }

    public function store(StoreTeamRequest $request, Tournament $tournament): RedirectResponse
    {
        $tournament->teams()->create(array_merge(
            $request->validated(),
            ['creator_id' => $request->user()?->id]
        ));

        return back()->with('status', 'Team added successfully.');
    }

    public function assignCaptain(Request $request, Tournament $tournament, Team $team): RedirectResponse
    {
        abort_unless($team->tournament_id === $tournament->id, 404);
        abort_if($tournament->draft?->status !== null && $tournament->draft?->status !== 'setup', 409, 'Captain assignments cannot be changed after the draft starts.');

        $data = $request->validate(['user_id' => ['required', 'integer', 'exists:users,id']]);
        $captain = User::query()
            ->where(function ($query) use ($tournament) {
                $query->whereHas('playerProfile.tournamentRegistrations', function ($q) use ($tournament) {
                    $q->where('tournament_id', $tournament->id)
                      ->where('status', 'approved');
                })
                ->orWhere(function ($q) use ($tournament) {
                    $q->whereHas('roles', function ($sub) {
                        $sub->where('name', 'captain');
                    })
                    ->whereDoesntHave('teamCaptainAssignments', function ($sub) use ($tournament) {
                        $sub->whereNull('revoked_at')
                            ->whereHas('team', function ($teamQuery) use ($tournament) {
                                $teamQuery->where('tournament_id', '<>', $tournament->id);
                            });
                    });
                });
            })
            ->findOrFail($data['user_id']);

        DB::transaction(function () use ($team, $captain) {
            $team->captainAssignments()->whereNull('revoked_at')->update(['revoked_at' => now()]);
            TeamCaptain::create([
                'team_id' => $team->id,
                'user_id' => $captain->id,
                'assigned_at' => now(),
            ]);

            if (! $captain->hasRole('captain')) {
                $captain->assignRole('captain');
            }
        });

        return back()->with('status', 'Captain assigned successfully.');
    }

    public function revokeCaptain(Tournament $tournament, Team $team): RedirectResponse
    {
        abort_unless($team->tournament_id === $tournament->id, 404);
        abort_if($tournament->draft?->status !== null && $tournament->draft?->status !== 'setup', 409, 'Captain assignments cannot be changed after the draft starts.');

        $team->captainAssignments()->whereNull('revoked_at')->update(['revoked_at' => now()]);

        return back()->with('status', 'Captain assignment revoked.');
    }

    public function destroy(Tournament $tournament, Team $team): RedirectResponse
    {
        abort_unless($team->tournament_id === $tournament->id, 404);
        abort_if($tournament->draft?->status !== null && $tournament->draft?->status !== 'setup', 409, 'Teams cannot be removed after the draft starts.');

        $team->delete();

        return back()->with('status', 'Team removed successfully.');
    }

    public function exportTournamentCaptains(Tournament $tournament): \Symfony\Component\HttpFoundation\StreamedResponse
    {
        $captains = User::query()
            ->whereHas('teamCaptainAssignments', function ($query) use ($tournament) {
                $query->whereNull('revoked_at')
                    ->whereHas('team', function ($sub) use ($tournament) {
                        $sub->where('tournament_id', $tournament->id);
                    });
            })
            ->get();

        $captainsData = [];

        foreach ($captains as $captain) {
            $newPassword = 'CD-' . rand(10000, 99999);
            $captain->update([
                'password' => \Illuminate\Support\Facades\Hash::make($newPassword)
            ]);
            $captainsData[] = [
                'name' => $captain->name,
                'email' => $captain->email,
                'password' => $newPassword,
            ];
        }

        $headers = [
            'Content-Type' => 'text/csv',
            'Content-Disposition' => 'attachment; filename="tournament_' . $tournament->slug . '_captains.csv"',
        ];

        $callback = function () use ($captainsData) {
            $file = fopen('php://output', 'w');
            fputcsv($file, ['Name', 'Email', 'Password']);

            foreach ($captainsData as $row) {
                fputcsv($file, $row);
            }

            fclose($file);
        };

        return response()->stream($callback, 200, $headers);
    }

    public function pdf(Tournament $tournament): \Illuminate\Http\Response
    {
        $tournament->load('teams.activeCaptain.user');

        $logoDataUri = null;
        if ($tournament->logo_path && \Illuminate\Support\Facades\Storage::disk('public')->exists($tournament->logo_path)) {
            $mime = \Illuminate\Support\Facades\Storage::disk('public')->mimeType($tournament->logo_path) ?: 'image/png';
            $logoDataUri = 'data:'.$mime.';base64,'.base64_encode(\Illuminate\Support\Facades\Storage::disk('public')->get($tournament->logo_path));
        }

        $html = view('reports.teams-pdf', [
            'tournament' => $tournament,
            'logoDataUri' => $logoDataUri,
            'title' => 'Teams & Captains Report',
        ])->render();

        $options = new \Dompdf\Options();
        $options->set('isRemoteEnabled', false);
        $dompdf = new \Dompdf\Dompdf($options);
        $dompdf->loadHtml($html);
        $dompdf->setPaper('A4', 'portrait');
        $dompdf->render();

        return response($dompdf->output(), 200, [
            'Content-Type' => 'application/pdf',
            'Content-Disposition' => 'attachment; filename="' . str()->slug($tournament->slug . '-teams') . '.pdf"',
        ]);
    }

    public function managePlayers(Tournament $tournament, Team $team): View
    {
        abort_unless($team->tournament_id === $tournament->id, 404);

        $draft = \App\Models\Draft::firstOrCreate(
            ['tournament_id' => $tournament->id],
            ['status' => 'completed', 'current_pick_number' => 1, 'revision' => 1]
        );

        $round = $draft->rounds()->firstOrCreate(
            ['round_number' => 1],
            ['name' => 'Default Round', 'status' => 'completed']
        );

        // Fetch team players (draft picks with tournament_player_id)
        $squadPicks = $team->draftPicks()
            ->whereNotNull('tournament_player_id')
            ->with(['tournamentPlayer.playerProfile.user'])
            ->get();

        // Get IDs of players already assigned to any team in this tournament
        $assignedPlayerIds = \App\Models\DraftPick::query()
            ->where('draft_id', $draft->id)
            ->whereNotNull('tournament_player_id')
            ->pluck('tournament_player_id');

        // Fetch available registered players who are NOT assigned to any team
        $availablePlayers = $tournament->tournamentPlayers()
            ->where('status', 'approved')
            ->whereNotIn('id', $assignedPlayerIds)
            ->with('playerProfile')
            ->get()
            ->sortBy('playerProfile.full_name');

        return view('admin.tournaments.team-players', [
            'tournament' => $tournament,
            'team' => $team,
            'squadPicks' => $squadPicks,
            'availablePlayers' => $availablePlayers,
        ]);
    }

    public function addPlayer(Request $request, Tournament $tournament, Team $team): RedirectResponse
    {
        abort_unless($team->tournament_id === $tournament->id, 404);

        $draft = \App\Models\Draft::firstOrCreate(
            ['tournament_id' => $tournament->id],
            ['status' => 'completed', 'current_pick_number' => 1, 'revision' => 1]
        );

        $round = $draft->rounds()->firstOrCreate(
            ['round_number' => 1],
            ['name' => 'Default Round', 'status' => 'completed']
        );

        $action = $request->input('action');

        if ($action === 'select_registered') {
            $data = $request->validate([
                'tournament_player_id' => ['required', 'integer', 'exists:tournament_players,id'],
            ]);

            // Ensure the player is not already assigned
            $alreadyAssigned = \App\Models\DraftPick::query()
                ->where('draft_id', $draft->id)
                ->where('tournament_player_id', $data['tournament_player_id'])
                ->exists();

            if ($alreadyAssigned) {
                return back()->withErrors(['status' => 'This player is already assigned to a team.']);
            }

            $nextPickNumber = ($draft->picks()->max('pick_number') ?? 0) + 1;

            \App\Models\DraftPick::create([
                'draft_id' => $draft->id,
                'draft_round_id' => $round->id,
                'team_id' => $team->id,
                'pick_number' => $nextPickNumber,
                'status' => 'selected',
                'tournament_player_id' => $data['tournament_player_id'],
                'selected_by' => $request->user()->id,
                'selected_at' => now(),
            ]);

            return back()->with('status', 'Player assigned to team squad successfully.');
        } elseif ($action === 'create_new') {
            $data = $request->validate([
                'full_name' => ['required', 'string', 'max:255'],
                'phone' => ['required', 'string', 'max:50'],
                'location' => ['required', 'string', 'max:255'],
                'playing_role' => ['required', 'string', 'in:Batter,Bowler,All-rounder,Wicketkeeper'],
                'email' => ['nullable', 'email', 'max:255'],
            ]);

            // Transaction to create user + player_profile + tournament_player registration
            $tournamentPlayer = DB::transaction(function () use ($tournament, $data) {
                $profile = \App\Models\PlayerProfile::query()
                    ->with('user')
                    ->where('phone', $data['phone'])
                    ->where('full_name', $data['full_name'])
                    ->first();
                $user = $profile?->user;

                if (!$user) {
                    $email = $data['email'];
                    if (!$email) {
                        $baseEmail = \Illuminate\Support\Str::slug($data['full_name'], '.');
                        $cleanPhone = preg_replace('/[^0-9]/', '', $data['phone']);
                        $phoneSuffix = substr($cleanPhone, -4);
                        if ($phoneSuffix !== '') {
                            $baseEmail .= '.' . $phoneSuffix;
                        }
                        $email = $baseEmail . '@cricketdraft.local';

                        $counter = 1;
                        while (\App\Models\User::query()->where('email', $email)->exists()) {
                            $email = $baseEmail . '.' . $counter . '@cricketdraft.local';
                            $counter++;
                        }
                    }

                    $user = \App\Models\User::create([
                        'name' => $data['full_name'],
                        'email' => $email,
                        'password' => \Illuminate\Support\Str::random(40),
                    ]);
                    $user->assignRole('player');
                }

                $profile = \App\Models\PlayerProfile::updateOrCreate(
                    ['user_id' => $user->id],
                    [
                        'full_name' => $data['full_name'],
                        'phone' => $data['phone'],
                        'city' => $data['location'],
                        'playing_role' => $data['playing_role'],
                        'is_active' => true,
                    ]
                );

                return \App\Models\TournamentPlayer::updateOrCreate(
                    ['tournament_id' => $tournament->id, 'player_profile_id' => $profile->id],
                    [
                        'status' => 'approved',
                        'reviewed_by' => request()->user()->id,
                        'reviewed_at' => now(),
                        'review_notes' => 'Added manually directly to squad.',
                    ]
                );
            });

            // Ensure the player is not already assigned
            $alreadyAssigned = \App\Models\DraftPick::query()
                ->where('draft_id', $draft->id)
                ->where('tournament_player_id', $tournamentPlayer->id)
                ->exists();

            if ($alreadyAssigned) {
                return back()->with('status', 'Player created, but they are already assigned to a team.');
            }

            $nextPickNumber = ($draft->picks()->max('pick_number') ?? 0) + 1;

            \App\Models\DraftPick::create([
                'draft_id' => $draft->id,
                'draft_round_id' => $round->id,
                'team_id' => $team->id,
                'pick_number' => $nextPickNumber,
                'status' => 'selected',
                'tournament_player_id' => $tournamentPlayer->id,
                'selected_by' => $request->user()->id,
                'selected_at' => now(),
            ]);

            return back()->with('status', 'New player created and assigned to team squad successfully.');
        }

        return back()->withErrors(['status' => 'Invalid action.']);
    }

    public function removePlayer(Tournament $tournament, Team $team, \App\Models\DraftPick $draftPick): RedirectResponse
    {
        abort_unless($team->tournament_id === $tournament->id, 404);
        abort_unless($draftPick->team_id === $team->id, 404);

        $draftPick->delete();

        return back()->with('status', 'Player removed from team squad successfully.');
    }
}
