-- Perguntas para os debates passam a aceitar quem não tem login (página
-- pública /perguntas): professores e convidados não têm conta. Sem login,
-- pessoa_id fica nulo e o nome digitado (opcional) vai em nome_autor; sem
-- nome, a pergunta aparece como "Anônimo" para a mediação. Com login, segue
-- como antes: ligada à pessoa, e o participante ganha a conquista.
ALTER TABLE public.pergunta_debate ALTER COLUMN pessoa_id DROP NOT NULL;

ALTER TABLE public.pergunta_debate ADD COLUMN nome_autor VARCHAR(120);
