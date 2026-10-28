-- Quiz dos Laboratorios (Mostra Tecnica de quinta, 29/10): cada
-- laboratorio prepara um quiz de multipla escolha. A comissao cadastra os
-- laboratorios e as perguntas no /admin; cada laboratorio tem um QR com o
-- link /laboratorio/<codigo_qr>, que o participante le para abrir o quiz.
-- Uma tentativa por pessoa por laboratorio; quem gabarita ganha XP (ver
-- LaboratorioQuizService).
--
-- Independente de trabalho_mostra: nao conta para a conquista
-- MOSTRA_COMPLETA.

CREATE TABLE public.laboratorio (
    id           SERIAL PRIMARY KEY,
    ano          INT          NOT NULL,
    nome         VARCHAR(200) NOT NULL,
    -- Texto livre: quem responde pelo laboratorio nao precisa ter conta.
    responsaveis VARCHAR(300),
    -- Aleatorio, e nao o id: com o id daria para chutar /laboratorio/1...
    codigo_qr    VARCHAR(32)  NOT NULL UNIQUE,
    -- A comissao cadastra antes e libera na hora; fechado, o QR so avisa.
    quiz_aberto  BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX laboratorio_ano_idx ON public.laboratorio (ano);

CREATE TABLE public.pergunta_laboratorio (
    id             SERIAL PRIMARY KEY,
    laboratorio_id INT          NOT NULL REFERENCES public.laboratorio(id) ON DELETE CASCADE,
    ordem          INT          NOT NULL,
    enunciado      VARCHAR(500) NOT NULL
);

CREATE INDEX pergunta_laboratorio_laboratorio_idx ON public.pergunta_laboratorio (laboratorio_id);

-- Exatamente uma alternativa correta por pergunta: validado no servico.
CREATE TABLE public.alternativa_pergunta_laboratorio (
    id          SERIAL PRIMARY KEY,
    pergunta_id INT          NOT NULL REFERENCES public.pergunta_laboratorio(id) ON DELETE CASCADE,
    ordem       INT          NOT NULL,
    texto       VARCHAR(300) NOT NULL,
    correta     BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX alternativa_pergunta_laboratorio_pergunta_idx ON public.alternativa_pergunta_laboratorio (pergunta_id);

-- Uma tentativa por pessoa por laboratorio: e o UNIQUE que garante, mesmo
-- com dois envios ao mesmo tempo. Guarda so o placar; editar as perguntas
-- depois nao muda o que ja foi respondido.
CREATE TABLE public.resposta_quiz_laboratorio (
    id             SERIAL PRIMARY KEY,
    pessoa_id      INT       NOT NULL REFERENCES public.pessoa(id) ON DELETE CASCADE,
    laboratorio_id INT       NOT NULL REFERENCES public.laboratorio(id) ON DELETE CASCADE,
    acertos        INT       NOT NULL,
    total          INT       NOT NULL,
    xp_creditado   INT       NOT NULL DEFAULT 0,
    respondido_em  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT resposta_quiz_laboratorio_pessoa_laboratorio_key UNIQUE (pessoa_id, laboratorio_id)
);

CREATE INDEX resposta_quiz_laboratorio_laboratorio_idx ON public.resposta_quiz_laboratorio (laboratorio_id);
