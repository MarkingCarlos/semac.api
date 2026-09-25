-- Dias escolhidos por quem comprou ingresso diario (tipo_inscricao.por_dia).
--
-- pessoa.dias_inscricao diz QUANTAS diarias a pessoa pagou; esta tabela diz
-- QUAIS. A escolha e feita pelo proprio participante em /participantes e
-- governa o check-in: o QR de um diarista so vale nos dias listados aqui
-- (ver InscricaoEventoService.exigirDiaDoIngresso).
--
-- A exclusao da pessoa leva junto os dias escolhidos -- sem ela, a linha
-- nao significa nada.

CREATE TABLE public.pessoa_dia_ingresso (
    id        SERIAL PRIMARY KEY,
    pessoa_id INTEGER NOT NULL REFERENCES public.pessoa (id) ON DELETE CASCADE,
    dia       DATE    NOT NULL,
    CONSTRAINT pessoa_dia_ingresso_pessoa_dia_key UNIQUE (pessoa_id, dia)
);
