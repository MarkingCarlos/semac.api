-- Cada brinde passa a ser uma unidade: ou esta disponivel, ou ja foi
-- entregue (tem linha em `ganhadores_sorteio`). A quantidade em estoque
-- deixa de existir -- o cadastro agora e feito pelo evento, um item por
-- sorteio, e "Caneca x 10" nunca foi o caso de uso.
--
-- Brinde que tinha quantidade > 1 passa a valer uma unidade so.

ALTER TABLE public.brinde DROP COLUMN quantidade;
