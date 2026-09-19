-- Textos dos e-mails automaticos, editaveis em /admin -> Mensagens.
--
-- Antes disso o texto vivia dentro de um template Thymeleaf no jar: mudar
-- uma palavra exigia deploy. Agora o corpo e o assunto sao dado, e o
-- layout (cabecalho, rodape, cores) continua sendo codigo — a comissao
-- edita o miolo sem conseguir quebrar a aparencia do e-mail.
--
-- `corpo_markdown` guarda Markdown, nao HTML: e o que impede alguem de
-- colar <script> ou derrubar a estrutura de <table> que os clientes de
-- e-mail exigem (ver RenderizadorEmailService, que desliga HTML bruto).
--
-- A tabela nasce VAZIA de proposito. Quem popula e o SeedModeloEmailRunner,
-- a partir do CatalogoVariaveisEmail, na mesma divisao de propriedade do
-- ConquistaSeedRunner: o codigo e dono de `chave`, o /admin e dono de
-- `assunto` e `corpo_markdown`. Assim o texto padrao mora num lugar so e
-- um deploy nunca desfaz o que a comissao escreveu.
CREATE TABLE public.modelo_email (
    id                SERIAL PRIMARY KEY,
    chave             VARCHAR(60)  NOT NULL UNIQUE,
    assunto           VARCHAR(200) NOT NULL,
    corpo_markdown    TEXT         NOT NULL,
    -- Desligar um modelo suspende o envio daquela mensagem sem apagar o
    -- texto: util para cortar um aviso no meio do evento e religar depois.
    ativo             BOOLEAN      NOT NULL DEFAULT true,
    atualizado_em     TIMESTAMP,
    -- ON DELETE SET NULL: excluir quem editou nao pode apagar a mensagem.
    atualizado_por_id INT          REFERENCES public.pessoa(id) ON DELETE SET NULL
);
