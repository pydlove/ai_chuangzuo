UPDATE u_share_config
SET content = '🎁 {title}来啦！我在爱创作发现了超多好礼，会员、创作币、折扣券等你来抽～\n使用我的邀请码 {code} 注册，还能额外获得一次抽奖机会！\n快来一起参与：{inviteUrl}'
WHERE scene_key = 'lottery'
  AND content LIKE '%{url}%';
