-- Roteiro do debate em blocos (painel /debate/<id>): cada bloco tem as
-- perguntas elaboradas pela mediacao e uma pergunta da plateia escolhida.
--
-- `bloco` nulo = a pergunta nao esta em nenhum bloco (as do publico ficam
-- assim ate serem escolhidas). `da_mediacao` marca as perguntas que a
-- mediacao escreveu: essas podem ser editadas e nao aparecem na lista do
-- publico do painel.
ALTER TABLE public.pergunta_debate
    ADD COLUMN bloco       SMALLINT,
    ADD COLUMN da_mediacao BOOLEAN NOT NULL DEFAULT FALSE;
