SET NAMES utf8mb4;

DELETE FROM c_ai_prompt
WHERE prompt_code IN (
    'self_media_recommend_goals_v1',
    'self_media_recommend_niches_v1',
    'self_media_recommend_personas_v1'
);
