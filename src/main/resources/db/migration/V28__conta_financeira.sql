-- A SEMAC movimenta duas contas: a da comissao (pessoa fisica) e a da
-- FUNDUNESP. A planilha de origem registrava isso em toda entrada
-- ("Conta destinataria") e em toda saida ("Conta de Origem"), mas o
-- banco nao tinha o campo -- e por isso o balanco por conta da planilha
-- estava errado (somava saidas das duas contas contra a entrada de uma).
--
-- Esta migration adiciona `conta` onde falta e generaliza a tabela do
-- caixa, que ate aqui so comportava o saldo da FUNDUNESP.

-- ---------------------------------------------------------------
-- Entradas
-- ---------------------------------------------------------------
ALTER TABLE public.patrocinador ADD COLUMN conta VARCHAR(20);
ALTER TABLE public.patrocinador ADD CONSTRAINT patrocinador_conta_check
    CHECK (conta IS NULL OR conta IN ('COMISSAO', 'FUNDUNESP'));
COMMENT ON COLUMN public.patrocinador.conta IS 'Conta destinataria do patrocinio. NULL = ainda nao informada.';

ALTER TABLE public.doador ADD COLUMN conta VARCHAR(20);
ALTER TABLE public.doador ADD CONSTRAINT doador_conta_check
    CHECK (conta IS NULL OR conta IN ('COMISSAO', 'FUNDUNESP'));
COMMENT ON COLUMN public.doador.conta IS 'Conta destinataria da doacao. NULL = ainda nao informada.';

-- Backfill a partir da planilha, onde o preenchimento era unanime:
-- as 8 cotas de patrocinio iam para a FUNDUNESP e as 14 doacoes para a
-- conta da comissao. Registrado aqui para ficar auditavel -- se algum
-- lancamento fugir a regra, corrigir pela interface.
UPDATE public.patrocinador SET conta = 'FUNDUNESP' WHERE conta IS NULL;
UPDATE public.doador       SET conta = 'COMISSAO'  WHERE conta IS NULL;

-- ---------------------------------------------------------------
-- Saidas
-- ---------------------------------------------------------------
ALTER TABLE public.compra ADD COLUMN conta VARCHAR(20);
ALTER TABLE public.compra ADD CONSTRAINT compra_conta_check
    CHECK (conta IS NULL OR conta IN ('COMISSAO', 'FUNDUNESP'));
COMMENT ON COLUMN public.compra.conta IS 'Conta de origem do pagamento. NULL = ainda nao informada.';

-- Uma compra registrada e, por padrao, dinheiro que ja saiu; PENDENTE
-- cobre o caso de compra fechada com o fornecedor e ainda nao paga.
ALTER TABLE public.compra ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PAGO';
ALTER TABLE public.compra ADD CONSTRAINT compra_status_check
    CHECK (status IN ('PENDENTE', 'PAGO'));

-- ---------------------------------------------------------------
-- caixa_fundunesp -> caixa
-- ---------------------------------------------------------------
-- O nome deixou de descrever a tabela no momento em que ela passou a
-- guardar tambem o saldo inicial da comissao. Renomeada junto com a
-- sequence e a PK para o proximo leitor nao tropecar.
ALTER TABLE public.caixa_fundunesp RENAME TO caixa;
ALTER SEQUENCE public.caixa_fundunesp_id_seq RENAME TO caixa_id_seq;
ALTER TABLE public.caixa RENAME CONSTRAINT caixa_fundunesp_pkey TO caixa_pkey;

ALTER TABLE public.caixa ADD COLUMN conta VARCHAR(20) NOT NULL DEFAULT 'FUNDUNESP';
ALTER TABLE public.caixa ADD CONSTRAINT caixa_conta_check
    CHECK (conta IN ('COMISSAO', 'FUNDUNESP'));

-- A tabela sempre foi tratada como registro unico (o controller lia com
-- findFirstByOrderByIdAsc). Agora e uma linha POR CONTA, entao a
-- unicidade passa a ser garantida no banco. Se houver sobra de linhas
-- de quando nao havia restricao, mantem-se a mais antiga.
DELETE FROM public.caixa WHERE id NOT IN (SELECT min(id) FROM public.caixa);
ALTER TABLE public.caixa ADD CONSTRAINT caixa_conta_key UNIQUE (conta);

-- Saldo inicial da conta da comissao, vindo da planilha (R$ 1.389,95).
-- A linha da FUNDUNESP (R$ 18.090,32) ja existia e foi marcada pelo
-- DEFAULT acima; criada aqui tambem caso a tabela esteja vazia.
INSERT INTO public.caixa (valor, data_atualizacao, conta)
SELECT 18090.32, now(), 'FUNDUNESP'
WHERE NOT EXISTS (SELECT 1 FROM public.caixa WHERE conta = 'FUNDUNESP');

INSERT INTO public.caixa (valor, data_atualizacao, conta)
SELECT 1389.95, now(), 'COMISSAO'
WHERE NOT EXISTS (SELECT 1 FROM public.caixa WHERE conta = 'COMISSAO');
