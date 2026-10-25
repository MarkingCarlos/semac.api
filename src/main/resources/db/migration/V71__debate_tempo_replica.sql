-- Replica do debate: o participante pode pedir replica e fala por um
-- tempo proprio, menor que o da fala. So vale para evento do tipo DEBATE;
-- nos demais fica com o padrao e e ignorado (mesmo esquema da V57).
ALTER TABLE public.evento
    ADD COLUMN debate_tempo_replica_seg INT NOT NULL DEFAULT 30;
