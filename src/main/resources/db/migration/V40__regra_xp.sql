-- Regras de XP editaveis no /admin -> Informacoes SEMAC.
--
-- Guarda so o que antes era constante em codigo: o xp de acertar o Termo
-- do dia (TermoService.XP_VITORIA) e os dois limites de atraso do
-- check-in (InscricaoEventoService). O xp de presenca NAO vem daqui --
-- ele continua em `tipo_evento`.`pontos`, uma linha por tipo (palestra,
-- minicurso, ...), pra nao existirem dois lugares dizendo quanto vale a
-- mesma presenca. Quem junta as duas fontes numa lista so e o
-- RegraXpService (GET /api/regra-xp).
--
-- `chave` e o identificador que o codigo procura: imutavel, nunca
-- exposto pra edicao. Nome e valor sao o que o admin mexe. `unidade`
-- diz como o valor e lido e exibido: PONTOS soma xp, MINUTOS e limite
-- de tempo. Linhas nao sao criadas nem excluidas pela API -- o conjunto
-- e fixo porque cada chave tem codigo que a le.
CREATE TABLE public.regra_xp (
    id      SERIAL PRIMARY KEY,
    chave   VARCHAR(40)  NOT NULL UNIQUE,
    nome    VARCHAR(120) NOT NULL,
    valor   INTEGER      NOT NULL,
    unidade VARCHAR(10)  NOT NULL,
    -- Ordem de exibicao na lista do admin e no card do participante.
    ordem   INTEGER      NOT NULL
);

INSERT INTO public.regra_xp (chave, nome, valor, unidade, ordem) VALUES
    ('TERMO_ACERTO',          'Acertar o Termo do dia',        5,  'PONTOS',  10),
    ('ATRASO_METADE_MINUTOS', 'Atraso que corta o XP pela metade', 20, 'MINUTOS', 20),
    ('ATRASO_ZERO_MINUTOS',   'Atraso que zera o XP',          30, 'MINUTOS', 30);
