<?php

namespace App\Http\Controllers\SuperAdmin;

use App\Http\Controllers\Controller;
use App\Models\Fixture;
use Illuminate\Http\Request;
use Illuminate\View\View;

class FixtureController extends Controller
{
    public function index(Request $request): View
    {
        $query = Fixture::query()
            ->with(['homeTeam', 'awayTeam', 'tournament', 'match'])
            ->orderByDesc('scheduled_at');

        if ($status = $request->string('status')->toString()) {
            $query->where('status', $status);
        }

        if ($search = trim((string) $request->string('search'))) {
            $query->where(function ($builder) use ($search) {
                $builder
                    ->whereHas('homeTeam', fn ($q) => $q->where('name', 'like', "%{$search}%"))
                    ->orWhereHas('awayTeam', fn ($q) => $q->where('name', 'like', "%{$search}%"))
                    ->orWhere('venue', 'like', "%{$search}%");
            });
        }

        return view('super-admin.fixtures.index', [
            'fixtures' => $query->paginate(20)->withQueryString(),
            'search' => $request->string('search')->toString(),
            'selectedStatus' => $request->string('status')->toString(),
            'statuses' => ['scheduled', 'in_progress', 'postponed', 'completed', 'cancelled'],
        ]);
    }
}
