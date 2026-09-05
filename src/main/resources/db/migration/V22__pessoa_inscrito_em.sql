-- Data/hora do cadastro publico (InscricaoService.cadastrar). Cadastros
-- anteriores a esta migration ficam com o campo nulo -- nao ha como saber
-- retroativamente a data real deles.
ALTER TABLE public.pessoa ADD COLUMN inscrito_em TIMESTAMP;
