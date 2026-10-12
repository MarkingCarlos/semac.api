-- Times da SEMAC. O participante descobre o proprio time em /time (link
-- do QR code do cartao entregue no evento). Nome e mascote sao editados
-- no /admin, aba Times; a cor e fixa, vem do design.
--
-- O sorteio fica numa tabela a parte (time_integrante) em vez de uma
-- coluna em pessoa: refazer o sorteio e um DELETE + INSERT, e quem nao
-- e participante nunca ganha linha.
--
-- Liga/desliga em configuracao_inscricao: desligado, /time manda o
-- participante para /participantes sem revelar nada. Comeca desligado.
ALTER TABLE public.configuracao_inscricao
    ADD COLUMN times_ativos BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE public.time_semac (
    id               SERIAL       PRIMARY KEY,
    nome             VARCHAR(40)  NOT NULL,
    cor              VARCHAR(7)   NOT NULL,
    mascote_arquivo  VARCHAR(255)
);

INSERT INTO public.time_semac (nome, cor) VALUES
    ('TIME ONDA',  '#52ABB1'),
    ('TIME PULSO', '#B13571'),
    ('TIME FAROL', '#E79839');

CREATE TABLE public.time_integrante (
    pessoa_id    INT        PRIMARY KEY REFERENCES public.pessoa(id) ON DELETE CASCADE,
    time_id      INT        NOT NULL REFERENCES public.time_semac(id) ON DELETE CASCADE,
    -- Primeira vez que a pessoa abriu /time; NULL = ainda nao viu.
    revelado_em  TIMESTAMP
);

CREATE INDEX idx_time_integrante_time ON public.time_integrante (time_id);
