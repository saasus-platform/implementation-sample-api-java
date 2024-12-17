CREATE TABLE delete_user_log (
     id SERIAL PRIMARY KEY,
     tenant_id VARCHAR(100) NOT NULL,
     user_id VARCHAR(100) NOT NULL,
     email VARCHAR(100) NOT NULL,
     delete_at TIMESTAMP DEFAULT current_timestamp
);

-- ロールを作成
CREATE ROLE delete_user_log_writer;

-- ポリシーを設定
CREATE POLICY delete_user_log_policy ON delete_user_log
    FOR ALL TO delete_user_log_writer
    USING (tenant_id = current_setting('app.current_tenant_id')::varchar);

-- ユーザーにロールを付与
GRANT delete_user_log_writer TO postgres;