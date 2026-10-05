-- Jogo da Criptografia (/criptografia): as palavras do desafio da 
-- criptografia e os acertos dos participantes.

CREATE TABLE public.criptografia_palavra (
    id      SERIAL PRIMARY KEY,
    palavra VARCHAR(20) NOT NULL,
    data DATE NOT NULL
);

CREATE TABLE public.criptografia_jogo_acerto (
    id          SERIAL PRIMARY KEY,
    pessoa_id   INT NOT NULL REFERENCES public.pessoa(id) ON DELETE CASCADE,
    palavra_id  INT NOT NULL REFERENCES public.criptografia_palavra(id) ON DELETE CASCADE,
    CONSTRAINT criptografia_jogo_pessoa_palavra_key UNIQUE (pessoa_id, palavra_id)
);

INSERT INTO public.regra_xp (chave, nome, valor, unidade, ordem) VALUES
    ('CRIPTOGRAFIA_ACERTO', 'Acertar o Palavra da Criptografia', 25, 'PONTOS', 40);