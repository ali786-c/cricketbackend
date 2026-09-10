<?php

namespace App\Modules\Scoring\Contracts;

enum MatchLifecycle: string
{
    case LocalDraft = 'local_draft';
    case PendingSync = 'pending_sync';
    case Scheduled = 'scheduled';
    case SquadSelection = 'squad_selection';
    case LineupPending = 'lineup_pending';
    case TossPending = 'toss_pending';
    case Live = 'live';
    case InningsBreak = 'innings_break';
    case Completed = 'completed';
    case ResultPending = 'result_pending';
    case Approved = 'approved';
    case Rejected = 'rejected';
    case Abandoned = 'abandoned';
    case Cancelled = 'cancelled';
}
