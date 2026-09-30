-- Quem leu o QR code do participante em cada check-in bem-sucedido.
--
-- Alimenta a dashboard do /admin: leituras por membro da comissao e
-- "o membro A leu o QR do participante B". Ate aqui o operador so era
-- gravado nas tentativas recusadas (tentativa_checkin_bloqueada).
--
-- Mesmo criterio daquela tabela: sem foreign key e com o nome copiado do
-- momento, para o registro sobreviver se o membro for excluido depois.
--
-- Check-ins feitos antes desta migration ficam com as duas colunas nulas
-- ("operador nao registrado" na dashboard) -- nao ha de onde recupera-los.

ALTER TABLE public.evento_participante
    ADD COLUMN registrado_por_id   integer,
    ADD COLUMN registrado_por_nome varchar(255);
