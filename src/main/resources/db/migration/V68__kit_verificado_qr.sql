-- Como a entrega do kit foi confirmada (ver PessoaService.marcarKit):
-- TRUE = quem entregou leu o QR code do crachá da pessoa (fluxo do
-- celular); FALSE = marcada sem leitura (desktop ou comissão, que não lê
-- QR). Nulo enquanto o kit não foi entregue, e também nas entregas
-- feitas antes desta coluna existir — dessas não se sabe como foi.
ALTER TABLE public.pessoa ADD COLUMN kit_verificado_qr BOOLEAN;
