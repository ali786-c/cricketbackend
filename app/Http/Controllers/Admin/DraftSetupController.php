<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use App\Http\Requests\Admin\StoreDraftSetupRequest;
use App\Models\Draft;
use App\Models\DraftRound;
use App\Models\Tournament;
use Illuminate\Http\RedirectResponse;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\ValidationException;
use Illuminate\View\View;

class DraftSetupController extends Controller
{
    public function edit(Tournament $tournament): View
    {
        $draft = Draft::query()->firstOrCreate(
            ['tournament_id' => $tournament->id],
            ['status' => 'setup']
        );

        return view('admin.tournaments.draft-setup', [
            'tournament' => $tournament->load('teams'),
            'draft' => $draft->load('rounds.picks.team'),
            'isLocked' => $draft->status !== 'setup',
        ]);
    }

    public function update(StoreDraftSetupRequest $request, Tournament $tournament): RedirectResponse
    {
        $payload = $request->validated();
        $teamIds = $tournament->teams()->pluck('id')->all();
        $pickNumbers = [];

        foreach ($payload['rounds'] as $round) {
            foreach ($round['picks'] as $pick) {
                if (! in_array((int) $pick['team_id'], $teamIds, true)) {
                    throw ValidationException::withMessages([
                        'rounds' => 'Every assigned team must belong to this tournament.',
                    ]);
                }

                if (in_array($pick['pick_number'], $pickNumbers, true)) {
                    throw ValidationException::withMessages([
                        'rounds' => 'Pick numbers must be unique across the complete draft.',
                    ]);
                }

                $pickNumbers[] = $pick['pick_number'];
            }
        }

        DB::transaction(function () use ($payload, $tournament, $pickNumbers) {
            $draft = Draft::query()->firstOrCreate(
                ['tournament_id' => $tournament->id],
                ['status' => 'setup']
            );

            $draft = Draft::query()->lockForUpdate()->findOrFail($draft->id);

            if ($draft->status !== 'setup') {
                throw ValidationException::withMessages([
                    'draft' => 'Draft configuration cannot be changed after the draft starts.',
                ]);
            }

            $draft->picks()->delete();
            $draft->rounds()->delete();

            foreach ($payload['rounds'] as $roundPayload) {
                $round = $draft->rounds()->create([
                    'round_number' => $roundPayload['round_number'],
                    'name' => $roundPayload['name'] ?? null,
                    'status' => 'pending',
                ]);

                foreach ($roundPayload['picks'] as $pickPayload) {
                    $round->picks()->create([
                        'draft_id' => $draft->id,
                        'team_id' => $pickPayload['team_id'],
                        'pick_number' => $pickPayload['pick_number'],
                        'pick_duration' => $pickPayload['pick_duration'],
                        'status' => 'pending',
                    ]);
                }
            }

            $draft->update([
                'current_pick_number' => min($pickNumbers),
                'revision' => $draft->revision + 1,
            ]);
        });

        return redirect()
            ->route('admin.tournaments.show', $tournament)
            ->with('status', 'Draft rounds and pick assignments saved successfully.');
    }

    /**
     * API endpoint for draft setup — accepts JSON payload.
     */
    public function updateApi(\Illuminate\Http\Request $request, Tournament $tournament): \Illuminate\Http\JsonResponse
    {
        $data = $request->validate([
            'rounds' => ['required', 'array', 'min:1'],
            'rounds.*.round_number' => ['required', 'integer', 'min:1'],
            'rounds.*.name' => ['nullable', 'string', 'max:100'],
            'rounds.*.picks' => ['required', 'array', 'min:1'],
            'rounds.*.picks.*.team_id' => ['required', 'integer', 'exists:teams,id'],
            'rounds.*.picks.*.pick_number' => ['required', 'integer', 'min:1'],
            'rounds.*.picks.*.pick_duration' => ['nullable', 'integer', 'min:5', 'max:3600'],
        ]);

        $teamIds = $tournament->teams()->pluck('id')->all();
        $pickNumbers = [];
        $defaultDuration = $tournament->default_pick_duration ?? 60;

        foreach ($data['rounds'] as $round) {
            foreach ($round['picks'] as $pick) {
                if (! in_array((int) $pick['team_id'], $teamIds, true)) {
                    return response()->json(['message' => 'Every team must belong to this tournament.'], 422);
                }
                if (in_array($pick['pick_number'], $pickNumbers, true)) {
                    return response()->json(['message' => 'Pick numbers must be unique.'], 422);
                }
                $pickNumbers[] = $pick['pick_number'];
            }
        }

        DB::transaction(function () use ($data, $tournament, $pickNumbers, $defaultDuration) {
            $draft = Draft::query()->firstOrCreate(
                ['tournament_id' => $tournament->id],
                ['status' => 'setup']
            );
            $draft = Draft::query()->lockForUpdate()->findOrFail($draft->id);

            if ($draft->status !== 'setup') {
                return response()->json(['message' => 'Draft configuration cannot be changed after start.'], 422);
            }

            $draft->picks()->delete();
            $draft->rounds()->delete();

            foreach ($data['rounds'] as $roundPayload) {
                $round = $draft->rounds()->create([
                    'round_number' => $roundPayload['round_number'],
                    'name' => $roundPayload['name'] ?? null,
                    'status' => 'pending',
                ]);

                foreach ($roundPayload['picks'] as $pickPayload) {
                    $round->picks()->create([
                        'draft_id' => $draft->id,
                        'team_id' => $pickPayload['team_id'],
                        'pick_number' => $pickPayload['pick_number'],
                        'pick_duration' => $pickPayload['pick_duration'] ?? $defaultDuration,
                        'status' => 'pending',
                    ]);
                }
            }

            $draft->update([
                'current_pick_number' => min($pickNumbers),
                'revision' => $draft->revision + 1,
            ]);
        });

        return response()->json(['message' => 'Draft setup saved successfully.']);
    }

    public function downloadSample(Tournament $tournament): \Symfony\Component\HttpFoundation\StreamedResponse
    {
        $teams = $tournament->teams;
        $teamNames = $teams->pluck('name')->toArray();
        $sampleTeams = !empty($teamNames) ? $teamNames : ['Demo Team 1', 'Demo Team 2'];

        $headers = [
            'Content-Type' => 'text/csv',
            'Content-Disposition' => 'attachment; filename="draft_setup_sample.csv"',
        ];

        $callback = function () use ($sampleTeams) {
            $file = fopen('php://output', 'w');
            fputcsv($file, ['round_number', 'round_name', 'pick_number', 'team_name', 'pick_duration']);

            // Row 1
            fputcsv($file, [1, 'Round 1', 1, $sampleTeams[0], 120]);
            // Row 2
            fputcsv($file, [1, 'Round 1', 2, $sampleTeams[1] ?? $sampleTeams[0], 120]);
            // Row 3
            fputcsv($file, [2, 'Round 2', 3, $sampleTeams[1] ?? $sampleTeams[0], 120]);
            // Row 4
            fputcsv($file, [2, 'Round 2', 4, $sampleTeams[0], 120]);

            fclose($file);
        };

        return response()->stream($callback, 200, $headers);
    }

    public function importCsv(\Illuminate\Http\Request $request, Tournament $tournament): RedirectResponse
    {
        $request->validate([
            'csv_file' => ['required', 'file', 'mimes:csv,txt', 'max:2048'],
        ]);

        $draft = Draft::query()->firstOrCreate(
            ['tournament_id' => $tournament->id],
            ['status' => 'setup']
        );

        if ($draft->status !== 'setup') {
            return back()->withErrors(['csv_file' => 'Draft configuration cannot be changed after the draft starts.']);
        }

        $file = $request->file('csv_file');
        $filePath = $file->getRealPath();

        $rows = [];
        $pickNumbers = [];
        $rowNumber = 1;

        if (($handle = fopen($filePath, 'r')) !== false) {
            $header = fgetcsv($handle);
            if (!$header || count($header) < 4) {
                fclose($handle);
                return back()->withErrors(['csv_file' => 'Invalid CSV format. Missing required columns.']);
            }

            $headerMap = array_flip(array_map('trim', $header));
            $requiredColumns = ['round_number', 'pick_number', 'team_name'];
            foreach ($requiredColumns as $col) {
                if (!isset($headerMap[$col])) {
                    fclose($handle);
                    return back()->withErrors(['csv_file' => "Required column '{$col}' is missing in the CSV header."]);
                }
            }

            $teams = $tournament->teams;
            $teamLookup = [];
            foreach ($teams as $team) {
                $teamLookup[strtolower(trim($team->name))] = $team->id;
                if ($team->short_name) {
                    $teamLookup[strtolower(trim($team->short_name))] = $team->id;
                }
            }

            $defaultDuration = $tournament->default_pick_duration ?: 120;

            while (($data = fgetcsv($handle)) !== false) {
                $rowNumber++;
                
                $roundNum = isset($headerMap['round_number']) ? trim($data[$headerMap['round_number']] ?? '') : '';
                $roundName = isset($headerMap['round_name']) ? trim($data[$headerMap['round_name']] ?? '') : '';
                $pickNum = isset($headerMap['pick_number']) ? trim($data[$headerMap['pick_number']] ?? '') : '';
                $teamName = isset($headerMap['team_name']) ? trim($data[$headerMap['team_name']] ?? '') : '';
                $duration = isset($headerMap['pick_duration']) ? trim($data[$headerMap['pick_duration']] ?? '') : '';

                if ($roundNum === '' && $pickNum === '' && $teamName === '') {
                    continue;
                }

                if (!is_numeric($roundNum) || (int) $roundNum < 1) {
                    fclose($handle);
                    return back()->withErrors(['csv_file' => "Row {$rowNumber}: 'round_number' must be a valid positive integer."]);
                }

                if (!is_numeric($pickNum) || (int) $pickNum < 1) {
                    fclose($handle);
                    return back()->withErrors(['csv_file' => "Row {$rowNumber}: 'pick_number' must be a valid positive integer."]);
                }

                $pickNumInt = (int) $pickNum;
                if (in_array($pickNumInt, $pickNumbers, true)) {
                    fclose($handle);
                    return back()->withErrors(['csv_file' => "Row {$rowNumber}: Duplicate pick number '{$pickNumInt}'."]);
                }
                $pickNumbers[] = $pickNumInt;

                $lookupKey = strtolower($teamName);
                if (!isset($teamLookup[$lookupKey])) {
                    fclose($handle);
                    return back()->withErrors(['csv_file' => "Row {$rowNumber}: Team '{$teamName}' not found in this tournament."]);
                }
                $teamId = $teamLookup[$lookupKey];

                $pickDuration = is_numeric($duration) ? (int) $duration : $defaultDuration;
                if ($pickDuration < 5 || $pickDuration > 3600) {
                    fclose($handle);
                    return back()->withErrors(['csv_file' => "Row {$rowNumber}: 'pick_duration' must be between 5 and 3600 seconds."]);
                }

                $rows[] = [
                    'round_number' => (int) $roundNum,
                    'round_name' => $roundName ?: "Round {$roundNum}",
                    'pick_number' => $pickNumInt,
                    'team_id' => $teamId,
                    'pick_duration' => $pickDuration,
                ];
            }
            fclose($handle);
        }

        if (empty($rows)) {
            return back()->withErrors(['csv_file' => 'The uploaded CSV file contains no data rows.']);
        }

        DB::transaction(function () use ($rows, $draft, $pickNumbers) {
            $draft->picks()->delete();
            $draft->rounds()->delete();

            $roundsGrouped = [];
            foreach ($rows as $row) {
                $roundsGrouped[$row['round_number']]['name'] = $row['round_name'];
                $roundsGrouped[$row['round_number']]['picks'][] = [
                    'pick_number' => $row['pick_number'],
                    'team_id' => $row['team_id'],
                    'pick_duration' => $row['pick_duration'],
                ];
            }

            ksort($roundsGrouped);

            foreach ($roundsGrouped as $roundNum => $roundData) {
                $round = $draft->rounds()->create([
                    'round_number' => $roundNum,
                    'name' => $roundData['name'] ?? null,
                    'status' => 'pending',
                ]);

                foreach ($roundData['picks'] as $pickData) {
                    $round->picks()->create([
                        'draft_id' => $draft->id,
                        'pick_number' => $pickData['pick_number'],
                        'team_id' => $pickData['team_id'],
                        'pick_duration' => $pickData['pick_duration'],
                        'status' => 'pending',
                    ]);
                }
            }

            $draft->update([
                'current_pick_number' => min($pickNumbers),
                'revision' => $draft->revision + 1,
            ]);
        });

        return back()->with('status', 'Draft order setup imported successfully via CSV.');
    }

    public function pdf(Tournament $tournament): \Illuminate\Http\Response
    {
        $draft = Draft::query()->where('tournament_id', $tournament->id)->firstOrFail();
        $draft->load('rounds.picks.team');

        $logoDataUri = null;
        if ($tournament->logo_path && \Illuminate\Support\Facades\Storage::disk('public')->exists($tournament->logo_path)) {
            $mime = \Illuminate\Support\Facades\Storage::disk('public')->mimeType($tournament->logo_path) ?: 'image/png';
            $logoDataUri = 'data:'.$mime.';base64,'.base64_encode(\Illuminate\Support\Facades\Storage::disk('public')->get($tournament->logo_path));
        }

        $html = view('reports.draft-setup-pdf', [
            'tournament' => $tournament,
            'draft' => $draft,
            'logoDataUri' => $logoDataUri,
            'title' => 'Draft Rounds & Picks Config',
        ])->render();

        $options = new \Dompdf\Options();
        $options->set('isRemoteEnabled', false);
        $dompdf = new \Dompdf\Dompdf($options);
        $dompdf->loadHtml($html);
        $dompdf->setPaper('A4', 'portrait');
        $dompdf->render();

        return response($dompdf->output(), 200, [
            'Content-Type' => 'application/pdf',
            'Content-Disposition' => 'attachment; filename="' . str()->slug($tournament->slug . '-draft-setup') . '.pdf"',
        ]);
    }
}
