-- Oculta o ranking de XP (participantes e times) no /participantes e no
-- /time (botao no /admin, Informacoes SEMAC > Gamificacao). Comeca visivel.
-- O XP do proprio participante continua aparecendo. Ver ConfiguracaoInscricao /
-- PessoaService.buscarRanking / TimeService.
ALTER TABLE public.configuracao_inscricao
    ADD COLUMN ranking_oculto BOOLEAN NOT NULL DEFAULT FALSE;
