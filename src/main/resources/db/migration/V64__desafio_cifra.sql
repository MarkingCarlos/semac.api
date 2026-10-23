-- Desafio diario "Cifra do Dia" (/desafio-cifra): uma senha criptografada
-- por dia de evento, que o participante decifra em 1 ou 2 etapas.
--
-- Mesma regra do Termo: a resposta vive so aqui e no servidor. O
-- navegador manda a tentativa e recebe de volta so se acertou; nem ao
-- errar as 5 tentativas a resposta e revelada (ver DesafioCifraService).

-- Um desafio por dia. `data` decide sozinha qual esta valendo, como em
-- termo_dia. `senha_cifrada` e o que aparece na tela -- e publica.
CREATE TABLE public.desafio_cifra (
    id            SERIAL PRIMARY KEY,
    ano           INT          NOT NULL,
    data          DATE         NOT NULL UNIQUE,
    palestra      VARCHAR(200) NOT NULL,
    senha_cifrada VARCHAR(200) NOT NULL
);

-- As etapas do desafio, em ordem: a 1 e a primeira a decifrar e a ultima
-- e a resposta final. Segunda (26/10) tem duas -- a senha parcialmente
-- descriptografada e depois a palavra --, terca tem uma.
--
-- `resposta` guarda os espacos de proposito: sao eles que dividem as casas
-- do campo na tela ("MAKE THEM BELIEVE" vira 4 · 4 · 7). Na comparacao os
-- espacos somem.
CREATE TABLE public.desafio_cifra_etapa (
    id         SERIAL PRIMARY KEY,
    desafio_id INT          NOT NULL REFERENCES public.desafio_cifra(id) ON DELETE CASCADE,
    ordem      INT          NOT NULL CHECK (ordem BETWEEN 1 AND 2),
    resposta   VARCHAR(100) NOT NULL,
    dica       VARCHAR(600) NOT NULL,
    CONSTRAINT desafio_cifra_etapa_ordem_key UNIQUE (desafio_id, ordem)
);

-- Um jogo por pessoa por desafio -- e o UNIQUE que garante o credito unico
-- do xp e que as tentativas nao voltem com um F5. As 5 tentativas valem
-- para o desafio inteiro, somando todas as etapas.
--
-- `etapas_concluidas` 0 = ainda na primeira. `venceu`/`encerrado_em` ficam
-- null enquanto o jogo esta em andamento; `xp_creditado` segue a convencao
-- de termo_jogo.
CREATE TABLE public.desafio_cifra_jogo (
    id                SERIAL PRIMARY KEY,
    pessoa_id         INT NOT NULL REFERENCES public.pessoa(id) ON DELETE CASCADE,
    desafio_id        INT NOT NULL REFERENCES public.desafio_cifra(id) ON DELETE CASCADE,
    etapas_concluidas INT NOT NULL DEFAULT 0,
    tentativas_usadas INT NOT NULL DEFAULT 0,
    venceu            BOOLEAN,
    encerrado_em      TIMESTAMP,
    xp_creditado      INT,
    CONSTRAINT desafio_cifra_jogo_pessoa_desafio_key UNIQUE (pessoa_id, desafio_id)
);

CREATE INDEX desafio_cifra_jogo_desafio_idx ON public.desafio_cifra_jogo (desafio_id);

-- Xp de decifrar, editavel no /admin -> Informacoes SEMAC como o do Termo.
INSERT INTO public.regra_xp (chave, nome, valor, unidade, ordem) VALUES
    ('CIFRA_ACERTO', 'Decifrar a Cifra do Dia', 25, 'PONTOS', 15);

-- Terca-feira (27/10): substituicao pela tabela MAIRA seguida de Beaufort
-- com a chave MAIRA1 (= ABCDE). A dica so entrega a chave; descobrir a
-- tabela e o metodo faz parte do desafio.
WITH terca AS (
    INSERT INTO public.desafio_cifra (ano, data, palestra, senha_cifrada) VALUES
        (2026, DATE '2026-10-27',
         'Inglês para ir além: oportunidades, aprendizagem e carreira em tecnologia',
         'A⁵AR³M⁵A⁴A⁷R⁴A⁶IMM³A⁷A⁹R¹M')
    RETURNING id
)
INSERT INTO public.desafio_cifra_etapa (desafio_id, ordem, resposta, dica)
SELECT id, 1, 'MAKE THEM BELIEVE',
       'A resposta está em como a almirante da SEMAC é conhecida.'
  FROM terca;
