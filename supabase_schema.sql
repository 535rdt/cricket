-- ========================================================
-- 🏏 CRICKET SCOREBOARD - SUPABASE DATABASE SCHEMA
-- ========================================================
-- Instructions:
-- 1. Open your Supabase Dashboard: https://supabase.com/dashboard
-- 2. Navigate to SQL Editor -> "New Query"
-- 3. Paste this entire script and click "Run"
-- ========================================================

-- 1. Table for Real-time Live Scoreboard Sharing
CREATE TABLE IF NOT EXISTS public.live_matches (
    match_code TEXT PRIMARY KEY,
    match_id TEXT NOT NULL,
    team_a_name TEXT,
    team_b_name TEXT,
    match_data JSONB NOT NULL,
    last_updated TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Table for Cloud Saved Teams Library
CREATE TABLE IF NOT EXISTS public.saved_teams (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    players JSONB NOT NULL,
    player_count INT DEFAULT 11,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Table for Match History & Scorecard Archives
CREATE TABLE IF NOT EXISTS public.match_history (
    id TEXT PRIMARY KEY,
    date TEXT,
    team_a_name TEXT NOT NULL,
    team_b_name TEXT NOT NULL,
    result_message TEXT,
    match_data JSONB NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enable Row Level Security (RLS) on all tables
ALTER TABLE public.live_matches ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.saved_teams ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.match_history ENABLE ROW LEVEL SECURITY;

-- Allow anonymous access via Supabase public anon key
DROP POLICY IF EXISTS "Public full access to live_matches" ON public.live_matches;
CREATE POLICY "Public full access to live_matches"
    ON public.live_matches FOR ALL
    TO anon, authenticated
    USING (true)
    WITH CHECK (true);

DROP POLICY IF EXISTS "Public full access to saved_teams" ON public.saved_teams;
CREATE POLICY "Public full access to saved_teams"
    ON public.saved_teams FOR ALL
    TO anon, authenticated
    USING (true)
    WITH CHECK (true);

DROP POLICY IF EXISTS "Public full access to match_history" ON public.match_history;
CREATE POLICY "Public full access to match_history"
    ON public.match_history FOR ALL
    TO anon, authenticated
    USING (true)
    WITH CHECK (true);

-- Enable Supabase Realtime broadcast on live_matches
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' 
        AND schemaname = 'public' 
        AND tablename = 'live_matches'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.live_matches;
    END IF;
END $$;

-- Indexes for lightning fast lookups
CREATE INDEX IF NOT EXISTS idx_live_matches_code ON public.live_matches (match_code);
CREATE INDEX IF NOT EXISTS idx_match_history_date ON public.match_history (created_at DESC);
