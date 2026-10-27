-- Conquista manual ALQUIMIA_COMPLETA (27/10): os 3 primeiros a concluir a
-- tarefa de cada minicurso. A conquista em si continua uma por pessoa
-- (participante_conquista), mas a mesma pessoa pode vencer de novo no
-- outro dia de minicurso -- e cada minicurso so tem 3 vagas no podio.
-- Por isso o podio vive aqui, uma linha por minicurso e pessoa.
-- `xp_creditado` e 0 na segunda vitoria: os pontos entram uma vez so.
-- As regras vivem no ConquistaService.concederVitoriaTarefaMinicurso.
CREATE TABLE public.vitoria_tarefa_minicurso (
    id               SERIAL    PRIMARY KEY,
    evento_id        INT       NOT NULL REFERENCES public.evento (id) ON DELETE CASCADE,
    pessoa_id        INT       NOT NULL REFERENCES public.pessoa (id) ON DELETE CASCADE,
    concedida_em     TIMESTAMP NOT NULL,
    concedida_por_id INT       REFERENCES public.pessoa (id),
    xp_creditado     INT       NOT NULL,
    CONSTRAINT uk_vitoria_tarefa_minicurso_evento_pessoa UNIQUE (evento_id, pessoa_id)
);

CREATE INDEX idx_vitoria_tarefa_minicurso_pessoa ON public.vitoria_tarefa_minicurso (pessoa_id);
