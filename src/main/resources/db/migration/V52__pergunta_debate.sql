-- Perguntas enviadas previamente pelos participantes para os eventos do
-- tipo DEBATE (botao "Mande sua pergunta" no /participantes). Lidas pela
-- diretoria e presidencia no /admin, aba Conteudo.
--
-- Liga/desliga em Informacoes SEMAC: fechado, o /participantes esconde o
-- botao e o backend recusa novas perguntas com 409. Comeca fechado.
ALTER TABLE public.configuracao_inscricao
    ADD COLUMN perguntas_debate_abertas BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE public.pergunta_debate (
    id          SERIAL        PRIMARY KEY,
    evento_id   INT           NOT NULL REFERENCES public.evento(id) ON DELETE CASCADE,
    pessoa_id   INT           NOT NULL REFERENCES public.pessoa(id) ON DELETE CASCADE,
    texto       VARCHAR(500)  NOT NULL,
    enviada_em  TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_pergunta_debate_evento ON public.pergunta_debate (evento_id);
CREATE INDEX idx_pergunta_debate_evento_pessoa ON public.pergunta_debate (evento_id, pessoa_id);
