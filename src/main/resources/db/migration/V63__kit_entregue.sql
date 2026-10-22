-- Entrega do kit (camisa, caneta e afins) no credenciamento, marcada pela
-- diretoria na sub-aba Kits de Pessoas do /admin. kit_entregue_em nulo =
-- ainda não entregue; kit_entregue_por_id guarda quem da comissão marcou.
-- ON DELETE SET NULL: excluir o membro que entregou não apaga a entrega.
ALTER TABLE public.pessoa ADD COLUMN kit_entregue_em TIMESTAMP;

ALTER TABLE public.pessoa ADD COLUMN kit_entregue_por_id INTEGER
    REFERENCES public.pessoa (id) ON DELETE SET NULL;
