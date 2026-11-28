-- 비교목록 정렬 설정을 제거한 기존 DB의 잔여 컬럼을 정리
ALTER TABLE comparison_lists DROP COLUMN IF EXISTS sort_nutrient;
ALTER TABLE comparison_lists DROP COLUMN IF EXISTS sort_direction;

CREATE UNIQUE INDEX IF NOT EXISTS uq_comparison_lists_user_name -- 같은 사용자가 같은 이름의 비교목록을 생성 못하도록 정의
    ON comparison_lists (
    user_id,
    LOWER(REGEXP_REPLACE(list_name, '\s+', '', 'g'))
    );
