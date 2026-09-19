-- Jogo do Termo (/termo): a palavra de cada dia do evento, os jogos de
-- cada participante e as tentativas gastas.
--
-- A palavra vive so aqui e no servidor. O navegador nunca a recebe
-- enquanto o jogo esta em andamento: o palpite vai para a API e volta so
-- o padrao de cores (ver TermoService). Antes disso a palavra estava
-- hardcoded no bundle JS, visivel no DevTools.

-- Uma palavra por dia de evento. `data` decide sozinha qual esta valendo:
-- o servidor compara com a data de hoje, sem ninguem precisar virar
-- chave nenhuma durante a semana.
CREATE TABLE public.termo_palavra (
    id      SERIAL PRIMARY KEY,
    ano     INT  NOT NULL,
    dia     INT  NOT NULL CHECK (dia BETWEEN 1 AND 4),
    data    DATE NOT NULL UNIQUE,
    palavra VARCHAR(5) NOT NULL,
    CONSTRAINT termo_palavra_ano_dia_key UNIQUE (ano, dia)
);

-- Um jogo por pessoa por dia -- e o UNIQUE abaixo que garante isso, e com
-- ele que a vitoria so pode creditar xp uma vez. `xp_creditado` guarda
-- quanto foi creditado de fato, mesma convencao de
-- evento_participante.xp_creditado e participante_conquista.xp_creditado:
-- se os pontos mudarem depois, um estorno ainda devolve o valor certo.
CREATE TABLE public.termo_jogo (
    id           SERIAL PRIMARY KEY,
    pessoa_id    INT NOT NULL REFERENCES public.pessoa(id) ON DELETE CASCADE,
    palavra_id   INT NOT NULL REFERENCES public.termo_palavra(id) ON DELETE CASCADE,
    venceu       BOOLEAN,
    encerrado_em TIMESTAMP,
    xp_creditado INT,
    CONSTRAINT termo_jogo_pessoa_palavra_key UNIQUE (pessoa_id, palavra_id)
);

-- Tentativas gastas, em ordem. Ficam no banco (e nao no navegador) para
-- que o limite de 6 seja real: F5, aba anonima ou limpar o localStorage
-- nao devolve tentativa. De quebra, reabrir a pagina retoma o jogo.
--
-- `resultado` guarda o padrao de cores compactado, uma letra por posicao:
-- C = certo, P = presente, A = ausente. Nunca a palavra secreta.
CREATE TABLE public.termo_tentativa (
    id        SERIAL PRIMARY KEY,
    jogo_id   INT NOT NULL REFERENCES public.termo_jogo(id) ON DELETE CASCADE,
    ordem     INT NOT NULL,
    palpite   VARCHAR(5) NOT NULL,
    resultado VARCHAR(5) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT termo_tentativa_jogo_ordem_key UNIQUE (jogo_id, ordem)
);

CREATE INDEX termo_jogo_pessoa_idx ON public.termo_jogo (pessoa_id);
CREATE INDEX termo_tentativa_jogo_idx ON public.termo_tentativa (jogo_id);
