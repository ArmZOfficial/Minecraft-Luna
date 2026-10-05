BEGIN;
-- DEV data cannot satisfy a live order or enqueue a real Minecraft delivery.
CREATE TABLE IF NOT EXISTS dev_payment_sessions (
 token_hash text PRIMARY KEY, expires_at timestamptz NOT NULL DEFAULT now()+interval '24 hours'
);
CREATE TABLE IF NOT EXISTS dev_payment_orders (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), session_hash text NOT NULL REFERENCES dev_payment_sessions(token_hash),
 idempotency_key uuid NOT NULL, product_id text NOT NULL,
 amount_minor integer NOT NULL CHECK(amount_minor>0), currency text NOT NULL DEFAULT 'THB' CHECK(currency='THB'),
 mode text NOT NULL DEFAULT 'mock' CHECK(mode='mock'), snapshot jsonb NOT NULL,
 status text NOT NULL DEFAULT 'pending' CHECK(status IN ('pending','paid','failed','expired','cancelled')),
 created_at timestamptz NOT NULL DEFAULT now(), expires_at timestamptz NOT NULL DEFAULT now()+interval '10 minutes',
 paid_at timestamptz, UNIQUE(session_hash,idempotency_key)
);
CREATE TABLE IF NOT EXISTS dev_payment_receipts (
 order_id uuid PRIMARY KEY REFERENCES dev_payment_orders(id), snapshot jsonb NOT NULL,
 state text NOT NULL DEFAULT 'simulated-mailbox' CHECK(state='simulated-mailbox'), created_at timestamptz NOT NULL DEFAULT now()
);
INSERT INTO schema_migrations(version) VALUES ('002_dev_payments') ON CONFLICT DO NOTHING;
COMMIT;
