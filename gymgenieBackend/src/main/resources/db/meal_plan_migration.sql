-- ============================================================
-- Meal plan / AI nutrition migration
--
-- The application uses JPA with `ddl-auto: update`, so on a fresh dev
-- environment Hibernate will create these tables itself from the entity
-- definitions. This script is the authoritative reference for production
-- deployments where JPA auto-DDL is not appropriate, and it adds indexes
-- that Hibernate would not produce automatically.
--
-- PREREQUISITE: gen_random_uuid() is built-in in Postgres 13+.
-- For Postgres 12 or earlier:
--   CREATE EXTENSION IF NOT EXISTS pgcrypto;
-- ============================================================

-- Drop legacy meal-plan tables from the old day-based / item-based schema
-- if they happen to exist in a non-fresh environment. Safe to no-op when
-- starting from a clean DB.
DROP TABLE IF EXISTS meal_items CASCADE;
DROP TABLE IF EXISTS meals CASCADE;
DROP TABLE IF EXISTS meal_plan_days CASCADE;
DROP TABLE IF EXISTS meal_plans CASCADE;

-- ============================================================
-- meal_plans
-- ============================================================
CREATE TABLE meal_plans (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    goal            VARCHAR(32),
    total_calories  INTEGER,
    created_by      VARCHAR(32) NOT NULL,
    schedule_type   VARCHAR(20),
    one_off_date    DATE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_meal_plans_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_meal_plans_user_id ON meal_plans(user_id);
CREATE INDEX idx_meal_plans_user_created_at ON meal_plans(user_id, created_at DESC);

-- ============================================================
-- meal_plan_schedule_days (recurring weekday bindings, @ElementCollection)
-- ============================================================
CREATE TABLE meal_plan_schedule_days (
    meal_plan_id    UUID NOT NULL,
    day_of_week     VARCHAR(16) NOT NULL,
    PRIMARY KEY (meal_plan_id, day_of_week),
    CONSTRAINT fk_meal_plan_schedule_days_meal_plan
        FOREIGN KEY (meal_plan_id) REFERENCES meal_plans(id) ON DELETE CASCADE
);

CREATE INDEX idx_meal_plan_schedule_days_plan ON meal_plan_schedule_days(meal_plan_id);

-- ============================================================
-- meals
-- ============================================================
CREATE TABLE meals (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meal_plan_id        UUID NOT NULL,
    meal_type           VARCHAR(32) NOT NULL,
    name                VARCHAR(100) NOT NULL,
    estimated_calories  INTEGER,
    CONSTRAINT fk_meals_meal_plan
        FOREIGN KEY (meal_plan_id) REFERENCES meal_plans(id) ON DELETE CASCADE
);

CREATE INDEX idx_meals_meal_plan_id ON meals(meal_plan_id);

-- ============================================================
-- dishes
-- ============================================================
CREATE TABLE dishes (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meal_id              UUID NOT NULL,
    name                 VARCHAR(150) NOT NULL,
    description          VARCHAR(500),
    portion_description  VARCHAR(50),
    calories             INTEGER,
    protein_g            INTEGER,
    carbs_g              INTEGER,
    fat_g                INTEGER,
    food_product_id      UUID,
    grams                DOUBLE PRECISION,
    CONSTRAINT fk_dishes_meal
        FOREIGN KEY (meal_id) REFERENCES meals(id) ON DELETE CASCADE
);

CREATE INDEX idx_dishes_meal_id ON dishes(meal_id);

-- ============================================================
-- Backfill columns for environments where the table already exists
-- (Hibernate ddl-auto: update should have added them, but this ensures
-- they are present regardless of JPA auto-DDL configuration.)
-- ============================================================
ALTER TABLE dishes ADD COLUMN IF NOT EXISTS food_product_id UUID;
ALTER TABLE dishes ADD COLUMN IF NOT EXISTS grams DOUBLE PRECISION;
