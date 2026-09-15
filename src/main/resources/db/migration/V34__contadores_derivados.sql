-- Os dois contadores restantes da escala deixam de ser digitados, pelo
-- mesmo motivo dos inscritos na V33: zerados, faziam todo item com
-- escala por cabeca valer R$ 0,00 sem indicacao nenhuma na tela.
--
--   membros_comissao       -> pessoas com role definido e != PARTICIPANTE
--                             (MEMBRO, DIRETOR_*, PRESIDENTE) -- as mesmas
--                             que o /admin lista em "Comissao"
--   palestrantes_previstos -> total de registros em `palestrante`
--
-- Com isso nenhum parametro do orcamento e digitado: o teto ja vinha do
-- saldo da comissao (V31) e os tres multiplicadores agora vem do banco.
-- A tabela fica so com o ano da edicao.
ALTER TABLE public.orcamento DROP COLUMN membros_comissao;
ALTER TABLE public.orcamento DROP COLUMN palestrantes_previstos;
