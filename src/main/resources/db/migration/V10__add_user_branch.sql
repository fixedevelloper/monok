-- Which branch an employee works at — needed so POS resources (floors/tables,
-- SSE) can be scoped per branch instead of leaking across the whole company.
-- Nullable: a user with no branch (e.g. an owner/super-admin) sees everything,
-- same convention as company/branch admin screens already use.
ALTER TABLE users
    ADD COLUMN branch_id BIGINT UNSIGNED NULL,
    ADD CONSTRAINT fk_users_branch FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE SET NULL;
