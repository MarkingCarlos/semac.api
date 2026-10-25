-- Motivo informado quando, no celular, o kit foi entregue sem conseguir
-- ler o QR do crachá (botão "Entregar sem ler QR code"). Nulo nas
-- entregas por QR, nas do desktop (que não lê QR) e nas da comissão.
ALTER TABLE public.pessoa ADD COLUMN kit_justificativa_sem_qr VARCHAR(255);
