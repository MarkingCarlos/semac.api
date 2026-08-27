-- Meta de arrecadacao da campanha de doacao, por edicao. Uma linha por
-- ano: cada SEMAC define sua propria meta no admin (secao "Informacoes
-- SEMAC"), exibida na barra de progresso da pagina publica de doacao.
CREATE TABLE public.meta_doacao (
    id    SERIAL PRIMARY KEY,
    ano   INT NOT NULL UNIQUE,
    valor DECIMAL(10,2) NOT NULL
);
