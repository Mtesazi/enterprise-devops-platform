ALTER TABLE departments
    ADD COLUMN code VARCHAR(50);

UPDATE departments
SET code = CONCAT('DEPT-', id)
WHERE code IS NULL;

ALTER TABLE departments
    ALTER COLUMN code SET NOT NULL;

ALTER TABLE departments
    ADD CONSTRAINT uk_departments_code UNIQUE (code);
