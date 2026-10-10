-- Comprovantes de compra anexados a um item da previsao (nota fiscal,
-- recibo, print do Pix...). Um item pode ter quantos comprovantes forem
-- necessarios.
--
-- O arquivo fica em disco (app.upload.dir.comprovantes-previsao); aqui
-- vai so o nome gerado pelo servidor e os metadados para listar. O
-- ON DELETE CASCADE limpa as linhas quando o item e excluido -- os
-- arquivos em disco sao apagados pelo PrevisaoComprovanteService.
CREATE TABLE public.previsao_item_comprovante (
    id               SERIAL        PRIMARY KEY,
    previsao_item_id INT           NOT NULL REFERENCES public.previsao_item(id) ON DELETE CASCADE,
    nome_original    VARCHAR(255)  NOT NULL,
    nome_arquivo     VARCHAR(255)  NOT NULL UNIQUE,
    tipo_conteudo    VARCHAR(50)   NOT NULL,
    tamanho_bytes    BIGINT        NOT NULL,
    enviado_em       TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_previsao_item_comprovante_item ON public.previsao_item_comprovante (previsao_item_id);

COMMENT ON COLUMN public.previsao_item_comprovante.nome_original IS 'Nome do arquivo como veio do cliente; so para exibicao e download.';
COMMENT ON COLUMN public.previsao_item_comprovante.nome_arquivo  IS 'Nome em disco, sempre gerado pelo servidor.';
