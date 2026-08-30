-- Trilha temática do evento (IA, Bioinformática, Robótica, UI/UX,
-- Software Livre...), usada para filtrar a programação pública. É um
-- conceito diferente de `tipo_evento` (que classifica palestra/minicurso/
-- coffee break para pontuação e inscrição, não o assunto abordado).
-- Gerenciável pela comissão no /admin, mesmo padrão de `tipo_evento`
-- (CRUD próprio) — não é mais uma lista fixa no código. Opcional em
-- `evento`: nem todo evento tem trilha (ex: coffee break).

CREATE TABLE public.trilha (
    id   SERIAL PRIMARY KEY,
    nome VARCHAR(50) NOT NULL UNIQUE
);

ALTER TABLE public.evento ADD COLUMN trilha_id INTEGER REFERENCES public.trilha(id);

-- Trilhas usadas até aqui na programação, pra não perder o que já existia
-- como lista fixa no frontend antes desta migration.
INSERT INTO public.trilha (nome) VALUES
    ('IA'), ('Bioinformática'), ('Robótica'), ('UI/UX'), ('Software Livre');
