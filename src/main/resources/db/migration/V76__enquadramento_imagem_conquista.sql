-- Enquadramento da imagem da conquista na moldura dos cards, mesmo esquema
-- da foto do palestrante (V58): a imagem original fica intacta; aqui so o
-- ponto em evidencia (em % da imagem, 50/50 = centro) e o zoom (1 = imagem
-- inteira na moldura). Ajustado no /admin -> Informacoes SEMAC.
ALTER TABLE public.conquista
    ADD COLUMN imagem_pos_x NUMERIC(5, 2) NOT NULL DEFAULT 50,
    ADD COLUMN imagem_pos_y NUMERIC(5, 2) NOT NULL DEFAULT 50,
    ADD COLUMN imagem_zoom  NUMERIC(4, 2) NOT NULL DEFAULT 1;
