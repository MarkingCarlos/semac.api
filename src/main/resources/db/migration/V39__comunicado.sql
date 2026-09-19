-- Comunicados avulsos: mensagem escrita na hora e disparada para um
-- publico escolhido em /admin -> Comunicados.
--
-- Diferente de `modelo_email`, que guarda o texto de uma mensagem
-- automatica reaproveitada a cada acontecimento, cada linha aqui e UM
-- disparo ja acontecido. A tabela e o registro do que foi enviado, para
-- quem e com que resultado -- e-mail nao tem "desfazer", entao o historico
-- e a unica forma de responder "o que a gente mandou mesmo?" depois.
CREATE TABLE public.comunicado (
    id                  SERIAL PRIMARY KEY,
    assunto             VARCHAR(200) NOT NULL,
    corpo_markdown      TEXT         NOT NULL,
    -- PARTICIPANTES_CONFIRMADOS, INSCRICOES_PENDENTES, COMISSAO ou
    -- INSCRITOS_EM_EVENTO (ver enum PublicoComunicado).
    publico             VARCHAR(40)  NOT NULL,
    -- Preenchido so quando publico = INSCRITOS_EM_EVENTO.
    -- ON DELETE SET NULL: apagar o evento nao pode apagar o historico do
    -- que ja foi enviado sobre ele.
    evento_id           INT          REFERENCES public.evento(id) ON DELETE SET NULL,
    -- Congelado no disparo: o publico muda com o tempo, e o historico
    -- precisa dizer quantas pessoas eram naquele momento.
    total_destinatarios INT          NOT NULL,
    enviados            INT          NOT NULL DEFAULT 0,
    falhas              INT          NOT NULL DEFAULT 0,
    -- EM_ANDAMENTO, CONCLUIDO ou CONCLUIDO_COM_FALHAS.
    status              VARCHAR(30)  NOT NULL,
    criado_em           TIMESTAMP    NOT NULL DEFAULT now(),
    concluido_em        TIMESTAMP,
    enviado_por_id      INT          REFERENCES public.pessoa(id) ON DELETE SET NULL
);

CREATE INDEX comunicado_criado_em_idx ON public.comunicado (criado_em DESC);
