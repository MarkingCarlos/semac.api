-- Liga/desliga o botao "Inscreva-se" da Home, uma linha por ano. Ver
-- ConfiguracaoInscricao / ConfiguracaoInscricaoController.
CREATE TABLE public.configuracao_inscricao (
    id                  SERIAL PRIMARY KEY,
    ano                 INT NOT NULL,
    inscricoes_abertas  BOOLEAN NOT NULL,
    CONSTRAINT configuracao_inscricao_ano_key UNIQUE (ano)
);
