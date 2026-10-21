-- Enquadramento da foto do palestrante na moldura quadrada do telao do
-- debate. A foto original fica intacta; aqui so o ponto que vai no centro
-- (em % da imagem, 50/50 = centro) e o zoom (1 = foto cobrindo a moldura).
-- Ajustado no /admin -> Conteudo -> editar evento.
ALTER TABLE public.palestrante
    ADD COLUMN foto_pos_x NUMERIC(5, 2) NOT NULL DEFAULT 50,
    ADD COLUMN foto_pos_y NUMERIC(5, 2) NOT NULL DEFAULT 50,
    ADD COLUMN foto_zoom  NUMERIC(4, 2) NOT NULL DEFAULT 1;
