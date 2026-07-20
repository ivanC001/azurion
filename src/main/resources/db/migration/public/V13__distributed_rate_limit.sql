CREATE TABLE IF NOT EXISTS public.request_rate_limit_events (
    id BIGSERIAL PRIMARY KEY,
    bucket_key VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_request_rate_limit_events_bucket_time
    ON public.request_rate_limit_events(bucket_key, occurred_at);

CREATE INDEX IF NOT EXISTS idx_request_rate_limit_events_time
    ON public.request_rate_limit_events(occurred_at);
