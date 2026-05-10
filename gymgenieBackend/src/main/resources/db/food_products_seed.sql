-- ============================================================
-- Food Products Catalog — schema reference + seed data
--
-- Schema reference:
--   The `food_products` table itself is created automatically by Hibernate
--   (spring.jpa.hibernate.ddl-auto=update). The CREATE TABLE / ALTER TABLE
--   statements below are kept here only as a reference of the expected
--   schema and as an idempotent safety-net to make this script runnable
--   on a fresh database without the application started yet.
--
-- Seed data:
--   ~173 products across all FoodCategory values, with realistic per-100g
--   nutritional values. Every INSERT uses ON CONFLICT (name_ru) DO NOTHING
--   so the script is safe to re-run.
--
-- PREREQUISITE: gen_random_uuid() is built-in in Postgres 13+.
-- For Postgres 12 or earlier, uncomment the line below:
-- CREATE EXTENSION IF NOT EXISTS pgcrypto;
-- ============================================================

-- ============================================================
-- STEP 1 (safety-net): table & columns (no-op if Hibernate already ran)
-- ============================================================

CREATE TABLE IF NOT EXISTS food_products (
    id                  UUID PRIMARY KEY,
    name_ru             VARCHAR(150)     NOT NULL,
    name_en             VARCHAR(150),
    category            VARCHAR(32)      NOT NULL,
    emoji               VARCHAR(10),
    calories_per100g    DOUBLE PRECISION NOT NULL,
    protein_per100g     DOUBLE PRECISION NOT NULL,
    fat_per100g         DOUBLE PRECISION NOT NULL,
    carbs_per100g       DOUBLE PRECISION NOT NULL,
    fiber_per100g       DOUBLE PRECISION,
    sugar_per100g       DOUBLE PRECISION,
    is_active           BOOLEAN          NOT NULL DEFAULT TRUE
);

-- Unique key on name_ru is required for the ON CONFLICT clause below.
CREATE UNIQUE INDEX IF NOT EXISTS ux_food_products_name_ru
    ON food_products (name_ru);

CREATE INDEX IF NOT EXISTS ix_food_products_category
    ON food_products (category);

CREATE INDEX IF NOT EXISTS ix_food_products_is_active
    ON food_products (is_active);

-- ============================================================
-- STEP 2 (reference): meal_items reference to catalog
--   Hibernate ddl-auto=update will normally add these columns automatically,
--   but they are listed here for manual reference / older environments.
-- ============================================================

ALTER TABLE meal_items
    ADD COLUMN IF NOT EXISTS food_product_id UUID;

ALTER TABLE meal_items
    ADD COLUMN IF NOT EXISTS amount_grams DOUBLE PRECISION;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_meal_items_food_product'
    ) THEN
        ALTER TABLE meal_items
            ADD CONSTRAINT fk_meal_items_food_product
            FOREIGN KEY (food_product_id) REFERENCES food_products (id);
    END IF;
END$$;

CREATE INDEX IF NOT EXISTS ix_meal_items_food_product_id
    ON meal_items (food_product_id);

-- ============================================================
-- STEP 3: Seed data
-- ============================================================

INSERT INTO food_products (
    id, name_ru, name_en, category, emoji,
    calories_per100g, protein_per100g, fat_per100g, carbs_per100g,
    fiber_per100g, sugar_per100g, is_active
) VALUES

-- ============================================================
-- MEAT (мясо и птица)
-- ============================================================
(gen_random_uuid(), 'Куриная грудка',           'Chicken breast',         'MEAT', '🍗', 165, 31.0,  3.6,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Куриное бедро',             'Chicken thigh',          'MEAT', '🍗', 215, 26.0, 12.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Куриная голень',            'Chicken drumstick',      'MEAT', '🍗', 185, 27.0,  9.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Говядина варёная',          'Boiled beef',            'MEAT', '🥩', 254, 30.0, 15.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Говяжий фарш',              'Ground beef',            'MEAT', '🥩', 250, 26.0, 17.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Свинина (лопатка)',         'Pork shoulder',          'MEAT', '🥩', 260, 21.0, 19.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Свиная вырезка',            'Pork tenderloin',        'MEAT', '🥩', 143, 22.0,  6.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Индейка (грудка)',          'Turkey breast',          'MEAT', '🦃', 153, 34.0,  1.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Индейка (бедро)',           'Turkey thigh',           'MEAT', '🦃', 208, 28.0, 11.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Баранина',                  'Lamb',                   'MEAT', '🥩', 294, 25.0, 21.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Кролик',                    'Rabbit',                 'MEAT', '🐇', 183, 21.0, 11.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Утка',                      'Duck',                   'MEAT', '🦆', 337, 19.0, 29.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Телятина',                  'Veal',                   'MEAT', '🥩', 172, 27.0,  7.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Печень говяжья',            'Beef liver',             'MEAT', '🥩', 125, 18.0,  4.0,  4.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Куриная печень',            'Chicken liver',          'MEAT', '🍗', 140, 19.0,  6.0,  1.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Сердце говяжье',            'Beef heart',             'MEAT', '🥩', 101, 16.0,  4.0,  0.1, NULL, NULL, TRUE),
(gen_random_uuid(), 'Колбаса варёная Докторская','Doctor''s sausage',      'MEAT', '🌭', 257, 13.0, 22.0,  2.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Сосиски молочные',          'Milk sausages',          'MEAT', '🌭', 266, 11.0, 24.0,  1.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Бекон',                     'Bacon',                  'MEAT', '🥓', 541, 37.0, 42.0,  1.4, NULL, NULL, TRUE),
(gen_random_uuid(), 'Ветчина',                   'Ham',                    'MEAT', '🥓', 188, 22.0, 11.0,  0.0, NULL, NULL, TRUE),

-- ============================================================
-- FISH (рыба и морепродукты)
-- ============================================================
(gen_random_uuid(), 'Лосось атлантический',      'Atlantic salmon',        'FISH', '🐟', 208, 20.0, 13.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Форель',                    'Trout',                  'FISH', '🐟', 148, 19.0,  8.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Треска',                    'Cod',                    'FISH', '🐟',  82, 18.0,  0.7,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Тунец в собственном соку',  'Tuna in own juice',      'FISH', '🐟',  96, 23.0,  0.5,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Скумбрия',                  'Mackerel',               'FISH', '🐟', 191, 18.0, 13.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Сельдь',                    'Herring',                'FISH', '🐟', 248, 15.0, 21.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Горбуша',                   'Pink salmon',            'FISH', '🐟', 142, 22.0,  6.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Минтай',                    'Pollock',                'FISH', '🐟',  72, 16.0,  1.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Судак',                     'Pikeperch',              'FISH', '🐟',  83, 19.0,  1.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Карп',                      'Carp',                   'FISH', '🐟', 121, 18.0,  5.5,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Мидии',                     'Mussels',                'FISH', '🦪',  86, 12.0,  2.0,  4.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Креветки',                  'Shrimp',                 'FISH', '🦐',  97, 19.0,  2.0,  0.9, NULL, NULL, TRUE),
(gen_random_uuid(), 'Кальмар',                   'Squid',                  'FISH', '🦑',  92, 18.0,  1.4,  2.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Краб',                      'Crab',                   'FISH', '🦀',  97, 19.0,  1.8,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Осьминог',                  'Octopus',                'FISH', '🐙',  82, 15.0,  1.0,  2.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Икра красная',              'Red caviar',             'FISH', '🍣', 264, 31.0, 14.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Сёмга слабосолёная',        'Lightly salted salmon',  'FISH', '🐟', 202, 23.0, 12.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Тилапия',                   'Tilapia',                'FISH', '🐟',  96, 20.0,  2.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Пикша',                     'Haddock',                'FISH', '🐟',  74, 17.0,  0.6,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Морской окунь',             'Sea bass',               'FISH', '🐟', 103, 18.0,  3.4,  0.0, NULL, NULL, TRUE),

-- ============================================================
-- DAIRY (молочные)
-- ============================================================
(gen_random_uuid(), 'Молоко 2.5%',                'Milk 2.5%',              'DAIRY', '🥛',  54,  2.8,  2.5,  4.7, NULL, 4.7, TRUE),
(gen_random_uuid(), 'Молоко 3.2%',                'Milk 3.2%',              'DAIRY', '🥛',  60,  2.8,  3.2,  4.7, NULL, 4.7, TRUE),
(gen_random_uuid(), 'Кефир 1%',                   'Kefir 1%',               'DAIRY', '🥛',  40,  3.0,  1.0,  4.0, NULL, 4.0, TRUE),
(gen_random_uuid(), 'Кефир 2.5%',                 'Kefir 2.5%',             'DAIRY', '🥛',  53,  2.9,  2.5,  4.0, NULL, 4.0, TRUE),
(gen_random_uuid(), 'Творог 0%',                  'Cottage cheese 0%',      'DAIRY', '🧀',  79, 18.0,  0.0,  3.3, NULL, 3.3, TRUE),
(gen_random_uuid(), 'Творог 5%',                  'Cottage cheese 5%',      'DAIRY', '🧀', 121, 17.0,  5.0,  3.0, NULL, 3.0, TRUE),
(gen_random_uuid(), 'Творог 9%',                  'Cottage cheese 9%',      'DAIRY', '🧀', 159, 16.0,  9.0,  2.8, NULL, 2.8, TRUE),
(gen_random_uuid(), 'Сметана 10%',                'Sour cream 10%',         'DAIRY', '🥛', 116,  3.0, 10.0,  4.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Сметана 20%',                'Sour cream 20%',         'DAIRY', '🥛', 206,  2.8, 20.0,  3.7, NULL, NULL, TRUE),
(gen_random_uuid(), 'Йогурт натуральный 1.5%',    'Natural yogurt 1.5%',    'DAIRY', '🥛',  47,  4.3,  1.5,  3.8, NULL, 3.8, TRUE),
(gen_random_uuid(), 'Йогурт греческий',           'Greek yogurt',           'DAIRY', '🥛',  97,  9.0,  5.0,  3.8, NULL, 3.8, TRUE),
(gen_random_uuid(), 'Сыр Российский',             'Russian cheese',         'DAIRY', '🧀', 363, 23.0, 30.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Сыр Адыгейский',             'Adyghe cheese',          'DAIRY', '🧀', 264, 19.0, 20.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Сыр моцарелла',              'Mozzarella',             'DAIRY', '🧀', 280, 22.0, 22.0,  2.2, NULL, NULL, TRUE),
(gen_random_uuid(), 'Сыр пармезан',               'Parmesan',               'DAIRY', '🧀', 431, 38.0, 29.0,  4.1, NULL, NULL, TRUE),
(gen_random_uuid(), 'Сыр фета',                   'Feta',                   'DAIRY', '🧀', 264, 14.0, 21.0,  4.1, NULL, NULL, TRUE),
(gen_random_uuid(), 'Сыр Голландский',            'Dutch cheese',           'DAIRY', '🧀', 352, 26.0, 27.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Масло сливочное 72%',        'Butter 72%',             'DAIRY', '🧈', 662,  0.8, 73.0,  1.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Сливки 10%',                 'Cream 10%',              'DAIRY', '🥛', 119,  2.8, 10.0,  4.7, NULL, NULL, TRUE),
(gen_random_uuid(), 'Сливки 20%',                 'Cream 20%',              'DAIRY', '🥛', 205,  2.5, 20.0,  4.0, NULL, NULL, TRUE),

-- ============================================================
-- EGGS
-- ============================================================
(gen_random_uuid(), 'Яйцо куриное целое',         'Whole chicken egg',      'EGGS', '🥚', 157, 13.0, 12.0,  1.1, NULL, NULL, TRUE),
(gen_random_uuid(), 'Яичный белок',               'Egg white',              'EGGS', '🥚',  44, 11.0,  0.2,  0.7, NULL, NULL, TRUE),
(gen_random_uuid(), 'Яичный желток',              'Egg yolk',               'EGGS', '🥚', 358, 17.0, 32.0,  1.8, NULL, NULL, TRUE),
(gen_random_uuid(), 'Яйцо перепелиное',           'Quail egg',              'EGGS', '🥚', 168, 13.0, 13.0,  0.6, NULL, NULL, TRUE),
(gen_random_uuid(), 'Яйцо куриное варёное',       'Boiled chicken egg',     'EGGS', '🥚', 155, 13.0, 11.0,  1.1, NULL, NULL, TRUE),

-- ============================================================
-- GRAINS (злаки)
-- ============================================================
(gen_random_uuid(), 'Гречка варёная',             'Boiled buckwheat',       'GRAINS', '🌾',  92,  3.4,  0.6, 20.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Гречка сухая',               'Dry buckwheat',          'GRAINS', '🌾', 343, 13.0,  3.4, 68.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Рис белый варёный',          'Boiled white rice',      'GRAINS', '🍚', 130,  2.7,  0.3, 28.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Рис бурый варёный',          'Boiled brown rice',      'GRAINS', '🍚', 123,  2.6,  1.0, 26.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Овсянка варёная',            'Boiled oatmeal',         'GRAINS', '🌾',  68,  2.4,  1.4, 12.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Хлопья овсяные',             'Oat flakes',             'GRAINS', '🌾', 368, 11.0,  7.2, 65.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Пшено варёное',              'Boiled millet',          'GRAINS', '🌾', 119,  3.5,  0.9, 26.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Перловка варёная',           'Boiled pearl barley',    'GRAINS', '🌾', 109,  3.1,  0.4, 24.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Булгур варёный',             'Boiled bulgur',          'GRAINS', '🌾',  83,  3.1,  0.2, 18.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Киноа варёная',              'Boiled quinoa',          'GRAINS', '🌾', 120,  4.4,  1.9, 21.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Кукурузная крупа варёная',   'Boiled corn grits',      'GRAINS', '🌽',  96,  2.5,  0.9, 21.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Макароны варёные',           'Boiled pasta',           'GRAINS', '🍝', 158,  5.5,  0.9, 31.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Хлеб пшеничный белый',       'White wheat bread',      'GRAINS', '🍞', 265,  8.1,  3.2, 51.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Хлеб ржаной',                'Rye bread',              'GRAINS', '🍞', 259,  6.8,  3.3, 49.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Хлеб цельнозерновой',        'Whole-grain bread',      'GRAINS', '🍞', 247,  9.0,  3.5, 46.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Кукурузные хлопья',          'Corn flakes',            'GRAINS', '🌽', 357,  7.0,  1.3, 79.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Ячневая крупа варёная',      'Boiled barley grits',    'GRAINS', '🌾',  76,  2.3,  0.4, 16.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Манная крупа',               'Semolina',               'GRAINS', '🌾', 333, 10.0,  1.0, 73.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Рис пропаренный варёный',    'Boiled parboiled rice',  'GRAINS', '🍚', 123,  2.4,  0.4, 27.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Полба варёная',              'Boiled spelt',           'GRAINS', '🌾', 127,  5.5,  1.0, 26.0, NULL, NULL, TRUE),

-- ============================================================
-- LEGUMES (бобовые)
-- ============================================================
(gen_random_uuid(), 'Чечевица варёная',           'Boiled lentils',         'LEGUMES', '🫘', 116,  9.0,  0.4, 20.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Горох варёный',              'Boiled peas',            'LEGUMES', '🫛', 116,  8.2,  0.4, 21.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Фасоль красная варёная',     'Boiled red beans',       'LEGUMES', '🫘', 127,  8.7,  0.5, 23.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Фасоль белая варёная',       'Boiled white beans',     'LEGUMES', '🫘', 139,  9.7,  0.4, 25.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Нут варёный',                'Boiled chickpeas',       'LEGUMES', '🫘', 164,  8.9,  2.6, 27.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Соевые бобы варёные',        'Boiled soybeans',        'LEGUMES', '🫘', 173, 17.0,  9.0, 10.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Тофу',                       'Tofu',                   'LEGUMES', '🥡',  76,  8.0,  4.8,  1.9, NULL, NULL, TRUE),
(gen_random_uuid(), 'Горошек зелёный консервы',   'Canned green peas',      'LEGUMES', '🫛',  55,  3.5,  0.4, 10.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Кукуруза консервированная',  'Canned corn',            'LEGUMES', '🌽',  78,  2.9,  0.6, 17.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Маш варёный',                'Boiled mung beans',      'LEGUMES', '🫘', 105,  7.0,  0.4, 19.0, NULL, NULL, TRUE),

-- ============================================================
-- VEGETABLES (овощи)
-- ============================================================
(gen_random_uuid(), 'Картофель варёный',          'Boiled potato',          'VEGETABLES', '🥔',  86,  2.0,  0.1, 20.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Батат варёный',              'Boiled sweet potato',    'VEGETABLES', '🍠',  86,  1.6,  0.1, 20.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Морковь сырая',              'Raw carrot',             'VEGETABLES', '🥕',  41,  0.9,  0.2,  9.6, NULL, NULL, TRUE),
(gen_random_uuid(), 'Свекла варёная',             'Boiled beetroot',        'VEGETABLES', '🥗',  44,  1.7,  0.1, 10.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Капуста белокочанная',       'White cabbage',          'VEGETABLES', '🥬',  28,  1.8,  0.1,  5.8, NULL, NULL, TRUE),
(gen_random_uuid(), 'Брокколи',                   'Broccoli',               'VEGETABLES', '🥦',  34,  2.8,  0.4,  6.6, NULL, NULL, TRUE),
(gen_random_uuid(), 'Цветная капуста',            'Cauliflower',            'VEGETABLES', '🥦',  25,  1.9,  0.3,  5.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Шпинат',                     'Spinach',                'VEGETABLES', '🥬',  23,  2.9,  0.4,  3.6, NULL, NULL, TRUE),
(gen_random_uuid(), 'Помидор',                    'Tomato',                 'VEGETABLES', '🍅',  18,  0.9,  0.2,  3.9, NULL, NULL, TRUE),
(gen_random_uuid(), 'Огурец',                     'Cucumber',               'VEGETABLES', '🥒',  15,  0.7,  0.1,  3.1, NULL, NULL, TRUE),
(gen_random_uuid(), 'Перец болгарский красный',   'Red bell pepper',        'VEGETABLES', '🫑',  31,  1.0,  0.3,  7.3, NULL, NULL, TRUE),
(gen_random_uuid(), 'Лук репчатый',               'Onion',                  'VEGETABLES', '🧅',  41,  1.4,  0.2,  9.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Чеснок',                     'Garlic',                 'VEGETABLES', '🧄', 149,  6.4,  0.5, 33.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Баклажан',                   'Eggplant',               'VEGETABLES', '🍆',  24,  1.2,  0.2,  5.5, NULL, NULL, TRUE),
(gen_random_uuid(), 'Кабачок',                    'Zucchini',               'VEGETABLES', '🥒',  17,  1.0,  0.3,  3.4, NULL, NULL, TRUE),
(gen_random_uuid(), 'Тыква',                      'Pumpkin',                'VEGETABLES', '🎃',  26,  1.0,  0.1,  6.5, NULL, NULL, TRUE),
(gen_random_uuid(), 'Сельдерей',                  'Celery',                 'VEGETABLES', '🥬',  14,  0.7,  0.1,  2.9, NULL, NULL, TRUE),
(gen_random_uuid(), 'Редис',                      'Radish',                 'VEGETABLES', '🥗',  20,  0.7,  0.1,  4.1, NULL, NULL, TRUE),
(gen_random_uuid(), 'Салат романо',               'Romaine lettuce',        'VEGETABLES', '🥬',  17,  1.2,  0.2,  3.3, NULL, NULL, TRUE),
(gen_random_uuid(), 'Авокадо',                    'Avocado',                'VEGETABLES', '🥑', 160,  2.0, 15.0,  9.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Томатная паста',             'Tomato paste',           'VEGETABLES', '🍅',  82,  4.3,  0.5, 17.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Спаржа',                     'Asparagus',              'VEGETABLES', '🥦',  20,  2.2,  0.1,  3.9, NULL, NULL, TRUE),
(gen_random_uuid(), 'Грибы шампиньоны',           'Champignon mushrooms',   'VEGETABLES', '🍄',  22,  3.1,  0.3,  3.3, NULL, NULL, TRUE),
(gen_random_uuid(), 'Лук-порей',                  'Leek',                   'VEGETABLES', '🧅',  61,  1.5,  0.3, 14.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Руккола',                    'Arugula',                'VEGETABLES', '🥬',  25,  2.6,  0.7,  3.7, NULL, NULL, TRUE),

-- ============================================================
-- FRUITS (фрукты и ягоды)
-- ============================================================
(gen_random_uuid(), 'Яблоко',                     'Apple',                  'FRUITS', '🍎',  52,  0.3,  0.2, 14.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Банан',                      'Banana',                 'FRUITS', '🍌',  89,  1.1,  0.3, 23.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Апельсин',                   'Orange',                 'FRUITS', '🍊',  47,  0.9,  0.1, 12.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Груша',                      'Pear',                   'FRUITS', '🍐',  57,  0.4,  0.1, 15.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Виноград',                   'Grapes',                 'FRUITS', '🍇',  67,  0.6,  0.4, 17.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Клубника',                   'Strawberry',             'FRUITS', '🍓',  32,  0.7,  0.3,  7.7, NULL, NULL, TRUE),
(gen_random_uuid(), 'Черника',                    'Blueberry',              'FRUITS', '🫐',  57,  0.7,  0.3, 14.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Малина',                     'Raspberry',              'FRUITS', '🍓',  52,  1.2,  0.7, 12.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Арбуз',                      'Watermelon',             'FRUITS', '🍉',  30,  0.6,  0.1,  7.6, NULL, NULL, TRUE),
(gen_random_uuid(), 'Дыня',                       'Melon',                  'FRUITS', '🍈',  34,  0.9,  0.2,  8.2, NULL, NULL, TRUE),
(gen_random_uuid(), 'Ананас',                     'Pineapple',              'FRUITS', '🍍',  50,  0.5,  0.1, 13.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Манго',                      'Mango',                  'FRUITS', '🥭',  60,  0.8,  0.4, 15.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Лимон',                      'Lemon',                  'FRUITS', '🍋',  29,  1.1,  0.3,  9.3, NULL, NULL, TRUE),
(gen_random_uuid(), 'Грейпфрут',                  'Grapefruit',             'FRUITS', '🍊',  42,  0.8,  0.1, 11.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Вишня',                      'Cherry',                 'FRUITS', '🍒',  50,  1.0,  0.3, 12.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Слива',                      'Plum',                   'FRUITS', '🍑',  46,  0.7,  0.3, 11.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Персик',                     'Peach',                  'FRUITS', '🍑',  39,  0.9,  0.3, 10.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Хурма',                      'Persimmon',              'FRUITS', '🍑',  67,  0.5,  0.4, 17.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Черешня',                    'Sweet cherry',           'FRUITS', '🍒',  52,  1.1,  0.4, 12.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Киви',                       'Kiwi',                   'FRUITS', '🥝',  61,  1.1,  0.5, 15.0, NULL, NULL, TRUE),

-- ============================================================
-- NUTS_SEEDS (орехи и семена)
-- ============================================================
(gen_random_uuid(), 'Грецкий орех',               'Walnut',                 'NUTS_SEEDS', '🌰', 654, 15.0, 65.0, 14.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Миндаль',                    'Almond',                 'NUTS_SEEDS', '🌰', 579, 21.0, 50.0, 22.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Кешью',                      'Cashew',                 'NUTS_SEEDS', '🌰', 553, 18.0, 44.0, 30.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Арахис',                     'Peanut',                 'NUTS_SEEDS', '🥜', 567, 26.0, 49.0, 16.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Фундук',                     'Hazelnut',               'NUTS_SEEDS', '🌰', 628, 15.0, 61.0, 17.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Тыквенные семечки',          'Pumpkin seeds',          'NUTS_SEEDS', '🎃', 559, 30.0, 49.0, 11.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Семена льна',                'Flax seeds',             'NUTS_SEEDS', '🌱', 534, 18.0, 42.0, 29.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Семена чиа',                 'Chia seeds',             'NUTS_SEEDS', '🌱', 486, 17.0, 31.0, 42.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Подсолнечные семечки',       'Sunflower seeds',        'NUTS_SEEDS', '🌻', 584, 21.0, 51.0, 20.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Кунжут',                     'Sesame',                 'NUTS_SEEDS', '🌱', 573, 18.0, 50.0, 23.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Кедровый орех',              'Pine nut',               'NUTS_SEEDS', '🌰', 673, 14.0, 68.0, 13.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Фисташки',                   'Pistachio',              'NUTS_SEEDS', '🌰', 562, 20.0, 45.0, 28.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Семена конопли',             'Hemp seeds',             'NUTS_SEEDS', '🌱', 553, 32.0, 49.0,  8.7, NULL, NULL, TRUE),
(gen_random_uuid(), 'Арахисовая паста',           'Peanut butter',          'NUTS_SEEDS', '🥜', 598, 25.0, 51.0, 20.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Кокос (мякоть)',             'Coconut flesh',          'NUTS_SEEDS', '🥥', 354,  3.3, 33.0, 15.0, NULL, NULL, TRUE),

-- ============================================================
-- OILS (масла)
-- ============================================================
(gen_random_uuid(), 'Масло оливковое',             'Olive oil',              'OILS', '🫒', 884,  0.0, 100.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Масло подсолнечное',          'Sunflower oil',          'OILS', '🌻', 884,  0.0, 100.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Масло кокосовое',             'Coconut oil',            'OILS', '🥥', 892,  0.0,  99.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Масло льняное',               'Flaxseed oil',           'OILS', '🌱', 884,  0.0, 100.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Масло авокадо',               'Avocado oil',            'OILS', '🥑', 884,  0.0, 100.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Топлёное масло гхи',          'Ghee',                   'OILS', '🧈', 900,  0.3,  99.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Масло кунжутное',             'Sesame oil',             'OILS', '🌱', 884,  0.0, 100.0,  0.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Масло грецкого ореха',        'Walnut oil',             'OILS', '🌰', 884,  0.0, 100.0,  0.0, NULL, NULL, TRUE),

-- ============================================================
-- OTHER (другое)
-- ============================================================
(gen_random_uuid(), 'Мёд',                         'Honey',                  'OTHER', '🍯', 304,  0.3,  0.0, 82.0, NULL, 82.0, TRUE),
(gen_random_uuid(), 'Тёмный шоколад 70%',          'Dark chocolate 70%',     'OTHER', '🍫', 598,  8.0, 43.0, 46.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Молочный шоколад',            'Milk chocolate',         'OTHER', '🍫', 535,  7.7, 30.0, 60.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Майонез',                     'Mayonnaise',             'OTHER', '🥫', 680,  2.4, 74.0,  3.8, NULL, NULL, TRUE),
(gen_random_uuid(), 'Кетчуп',                      'Ketchup',                'OTHER', '🥫',  97,  1.8,  0.1, 24.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Горчица',                     'Mustard',                'OTHER', '🥫',  66,  3.7,  3.8,  5.3, NULL, NULL, TRUE),
(gen_random_uuid(), 'Соевый соус',                 'Soy sauce',              'OTHER', '🥫',  53,  5.6,  0.1,  8.5, NULL, NULL, TRUE),
(gen_random_uuid(), 'Сахар',                       'Sugar',                  'OTHER', '🍬', 399,  0.0,  0.0, 100.0, NULL, 100.0, TRUE),
(gen_random_uuid(), 'Протеиновый порошок сывороточный','Whey protein powder','OTHER', '💪', 370, 80.0,  4.0,  9.0, NULL, NULL, TRUE),
(gen_random_uuid(), 'Спортивный батончик',         'Sports bar',             'OTHER', '🍫', 420, 20.0, 12.0, 55.0, NULL, NULL, TRUE)

ON CONFLICT (name_ru) DO NOTHING;
