-- Удаляем существующее ограничение внешнего ключа
ALTER TABLE task_history 
DROP FOREIGN KEY FKjqraeud129avhcva579fhioj3;

-- Создаем новое ограничение с ON DELETE SET NULL
ALTER TABLE task_history
ADD CONSTRAINT fk_task_history_task
FOREIGN KEY (task_id)
REFERENCES tasks(id)
ON DELETE SET NULL; 