-- Separa os eventos abertos (palestra, mesa redonda, debate) dos que o
-- participante precisa escolher (minicurso), e semeia as pré-inscrições
-- de quem já foi confirmado antes desta migration.
--
-- `exige_inscricao` é dado, não convenção de nome: o nome do tipo é texto
-- livre editável no /admin, e uma regra baseada em LIKE '%minicurso%'
-- quebraria em silêncio se alguém renomeasse o tipo. O LIKE aparece aqui
-- uma única vez, para classificar os tipos que já existem.

ALTER TABLE public.tipo_evento ADD COLUMN exige_inscricao boolean DEFAULT false NOT NULL;

UPDATE public.tipo_evento
SET exige_inscricao = true
WHERE lower(nome) LIKE '%minicurso%'
   OR lower(nome) LIKE '%mini curso%'
   OR lower(nome) LIKE '%mini-curso%';

-- Todo participante confirmado entra automaticamente nos eventos abertos
-- (status 2 = INSCRITO). Daqui pra frente isso acontece na confirmação da
-- inscrição e na criação do evento (ver PessoaService/EventoService);
-- este INSERT cobre apenas o que já estava no banco.
INSERT INTO public.evento_participante (evento_id, participante_id, status)
SELECT e.id, p.id, 2
FROM public.evento e
JOIN public.tipo_evento te ON te.id = e.tipo_evento_id
CROSS JOIN public.pessoa p
WHERE te.exige_inscricao = false
  AND p.role = 'PARTICIPANTE'
  AND NOT EXISTS (
      SELECT 1
      FROM public.evento_participante ep
      WHERE ep.evento_id = e.id
        AND ep.participante_id = p.id
  );
