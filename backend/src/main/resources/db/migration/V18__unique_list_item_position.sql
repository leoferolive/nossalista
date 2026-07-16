-- V18__unique_list_item_position.sql
-- Fecha a race de `position` em list_items: dois add_items concorrentes na
-- mesma lista podem ler o mesmo maxPosition (findMaxPositionByListId) e
-- gravar itens com position duplicada, corrompendo a ordenação exibida ao
-- usuário. Ver docs/plans/onda2-honestidade-metrica/T3-race-position.md.
--
-- 1) Renumera position de forma determinística (por list_id, ordenando por
--    position/created_at/id) ANTES de criar a constraint. Isso normaliza
--    qualquer duplicata/gap pré-existente; se os dados já estiverem
--    consistentes, o UPDATE é um no-op lógico (mesma ordem relativa).
-- 2) Cria UNIQUE(list_id, position): o banco passa a rejeitar qualquer
--    tentativa de gravar duas positions iguais na mesma lista. O retry
--    manual em ListItemService.addItem recalcula a position e tenta de
--    novo quando isso acontece, em vez de propagar erro ao chamador.
--
-- Sintaxe validada em PostgreSQL 16 e H2 (MODE=PostgreSQL) — usa subquery
-- correlacionada em vez de "UPDATE ... FROM" (extensão não padrão) para
-- ser portável entre os dois bancos.

UPDATE list_items li
SET position = (
    SELECT ranked.new_position
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY list_id
                   ORDER BY position ASC, created_at ASC, id ASC
               ) - 1 AS new_position
        FROM list_items
    ) ranked
    WHERE ranked.id = li.id
);

ALTER TABLE list_items
    ADD CONSTRAINT uq_list_items_list_position UNIQUE (list_id, position);
