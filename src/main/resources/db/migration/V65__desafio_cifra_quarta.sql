-- Cifra do Dia de quarta-feira (28/10): ASCII + Quadrado de Polibio.
--
-- Cada letra vira o codigo ASCII decimal (U = 85), cada digito vira o
-- resto por 5 com 0 escrito como 5 (85 -> 35), e o par e lido no Quadrado
-- de Polibio 5x5 com I/J juntos, linha e coluna (35 -> P).
-- USABILIDADE -> PNEAHFHCECD.
--
-- O mod 5 deixa duas letras possiveis em algumas posicoes (P/U, N/S, H/I,
-- F/L); so USABILIDADE forma palavra, e e isso que fecha o desafio.
WITH quarta AS (
    INSERT INTO public.desafio_cifra (ano, data, palestra, senha_cifrada) VALUES
        (2026, DATE '2026-10-28',
         'Além da Sintaxe: Como UI/UX, Acessibilidade e IA Transformam Linhas de Código em Impacto Real',
         'PNEAHFHCECD')
    RETURNING id
)
INSERT INTO public.desafio_cifra_etapa (desafio_id, ordem, resposta, dica)
SELECT id, 1, 'USABILIDADE',
       'A tabela ASCII e o Quadrado de Políbio se juntam em uma operação para camuflar o escondido.'
  FROM quarta;
