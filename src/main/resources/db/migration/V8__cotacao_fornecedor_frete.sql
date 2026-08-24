-- Cada linha de fornecedor de uma cotação pode ter um frete próprio (o
-- fornecedor pode cobrar entrega ou não). Zero = não cobra frete.

ALTER TABLE public.cotacao_fornecedor ADD COLUMN frete numeric(10,2) DEFAULT 0 NOT NULL;
