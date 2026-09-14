ALTER TABLE `user` ADD COLUMN account_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE img ADD COLUMN pool_id INT;
ALTER TABLE img ADD COLUMN parent_image_id INT;
ALTER TABLE img ADD COLUMN storage_kind VARCHAR(16) NOT NULL DEFAULT 'BASE';
ALTER TABLE img ADD COLUMN visibility VARCHAR(16) NOT NULL DEFAULT 'PUBLIC';
ALTER TABLE img ADD COLUMN lifecycle VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';

UPDATE img target
SET pool_id = (SELECT relation.pool_id FROM img_pool relation WHERE relation.img_id = target.img_id);

UPDATE img target
SET parent_image_id = (
    SELECT relation.img_id
    FROM img_child relation
    WHERE relation.img_child_id = target.img_id
);

UPDATE img
SET storage_kind = CASE WHEN parent_image_id IS NULL THEN 'BASE' ELSE 'DELTA' END,
    visibility = CASE WHEN img_type = 0 THEN 'PUBLIC' ELSE 'PRIVATE' END,
    lifecycle = CASE WHEN is_delete = 0 THEN 'ACTIVE' ELSE 'DELETED' END;

ALTER TABLE vm ADD COLUMN image_id INT;
UPDATE vm target
SET image_id = (SELECT relation.img_id FROM img_vm relation WHERE relation.vm_id = target.vm_id);

ALTER TABLE version ADD COLUMN pool_id INT;
UPDATE version target
SET pool_id = (SELECT relation.pool_id FROM version_pool relation WHERE relation.version_id = target.id);

ALTER TABLE pool ADD COLUMN root_image_id INT;
UPDATE pool target
SET root_image_id = (
    SELECT image.img_id
    FROM img image
    WHERE image.pool_id = target.id AND image.parent_image_id IS NULL
);
