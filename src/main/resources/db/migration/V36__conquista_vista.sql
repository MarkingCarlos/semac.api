-- `vista_em` marca que a celebracao daquela conquista ja foi exibida ao
-- participante. E o que garante que a animacao de conquista desbloqueada
-- apareca UMA VEZ SO -- ve-la de novo a cada abertura do app viraria
-- incomodo.
--
-- Fica no banco, e nao em localStorage, porque precisa valer em qualquer
-- dispositivo: quem abre no celular e depois no notebook nao pode assistir
-- duas vezes, e limpar os dados do navegador nao pode ressuscitar tres ou
-- quatro celebracoes de uma vez.
--
-- Null = conquistada mas ainda nao celebrada (entra na fila da animacao).
-- Marcada uma a uma, quando a animacao daquela conquista termina, para que
-- fechar o app no meio nao perca as que faltam nem repita as ja vistas.
ALTER TABLE public.participante_conquista ADD COLUMN vista_em timestamp(6) without time zone;

-- As conquistas que ja existiam antes desta coluna nascem como vistas: elas
-- foram concedidas quando ainda nao havia animacao nenhuma, e celebra-las
-- agora, retroativamente, seria justamente o contrario do que a coluna
-- existe para evitar.
UPDATE public.participante_conquista SET vista_em = obtida_em WHERE vista_em IS NULL;
