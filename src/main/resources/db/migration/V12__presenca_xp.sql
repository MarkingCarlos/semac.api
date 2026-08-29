-- Registra quando a presença foi marcada e o xp efetivamente creditado
-- naquele check-in (0, metade ou cheio, conforme o atraso — ver
-- InscricaoEventoService.marcarPresente). xp_creditado é uma cópia do
-- valor no momento, no mesmo espírito de entrada_inscricao.valor: não
-- muda se tipo_evento.pontos mudar depois.

ALTER TABLE public.evento_participante
    ADD COLUMN presenca_em timestamp without time zone,
    ADD COLUMN xp_creditado integer;
