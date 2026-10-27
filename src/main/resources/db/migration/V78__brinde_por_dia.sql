-- Brinde (Monster) por dia: alem do 26/10 da comissao, ha nova rodada no
-- 29/10, agora tambem para participantes. Cada pessoa passa a ter uma
-- linha por dia (UNIQUE pessoa_id + dia), entao quem retirou no 26 pode
-- retirar de novo no 29. Participante retira pelo cracha e nao tem QR
-- proprio, por isso `codigo` passa a aceitar nulo (UNIQUE ignora nulos).
ALTER TABLE public.brinde_comissao DROP CONSTRAINT brinde_comissao_pkey;

ALTER TABLE public.brinde_comissao ADD COLUMN id SERIAL PRIMARY KEY;

ALTER TABLE public.brinde_comissao ADD COLUMN dia DATE NOT NULL DEFAULT DATE '2026-10-26';
ALTER TABLE public.brinde_comissao ALTER COLUMN dia DROP DEFAULT;

ALTER TABLE public.brinde_comissao
    ADD CONSTRAINT brinde_comissao_pessoa_dia_unique UNIQUE (pessoa_id, dia);

ALTER TABLE public.brinde_comissao ALTER COLUMN codigo DROP NOT NULL;
