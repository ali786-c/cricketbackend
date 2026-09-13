<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\CricketRuleProfile;
use Illuminate\Http\JsonResponse;

class RuleProfileController extends Controller
{
    public function index(): JsonResponse
    {
        return response()->json([
            'data' => CricketRuleProfile::query()
                ->where('is_active', true)
                ->orderByDesc('is_system')
                ->orderBy('name')
                ->get(),
        ]);
    }
}
