-- Momento em que o evento comecou de verdade, marcado pelo botao
-- "INICIAR EVENTO" da ferramenta /checkin.
--
-- Existe porque o atraso do check-in era medido sempre de
-- `data_hora_inicio`, o horario agendado: se a palestra atrasava, quem
-- chegava pontual perdia xp por culpa da organizacao. Com esta coluna o
-- atraso passa a contar do inicio real (ver
-- InscricaoEventoService.inicioEfetivo).
--
-- Nulo = ninguem clicou, e ai o atraso conta do horario agendado, como
-- antes. Preenchido, o valor e limitado em codigo ao intervalo
-- [data_hora_inicio, data_hora_inicio + 30min]: clicar antes da hora nao
-- antecipa o contador (senao punia quem chegou pontual) e clicar muito
-- depois nao anula a regra de atraso.

ALTER TABLE public.evento
    ADD COLUMN iniciado_em timestamp without time zone;
