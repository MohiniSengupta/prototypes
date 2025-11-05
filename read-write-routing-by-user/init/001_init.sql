-- Create demo table and seed rows on primary at init time
CREATE TABLE IF NOT EXISTS messages (
    id SERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO messages (user_id, content)
VALUES
    ('userC', 'hello from C'),
    ('userX', 'hello from X')
ON CONFLICT DO NOTHING;

