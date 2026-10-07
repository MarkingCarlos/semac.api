-- Liga/desliga a escolha de minicursos no /participantes (botao no /admin,
-- aba Conteudo). Comeca fechada: a plataforma de participantes e liberada
-- antes das inscricoes em minicurso. Ver ConfiguracaoInscricao /
-- InscricaoEventoService.exigirEscolhaMinicursosAberta.
ALTER TABLE public.configuracao_inscricao
    ADD COLUMN escolha_minicursos_aberta BOOLEAN NOT NULL DEFAULT FALSE;
