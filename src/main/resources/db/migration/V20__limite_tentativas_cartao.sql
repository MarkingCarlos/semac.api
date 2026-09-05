-- Conta quantas vezes o cartão foi cobrado (aprovado, recusado ou em
-- análise) para uma inscrição. PagamentoCartaoService usa isso para
-- travar tentativas ilimitadas no mesmo pessoaUuid, mitigando carding
-- (teste de números de cartão roubados) via /api/pagamento/cartao.
ALTER TABLE public.pessoa ADD COLUMN tentativas_cartao integer NOT NULL DEFAULT 0;
