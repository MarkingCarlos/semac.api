-- Conquista deixa de ser esqueleto e vira catalogo configuravel.
--
-- A regra de cada conquista continua em codigo (ver CatalogoConquistas e
-- ConquistaService), referenciada pela chave estavel `codigo`. O que passa
-- a ser editavel em /admin -> Informacoes SEMAC e a apresentacao: nome,
-- pontos, imagem, descricao e a liberacao para o participante.
--
--   descricao      -> "como conseguir", exibido no card do participante
--   ativa          -> so conquista ativa aparece na area do participante e
--                     so ela pode ser concedida. Nasce false: nada vaza
--                     antes de a presidencia revisar e liberar
--   tipo_validacao -> AUTOMATICA (regra avaliada pelo ConquistaService) ou
--                     MANUAL (diretoria concede lendo o QR no /checkin)
--   ordem          -> posicao na grade da area do participante
ALTER TABLE public.conquista ADD COLUMN descricao character varying(500);
ALTER TABLE public.conquista ADD COLUMN ativa boolean DEFAULT false NOT NULL;
ALTER TABLE public.conquista ADD COLUMN tipo_validacao character varying(20) DEFAULT 'AUTOMATICA' NOT NULL;
ALTER TABLE public.conquista ADD COLUMN ordem integer DEFAULT 0 NOT NULL;

-- xp_creditado grava quanto a conquista de fato somou em pessoa.xp no
-- momento da concessao -- mesmo padrao de evento_participante.xp_creditado
-- (V12). Sem ele, revogar uma conquista cujos pontos foram editados no
-- /admin depois da concessao estornaria um valor diferente do creditado.
ALTER TABLE public.participante_conquista ADD COLUMN xp_creditado integer;

-- Conquista manual credita pontos que mexem no ranking, entao precisa de
-- autoria para um scan errado ser auditavel. Mesmo papel que
-- sorteio.organizador_id ja cumpre do outro lado. Fica null nas
-- automaticas: ali quem concedeu foi o sistema.
ALTER TABLE public.participante_conquista ADD COLUMN concedida_por_id integer;
ALTER TABLE ONLY public.participante_conquista
    ADD CONSTRAINT fk_participante_conquista_concedida_por
    FOREIGN KEY (concedida_por_id) REFERENCES public.pessoa(id);

-- PRIMEIROS_10 (semeada pela V26) era regra de teste e nao vai a producao.
-- O xp que ela creditou nao sai sozinho: pessoa.xp e um acumulado e nao
-- guarda a origem de cada ponto -- por isso o estorno explicito.
UPDATE public.pessoa p
   SET xp = GREATEST(0, COALESCE(p.xp, 0) - c.pontos_base)
  FROM public.participante_conquista pc
  JOIN public.conquista c ON c.id = pc.conquista_id
 WHERE pc.participante_id = p.id
   AND c.codigo = 'PRIMEIROS_10';

-- Nivel e funcao do xp, entao o estorno acima pode ter deixado gente num
-- nivel que ela nao alcanca mais. Reaplica aqui o mesmo criterio de
-- NivelRepository.findTopByXpMinimoLessThanEqualOrderByXpMinimoDesc, so
-- para quem foi afetado.
UPDATE public.pessoa p
   SET nivel_id = (SELECT n.id
                     FROM public.nivel n
                    WHERE n.xp_minimo <= COALESCE(p.xp, 0)
                    ORDER BY n.xp_minimo DESC
                    LIMIT 1)
 WHERE p.id IN (SELECT pc.participante_id
                  FROM public.participante_conquista pc
                  JOIN public.conquista c ON c.id = pc.conquista_id
                 WHERE c.codigo = 'PRIMEIROS_10');

DELETE FROM public.participante_conquista
 WHERE conquista_id IN (SELECT id FROM public.conquista WHERE codigo = 'PRIMEIROS_10');
DELETE FROM public.conquista WHERE codigo = 'PRIMEIROS_10';

-- `codigo` e a chave do catalogo: o ConquistaSeedRunner casa cada conquista
-- implementada em codigo com sua linha por ele. Duas linhas com o mesmo
-- codigo derrubariam ConquistaRepository.findByCodigo (devolve Optional)
-- com IncorrectResultSizeDataAccessException -- e no boot, dentro do
-- seeder. O prefixo LEGADO_ cobre linhas antigas sem codigo, se houver.
UPDATE public.conquista SET codigo = 'LEGADO_' || id WHERE codigo IS NULL;
ALTER TABLE public.conquista ALTER COLUMN codigo SET NOT NULL;
ALTER TABLE ONLY public.conquista ADD CONSTRAINT uq_conquista_codigo UNIQUE (codigo);
