-- Item da previsao passa a aceitar mais de uma escala por cabeca, somadas
-- (ex.: cracha para inscritos + comissao = fator inscritos + comissao).
--
-- A coluna previsao_item.escala (uma escala so) vira a tabela
-- previsao_item_escala, uma linha por escala marcada. FIXA deixa de
-- existir como valor: item sem nenhuma linha aqui e valor fechado
-- (fator 1).
--
-- Entra tambem POR_INSCRITO_TOTAL: participantes + pendentes contando quem
-- comprou ingresso diario. POR_INSCRITO segue contando so quem ganha kit
-- (sem diaria) -- ver PessoaRepository.contarInscritosComKit.

CREATE TABLE public.previsao_item_escala (
    previsao_item_id INT         NOT NULL REFERENCES public.previsao_item(id) ON DELETE CASCADE,
    escala           VARCHAR(20) NOT NULL,
    CONSTRAINT previsao_item_escala_pkey PRIMARY KEY (previsao_item_id, escala),
    CONSTRAINT previsao_item_escala_valor_check CHECK (escala IN ('POR_INSCRITO', 'POR_INSCRITO_TOTAL', 'POR_COMISSAO', 'POR_PALESTRANTE'))
);

COMMENT ON TABLE public.previsao_item_escala IS 'Escalas por cabeca de um item previsto; o fator e a soma dos contadores. Sem linha = valor fechado (fator 1).';

-- Mantem o valor de todo item existente: a escala unica vira uma linha.
INSERT INTO public.previsao_item_escala (previsao_item_id, escala)
SELECT id, escala
  FROM public.previsao_item
 WHERE escala <> 'FIXA';

ALTER TABLE public.previsao_item DROP COLUMN escala;
