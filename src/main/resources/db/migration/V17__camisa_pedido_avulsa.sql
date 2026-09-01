-- Marca cada camiseta pedida como gratuita (inclusa no ingresso/kit) ou
-- avulsa (paga a parte). Ate aqui essa distincao era so calculada na hora
-- (o relatorio de camisetas comparava a quantidade pedida com o
-- camisetas_gratis do ingresso da pessoa); agora fica gravada por linha,
-- para que DIRETOR_SITE/PRESIDENTE possam corrigir manualmente no /admin --
-- inclusive para gente da comissao, que ate aqui era sempre gratis por
-- regra fixa no codigo (RelatorioService), sem excecao possivel.
ALTER TABLE public.camisa_pedido ADD COLUMN avulsa BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill replicando a regra anterior: as N primeiras camisetas de cada
-- pessoa (N = camisetas_gratis do ingresso dela, ordenando por id para
-- manter a ordem em que foram pedidas) ficam gratuitas, o resto avulsa.
-- Quem nao tem ingresso vinculado (comissao) fica com tudo gratuito, igual
-- a regra que essa coluna substitui.
WITH numeradas AS (
    SELECT
        cp.id,
        ROW_NUMBER() OVER (PARTITION BY cp.pessoa_id ORDER BY cp.id) AS posicao,
        ti.camisetas_gratis AS gratis_ingresso
    FROM public.camisa_pedido cp
    JOIN public.pessoa p ON p.id = cp.pessoa_id
    LEFT JOIN public.tipo_inscricao ti ON ti.id = p.tipo_inscricao_id
)
UPDATE public.camisa_pedido cp
SET avulsa = CASE
    WHEN numeradas.gratis_ingresso IS NULL THEN FALSE
    ELSE numeradas.posicao > numeradas.gratis_ingresso
END
FROM numeradas
WHERE numeradas.id = cp.id;
