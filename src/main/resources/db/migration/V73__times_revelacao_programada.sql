-- Revelacao programada dos times (/time): a partir deste horario os times
-- valem como liberados mesmo com o interruptor times_ativos desligado.
-- Nulo = sem horario, so o interruptor decide (ver TimeService.timesAtivos).
ALTER TABLE public.configuracao_inscricao
    ADD COLUMN times_revelacao_em TIMESTAMP;

-- SEMAC 2026: revelacao as 7h do primeiro dia (horario do servidor, -03).
UPDATE public.configuracao_inscricao
   SET times_revelacao_em = TIMESTAMP '2026-10-26 07:00:00'
 WHERE ano = 2026;
