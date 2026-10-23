-- Mostra Tecnica: o numero do stand nao sera usado. Os trabalhos sao
-- identificados so pelo titulo e pelos apresentadores, e o QR impresso
-- continua o mesmo (codigo_qr nao muda).
ALTER TABLE public.trabalho_mostra DROP COLUMN IF EXISTS stand;
