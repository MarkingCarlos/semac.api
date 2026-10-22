-- O crachá de professor passou a ter arte própria, sem rótulo
-- PROFESSOR/PROFESSORA, então o gênero não é mais usado.
ALTER TABLE public.professor_convidado DROP COLUMN genero;

-- Professores e servidores convidados da SEMAC 2026. Nomes sem título
-- (Prof. Dr. etc.): o crachá usa as primeiras palavras do nome. Quem já
-- foi cadastrado pela tela com o mesmo nome não é duplicado.
INSERT INTO public.professor_convidado (nome)
SELECT novo.nome
FROM (VALUES
    ('Adriano Mauro Cansian'),
    ('Aleardo Manacero Junior'),
    ('Arnaldo Candido Junior'),
    ('Carlos Roberto Valêncio'),
    ('Carina Alexandra Rondini'),
    ('Diego Renan Bruno'),
    ('Geraldo Francisco Donegá Zafalon'),
    ('Guilherme Freire Roberto'),
    ('Leandro Alves Neves'),
    ('Lucas Correia Ribas'),
    ('Marilaine Colnago'),
    ('Renata Spolon Lobato'),
    ('Rodrigo Capobianco Guido'),
    ('Rogéria Cristiane Gratão de Souza'),
    ('Wallace Correa de Oliveira Casaca'),
    ('Adriana Felix Roberto Ártico'),
    ('Sarah Fortino Lasmar Cunha'),
    ('Thiago Luiz Parolin')
) AS novo (nome)
WHERE NOT EXISTS (
    SELECT 1 FROM public.professor_convidado existente
    WHERE lower(existente.nome) = lower(novo.nome)
);
