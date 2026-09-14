-- `codigo` permite o backend referenciar uma conquista por uma chave estável
-- (ver ConquistaService), mesmo padrão de tipo_inscricao.codigo (V24).
ALTER TABLE public.conquista ADD COLUMN codigo character varying(50);

INSERT INTO public.conquista (nome, raridade, pontos_base, codigo)
VALUES ('Um dos Primeiros', 3, 50, 'PRIMEIROS_10');
