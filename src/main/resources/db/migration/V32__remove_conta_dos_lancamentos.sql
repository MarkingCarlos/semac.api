-- A FUNDUNESP passa a ser apenas uma reserva fixa de emergencia: nao se
-- movimenta, nao recebe entrada e nao paga saida. Com isso nao existe
-- mais lancamento para marcar como FUNDUNESP, e a unica conta possivel
-- seria a da comissao -- ou seja, um campo com um valor so.
--
-- Entao o campo sai. O que a comissao tem para gastar passa a ser
-- simplesmente a soma de patrocinios recebidos, doacoes e inscricoes
-- (ver PrevisaoService.resumo), sem filtro de conta.
--
-- A V28 introduziu essas colunas e a V31 ja havia tirado o teto
-- digitado; esta fecha a simplificacao.

ALTER TABLE public.patrocinador  DROP COLUMN conta;
ALTER TABLE public.doador        DROP COLUMN conta;
ALTER TABLE public.compra        DROP COLUMN conta;
ALTER TABLE public.previsao_item DROP COLUMN conta;

-- A comissao nao tem mais caixa inicial: o saldo dela e calculado a
-- partir das entradas. Sobra uma linha em `caixa`, a da reserva da
-- FUNDUNESP, que continua sendo um valor digitado -- nao ha de onde
-- deriva-lo.
DELETE FROM public.caixa WHERE conta = 'COMISSAO';
