package com.aichuangzuo.user.modules.article.mapper;

import com.aichuangzuo.user.modules.article.entity.Article;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 作品 Mapper。
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
}