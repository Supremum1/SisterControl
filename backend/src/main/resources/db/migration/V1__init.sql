CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY,
  email TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  role TEXT NOT NULL CHECK (role IN ('parent','child')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sessions (
  token TEXT PRIMARY KEY,
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  expires_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS pairing_codes (
  code TEXT PRIMARY KEY,
  child_user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  expires_at TIMESTAMPTZ NOT NULL,
  used BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS connections (
  id UUID PRIMARY KEY,
  parent_user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  child_user_id UUID UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS child_events (
  id UUID PRIMARY KEY,
  parent_user_id UUID NOT NULL,
  child_user_id UUID NOT NULL,
  event_type TEXT NOT NULL,
  message TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_child_events_parent_time
  ON child_events(parent_user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS child_commands (
  id UUID PRIMARY KEY,
  child_user_id UUID NOT NULL,
  command_type TEXT NOT NULL,
  payload JSONB,
  status TEXT NOT NULL DEFAULT 'pending',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_child_commands_child_status_time
  ON child_commands(child_user_id, status, created_at DESC);

CREATE TABLE IF NOT EXISTS child_locations (
  child_user_id UUID PRIMARY KEY,
  parent_user_id UUID NOT NULL,
  lat DOUBLE PRECISION NOT NULL,
  lon DOUBLE PRECISION NOT NULL,
  accuracy DOUBLE PRECISION,
  provider TEXT,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_child_locations_parent_time
  ON child_locations(parent_user_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS screen_time_policies (
  id UUID PRIMARY KEY,
  parent_user_id UUID NOT NULL,
  child_user_id UUID NOT NULL UNIQUE,
  total_limit_min INT NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS screen_time_rules (
  id UUID PRIMARY KEY,
  policy_id UUID NOT NULL REFERENCES screen_time_policies(id) ON DELETE CASCADE,
  package_name TEXT NOT NULL,
  limit_min INT NOT NULL DEFAULT 0,
  UNIQUE(policy_id, package_name)
);

CREATE INDEX IF NOT EXISTS idx_screen_time_rules_policy
  ON screen_time_rules(policy_id);

CREATE TABLE IF NOT EXISTS screen_time_usage (
  id UUID PRIMARY KEY,
  child_user_id UUID NOT NULL,
  day DATE NOT NULL,
  package_name TEXT NOT NULL,
  used_sec INT NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(child_user_id, day, package_name)
);

CREATE INDEX IF NOT EXISTS idx_screen_time_usage_child_day
  ON screen_time_usage(child_user_id, day);

CREATE TABLE IF NOT EXISTS trust_letters (
  id UUID PRIMARY KEY,
  parent_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  child_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  message TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  is_read BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_trust_letters_parent_created
  ON trust_letters(parent_user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS parent_messages (
  id UUID PRIMARY KEY,
  parent_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  child_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  message TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  is_read BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_parent_messages_child_created
  ON parent_messages(child_user_id, created_at DESC);
