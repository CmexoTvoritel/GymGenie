-- Activity catalog seed
-- PostgreSQL, idempotent (ON CONFLICT DO NOTHING)
-- Can also be skipped entirely — ActivityCatalogSeeder runs this automatically on backend startup.

INSERT INTO activity_definitions (id, slug, name, ring, kind, presets, unit, default_goal, inverse, sort_order)
VALUES

  -- ДВИЖЕНИЕ
  (gen_random_uuid(), 'walk',       'Прогулка',             'MOVE', 'PRESET',  '15,30,45,60', 'мин',   30,   false, 0),
  (gen_random_uuid(), 'morning',    'Утренняя зарядка',     'MOVE', 'BINARY',  NULL,          NULL,    NULL, false, 1),
  (gen_random_uuid(), 'stretch',    'Растяжка',             'MOVE', 'BINARY',  NULL,          NULL,    NULL, false, 2),
  (gen_random_uuid(), 'run',        'Бег',                  'MOVE', 'PRESET',  '15,20,30,45', 'мин',   20,   false, 3),
  (gen_random_uuid(), 'bike',       'Велосипед',            'MOVE', 'BINARY',  NULL,          NULL,    NULL, false, 4),
  (gen_random_uuid(), 'swim',       'Плавание',             'MOVE', 'BINARY',  NULL,          NULL,    NULL, false, 5),

  -- РАЗУМ
  (gen_random_uuid(), 'meditate',   'Медитация',            'MIND', 'PRESET',  '5,10,15,20',  'мин',   10,   false, 6),
  (gen_random_uuid(), 'breath',     'Дыхательная практика', 'MIND', 'BINARY',  NULL,          NULL,    NULL, false, 7),
  (gen_random_uuid(), 'read',       'Чтение',               'MIND', 'PRESET',  '15,30,45,60', 'мин',   30,   false, 8),
  (gen_random_uuid(), 'journal',    'Дневник',              'MIND', 'BINARY',  NULL,          NULL,    NULL, false, 9),
  (gen_random_uuid(), 'nophone',    'Без телефона',         'MIND', 'BINARY',  NULL,          NULL,    NULL, false, 10),
  (gen_random_uuid(), 'sleep',      'Сон 8 часов',          'MIND', 'BINARY',  NULL,          NULL,    NULL, false, 11),

  -- РЕЖИМ
  (gen_random_uuid(), 'water',      'Вода',                 'LIFE', 'COUNTER', NULL,          'стак.', 8,    false, 12),
  (gen_random_uuid(), 'vitamins',   'Витамины',             'LIFE', 'BINARY',  NULL,          NULL,    NULL, false, 13),
  (gen_random_uuid(), 'shower',     'Контрастный душ',      'LIFE', 'BINARY',  NULL,          NULL,    NULL, false, 14),
  (gen_random_uuid(), 'wakeup',     'Ранний подъём',        'LIFE', 'BINARY',  NULL,          NULL,    NULL, false, 15),
  (gen_random_uuid(), 'breakfast',  'Полезный завтрак',     'LIFE', 'BINARY',  NULL,          NULL,    NULL, false, 16),
  (gen_random_uuid(), 'veg',        'Овощи',                'LIFE', 'COUNTER', NULL,          'порц.', 2,    false, 17),
  (gen_random_uuid(), 'noalc',      'Без алкоголя',         'LIFE', 'BINARY',  NULL,          NULL,    NULL, true,  18),
  (gen_random_uuid(), 'nosugar',    'Без сахара',           'LIFE', 'BINARY',  NULL,          NULL,    NULL, false, 19),
  (gen_random_uuid(), 'earlysleep', 'Ранний отбой',         'LIFE', 'BINARY',  NULL,          NULL,    NULL, false, 20),
  (gen_random_uuid(), 'mfr',        'МФР / массаж',         'LIFE', 'BINARY',  NULL,          NULL,    NULL, false, 21)

ON CONFLICT (slug) DO NOTHING;
