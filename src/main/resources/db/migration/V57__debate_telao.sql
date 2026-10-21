-- Telao do debate (/debate/<id>/telao): os dois participantes lado a lado,
-- com foto e cargo, e um timer para cada fala.
--
-- Foto e cargo ficam no palestrante. A foto e so o nome do arquivo gravado
-- em app.upload.dir.palestrantes (ver PalestranteController), mesmo padrao
-- da logo de patrocinador.
ALTER TABLE public.palestrante
    ADD COLUMN foto_arquivo VARCHAR(255),
    ADD COLUMN cargo        VARCHAR(150);

-- Ordem em que os palestrantes foram cadastrados no formulario do evento.
-- No debate, o 1o e o participante A (esquerda) e o 2o o participante B.
-- Vinculos antigos ficam com a ordem do id, que era a ordem usada ate aqui.
ALTER TABLE public.evento_palestrante
    ADD COLUMN ordem INT NOT NULL DEFAULT 0;

UPDATE public.evento_palestrante SET ordem = palestrante_id;

-- Tempo de cada fala e o momento em que o timer entra em alerta. So valem
-- para evento do tipo DEBATE; nos demais ficam com o padrao e sao ignorados.
ALTER TABLE public.evento
    ADD COLUMN debate_tempo_fala_seg INT NOT NULL DEFAULT 180,
    ADD COLUMN debate_alerta_seg     INT NOT NULL DEFAULT 30;
