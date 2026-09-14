-- Previsao de gastos da SEMAC: o elo que faltava entre a cotacao (preco
-- pesquisado) e a compra (dinheiro que ja saiu). Modelo generico -- cada
-- sub-tabela da planilha de previsao (Coffee Break, Kit, Passagem,
-- Hospedagem, Trafego Pago, Mostra Tecnica, Colecionaveis, Servicos,
-- Operacionais, Outros) vira uma CATEGORIA, nao uma tabela propria.
--
-- Carga inicial ao final do arquivo: os lancamentos da planilha
-- "previsao de gastos da semac" (setembro/2026).

-- ---------------------------------------------------------------
-- previsao_categoria
-- ---------------------------------------------------------------
CREATE TABLE public.previsao_categoria (
    id    SERIAL       PRIMARY KEY,
    nome  VARCHAR(80)  NOT NULL UNIQUE,
    cor   VARCHAR(20)  NOT NULL,
    teto  DECIMAL(10,2),
    ordem INT          NOT NULL DEFAULT 0
);

COMMENT ON COLUMN public.previsao_categoria.teto  IS 'Limite opcional da categoria; NULL = sem limite proprio, sujeito apenas ao teto global do orcamento.';
COMMENT ON COLUMN public.previsao_categoria.ordem IS 'Ordem de exibicao nos graficos e na tabela.';

-- ---------------------------------------------------------------
-- orcamento
-- ---------------------------------------------------------------
-- Uma linha por edicao. Alem do teto, guarda os contadores que a planilha
-- tratava como constantes soltas ("estimativa total: 140 pessoas",
-- "comissao: 39 membros", "Qtd Kits 34") e que aqui alimentam a `escala`
-- de previsao_item.
CREATE TABLE public.orcamento (
    id                     SERIAL        PRIMARY KEY,
    ano                    INT           NOT NULL UNIQUE,
    teto                   DECIMAL(10,2) NOT NULL,
    inscritos_previstos    INT           NOT NULL DEFAULT 0,
    membros_comissao       INT           NOT NULL DEFAULT 0,
    palestrantes_previstos INT           NOT NULL DEFAULT 0
);

-- ---------------------------------------------------------------
-- previsao_item
-- ---------------------------------------------------------------
-- NAO ha coluna valor_total, ao contrario de `compra`. O total depende da
-- `escala`, e o fator de escala (nº de inscritos, de membros, de
-- palestrantes) muda ao longo da organizacao -- um total persistido
-- nasceria desatualizado. O backend calcula na leitura:
--     (valor_unitario * quantidade + frete) * fator(escala)
CREATE TABLE public.previsao_item (
    id             SERIAL        PRIMARY KEY,
    descricao      VARCHAR(255)  NOT NULL,
    categoria_id   INT           NOT NULL REFERENCES public.previsao_categoria(id),
    fornecedor_id  INT           REFERENCES public.fornecedor(id),
    quantidade     INT           NOT NULL DEFAULT 1,
    valor_unitario DECIMAL(10,2) NOT NULL,
    frete          DECIMAL(10,2) NOT NULL DEFAULT 0,
    escala         VARCHAR(20)   NOT NULL DEFAULT 'FIXA',
    conta          VARCHAR(20),
    status         VARCHAR(20)   NOT NULL DEFAULT 'PREVISTO',
    data_prevista  DATE,
    observacao     VARCHAR(500),
    compra_id      INT           REFERENCES public.compra(id),
    CONSTRAINT previsao_item_quantidade_check CHECK (quantidade >= 0),
    CONSTRAINT previsao_item_escala_check CHECK (escala IN ('FIXA', 'POR_INSCRITO', 'POR_COMISSAO', 'POR_PALESTRANTE')),
    CONSTRAINT previsao_item_conta_check  CHECK (conta IS NULL OR conta IN ('COMISSAO', 'FUNDUNESP')),
    CONSTRAINT previsao_item_status_check CHECK (status IN ('PREVISTO', 'COTADO', 'CONTRATADO', 'PAGO'))
);

COMMENT ON COLUMN public.previsao_item.escala     IS 'FIXA = total e o proprio valor; POR_INSCRITO/POR_COMISSAO/POR_PALESTRANTE multiplicam pelo contador correspondente em `orcamento`.';
COMMENT ON COLUMN public.previsao_item.conta      IS 'Conta de origem do gasto. NULL = ainda nao definida -- a planilha de origem nao preenchia esta coluna nas saidas.';
COMMENT ON COLUMN public.previsao_item.quantidade IS 'Pode ser 0: item com preco ja pesquisado mas quantidade ainda nao decidida (ex.: colecionaveis).';
COMMENT ON COLUMN public.previsao_item.compra_id  IS 'Preenchido quando a previsao vira compra real; a previsao passa a status PAGO.';

CREATE INDEX idx_previsao_item_categoria ON public.previsao_item (categoria_id);
CREATE INDEX idx_previsao_item_status    ON public.previsao_item (status);

-- ===============================================================
-- CARGA INICIAL
-- ===============================================================

INSERT INTO public.orcamento (ano, teto, inscritos_previstos, membros_comissao, palestrantes_previstos)
VALUES (2026, 35000.00, 140, 39, 34);

INSERT INTO public.previsao_categoria (nome, cor, ordem) VALUES
    ('Coffee Break',         '#fb923c',  1),
    ('Kit do Participante',  '#E79839',  2),
    ('Passagem',             '#4ade80',  3),
    ('Hospedagem',           '#22d3ee',  4),
    ('Gastos Operacionais',  '#94a3b8',  5),
    ('Trafego Pago',         '#c084fc',  6),
    ('Mostra Tecnica',       '#d4609a',  7),
    ('Colecionaveis',        '#fbbf24',  8),
    ('Servicos Contratados', '#60a5fa',  9),
    ('Outros',               '#78716c', 10);

-- Fornecedores citados na planilha que ainda nao existirem na base.
-- `fornecedor.nome` nao tem UNIQUE, por isso o guard com NOT EXISTS.
INSERT INTO public.fornecedor (nome)
SELECT v.nome FROM (VALUES
    ('Zafer'), ('Ariart'), ('Jacques'), ('Shopee'), ('Copfac'),
    ('Affectio'), ('Tia So'), ('Bravo City Hotel')
) AS v(nome)
WHERE NOT EXISTS (SELECT 1 FROM public.fornecedor f WHERE f.nome = v.nome);

-- --- Coffee Break -- R$ 6.978,74 -------------------------------
-- "Quantidade - centos" da planilha e a quantidade de centos de salgado;
-- o valor lancado ja e o total do turno (salgado + bolo), por isso vai
-- como item unico e os centos ficam na observacao.
INSERT INTO public.previsao_item (descricao, categoria_id, valor_unitario, observacao)
SELECT v.descricao, c.id, v.valor, v.obs
FROM (VALUES
    ('Coffee Break - Segunda (manha)', 815.00, '7 centos - 1 bolo M + 30un carolina + 40 lanche natural'),
    ('Coffee Break - Segunda (tarde)', 910.00, '8 centos - 1 bolo G'),
    ('Coffee Break - Terca (manha)',   890.00, '8 centos - 1 bolo G'),
    ('Coffee Break - Terca (tarde)',   420.00, '4 centos - 1 bolo M'),
    ('Coffee Break - Quarta (manha)',  440.00, '4 centos - 1 bolo M'),
    ('Coffee Break - Quarta (tarde)',  420.00, '4 centos - 1 bolo M'),
    ('Coffee Break - Quinta (manha)',  685.00, '6 centos - 1 bolo G'),
    ('Coffee Break - Quinta (tarde)',  440.00, '4 centos - 1 bolo M'),
    ('Coffee Break - Sexta (manha)',   910.00, '8 centos - 1 bolo G'),
    ('Coffee Break - Sexta (tarde)',   855.00, '8 centos - 1 bolo G'),
    ('Sucos',                          193.74, NULL)
) AS v(descricao, valor, obs)
CROSS JOIN public.previsao_categoria c WHERE c.nome = 'Coffee Break';

-- --- Kit do Participante ---------------------------------------
-- Custo unitario do kit: R$ 98,00 (soma dos 10 itens abaixo).
-- Escala POR_INSCRITO: com 140 inscritos previstos o total e R$ 13.720,00
-- -- valor que a planilha nao contabilizava, porque a coluna
-- "Nº de inscritos" estava zerada e zerava o total do kit.
-- A coluna "Frete (140 un.)" da planilha estava vazia em todos os itens.
INSERT INTO public.previsao_item (descricao, categoria_id, fornecedor_id, valor_unitario, escala)
SELECT v.descricao, c.id,
       (SELECT id FROM public.fornecedor WHERE nome = v.fornecedor LIMIT 1),
       v.valor, 'POR_INSCRITO'
FROM (VALUES
    ('Caderneta',       'Zafer',     9.78),
    ('Camiseta',        'Ariart',   44.00),
    ('Folder',          'Jacques',   4.00),
    ('Botton',          'Shopee',    1.94),
    ('Ecobag',          'Zafer',    18.61),
    ('Garrafa',         'Zafer',    12.90),
    ('Cracha',          'Jacques',   1.40),
    ('Cordao (cracha)', 'Copfac',    2.50),
    ('Adesivo',         'Jacques',   1.00),
    ('Caneta',          'Affectio',  1.87)
) AS v(descricao, fornecedor, valor)
CROSS JOIN public.previsao_categoria c WHERE c.nome = 'Kit do Participante';

-- Kit da comissao: lancado como valor fechado, nao por membro.
-- A planilha rotula a formula como "Qtd * (custo kit + 2)" = 39 * 100 =
-- R$ 3.900,00, mas o valor da celula e R$ 5.344,00 (~R$ 137,03/kit, o que
-- bate com a nota sobre a 2a camiseta e a ausencia de folder/adesivo).
-- Rotulo e valor se contradizem na origem; mantido o VALOR, com a
-- divergencia registrada para a comissao decidir o custo unitario real.
INSERT INTO public.previsao_item (descricao, categoria_id, valor_unitario, escala, observacao)
SELECT 'Kit Comissao (39 membros)', id, 5344.00, 'FIXA',
       'DIVERGENCIA NA PLANILHA DE ORIGEM: rotulo da formula "Qtd * (custo kit + 2)" daria R$ 3.900,00, mas a celula trazia R$ 5.344,00 (~R$ 137,03/kit: kit base + 2a camiseta, sem folder e sem adesivo). Mantido o valor; definir o custo unitario e trocar para escala POR_COMISSAO.'
FROM public.previsao_categoria WHERE nome = 'Kit do Participante';

-- Kit de palestrante/professor: somente cracha (1,40) + cordao (2,50).
-- 34 * 3,90 = R$ 132,60, fecha exatamente com a planilha.
INSERT INTO public.previsao_item (descricao, categoria_id, valor_unitario, escala, observacao)
SELECT 'Kit Palestrante / Professor', id, 3.90, 'POR_PALESTRANTE', 'Somente cracha (R$ 1,40) + cordao (R$ 2,50).'
FROM public.previsao_categoria WHERE nome = 'Kit do Participante';

-- --- Passagem de palestrantes -- R$ 1.920,00 -------------------
INSERT INTO public.previsao_item (descricao, categoria_id, valor_unitario, observacao)
SELECT v.descricao, c.id, v.valor, v.obs
FROM (VALUES
    ('Passagem aerea - palestrante RJ', 1084.00, 'Aviao. Ida e volta (dia 8 / dia 9).'),
    ('Passagem aerea - palestrante SP',  836.00, 'Aviao. Ida e volta (dia 8 / dia 9).')
) AS v(descricao, valor, obs)
CROSS JOIN public.previsao_categoria c WHERE c.nome = 'Passagem';

-- --- Hospedagem de palestrantes -- R$ 286,00 -------------------
INSERT INTO public.previsao_item (descricao, categoria_id, fornecedor_id, valor_unitario, observacao)
SELECT v.descricao, c.id,
       (SELECT id FROM public.fornecedor WHERE nome = 'Bravo City Hotel' LIMIT 1),
       v.valor, '1 diaria.'
FROM (VALUES
    ('Hospedagem - palestrante RJ', 143.00),
    ('Hospedagem - palestrante SP', 143.00)
) AS v(descricao, valor)
CROSS JOIN public.previsao_categoria c WHERE c.nome = 'Hospedagem';

-- --- Gastos operacionais -- R$ 535,00 --------------------------
INSERT INTO public.previsao_item (descricao, categoria_id, quantidade, valor_unitario, observacao)
SELECT v.descricao, c.id, v.qtd, v.valor, v.obs
FROM (VALUES
    ('Banner (CIC)',              10,   5.50, NULL),
    ('Banner (Pulpito)',           2,  20.00, NULL),
    ('Banner (Mesa Auditorio)',    1, 195.00, NULL),
    ('Banner (Frente Auditorio)',  1, 205.00, 'Hackathon'),
    ('Banner (Coffee Break)',      1,  40.00, 'Hackathon')
) AS v(descricao, qtd, valor, obs)
CROSS JOIN public.previsao_categoria c WHERE c.nome = 'Gastos Operacionais';

-- --- Trafego pago -- R$ 430,00 ---------------------------------
INSERT INTO public.previsao_item (descricao, categoria_id, quantidade, valor_unitario, observacao)
SELECT v.descricao, c.id, v.qtd, v.valor, v.obs
FROM (VALUES
    ('Post patrocinado - cota Ouro',                3,  50.00, NULL),
    ('Post + video entrevista - cota Especial',     1, 250.00, 'Post R$ 50,00 + video entrevista R$ 200,00.'),
    ('Post - abertura das inscricoes',              1,  30.00, NULL),
    ('Post - kit do participante',                  1,   0.00, 'Valor ainda nao definido na planilha de origem.'),
    ('Post patrocinado - cota Platina',             0,  50.00, 'Preco por post definido, quantidade ainda nao decidida -- por isso nao entrava no total da planilha.')
) AS v(descricao, qtd, valor, obs)
CROSS JOIN public.previsao_categoria c WHERE c.nome = 'Trafego Pago';

-- --- Mostra Tecnica -- R$ 353,00 -------------------------------
-- A planilha trazia uma segunda tabela com milho/manteiga/oleo/sal
-- (R$ 22,99). Nao e gasto adicional: e a composicao da pipoca (R$ 23,00),
-- feita pelo apoio. Lancada como observacao para nao contar em dobro.
INSERT INTO public.previsao_item (descricao, categoria_id, fornecedor_id, valor_unitario, observacao)
SELECT v.descricao, c.id,
       (SELECT id FROM public.fornecedor WHERE nome = v.fornecedor LIMIT 1),
       v.valor, v.obs
FROM (VALUES
    ('Pipoca',  NULL::varchar, 23.00, 'Feito pelo apoio. Insumos: milho 4 x R$ 3,48 = R$ 13,92; oleo 1 x R$ 6,79; sal 1 x R$ 2,29; manteiga R$ 4,99 (qtd 0).'),
    ('Churros', 'Tia So', 330.00, NULL),
    ('Sorvete', NULL,       0.00, 'Valor ainda nao levantado na planilha de origem.')
) AS v(descricao, fornecedor, valor, obs)
CROSS JOIN public.previsao_categoria c WHERE c.nome = 'Mostra Tecnica';

-- --- Colecionaveis -- R$ 0,00 ----------------------------------
-- Precos pesquisados, quantidades ainda nao decididas (quantidade = 0),
-- por isso nao somam. A planilha listava "mousepad" duas vezes com o
-- mesmo preco; mantida uma linha so.
INSERT INTO public.previsao_item (descricao, categoria_id, quantidade, valor_unitario, observacao)
SELECT v.descricao, c.id, 0, v.valor, 'Preco pesquisado; quantidade a definir.'
FROM (VALUES
    ('Mousepad',            9.64),
    ('Chaveiro tecla',     10.16),
    ('Botton holografico',  4.00),
    ('Adesivo holografico', 2.13),
    ('Marca pagina',        2.40)
) AS v(descricao, valor)
CROSS JOIN public.previsao_categoria c WHERE c.nome = 'Colecionaveis';

-- --- Outros -- R$ 183,55 ---------------------------------------
-- Unica secao de saidas em que a planilha preenchia a conta de origem.
INSERT INTO public.previsao_item (descricao, categoria_id, quantidade, valor_unitario, conta, observacao)
SELECT v.descricao, c.id, v.qtd, v.valor, v.conta, v.obs
FROM (VALUES
    ('Alimentacao',              1,  18.00, 'COMISSAO', 'Dia 1 - Tech Summit'),
    ('Alimentacao',              1,  24.00, 'COMISSAO', 'Dia 1 - Tech Summit'),
    ('Uber (ida e volta)',       1,  23.82, 'COMISSAO', 'Dia 1 - Tech Summit'),
    ('Alimentacao',              1,  18.00, 'COMISSAO', 'Dia 2 - Tech Summit'),
    ('Uber (2 ida e volta)',     1,  37.91, 'COMISSAO', 'Dia 2 - Tech Summit'),
    ('Panfletos (Newsletter)',   1,  30.00, 'COMISSAO', 'Tech Summit. Jacques: R$ 1,00 / folha.'),
    ('Porta cartao',             1,  31.82, NULL,       'Lote de 150 unidades por R$ 31,82 (unitario nao fecha em centavos). Conta de origem nao informada na planilha.'),
    ('Estancia Caipira',         0,  39.90, NULL,       'Alimentacao de palestrantes, R$ 39,90 por pessoa. Quantidade a definir.'),
    ('Boliche',                  0,  40.00, NULL,       'Game night, R$ 40,00 por pessoa. Quantidade a definir.'),
    ('Cartao de agradecimento',  0,   0.00, NULL,       'Valor ainda nao levantado na planilha de origem.')
) AS v(descricao, qtd, valor, conta, obs)
CROSS JOIN public.previsao_categoria c WHERE c.nome = 'Outros';
