-- V13 criou `trilha` como texto livre. Decisão mudou: a comissão precisa
-- poder cadastrar novas trilhas pelo /admin, então vira tabela própria com
-- CRUD (mesmo padrão de `tipo_evento`). Nunca se edita uma migration já
-- aplicada — esta migration cria a tabela, migra o que já estava
-- preenchido em `evento.trilha` (inclusive nomes fora da lista padrão,
-- se alguém já tiver testado o formulário) e troca o texto livre pela FK.

CREATE TABLE public.trilha (
    id   SERIAL PRIMARY KEY,
    nome VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO public.trilha (nome) VALUES
    ('IA'), ('Bioinformática'), ('Robótica'), ('UI/UX'), ('Software Livre');

-- Preserva trilhas que já estivessem em uso e não estão na lista padrão.
INSERT INTO public.trilha (nome)
SELECT DISTINCT trilha FROM public.evento
WHERE trilha IS NOT NULL
  AND trilha NOT IN (SELECT nome FROM public.trilha);

ALTER TABLE public.evento ADD COLUMN trilha_id INTEGER REFERENCES public.trilha(id);

UPDATE public.evento e
SET trilha_id = t.id
FROM public.trilha t
WHERE e.trilha = t.nome;

ALTER TABLE public.evento DROP COLUMN trilha;
