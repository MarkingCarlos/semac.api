-- Sorteio deixa de ser o registro de uma entrega e passa a ser um
-- cadastro: nome + evento onde acontece (um evento pode ter varios
-- sorteios). Cada brinde pertence a um sorteio, e cada unidade entregue
-- vira uma linha em `ganhadores_sorteio` -- quem ganhou, qual brinde e
-- quem realizou o sorteio (organizador).
--
-- Parte do zero: nao havia brindes/sorteios em producao a preservar.

TRUNCATE public.ganhadores_sorteio, public.sorteio, public.brinde RESTART IDENTITY;

-- ── sorteio: cadastro ────────────────────────────────────────────
-- Remover a coluna ja derruba a FK dela (fk_sorteio_brinde e a do
-- organizador_id, que tinha nome gerado pelo Hibernate na V1).
ALTER TABLE public.sorteio DROP COLUMN brinde_id;
ALTER TABLE public.sorteio DROP COLUMN organizador_id;
ALTER TABLE public.sorteio DROP COLUMN realizado_em;
ALTER TABLE public.sorteio ADD COLUMN nome VARCHAR(255) NOT NULL;

-- ── brinde: pertence a um sorteio ────────────────────────────────
ALTER TABLE public.brinde ADD COLUMN sorteio_id INT NOT NULL;
ALTER TABLE public.brinde
    ADD CONSTRAINT fk_brinde_sorteio FOREIGN KEY (sorteio_id) REFERENCES public.sorteio(id);

-- ── ganhadores_sorteio: uma linha por entrega ────────────────────
-- A PK era sorteio_id (um ganhador por sorteio); agora um sorteio tem
-- varias entregas, entao ganha id proprio.
ALTER TABLE public.ganhadores_sorteio DROP CONSTRAINT ganhadores_sorteio_pkey;
ALTER TABLE public.ganhadores_sorteio ADD COLUMN id SERIAL PRIMARY KEY;
ALTER TABLE public.ganhadores_sorteio ADD COLUMN brinde_id INT NOT NULL;
ALTER TABLE public.ganhadores_sorteio ADD COLUMN organizador_id INT NOT NULL;
ALTER TABLE public.ganhadores_sorteio
    ADD CONSTRAINT fk_ganhadores_sorteio_brinde FOREIGN KEY (brinde_id) REFERENCES public.brinde(id);
ALTER TABLE public.ganhadores_sorteio
    ADD CONSTRAINT fk_ganhadores_sorteio_organizador FOREIGN KEY (organizador_id) REFERENCES public.pessoa(id);

-- Regra da semana: quem ja ganhou um brinde fica fora dos sorteios
-- seguintes. O SorteioService ja checa; o indice segura corrida entre
-- duas telas confirmando ao mesmo tempo.
CREATE UNIQUE INDEX uq_ganhadores_sorteio_participante ON public.ganhadores_sorteio (participante_id);
