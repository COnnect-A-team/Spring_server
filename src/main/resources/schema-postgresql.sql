CREATE UNIQUE INDEX IF NOT EXISTS uq_comparison_lists_user_name -- 같은 사용자가 같은 이름의 비교목록을 생성 못하도록 정의
    ON comparison_lists (
    user_id,
    LOWER(REGEXP_REPLACE(list_name, '\s+', '', 'g'))
    );