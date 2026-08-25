-- Direito a camiseta e cobranca por diaria no ingresso. O ingresso deixa de
-- ser so nome+valor: ele passa a declarar quantas camisetas gratuitas inclui
-- e se e cobrado por dia. Essas duas informacoes governam o fluxo publico de
-- /inscricoes (etapa de camiseta e total a pagar) e sao editadas no /admin,
-- secao "Informacoes SEMAC".

ALTER TABLE public.tipo_inscricao ADD COLUMN camisetas_gratis INT NOT NULL DEFAULT 0;
ALTER TABLE public.tipo_inscricao ADD COLUMN por_dia BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE public.tipo_inscricao ADD COLUMN max_dias INT;

-- Diarias escolhidas pela pessoa no cadastro. NULL em ingresso de valor fixo
-- e em quem e comissao. O valor financeiro da inscricao passa a ser
-- valor x dias_inscricao quando o ingresso e por_dia.
ALTER TABLE public.pessoa ADD COLUMN dias_inscricao INT;

-- Tamanhos XG e XXG, presentes na tabela de medidas do fornecedor mas
-- ausentes do CHECK original.
ALTER TABLE public.camisa_pedido DROP CONSTRAINT camisa_pedido_tamanho_check;
ALTER TABLE public.camisa_pedido ADD CONSTRAINT camisa_pedido_tamanho_check
    CHECK (tamanho IN ('PP', 'P', 'M', 'G', 'GG', 'XG', 'XXG'));

-- Preco da camiseta avulsa, por edicao. Uma linha por ano: quem quer mais
-- camisetas do que o ingresso da paga esse valor por unidade.
CREATE TABLE public.camiseta_extra (
    id    SERIAL PRIMARY KEY,
    ano   INT NOT NULL UNIQUE,
    valor DECIMAL(10,2) NOT NULL
);
