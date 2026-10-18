-- Termo com varias palavras por dia: a diretoria cadastra N palavras (de 5
-- a 10 letras) para cada dia do evento, e cada participante recebe uma
-- delas por sorteio na primeira vez que abre o /termo no dia. O sorteio
-- prefere a palavra com menos jogadores, entao ninguem repete a palavra de
-- outro enquanto houver palavra livre (ver TermoService.sortearJogo).
--
-- Antes era uma palavra por dia, e a propria termo_palavra guardava ano,
-- dia e data. Agora o dia vira tabela propria (termo_dia) e a palavra
-- pendura nele.

-- O dia: a data continua sendo o que decide qual dia esta valendo.
CREATE TABLE public.termo_dia (
    id   SERIAL PRIMARY KEY,
    ano  INT  NOT NULL,
    dia  INT  NOT NULL CHECK (dia BETWEEN 1 AND 4),
    data DATE NOT NULL UNIQUE,
    CONSTRAINT termo_dia_ano_dia_key UNIQUE (ano, dia)
);

INSERT INTO public.termo_dia (ano, dia, data)
SELECT ano, dia, data FROM public.termo_palavra;

-- A palavra passa a pertencer a um dia, e um dia tem quantas quiser.
ALTER TABLE public.termo_palavra
    ADD COLUMN dia_id INT REFERENCES public.termo_dia(id) ON DELETE CASCADE;

UPDATE public.termo_palavra p
   SET dia_id = d.id
  FROM public.termo_dia d
 WHERE d.ano = p.ano AND d.dia = p.dia;

ALTER TABLE public.termo_palavra
    ALTER COLUMN dia_id SET NOT NULL,
    DROP CONSTRAINT termo_palavra_ano_dia_key,
    DROP COLUMN ano,
    DROP COLUMN dia,
    DROP COLUMN data,
    ALTER COLUMN palavra TYPE VARCHAR(10),
    ADD CONSTRAINT termo_palavra_tamanho_check CHECK (char_length(palavra) BETWEEN 5 AND 10),
    -- A mesma palavra duas vezes no dia faria dois participantes jogarem
    -- a mesma coisa, que e justamente o que o sorteio evita.
    ADD CONSTRAINT termo_palavra_dia_palavra_key UNIQUE (dia_id, palavra);

-- O jogo agora aponta para o dia, alem da palavra sorteada. O unique sai
-- de pessoa+palavra para pessoa+dia: continua um jogo por pessoa por dia,
-- e e com ele que a vitoria so credita xp uma vez.
ALTER TABLE public.termo_jogo
    ADD COLUMN dia_id INT REFERENCES public.termo_dia(id) ON DELETE CASCADE;

UPDATE public.termo_jogo j
   SET dia_id = p.dia_id
  FROM public.termo_palavra p
 WHERE p.id = j.palavra_id;

ALTER TABLE public.termo_jogo
    ALTER COLUMN dia_id SET NOT NULL,
    DROP CONSTRAINT termo_jogo_pessoa_palavra_key,
    ADD CONSTRAINT termo_jogo_pessoa_dia_key UNIQUE (pessoa_id, dia_id);

-- O sorteio conta quantos jogos cada palavra do dia ja tem.
CREATE INDEX termo_jogo_palavra_idx ON public.termo_jogo (palavra_id);

ALTER TABLE public.termo_tentativa
    ALTER COLUMN palpite   TYPE VARCHAR(10),
    ALTER COLUMN resultado TYPE VARCHAR(10);
