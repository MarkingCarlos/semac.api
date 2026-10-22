-- Professores convidados para o evento (cadastrados na subaba Crachás da
-- aba Pessoas do /admin). Não têm conta, inscrição nem check-in: a tabela
-- existe só para imprimir o crachá deles. `genero` ('M' ou 'F') define o
-- rótulo do crachá — PROFESSOR ou PROFESSORA.

CREATE TABLE public.professor_convidado (
    id     SERIAL PRIMARY KEY,
    nome   VARCHAR(255) NOT NULL,
    genero VARCHAR(1)   NOT NULL CHECK (genero IN ('M', 'F'))
);
