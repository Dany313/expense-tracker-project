CREATE TABLE IF NOT EXISTS categories (
    id           UUID PRIMARY KEY,
    code         VARCHAR(50)  NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    synonyms     VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_categories_code ON categories (code);

-- Inserimento categorie iniziali con sinonimi
INSERT INTO categories (id, code, display_name, synonyms) VALUES
    (gen_random_uuid(), 'AFFITTO', 'Affitto', 'affitto, casa affitto, canone, locazione, housing, mutuo'),
    (gen_random_uuid(), 'BOLLETTE', 'Bollette', 'bollette, utenze, luce, gas, acqua, internet, bills, bolletta, tari'),
    (gen_random_uuid(), 'CARBURANTE_E_TRASPORTI', 'Carburante e trasporti', 'carburante, benzina, gasolio, trasporti, mezzi, treno, bus, metro, autostrada, pedaggio, transport'),
    (gen_random_uuid(), 'MANUTENZIONE_AUTO_ORDINARIA', 'Manutenzione auto ordinaria', 'tagliando, revisione, cambio olio, manutenzione ordinaria, gomme, gommista'),
    (gen_random_uuid(), 'MANUTENZIONE_AUTO_STRAORDINARIA', 'Manutenzione auto straordinaria', 'guasto, riparazione, carrozziere, manutenzione straordinaria, meccanico'),
    (gen_random_uuid(), 'SPESA_ALIMENTARE_E_CASA', 'Spesa alimentare e casa', 'spesa, alimentari, supermercato, casa, igiene, detersivi, drogheria, groceries, personal care, altro, other'),
    (gen_random_uuid(), 'VIAGGI', 'Viaggi', 'viaggi, vacanze, hotel, voli, b&b, airbnb, travel, vacanza, viaggio'),
    (gen_random_uuid(), 'SVAGO', 'Svago', 'svago, tempo libero, cinema, giochi, videogiochi, abbonamenti, netflix, spotify, concerti, hobby, teatro, entertainment'),
    (gen_random_uuid(), 'USCITE_FUORI', 'Uscite fuori', 'uscite fuori, uscite, uscita, ristorante, bar, pub, pizzeria, aperitivo, aperitivi, cena, pranzo, dining, delivery, mcdonald, fast food'),
    (gen_random_uuid(), 'SALUTE_E_SPORT', 'Salute e sport', 'salute, sport, farmacia, medico, visite, palestra, piscina, health'),
    (gen_random_uuid(), 'ABBIGLIAMENTO', 'Abbigliamento', 'abbigliamento, vestiti, scarpe, moda, accessori, shopping')
ON CONFLICT (code) DO NOTHING;

-- Vincolo di integrità referenziale per le spese (idempotente)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_expenses_category'
    ) THEN
        ALTER TABLE expenses
            ADD CONSTRAINT fk_expenses_category
            FOREIGN KEY (category)
            REFERENCES categories (code)
            ON UPDATE CASCADE;
    END IF;
END $$;
