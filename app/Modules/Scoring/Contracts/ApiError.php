<?php

namespace App\Modules\Scoring\Contracts;

final readonly class ApiError
{
    public function __construct(
        public string $code,
        public string $message,
        public array $fieldErrors = [],
        public bool $retryable = false,
        public ?int $currentRevision = null,
        public ?string $correlationId = null,
    ) {
    }

    public function toArray(): array
    {
        return [
            'code' => $this->code,
            'message' => $this->message,
            'field_errors' => $this->fieldErrors,
            'retryable' => $this->retryable,
            'current_revision' => $this->currentRevision,
            'correlation_id' => $this->correlationId,
        ];
    }
}
