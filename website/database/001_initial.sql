BEGIN;
CREATE TABLE IF NOT EXISTS schema_migrations(version text PRIMARY KEY, applied_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE IF NOT EXISTS players (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), minecraft_uuid uuid UNIQUE NOT NULL,
 username varchar(16) NOT NULL CHECK(username ~ '^[A-Za-z0-9_]{3,16}$'),
 public_profile boolean NOT NULL DEFAULT false, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS sessions (
 token_hash text PRIMARY KEY, player_id uuid NOT NULL REFERENCES players(id) ON DELETE CASCADE,
 expires_at timestamptz NOT NULL, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS news_posts (
 id text PRIMARY KEY, title text NOT NULL, intro text NOT NULL, body jsonb NOT NULL CHECK(jsonb_typeof(body)='array'),
 category text NOT NULL, image text NOT NULL, published boolean NOT NULL DEFAULT false,
 published_at timestamptz, created_at timestamptz NOT NULL DEFAULT now(),
 CHECK(NOT published OR published_at IS NOT NULL)
);
CREATE TABLE IF NOT EXISTS products (
 id text PRIMARY KEY, name text NOT NULL, description text NOT NULL, type text NOT NULL DEFAULT 'ของตกแต่ง',
 price_minor integer NOT NULL CHECK(price_minor>0), currency char(3) NOT NULL DEFAULT 'THB', image text NOT NULL,
 game_item_id text NOT NULL, catalog_version integer NOT NULL DEFAULT 1 CHECK(catalog_version>0),
 active boolean NOT NULL DEFAULT false, cosmetic_only boolean NOT NULL DEFAULT true
);
CREATE TABLE IF NOT EXISTS orders (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), player_id uuid NOT NULL REFERENCES players(id),
 idempotency_key text UNIQUE NOT NULL, amount_minor bigint NOT NULL CHECK(amount_minor>0),
 currency char(3) NOT NULL DEFAULT 'THB', status text NOT NULL DEFAULT 'pending'
 CHECK(status IN ('pending','paid','cancelled','refund_pending','refunded','review')),
 created_at timestamptz NOT NULL DEFAULT now(), paid_at timestamptz
);
CREATE TABLE IF NOT EXISTS order_lines (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), order_id uuid NOT NULL REFERENCES orders(id),
 product_id text NOT NULL REFERENCES products(id), catalog_version integer NOT NULL,
 quantity integer NOT NULL CHECK(quantity BETWEEN 1 AND 99),
 unit_price_minor integer NOT NULL CHECK(unit_price_minor>0),
 fulfillment_snapshot jsonb NOT NULL, UNIQUE(order_id,product_id)
);
CREATE TABLE IF NOT EXISTS payment_events (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), provider text NOT NULL, provider_event_id text NOT NULL,
 order_id uuid REFERENCES orders(id), verified_at timestamptz NOT NULL DEFAULT now(),
 payload_digest text NOT NULL, processed_at timestamptz, UNIQUE(provider,provider_event_id)
);
CREATE TABLE IF NOT EXISTS delivery_outbox (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), order_line_id uuid UNIQUE NOT NULL REFERENCES order_lines(id),
 recipient_uuid uuid NOT NULL, payload jsonb NOT NULL, state text NOT NULL DEFAULT 'pending'
 CHECK(state IN ('pending','leased','acknowledged','mailbox','review')),
 attempts integer NOT NULL DEFAULT 0 CHECK(attempts>=0), available_at timestamptz NOT NULL DEFAULT now(),
 lease_until timestamptz, acknowledged_at timestamptz, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS delivery_pending ON delivery_outbox(available_at) WHERE state='pending';
CREATE TABLE IF NOT EXISTS audit_log (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), actor_id uuid REFERENCES players(id),
 action text NOT NULL, target text NOT NULL, request_id uuid NOT NULL,
 metadata jsonb NOT NULL DEFAULT '{}', created_at timestamptz NOT NULL DEFAULT now()
);
INSERT INTO schema_migrations(version) VALUES ('001_initial') ON CONFLICT DO NOTHING;
COMMIT;
