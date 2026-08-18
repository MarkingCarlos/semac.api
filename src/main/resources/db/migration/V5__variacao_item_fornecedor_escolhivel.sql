-- Cada linha de uma variação agora é por PRODUTO (não produto+fornecedor):
-- o fornecedor passa a ser escolhido via dropdown por variação, então uma
-- variação nunca repete o mesmo produto em duas linhas.

ALTER TABLE public.variacao_item DROP CONSTRAINT fk_variacao_item_cotacao_fornecedor;
ALTER TABLE public.variacao_item DROP CONSTRAINT uq_variacao_item;

ALTER TABLE public.variacao_item ADD COLUMN cotacao_id integer;
ALTER TABLE public.variacao_item ADD COLUMN fornecedor_id integer;

-- Preenche as novas colunas a partir da cotacao_fornecedor referenciada,
-- pro caso de já existirem linhas de teste local.
UPDATE public.variacao_item vi
SET cotacao_id = cf.cotacao_id,
    fornecedor_id = cf.fornecedor_id
FROM public.cotacao_fornecedor cf
WHERE cf.id = vi.cotacao_fornecedor_id;

-- Sob o modelo antigo uma variação podia ter duas linhas pro mesmo
-- produto (uma por fornecedor cotado); o novo modelo permite só uma por
-- produto. Mantém a linha de maior id (mais recente) de cada duplicata.
DELETE FROM public.variacao_item vi
USING (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY variacao_id, cotacao_id ORDER BY id DESC) AS rn
    FROM public.variacao_item
) dups
WHERE vi.id = dups.id AND dups.rn > 1;

ALTER TABLE public.variacao_item ALTER COLUMN cotacao_id SET NOT NULL;
ALTER TABLE public.variacao_item ALTER COLUMN fornecedor_id SET NOT NULL;

ALTER TABLE public.variacao_item DROP COLUMN cotacao_fornecedor_id;

ALTER TABLE public.variacao_item ADD CONSTRAINT uq_variacao_item UNIQUE (variacao_id, cotacao_id);
ALTER TABLE public.variacao_item ADD CONSTRAINT fk_variacao_item_cotacao FOREIGN KEY (cotacao_id) REFERENCES public.cotacao(id) ON DELETE CASCADE;
ALTER TABLE public.variacao_item ADD CONSTRAINT fk_variacao_item_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES public.fornecedor(id);
