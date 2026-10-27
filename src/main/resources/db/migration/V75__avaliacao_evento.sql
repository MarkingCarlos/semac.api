-- Pesquisa de satisfacao (26/10): nota de 0 a 10 que o participante da a
-- cada evento em que esteve presente, depois que o evento terminou. Uma
-- linha por pessoa e evento; reenviar a nota atualiza a mesma linha. As
-- regras (tipos que entram, presenca, horario) vivem no
-- AvaliacaoEventoService.
CREATE TABLE public.avaliacao_evento (
    id            SERIAL    PRIMARY KEY,
    evento_id     INT       NOT NULL REFERENCES public.evento (id) ON DELETE CASCADE,
    pessoa_id     INT       NOT NULL REFERENCES public.pessoa (id) ON DELETE CASCADE,
    nota          SMALLINT  NOT NULL CHECK (nota BETWEEN 0 AND 10),
    criada_em     TIMESTAMP NOT NULL,
    atualizada_em TIMESTAMP NOT NULL,
    CONSTRAINT uk_avaliacao_evento_pessoa UNIQUE (evento_id, pessoa_id)
);

CREATE INDEX idx_avaliacao_evento_pessoa ON public.avaliacao_evento (pessoa_id);
