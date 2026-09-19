-- Log das tentativas de check-in recusadas por estarem fora da janela.
--
-- O check-in de um evento so abre 1h antes do inicio
-- (InscricaoEventoService.ANTECEDENCIA_MAXIMA_CHECKIN_MINUTOS). Quem
-- tenta ler um QR antes disso recebe 409 -- e a tentativa fica registrada
-- aqui, com quem operava a leitura.
--
-- Sem foreign key de proposito: log de auditoria precisa sobreviver ao
-- que descreve. Se o evento ou a pessoa forem excluidos depois, a FK
-- apagaria (ou travaria) o registro justamente no caso em que ele mais
-- importa. Pelo mesmo motivo os nomes sao gravados como copia do momento,
-- e nao lidos por join na hora da consulta.

CREATE TABLE public.tentativa_checkin_bloqueada (
    id                 SERIAL PRIMARY KEY,

    evento_id          INTEGER      NOT NULL,
    evento_nome        VARCHAR(255) NOT NULL,

    -- Quem teve o QR lido (ou foi escolhido na busca manual).
    participante_id    INTEGER      NOT NULL,
    participante_nome  VARCHAR(255) NOT NULL,

    -- Quem operava o /checkin, tirado do token da sessao.
    operador_id        INTEGER      NOT NULL,
    operador_nome      VARCHAR(255) NOT NULL,
    operador_role      VARCHAR(40)  NOT NULL,

    tentado_em         timestamp without time zone NOT NULL,
    -- Quantos minutos antes da abertura da janela a leitura aconteceu:
    -- separa o "chegou cedo demais" do "tentou marcar presenca ontem".
    minutos_antes      BIGINT       NOT NULL
);

-- A consulta natural e "o que rolou neste evento" e "o que fulano andou
-- tentando" -- os dois indices cobrem os dois lados.
CREATE INDEX idx_tentativa_checkin_bloqueada_evento ON public.tentativa_checkin_bloqueada (evento_id);
CREATE INDEX idx_tentativa_checkin_bloqueada_operador ON public.tentativa_checkin_bloqueada (operador_id);
