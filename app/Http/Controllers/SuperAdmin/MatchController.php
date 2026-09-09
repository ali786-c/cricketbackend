<?php

namespace App\Http\Controllers\SuperAdmin;

use App\Http\Controllers\Controller;
use App\Models\CricketMatch;
use Illuminate\Http\Request;
use Illuminate\View\View;

class MatchController extends Controller
{
    public function index(Request $request): View
    {
        $query = CricketMatch::with(['tournament', 'fixture.homeTeam', 'fixture.awayTeam'])->latest();

        if ($status = $request->string('status')->toString()) {
            $query->where('status', $status);
        }

        return view('super-admin.matches.index', [
            'matches' => $query->paginate(20)->withQueryString(),
            'selectedStatus' => $request->string('status')->toString(),
            'statuses' => ['scheduled', 'toss_pending', 'live', 'completed', 'abandoned', 'cancelled'],
            'statusCounts' => CricketMatch::query()->selectRaw('status, COUNT(*) as total')->groupBy('status')->pluck('total', 'status'),
        ]);
    }
}
