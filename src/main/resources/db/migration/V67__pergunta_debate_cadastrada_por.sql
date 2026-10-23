-- Perguntas incluidas a mao pela diretoria/presidencia no /admin (modal
-- "Ver perguntas" da aba Conteudo) -- por exemplo, as entregues em papel
-- durante o debate. cadastrada_por_id aponta quem incluiu; nulo quando a
-- pergunta veio do publico (/perguntas ou /participantes). Se a conta de
-- quem cadastrou for apagada, a pergunta fica e so perde a autoria.
ALTER TABLE public.pergunta_debate
    ADD COLUMN cadastrada_por_id INT REFERENCES public.pessoa(id) ON DELETE SET NULL;
