-- Pagamento por cartão de crédito parcelado (Mercado Pago) como alternativa
-- ao Pix. forma_pagamento distingue as duas vias; os demais campos só são
-- preenchidos quando forma_pagamento = 'CARTAO'.
ALTER TABLE public.pessoa ADD COLUMN forma_pagamento character varying(10);
ALTER TABLE public.pessoa ADD CONSTRAINT pessoa_forma_pagamento_check
    CHECK (forma_pagamento IS NULL OR forma_pagamento IN ('PIX', 'CARTAO'));

-- mp_status guarda o valor cru devolvido pela Mercado Pago (approved,
-- rejected, in_process, etc.) sem CHECK: esse vocabulário é da Mercado
-- Pago e pode mudar, diferente dos enums que o próprio SEMAC controla.
ALTER TABLE public.pessoa ADD COLUMN mp_payment_id bigint;
ALTER TABLE public.pessoa ADD COLUMN mp_status character varying(30);
ALTER TABLE public.pessoa ADD COLUMN mp_status_detail character varying(60);
ALTER TABLE public.pessoa ADD COLUMN parcelas integer;
ALTER TABLE public.pessoa ADD COLUMN valor_cobrado numeric(10,2);

-- Toda inscrição existente com comprovante anexado foi paga via Pix --
-- única forma de pagamento até aqui.
UPDATE public.pessoa SET forma_pagamento = 'PIX' WHERE comprovante_pagamento IS NOT NULL;
