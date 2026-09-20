-- Confirmar a inscricao nao vale mais xp.
--
-- Ate aqui, virar PARTICIPANTE dava 100 de xp de boas-vindas
-- (PessoaService.XP_INICIAL_CONFIRMACAO, removida junto com esta
-- migration): ponto ganho so por ter a inscricao confirmada, sem ter ido
-- a nada. Xp agora vem so de presenca em evento, acerto no Termo e
-- conquistas. Corrige tambem o que o comentario da V6__nivel.sql diz
-- sobre "xp inicial na confirmacao" -- aquela migration ja foi aplicada
-- e nao se mexe nela.
--
-- Quem ja estava confirmado carrega os 100 no acumulado, entao desconta
-- de todo PARTICIPANTE com xp definido, sem deixar negativo (quem tiver
-- menos de 100 por estorno de conquista cai pra 0). Sai uma vez so: a
-- V43 nao roda de novo.
UPDATE public.pessoa
   SET xp = GREATEST(0, xp - 100)
 WHERE role = 'PARTICIPANTE'
   AND xp IS NOT NULL;

-- Nivel acompanha o novo xp: o maior xp_minimo ja alcancado, mesmo
-- criterio de NivelRepository.findTopByXpMinimoLessThanEqualOrderByXpMinimoDesc.
-- Fica NULL se nao houver nivel cadastrado abaixo do xp da pessoa.
UPDATE public.pessoa p
   SET nivel_id = (
           SELECT n.id
             FROM public.nivel n
            WHERE n.xp_minimo <= p.xp
            ORDER BY n.xp_minimo DESC
            LIMIT 1
       )
 WHERE p.role = 'PARTICIPANTE'
   AND p.xp IS NOT NULL;
