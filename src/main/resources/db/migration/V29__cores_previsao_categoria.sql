-- Recolore as categorias de previsao.
--
-- As cores semeadas na V27 vieram de CORES_CATEGORIA do front, que foi
-- pensada para selos sobre fundo claro. Contra a superficie do modulo
-- financeiro (#730835) elas falham: `#fb923c` (Coffee Break) e `#E79839`
-- (Kit do Participante) ficam a ΔE 3,6 -- ou seja, as duas maiores
-- categorias sao indistinguiveis a olho nu, e a ΔE 0,3 sob protanopia.
-- Havia ainda dois tons que caem abaixo do piso de croma e leem como
-- cinza (`#94a3b8`, `#78716c`).
--
-- As cores abaixo estao em OKLCH L 0.63 / C 0.16 com matizes espacados, e
-- passam as checagens contra #730835: banda de luminosidade, piso de
-- croma e contraste >= 3:1. Coffee Break e Kit ficam em matizes opostos
-- de proposito, por serem as duas maiores.
--
-- Vem numa migration propria, e nao editando a V27, porque a V27 ja foi
-- aplicada -- mexer no arquivo quebraria a validacao de checksum do
-- Flyway em todo banco que ja a rodou.
UPDATE public.previsao_categoria SET cor = '#c57300' WHERE nome = 'Coffee Break';
UPDATE public.previsao_categoria SET cor = '#009fc3' WHERE nome = 'Kit do Participante';
UPDATE public.previsao_categoria SET cor = '#47a03f' WHERE nome = 'Passagem';
UPDATE public.previsao_categoria SET cor = '#b564c2' WHERE nome = 'Hospedagem';
UPDATE public.previsao_categoria SET cor = '#978d00' WHERE nome = 'Gastos Operacionais';
UPDATE public.previsao_categoria SET cor = '#288de5' WHERE nome = 'Trafego Pago';
UPDATE public.previsao_categoria SET cor = '#d25989' WHERE nome = 'Mostra Tecnica';
UPDATE public.previsao_categoria SET cor = '#00a789' WHERE nome = 'Colecionaveis';
UPDATE public.previsao_categoria SET cor = '#8377e4' WHERE nome = 'Servicos Contratados';
UPDATE public.previsao_categoria SET cor = '#d85e43' WHERE nome = 'Outros';
