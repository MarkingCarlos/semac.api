-- tipo_evento ganha uma chave estavel `codigo`, espelho do enum
-- CodigoTipoEvento. Ate aqui o /admin criava tipos livremente e o unico
-- jeito de uma regra reconhecer um tipo era pelo nome, que e editavel.
-- A partir daqui os tipos sao um catalogo fechado em codigo
-- (CatalogoTiposEvento + TipoEventoSeedRunner); o /admin so edita nome,
-- pontos e exige_inscricao.
--
-- As linhas existentes sao casadas pelo nome que tinham em producao em
-- 1 Out 2026. Comparacao sem caixa e sem espacos nas pontas; "Mostra
-- Tecnica" aceita com e sem acento.
ALTER TABLE public.tipo_evento ADD COLUMN codigo character varying(40);

UPDATE public.tipo_evento
   SET codigo = CASE lower(trim(nome))
        WHEN 'abertura'            THEN 'ABERTURA'
        WHEN 'atividades noturnas' THEN 'ATIVIDADES_NOTURNAS'
        WHEN 'coffee-break'        THEN 'COFFEE_BREAK'
        WHEN 'credenciamento'      THEN 'CREDENCIAMENTO'
        WHEN 'debate'              THEN 'DEBATE'
        WHEN 'encerramento'        THEN 'ENCERRAMENTO'
        WHEN 'mesa redonda'        THEN 'MESA_REDONDA'
        WHEN 'minicurso'           THEN 'MINICURSO'
        WHEN 'mostra técnica'      THEN 'MOSTRA_TECNICA'
        WHEN 'mostra tecnica'      THEN 'MOSTRA_TECNICA'
        WHEN 'palestra'            THEN 'PALESTRA'
   END;

-- Nao chuta: tipo com nome fora da lista derruba a migration com os nomes
-- na mensagem. Renomeie (ou remova, se nao tiver eventos) e suba de novo.
DO $$
DECLARE
    sem_codigo text;
BEGIN
    SELECT string_agg(format('%s (id %s)', nome, id), ', ')
      INTO sem_codigo
      FROM public.tipo_evento
     WHERE codigo IS NULL;

    IF sem_codigo IS NOT NULL THEN
        RAISE EXCEPTION 'V49: tipos de evento sem codigo correspondente: %', sem_codigo;
    END IF;
END $$;

ALTER TABLE public.tipo_evento ALTER COLUMN codigo SET NOT NULL;
ALTER TABLE ONLY public.tipo_evento
    ADD CONSTRAINT uk_tipo_evento_codigo UNIQUE (codigo);
