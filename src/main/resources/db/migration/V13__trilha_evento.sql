-- Trilha temática do evento (IA, Bioinformática, Robótica, UI/UX,
-- Software Livre...), usada para filtrar a programação pública. É um
-- conceito diferente de `tipo_evento` (que classifica palestra/minicurso/
-- coffee break para pontuação e inscrição, não o assunto abordado).
-- Texto livre e opcional (nem todo evento tem trilha, ex: coffee break),
-- validado no admin contra a lista fixa em src/data/trilhas.js.

ALTER TABLE public.evento ADD COLUMN trilha character varying(50);
