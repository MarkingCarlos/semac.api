-- Cifra do Dia de segunda-feira (26/10): duas etapas.
--
-- 1. Substituicao pelo alfabeto SEMAC26: as letras A..Z trocadas por
--    S E M A C 2 6 em ordem, e a cada volta o simbolo ganha um numero
--    pequeno (H = S1, O = S2, V = S3 ...).
--    CONFIANCA -> M S2 61 2 E1 S 61 M S   (resposta da etapa 1)
-- 2. Zigue-zague duplo sobre os 9 SIMBOLOS (letra + numero pequeno conta
--    como um so): 3 trilhos e depois 4 trilhos.
--    -> MME1S61S261S2. O "-4,3" na senha e a ordem de desfazer: 4 trilhos,
--    depois 3.
--
-- Quem decifra desfaz o zigue-zague (etapa 1, a senha parcial) e depois a
-- substituicao (etapa 2, a palavra). Nos comentarios os sobrescritos
-- estao escritos como numeros comuns; nos dados abaixo vao os de verdade.
WITH segunda AS (
    INSERT INTO public.desafio_cifra (ano, data, palestra, senha_cifrada) VALUES
        (2026, DATE '2026-10-26',
         'Interface com o Cidadão: Como Transparência e Linguagem Simples Fazem a Inovação Chegar',
         'MME¹S6¹S26¹S²-4,3')
    RETURNING id
)
INSERT INTO public.desafio_cifra_etapa (desafio_id, ordem, resposta, dica)
SELECT id, 1, 'MS²6¹2E¹S6¹MS',
       'A resposta é encontrada fazendo zigue-zague'
  FROM segunda
UNION ALL
SELECT id, 2, 'CONFIANCA',
       'O alfabeto desta cifra não é o que você conhece. Ele é formado pelo nome do evento: SEMAC26. As letras do alfabeto são trocadas por esses símbolos em ordem e, quando eles acabam, a sequência recomeça com um número pequeno no alto para marcar a volta.'
  FROM segunda;
