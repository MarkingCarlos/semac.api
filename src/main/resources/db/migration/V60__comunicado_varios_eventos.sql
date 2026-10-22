-- Comunicado para inscritos em VARIOS eventos de uma vez ("a turma do
-- minicurso X e a do Y"). O evento deixa de ser uma coluna de
-- `comunicado` e passa para uma tabela de ligacao.
CREATE TABLE public.comunicado_evento (
    comunicado_id INT NOT NULL REFERENCES public.comunicado(id) ON DELETE CASCADE,
    -- CASCADE aqui so apaga a ligacao: o comunicado continua no
    -- historico, como antes com o ON DELETE SET NULL de evento_id.
    evento_id     INT NOT NULL REFERENCES public.evento(id) ON DELETE CASCADE,
    PRIMARY KEY (comunicado_id, evento_id)
);

-- Disparos antigos (um evento so) entram na tabela nova para o historico
-- continuar mostrando o evento de cada um.
INSERT INTO public.comunicado_evento (comunicado_id, evento_id)
SELECT id, evento_id FROM public.comunicado WHERE evento_id IS NOT NULL;

ALTER TABLE public.comunicado DROP COLUMN evento_id;

-- O valor do enum continua INSCRITOS_EM_EVENTO; so o rotulo mudou para o
-- plural, entao as linhas existentes de `publico` nao precisam mudar.
