<template>
  <a-modal
    v-if='!pageMode'
    :open='visible'
    :title='modalTitle'
    width='900px'
    :footer='null'
    :mask-closable='false'
    @cancel='close'
  >
    <div class='create-flow'>
      <div class='flow-header'>
        <div class='flow-loading' v-if='loading'>
          <a-spin />
          <span>正在为你制定今日创作任务...</span>
        </div>
      </div>

      <div class='flow-steps'>
        <div class='flow-line-bg'></div>
        <div
          class='flow-line-progress'
          :style='{ width: ((flowData.step - 1) / (steps.length - 1) * 80) + "%" }'
        ></div>
        <div
          v-for='(s, idx) in steps'
          :key='idx'
          :class='["flow-step", { active: flowData.step >= idx + 1, current: flowData.step === idx + 1 }]'
        >
          <div class='flow-step-num'>{{ idx + 1 }}</div>
          <div class='flow-step-title'>{{ s.title }}</div>
        </div>
      </div>

      <div v-if='flowData.step === 1' class='flow-panel'>
        <div class='panel-title'>第一步：选择今日创作方向</div>
        <div class='panel-desc'>基于你的「{{ plan?.niche || '运营方案' }}」方案，从低粉高赞案例中挑了 {{ topicOptions.length }} 个选题</div>
        <div class='topic-options'>
          <div
            v-for='topic in topicOptions'
            :key='topic.id'
            :class='["topic-option", { selected: flowData.selectedTopic?.id === topic.id }]'
            @click='selectTopic(topic)'
          >
            <div class='topic-option-title'>{{ topic.title }}</div>
            <div class='topic-option-meta'>
              <a-tag :color='riskColor(topic.risk)'>{{ topic.riskLabel }}</a-tag>
              <span><FireOutlined /> {{ topic.caseCount }} 篇案例</span>
            </div>
            <div class='topic-option-angle'>
              <BulbOutlined /> 推荐角度：{{ topic.recommendedAngle }}
            </div>
          </div>
        </div>
      </div>

      <div v-if='flowData.step === 2' class='flow-panel'>
        <div class='panel-title'>第二步：确定文章观点</div>
        <div class='panel-desc'>围绕标题写作，为你生成了 {{ generatedAngleList.length }} 个观点角度，建议最多选择 3 个组合使用；选中后可点击「编辑」修改成你的表达</div>
        <div class='angle-options'>
          <div
            v-for='angle in generatedAngleList'
            :key='angle.id'
            :class='["angle-option", { selected: isAngleSelected(angle.id), editing: editingAngleId === angle.id }]'
            @click='toggleAngle(angle)'
          >
            <a-checkbox :checked='isAngleSelected(angle.id)' class='angle-check' @click.stop />
            <div v-if='editingAngleId !== angle.id' class='angle-text'>{{ angle.text }}</div>
            <a-input
              v-else
              v-model:value='angle.text'
              size='small'
              class='angle-edit-input'
              @click.stop
              @pressEnter='editingAngleId = null'
            />
            <a-button
              v-if='isAngleSelected(angle.id)'
              type='link'
              size='small'
              class='angle-edit-btn'
              @click.stop='toggleEdit(angle)'
            >
              {{ editingAngleId === angle.id ? '完成' : '编辑' }}
            </a-button>
          </div>
        </div>
        <div class='angle-summary' v-if='flowData.selectedAngleIds.length'>
          已选 {{ flowData.selectedAngleIds.length }}/3 个观点：{{ selectedAngleTexts.join('；') }}
        </div>
      </div>

      <div v-if='flowData.step === 3' class='flow-panel'>
        <div class='panel-title'>第三步：选择文章字数</div>
        <div class='panel-desc'>
          基于「{{ currentPlatform?.name || '当前平台' }}」推荐字数选择，会员可设置更高上限
        </div>
        <div class='word-presets'>
          <a-button
            v-for='p in platformWordCounts'
            :key='p.count'
            :type='flowData.wordCount === p.count ? "primary" : "default"'
            :disabled='p.count > wordCountLimit'
            @click='flowData.wordCount = p.count'
          >
            {{ p.label }}（{{ p.count }} 字）
          </a-button>
        </div>
        <div class='word-slider'>
          <span>自定义：</span>
          <a-slider v-model:value='flowData.wordCount' :min='1' :max='wordCountLimit' :step='100' style='flex: 1' />
          <span class='word-count'>{{ flowData.wordCount }} 字</span>
        </div>
        <div class='word-limit-tip'>当前会员等级字数上限：{{ wordCountLimit }} 字</div>
      </div>

      <div v-if='flowData.step === 4' class='flow-panel'>
        <div class='panel-title'>第四步：选择创作提示词</div>
        <a-alert
          class='prompt-market-tip'
          type='info'
          show-icon
          :message='"提示词不够顺手？去提示词市场逛逛，收藏你常用的风格，下次一键调用。"'
        />
        <a-tabs v-model:activeKey='flowData.promptTab'>
          <a-tab-pane key='mine' tab='我的'>
            <div class='prompt-grid'>
              <SkillCard
                v-for='skill in pagedPromptList'
                :key='skill.bizNo || skill.name'
                :name='skill.name'
                :prompt='promptSummary(skill.prompt)'
                :scope='skill.scope'
                size='compact'
                :selected='selectedSkillName === skill.name'
                clickable
                show-view-btn
                @click='selectPrompt(skill)'
                @view='openPromptModal(skill)'
              >
                <template #meta>
                  <span>{{ skill.desc || "我的提示词" }}</span>
                  <span class='prompt-card-meta-dot'>·</span>
                  <span>已用 {{ skill.count || 0 }} 次</span>
                </template>
              </SkillCard>
              <div v-if='!currentPromptList.length' class='prompt-empty'>{{ promptEmptyText('mine') }}</div>
            </div>
            <div v-if='promptTotal > PROMPT_PAGE_SIZE' class='prompt-pagination'>
              <a-pagination
                :current='promptPages.mine'
                :page-size='PROMPT_PAGE_SIZE'
                :total='promptTotal'
                size='small'
                @change='onPromptPageChange'
              />
            </div>
          </a-tab-pane>
          <a-tab-pane key='learn' tab='学习'>
            <div class='prompt-grid'>
              <SkillCard
                v-for='skill in pagedPromptList'
                :key='skill.bizNo || skill.name'
                :name='skill.name'
                :prompt='promptSummary(skill.prompt)'
                :scope='skill.scope'
                size='compact'
                avatar-variant='learned'
                :selected='selectedSkillName === skill.name'
                clickable
                show-view-btn
                @click='selectPrompt(skill)'
                @view='openPromptModal(skill)'
              >
                <template #meta>学习 · {{ (skill.createdAt || "").slice(0, 10) }}</template>
              </SkillCard>
              <div v-if='!currentPromptList.length' class='prompt-empty'>{{ promptEmptyText('learn') }}</div>
            </div>
            <div v-if='promptTotal > PROMPT_PAGE_SIZE' class='prompt-pagination'>
              <a-pagination
                :current='promptPages.learn'
                :page-size='PROMPT_PAGE_SIZE'
                :total='promptTotal'
                size='small'
                @change='onPromptPageChange'
              />
            </div>
          </a-tab-pane>
          <a-tab-pane key='favorite' tab='收藏'>
            <div class='prompt-grid'>
              <SkillCard
                v-for='skill in pagedPromptList'
                :key='skill.id || skill.name'
                :name='skill.name'
                :prompt='promptSummary(skill.prompt)'
                :scope='skill.scope'
                size='compact'
                :selected='selectedSkillName === skill.name'
                :clickable='skill.status === "approved"'
                :class='{ "favorite-offline": skill.status !== "approved" }'
                show-view-btn
                @click='selectPrompt(skill)'
                @view='openPromptModal(skill)'
              >
                <template #meta>
                  <span :class='["favorite-status-badge", skill.status !== "approved" ? "offline" : ""]'>
                    {{ skill.status === "approved" ? "by " + skill.creatorName : "已下架" }}
                  </span>
                </template>
              </SkillCard>
              <div v-if='!currentPromptList.length' class='prompt-empty'>{{ promptEmptyText('favorite') }}</div>
            </div>
            <div v-if='promptTotal > PROMPT_PAGE_SIZE' class='prompt-pagination'>
              <a-pagination
                :current='promptPages.favorite'
                :page-size='PROMPT_PAGE_SIZE'
                :total='promptTotal'
                size='small'
                @change='onPromptPageChange'
              />
            </div>
          </a-tab-pane>
          <a-tab-pane key='system' tab='系统'>
            <div class='prompt-grid'>
              <SkillCard
                v-for='skill in pagedPromptList'
                :key='skill.bizNo || skill.name'
                :name='skill.name'
                :prompt='promptSummary(skill.prompt)'
                :scope='skill.scope'
                size='compact'
                :selected='selectedSkillName === skill.name'
                clickable
                show-view-btn
                @click='selectPrompt(skill)'
                @view='openPromptModal(skill)'
              >
                <template #meta>{{ skill.desc || "系统预设" }}</template>
              </SkillCard>
              <div v-if='!currentPromptList.length' class='prompt-empty'>{{ promptEmptyText('system') }}</div>
            </div>
            <div v-if='promptTotal > PROMPT_PAGE_SIZE' class='prompt-pagination'>
              <a-pagination
                :current='promptPages.system'
                :page-size='PROMPT_PAGE_SIZE'
                :total='promptTotal'
                size='small'
                @change='onPromptPageChange'
              />
            </div>
          </a-tab-pane>
        </a-tabs>

        <a-modal
          class='skill-prompt-modal'
          :open='promptModalVisible'
          :title='viewingSkill?.name'
          :footer='null'
          :width='560'
          centered
          @cancel='closePromptModal'
        >
          <div v-if='viewingSkill' class='skill-prompt-body'>
            <div class='skill-prompt-meta'>
              <span v-if='viewingSkill.desc'>{{ viewingSkill.desc }}</span>
              <span v-else-if='typeof viewingSkill.count === "number"'>自定义提示词 · 已用 {{ viewingSkill.count }} 次</span>
              <span v-else-if='viewingSkill.createdAt'>学习 · {{ viewingSkill.createdAt.slice(0, 10) }}</span>
              <span v-else-if='viewingSkill.creatorName'>by {{ viewingSkill.creatorName }}</span>
            </div>
            <div v-if='parseScopeTags(viewingSkill.scope).length' class='skill-prompt-scope-list'>
              <span v-for='tag in parseScopeTags(viewingSkill.scope)' :key='tag' class='skill-prompt-scope'>{{ tag }}</span>
            </div>
            <div class='skill-prompt-text'>{{ viewingSkill.prompt }}</div>
            <div class='skill-prompt-actions'>
              <button class='skill-prompt-use-btn' @click='useFromPromptModal'>应用</button>
              <button class='skill-prompt-close-btn' @click='closePromptModal'>关闭</button>
            </div>
          </div>
        </a-modal>
      </div>

      <div v-if='flowData.step === 5' class='flow-panel'>
        <div class='panel-title'>第五步：选择导出模板</div>
        <div class='template-tabs'>
          <button
            v-for='tab in templatePlatformTabs'
            :key='tab.key'
            :class='["template-tab", { active: templatePlatformTab === tab.key }]'
            @click='templatePlatformTab = tab.key'
          >
            {{ tab.label }}
          </button>
        </div>
        <div class='template-body'>
          <div class='template-preview-pane' v-html='currentTemplatePreview'></div>
          <div class='template-list-pane'>
            <div
              v-for='t in filteredTemplates'
              :key='t.key'
              :class='["template-row", { selected: flowData.selectedTemplate === t.key, locked: !t.accessible }]'
              @click='selectTemplate(t)'
            >
              <div v-if='!t.accessible' class='template-row-badge'>{{ isFreePlan ? '需订阅' : '需升级' }}</div>
              <div class='template-row-info'>
                <div class='template-row-name'>{{ t.name }}</div>
                <div class='template-row-desc'>{{ t.desc }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class='flow-footer'>
        <a-button v-if='flowData.step > 1' size='large' :disabled='loading || submitting' @click='prevStep'>上一步</a-button>
        <a-button
          v-if='flowData.step < 5'
          type='primary'
          size='large'
          :disabled='!canNext || loading || submitting'
          :loading='loading'
          @click='nextStep'
        >
          下一步
        </a-button>
        <a-button
          v-if='flowData.step === 5'
          type='primary'
          size='large'
          :disabled='!canNext || loading || submitting'
          :loading='submitting'
          @click='finish'
        >
          生成文章
        </a-button>
      </div>
    </div>
  </a-modal>
  <div v-else class='create-flow-page'>
    <div class='create-flow-page-header'>
      <div class='create-flow-page-back' @click='close'>
        <svg viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'>
          <polyline points='15 18 9 12 15 6'></polyline>
        </svg>
        <span>返回</span>
      </div>
      <div class='create-flow-page-title'>{{ modalTitle }}</div>
    </div>
    <div class='create-flow-page-body'>
      <div class='create-flow create-flow--page'>
        <div class='flow-header'>
          <div class='flow-loading' v-if='loading'>
            <a-spin />
            <span>正在为你制定今日创作任务...</span>
          </div>
        </div>

        <div class='flow-steps'>
          <div class='flow-line-bg'></div>
          <div
            class='flow-line-progress'
            :style='{ width: ((flowData.step - 1) / (steps.length - 1) * 80) + "%" }'
          ></div>
          <div
            v-for='(s, idx) in steps'
            :key='idx'
            :class='["flow-step", { active: flowData.step >= idx + 1, current: flowData.step === idx + 1 }]'
          >
            <div class='flow-step-num'>{{ idx + 1 }}</div>
            <div class='flow-step-title'>{{ s.title }}</div>
          </div>
        </div>

        <div v-if='flowData.step === 1' class='flow-panel flow-panel--topics'>
          <div class='panel-title'>第一步：选择今日创作方向</div>
          <div class='panel-desc'>基于你的「{{ plan?.niche || '运营方案' }}」方案，从低粉高赞案例中挑了 {{ topicOptions.length }} 个选题</div>
          <div class='topic-options'>
            <div
              v-for='topic in topicOptions'
              :key='topic.id'
              :class='["topic-option", { selected: flowData.selectedTopic?.id === topic.id }]'
              @click='selectTopic(topic)'
            >
              <div class='topic-option-title'>{{ topic.title }}</div>
              <div class='topic-option-meta'>
                <a-tag :color='riskColor(topic.risk)'>{{ topic.riskLabel }}</a-tag>
                <span><FireOutlined /> {{ topic.caseCount }} 篇案例</span>
              </div>
              <div class='topic-option-angle'>
                <BulbOutlined /> 推荐角度：{{ topic.recommendedAngle }}
              </div>
            </div>
          </div>
        </div>

        <div v-if='flowData.step === 2' class='flow-panel flow-panel--angles'>
          <div class='panel-title'>第二步：确定文章观点</div>
          <div class='panel-desc'>建议最多选择 3 个观点组合；选中后可编辑成你的表达</div>
          <div class='angle-options angle-options--mobile'>
            <div
              v-for='angle in generatedAngleList'
              :key='angle.id'
              :class='["angle-card", { selected: isAngleSelected(angle.id) }]'
              @click='toggleAngle(angle)'
            >
              <div class='angle-card-check'>
                <div :class='["angle-card-checkbox", { checked: isAngleSelected(angle.id) }]'>
                  <svg v-if='isAngleSelected(angle.id)' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='3' stroke-linecap='round' stroke-linejoin='round'>
                    <polyline points='20 6 9 17 4 12'></polyline>
                  </svg>
                </div>
              </div>
              <div class='angle-card-body'>
                <div class='angle-card-text'>{{ angle.text }}</div>
                <div v-if='isAngleSelected(angle.id)' class='angle-card-actions'>
                  <button class='angle-card-edit-btn' @click.stop='openAngleEdit(angle)'>编辑观点</button>
                </div>
              </div>
            </div>
          </div>

          <div class='mobile-angle-summary' v-if='flowData.selectedAngleIds.length'>
            <div class='mobile-angle-summary-title'>已选 {{ flowData.selectedAngleIds.length }}/3 个观点</div>
            <div class='mobile-angle-chips'>
              <div
                v-for='chip in selectedAngleChips'
                :key='chip.id'
                class='mobile-angle-chip'
              >
                <span class='mobile-angle-chip-text'>{{ chip.label }}</span>
                <span class='mobile-angle-chip-remove' @click.stop='removeSelectedAngle(chip.id)'>
                  <svg viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'>
                    <line x1='18' y1='6' x2='6' y2='18'></line>
                    <line x1='6' y1='6' x2='18' y2='18'></line>
                  </svg>
                </span>
              </div>
            </div>
          </div>

          <a-modal
            :open='angleEditModalVisible'
            title='编辑观点'
            :footer='null'
            :mask-closable='false'
            class='angle-edit-modal'
            :width='560'
            centered
            @cancel='cancelAngleEdit'
          >
            <div class='angle-edit-body'>
              <a-textarea
                v-model:value='editingAngleDraft'
                :rows='5'
                placeholder='改成更贴近你风格的表达...'
                show-count
                :maxlength='120'
              />
              <div class='angle-edit-actions'>
                <a-button size='large' block @click='cancelAngleEdit'>取消</a-button>
                <a-button type='primary' size='large' block @click='saveAngleEdit'>保存</a-button>
              </div>
            </div>
          </a-modal>
        </div>

        <div v-if='flowData.step === 3' class='flow-panel flow-panel--words'>
          <div class='panel-title'>第三步：选择文章字数</div>
          <div class='panel-desc'>
            基于「{{ currentPlatform?.name || '当前平台' }}」推荐字数选择，会员可设置更高上限
          </div>
          <div class='word-presets'>
            <a-button
              v-for='p in platformWordCounts'
              :key='p.count'
              :type='flowData.wordCount === p.count ? "primary" : "default"'
              :disabled='p.count > wordCountLimit'
              @click='flowData.wordCount = p.count'
            >
              {{ p.label }}（{{ p.count }} 字）
            </a-button>
          </div>
          <div class='word-slider'>
            <span>自定义：</span>
            <a-slider v-model:value='flowData.wordCount' :min='1' :max='wordCountLimit' :step='100' style='flex: 1' />
            <span class='word-count'>{{ flowData.wordCount }} 字</span>
          </div>
          <div class='word-limit-tip'>当前会员等级字数上限：{{ wordCountLimit }} 字</div>
        </div>

        <div v-if='flowData.step === 4' class='flow-panel flow-panel--prompts'>
          <div class='panel-title'>第四步：选择创作提示词</div>
          <a-alert
            class='prompt-market-tip'
            type='info'
            show-icon
            :message='"提示词不够顺手？去提示词市场逛逛，收藏你常用的风格，下次一键调用。"'
          />
          <a-tabs v-model:activeKey='flowData.promptTab'>
            <a-tab-pane key='mine' tab='我的'>
              <div class='prompt-grid'>
                <SkillCard
                  v-for='skill in pagedPromptList'
                  :key='skill.bizNo || skill.name'
                  :name='skill.name'
                  :prompt='promptSummary(skill.prompt)'
                  :scope='skill.scope'
                  size='compact'
                  :selected='selectedSkillName === skill.name'
                  clickable
                  show-view-btn
                  @click='selectPrompt(skill)'
                  @view='openPromptModal(skill)'
                >
                  <template #meta>
                    <span>{{ skill.desc || "我的提示词" }}</span>
                    <span class='prompt-card-meta-dot'>·</span>
                    <span>已用 {{ skill.count || 0 }} 次</span>
                  </template>
                </SkillCard>
                <div v-if='!currentPromptList.length' class='prompt-empty'>{{ promptEmptyText('mine') }}</div>
              </div>
              <div v-if='promptTotal > PROMPT_PAGE_SIZE' class='prompt-pagination'>
                <a-pagination
                  :current='promptPages.mine'
                  :page-size='PROMPT_PAGE_SIZE'
                  :total='promptTotal'
                  size='small'
                  @change='onPromptPageChange'
                />
              </div>
            </a-tab-pane>
            <a-tab-pane key='learn' tab='学习'>
              <div class='prompt-grid'>
                <SkillCard
                  v-for='skill in pagedPromptList'
                  :key='skill.bizNo || skill.name'
                  :name='skill.name'
                  :prompt='promptSummary(skill.prompt)'
                  :scope='skill.scope'
                  size='compact'
                  avatar-variant='learned'
                  :selected='selectedSkillName === skill.name'
                  clickable
                  show-view-btn
                  @click='selectPrompt(skill)'
                  @view='openPromptModal(skill)'
                >
                  <template #meta>学习 · {{ (skill.createdAt || "").slice(0, 10) }}</template>
                </SkillCard>
                <div v-if='!currentPromptList.length' class='prompt-empty'>{{ promptEmptyText('learn') }}</div>
              </div>
              <div v-if='promptTotal > PROMPT_PAGE_SIZE' class='prompt-pagination'>
                <a-pagination
                  :current='promptPages.learn'
                  :page-size='PROMPT_PAGE_SIZE'
                  :total='promptTotal'
                  size='small'
                  @change='onPromptPageChange'
                />
              </div>
            </a-tab-pane>
            <a-tab-pane key='favorite' tab='收藏'>
              <div class='prompt-grid'>
                <SkillCard
                  v-for='skill in pagedPromptList'
                  :key='skill.id || skill.name'
                  :name='skill.name'
                  :prompt='promptSummary(skill.prompt)'
                  :scope='skill.scope'
                  size='compact'
                  :selected='selectedSkillName === skill.name'
                  :clickable='skill.status === "approved"'
                  :class='{ "favorite-offline": skill.status !== "approved" }'
                  show-view-btn
                  @click='selectPrompt(skill)'
                  @view='openPromptModal(skill)'
                >
                  <template #meta>
                    <span :class='["favorite-status-badge", skill.status !== "approved" ? "offline" : ""]'>
                      {{ skill.status === "approved" ? "by " + skill.creatorName : "已下架" }}
                    </span>
                  </template>
                </SkillCard>
                <div v-if='!currentPromptList.length' class='prompt-empty'>{{ promptEmptyText('favorite') }}</div>
              </div>
              <div v-if='promptTotal > PROMPT_PAGE_SIZE' class='prompt-pagination'>
                <a-pagination
                  :current='promptPages.favorite'
                  :page-size='PROMPT_PAGE_SIZE'
                  :total='promptTotal'
                  size='small'
                  @change='onPromptPageChange'
                />
              </div>
            </a-tab-pane>
            <a-tab-pane key='system' tab='系统'>
              <div class='prompt-grid'>
                <SkillCard
                  v-for='skill in pagedPromptList'
                  :key='skill.bizNo || skill.name'
                  :name='skill.name'
                  :prompt='promptSummary(skill.prompt)'
                  :scope='skill.scope'
                  size='compact'
                  :selected='selectedSkillName === skill.name'
                  clickable
                  show-view-btn
                  @click='selectPrompt(skill)'
                  @view='openPromptModal(skill)'
                >
                  <template #meta>{{ skill.desc || "系统预设" }}</template>
                </SkillCard>
                <div v-if='!currentPromptList.length' class='prompt-empty'>{{ promptEmptyText('system') }}</div>
              </div>
              <div v-if='promptTotal > PROMPT_PAGE_SIZE' class='prompt-pagination'>
                <a-pagination
                  :current='promptPages.system'
                  :page-size='PROMPT_PAGE_SIZE'
                  :total='promptTotal'
                  size='small'
                  @change='onPromptPageChange'
                />
              </div>
            </a-tab-pane>
          </a-tabs>

          <a-modal
            class='skill-prompt-modal'
            :open='promptModalVisible'
            :title='viewingSkill?.name'
            :footer='null'
            :width='560'
            centered
            @cancel='closePromptModal'
          >
            <div v-if='viewingSkill' class='skill-prompt-body'>
              <div class='skill-prompt-meta'>
                <span v-if='viewingSkill.desc'>{{ viewingSkill.desc }}</span>
                <span v-else-if='typeof viewingSkill.count === "number"'>自定义提示词 · 已用 {{ viewingSkill.count }} 次</span>
                <span v-else-if='viewingSkill.createdAt'>学习 · {{ viewingSkill.createdAt.slice(0, 10) }}</span>
                <span v-else-if='viewingSkill.creatorName'>by {{ viewingSkill.creatorName }}</span>
              </div>
              <div v-if='parseScopeTags(viewingSkill.scope).length' class='skill-prompt-scope-list'>
                <span v-for='tag in parseScopeTags(viewingSkill.scope)' :key='tag' class='skill-prompt-scope'>{{ tag }}</span>
              </div>
              <div class='skill-prompt-text'>{{ viewingSkill.prompt }}</div>
              <div class='skill-prompt-actions'>
                <button class='skill-prompt-use-btn' @click='useFromPromptModal'>应用</button>
                <button class='skill-prompt-close-btn' @click='closePromptModal'>关闭</button>
              </div>
            </div>
          </a-modal>
        </div>

        <div v-if='flowData.step === 5' class='flow-panel flow-panel--templates'>
          <div class='panel-title'>第五步：选择导出模板</div>
          <div class='template-tabs'>
            <button
              v-for='tab in templatePlatformTabs'
              :key='tab.key'
              :class='["template-tab", { active: templatePlatformTab === tab.key }]'
              @click='templatePlatformTab = tab.key'
            >
              {{ tab.label }}
            </button>
          </div>
          <div class='template-body'>
            <div class='template-preview-pane' v-html='currentTemplatePreview'></div>
            <div class='template-list-pane'>
              <div
                v-for='t in filteredTemplates'
                :key='t.key'
                :class='["template-row", { selected: flowData.selectedTemplate === t.key, locked: !t.accessible }]'
                @click='selectTemplate(t)'
              >
                <div v-if='!t.accessible' class='template-row-badge'>{{ isFreePlan ? '需订阅' : '需升级' }}</div>
                <div class='template-row-info'>
                  <div class='template-row-name'>{{ t.name }}</div>
                  <div class='template-row-desc'>{{ t.desc }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class='flow-footer'>
          <a-button v-if='flowData.step > 1' size='large' :disabled='loading || submitting' @click='prevStep'>上一步</a-button>
          <a-button
            v-if='flowData.step < 5'
            type='primary'
            size='large'
            :disabled='!canNext || loading || submitting'
            :loading='loading'
            @click='nextStep'
          >
            下一步
          </a-button>
          <a-button
            v-if='flowData.step === 5'
            type='primary'
            size='large'
            :disabled='!canNext || loading || submitting'
            :loading='submitting'
            @click='finish'
          >
            生成文章
          </a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { FireOutlined, BulbOutlined } from '@ant-design/icons-vue'
import {
  getRecommendedCreationSession,
  generateRecommendedTopics,
  generateRecommendedAngles,
  updateRecommendedSession,
  submitRecommendedGeneration,
  clearRecommendedSession
} from '@/api/recommendedCreation.js'
import { platforms, loadPlatforms } from '@/composables/usePlatforms.js'
import {
  systemSkills,
  mySkills,
  learnedSkills,
  loadSystemSkills,
  loadMySkills,
  loadLearnedSkills
} from '@/composables/useSkills.js'
import { favoriteSkills, loadFavoriteSkills } from '@/composables/useSkillMarket.js'
import { useExportTemplates } from '@/composables/useExportTemplates.js'
import { getWordCountLimit, getCurrentPlanKey } from '@/utils/membershipLimits.js'
import SkillCard from '@/components/SkillCard.vue'
import { buildLargePreview } from '@/utils/articleTemplates.js'

const props = defineProps({
  visible: Boolean,
  plan: Object,
  pageMode: Boolean
})

const emit = defineEmits(['update:visible', 'success'])

const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const steps = [
  { title: '选题', desc: '选择创作方向' },
  { title: '观点', desc: '确定文章观点' },
  { title: '字数', desc: '设置文章长度' },
  { title: '提示词', desc: '选择创作提示词' },
  { title: '模板', desc: '选择导出模板' }
]

const flowData = reactive({
  step: 1,
  selectedTopic: null,
  selectedAngleIds: [],
  wordCount: 1500,
  promptTab: 'system',
  selectedPrompt: '',
  customPrompt: '',
  selectedTemplate: ''
})

const editingAngleId = ref(null)
const angleEditModalVisible = ref(false)
const editingAngleDraft = ref('')
const editingAngleOriginalText = ref('')
const selectedSkillName = ref('')
const promptModalVisible = ref(false)
const viewingSkill = ref(null)

const topicOptions = ref([])
const generatedAngleList = ref([])
const lastGeneratedTopicId = ref('')

const { templates: apiTemplates, load: loadExportTemplates } = useExportTemplates()

const wordCountLimit = computed(() => getWordCountLimit())
const isFreePlan = computed(() => getCurrentPlanKey() === 'free')
const currentPlatform = computed(() => {
  if (props.plan?.platform) {
    const byName = platforms.value.find(p => p.name === props.plan.platform)
    if (byName) return byName
  }
  return platforms.value.find(p => p.isDefault) || platforms.value[0] || null
})
const platformWordCounts = computed(() => currentPlatform.value?.wordCountPresets || [])

const promptTabMap = {
  system: systemSkills,
  mine: mySkills,
  learn: learnedSkills,
  favorite: favoriteSkills
}
const currentPromptList = computed(() => promptTabMap[flowData.promptTab]?.value || [])

const PROMPT_PAGE_SIZE = 6
const promptPages = reactive({ system: 1, mine: 1, learn: 1, favorite: 1 })
const promptTotal = computed(() => currentPromptList.value.length)
const pagedPromptList = computed(() => {
  const page = promptPages[flowData.promptTab] || 1
  const start = (page - 1) * PROMPT_PAGE_SIZE
  return currentPromptList.value.slice(start, start + PROMPT_PAGE_SIZE)
})

watch(() => flowData.promptTab, () => {
  promptPages[flowData.promptTab] = 1
})

const templatePlatformTabs = [
  { key: 'all', label: '全部' },
  { key: 'wechat', label: '公众号' },
  { key: 'xiaohongshu', label: '小红书' },
  { key: 'toutiao', label: '头条' },
  { key: 'baijiahao', label: '百家号' },
  { key: 'zhihu', label: '知乎' },
  { key: 'douyin', label: '抖音' },
  { key: 'general', label: '通用' }
]
const templatePlatformTab = ref('all')

const filteredTemplates = computed(() => {
  if (templatePlatformTab.value === 'all') return apiTemplates.value
  return apiTemplates.value.filter(t => t.platform === templatePlatformTab.value)
})

const currentTemplate = computed(() => {
  const list = filteredTemplates.value
  const selected = list.find(t => t.key === flowData.selectedTemplate)
  if (selected) return selected
  return list.find(t => t.accessible) || list[0] || null
})
const currentTemplatePreview = computed(() => buildLargePreview(currentTemplate.value))

const selectedAngleTexts = computed(() => {
  return flowData.selectedAngleIds
    .map(id => generatedAngleList.value.find(a => a.id === id))
    .filter(Boolean)
    .map(a => a.text)
})

const selectedAngleChips = computed(() => {
  return flowData.selectedAngleIds
    .map(id => generatedAngleList.value.find(a => a.id === id))
    .filter(Boolean)
    .map(a => ({ id: a.id, label: a.text }))
})

const modalTitle = computed(() => `今日创作 · ${steps[flowData.step - 1].title}`)

function riskColor(risk) {
  return risk === 'low' ? 'success' : risk === 'medium' ? 'warning' : 'error'
}

function selectTopic(topic) {
  flowData.selectedTopic = topic
}

function isAngleSelected(id) {
  return flowData.selectedAngleIds.includes(id)
}

function toggleAngle(angle) {
  if (editingAngleId.value === angle.id) return
  const idx = flowData.selectedAngleIds.indexOf(angle.id)
  if (idx > -1) {
    flowData.selectedAngleIds.splice(idx, 1)
    if (editingAngleId.value === angle.id) editingAngleId.value = null
  } else {
    if (flowData.selectedAngleIds.length >= 3) {
      message.warning('最多选择 3 个观点进行组合')
      return
    }
    flowData.selectedAngleIds.push(angle.id)
  }
}

function toggleEdit(angle) {
  if (editingAngleId.value === angle.id) {
    editingAngleId.value = null
  } else {
    if (!isAngleSelected(angle.id)) return
    editingAngleId.value = angle.id
  }
}

function openAngleEdit(angle) {
  if (!isAngleSelected(angle.id)) return
  editingAngleId.value = angle.id
  editingAngleOriginalText.value = angle.text
  editingAngleDraft.value = angle.text
  angleEditModalVisible.value = true
}

function saveAngleEdit() {
  const target = generatedAngleList.value.find(a => a.id === editingAngleId.value)
  if (target) {
    const text = editingAngleDraft.value.trim()
    if (text) {
      target.text = text
    }
  }
  angleEditModalVisible.value = false
  editingAngleId.value = null
  editingAngleDraft.value = ''
  editingAngleOriginalText.value = ''
}

function cancelAngleEdit() {
  const target = generatedAngleList.value.find(a => a.id === editingAngleId.value)
  if (target && editingAngleOriginalText.value !== undefined) {
    target.text = editingAngleOriginalText.value
  }
  angleEditModalVisible.value = false
  editingAngleId.value = null
  editingAngleDraft.value = ''
  editingAngleOriginalText.value = ''
}

function removeSelectedAngle(id) {
  const idx = flowData.selectedAngleIds.indexOf(id)
  if (idx > -1) {
    flowData.selectedAngleIds.splice(idx, 1)
  }
}

function selectPrompt(skill) {
  if (skill.status && skill.status !== 'approved') {
    message.warning('该提示词已下架，无法使用')
    return
  }
  selectedSkillName.value = skill.name
  flowData.selectedPrompt = skill.prompt || skill.name || ''
}

function promptSummary(prompt) {
  if (!prompt) return ''
  return prompt.length > 60 ? prompt.slice(0, 60) + '...' : prompt
}

function promptEmptyText(tab) {
  const map = {
    mine: '你还没有保存自己的提示词，可在「学习」或「系统」里先选一个',
    learn: '还没有学习过的提示词，可去「我的提示词」页面学习',
    favorite: '还没有收藏提示词，去提示词市场发现更多好风格',
    system: '系统提示词加载中...'
  }
  return map[tab] || ''
}

function onPromptPageChange(page) {
  promptPages[flowData.promptTab] = page
}

function openPromptModal(skill) {
  viewingSkill.value = skill
  promptModalVisible.value = true
}

function closePromptModal() {
  promptModalVisible.value = false
  viewingSkill.value = null
}

function useFromPromptModal() {
  if (!viewingSkill.value) return
  if (viewingSkill.value.status && viewingSkill.value.status !== 'approved') {
    message.warning('该提示词已下架，无法使用')
    return
  }
  selectPrompt(viewingSkill.value)
  closePromptModal()
}

function parseScopeTags(scopeStr) {
  if (!scopeStr) return []
  return scopeStr.split(/[,，]/).map(t => t.trim()).filter(Boolean)
}

function selectTemplate(t) {
  if (!t.accessible) {
    message.info('该模板当前套餐不可用，请升级套餐后使用')
    return
  }
  flowData.selectedTemplate = t.key
}

const canNext = computed(() => {
  if (flowData.step === 1) return !!flowData.selectedTopic
  if (flowData.step === 2) return flowData.selectedAngleIds.length > 0 && flowData.selectedAngleIds.length <= 3
  if (flowData.step === 3) return flowData.wordCount >= 1
  if (flowData.step === 4) return !!flowData.selectedPrompt
  if (flowData.step === 5) return !!flowData.selectedTemplate
  return true
})

async function nextStep() {
  if (!canNext.value || loading.value || submitting.value) return

  let ok = true
  if (flowData.step === 1) {
    await ensureAnglesForSelectedTopic()
    if (!generatedAngleList.value.length) return
  } else if (flowData.step === 2) {
    const selectedAngles = flowData.selectedAngleIds
      .map(id => generatedAngleList.value.find(a => a.id === id))
      .filter(Boolean)
    ok = await persistStep(3, { selectedAngles })
  } else if (flowData.step === 3) {
    ok = await persistStep(4, { wordCount: flowData.wordCount })
  } else if (flowData.step === 4) {
    ok = await persistStep(5, { prompt: flowData.selectedPrompt })
  }

  if (ok) {
    flowData.step++
  }
}

function prevStep() {
  if (flowData.step > 1) {
    flowData.step--
  }
}

async function finish() {
  if (!canNext.value || submitting.value) return
  submitting.value = true
  try {
    const saved = await persistStep(5, { template: flowData.selectedTemplate })
    if (!saved) return
    const task = await submitRecommendedGeneration()
    clearRecommendedSession().catch(() => {})
    emit('success', task)
    if (props.pageMode) {
      router.back()
    } else {
      close()
    }
  } catch (err) {
    message.error(err?.message || '提交生成失败，请重试')
  } finally {
    submitting.value = false
  }
}

function close() {
  if (props.pageMode) {
    router.back()
    return
  }
  emit('update:visible', false)
}

function resetLocalState() {
  flowData.step = 1
  flowData.selectedTopic = null
  flowData.selectedAngleIds = []
  flowData.wordCount = currentPlatform.value?.recommendWords || 1500
  flowData.promptTab = 'system'
  flowData.selectedPrompt = systemSkills.value[0]?.prompt || ''
  selectedSkillName.value = systemSkills.value[0]?.name || ''
  flowData.customPrompt = ''
  flowData.selectedTemplate = apiTemplates.value.find(t => t.accessible)?.key || ''
  templatePlatformTab.value = 'all'
  editingAngleId.value = null
  topicOptions.value = []
  generatedAngleList.value = []
  lastGeneratedTopicId.value = ''
}

function applySession(session) {
  flowData.step = Math.max(1, Math.min(5, session.currentStep || 1))
  topicOptions.value = session.topics || []
  flowData.selectedTopic = session.selectedTopic || null
  generatedAngleList.value = (session.angles || []).map(a => ({
    ...a,
    text: (a.text || '').replace(/「/g, '“').replace(/」/g, '”')
  }))
  lastGeneratedTopicId.value = session.selectedTopic?.id || ''

  if (session.selectedAngles?.length) {
    flowData.selectedAngleIds = session.selectedAngles.map(a => a.id)
    session.selectedAngles.forEach(sel => {
      const sanitized = { ...sel, text: (sel.text || '').replace(/「/g, '“').replace(/」/g, '”') }
      const existing = generatedAngleList.value.find(a => a.id === sel.id)
      if (existing) {
        existing.text = sanitized.text
      } else {
        generatedAngleList.value.push(sanitized)
      }
    })
  } else {
    flowData.selectedAngleIds = []
  }

  flowData.wordCount = session.wordCount || currentPlatform.value?.recommendWords || 1500
  flowData.selectedPrompt = session.prompt || systemSkills.value[0]?.prompt || ''
  selectedSkillName.value = findSkillNameByPrompt(flowData.selectedPrompt) || systemSkills.value[0]?.name || ''
  flowData.selectedTemplate = session.template || apiTemplates.value.find(t => t.accessible)?.key || ''
  editingAngleId.value = null
}

function findSkillNameByPrompt(prompt) {
  if (!prompt) return ''
  const all = [
    ...systemSkills.value,
    ...mySkills.value,
    ...learnedSkills.value,
    ...favoriteSkills.value
  ]
  const found = all.find(s => s.prompt === prompt)
  return found?.name || ''
}

async function ensureAnglesForSelectedTopic() {
  const topic = flowData.selectedTopic
  if (!topic) return

  if (lastGeneratedTopicId.value === topic.id && generatedAngleList.value.length) {
    return
  }

  loading.value = true
  try {
    const angles = await generateRecommendedAngles(topic.id)
    generatedAngleList.value = (angles || []).map(a => ({
      ...a,
      text: (a.text || '').replace(/「/g, '“').replace(/」/g, '”')
    }))
    flowData.selectedAngleIds = []
    editingAngleId.value = null
    lastGeneratedTopicId.value = topic.id
    if (!generatedAngleList.value.length) {
      message.warning('未生成到观点，请重试')
    }
  } catch (err) {
    message.error(err?.message || '生成观点失败，请重试')
    generatedAngleList.value = []
  } finally {
    loading.value = false
  }
}

async function persistStep(step, extra = {}) {
  try {
    await updateRecommendedSession({ currentStep: step, ...extra })
    return true
  } catch (err) {
    message.error(err?.message || '保存创作进度失败，请重试')
    return false
  }
}

async function initSession() {
  loading.value = true
  try {
    await Promise.all([
      loadPlatforms().catch(() => {}),
      loadExportTemplates().catch(() => {}),
      loadSystemSkills().catch(() => {}),
      loadMySkills('', 1, 999).catch(() => {}),
      loadLearnedSkills().catch(() => {}),
      loadFavoriteSkills().catch(() => {})
    ])

    const session = await getRecommendedCreationSession()
    if (session) {
      if (session.status === 'completed') {
        await clearRecommendedSession().catch(() => {})
        resetLocalState()
        const topics = await generateRecommendedTopics()
        topicOptions.value = topics || []
        if (!topicOptions.value.length) {
          message.warning('未生成到选题，请重试')
          close()
        }
        return
      }
      applySession(session)
    } else {
      resetLocalState()
      const topics = await generateRecommendedTopics()
      topicOptions.value = topics || []
      if (!topicOptions.value.length) {
        message.warning('未生成到选题，请重试')
        close()
      }
    }
  } catch (err) {
    message.error(err?.message || '加载创作任务失败，请重试')
    close()
  } finally {
    loading.value = false
  }
}

watch(() => props.visible, (val) => {
  if (val) {
    initSession()
  }
})

onMounted(() => {
  if (props.pageMode) {
    initSession()
  }
})
</script>

<style scoped>
.create-flow {
  display: flex;
  flex-direction: column;
  height: 620px;
  padding: 16px 20px;
  box-sizing: border-box;
}
.flow-header {
  min-height: 40px;
  margin-bottom: 8px;
  flex-shrink: 0;
}
.flow-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #FF2442;
  font-size: 14px;
}
.flow-steps {
  position: relative;
  display: flex;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 16px;
}
.flow-line-bg,
.flow-line-progress {
  position: absolute;
  top: 14px;
  left: 10%;
  right: 10%;
  height: 2px;
}
.flow-line-bg {
  background: #f0f0f0;
}
.flow-line-progress {
  background: #FF2442;
  transition: width 0.3s;
}
.flow-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex: 1;
  position: relative;
  z-index: 1;
}
.flow-step-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: #8c8c8c;
  transition: all 0.3s;
}
.flow-step.active .flow-step-num {
  background: #FF2442;
  color: #fff;
}
.flow-step-title {
  font-size: 13px;
  color: #8c8c8c;
}
.flow-step.active .flow-step-title {
  color: #FF2442;
  font-weight: 500;
}
.flow-panel {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}
.panel-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 8px;
}
.panel-desc {
  font-size: 14px;
  color: #8c8c8c;
  margin-bottom: 20px;
}
.topic-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.topic-option {
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  padding: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.topic-option:hover {
  border-color: #FF2442;
}
.topic-option.selected {
  border-color: #FF2442;
  background: rgba(255, 36, 66, 0.04);
}
.topic-option-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}
.topic-option-meta {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 6px;
}
.topic-option-angle {
  font-size: 13px;
  color: #595959;
}
.angle-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}
.angle-option {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  color: #595959;
}
.angle-option:hover {
  border-color: #FF2442;
}
.angle-option.selected {
  border-color: #FF2442;
  background: rgba(255, 36, 66, 0.04);
  color: #1a1a1a;
}
.angle-option.editing {
  background: #fffbe6;
  border-color: #faad14;
}
.angle-check {
  flex-shrink: 0;
}
.angle-text {
  flex: 1;
  line-height: 1.5;
}
.angle-edit-input {
  flex: 1;
}
.angle-edit-btn {
  flex-shrink: 0;
  padding: 0 4px;
}
.angle-summary {
  padding: 10px 12px;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 8px;
  font-size: 13px;
  color: #237804;
  line-height: 1.6;
}
.angle-options--mobile {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}
.angle-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  border: 1.5px solid var(--color-border);
  border-radius: 14px;
  background: var(--color-bg-card);
  cursor: pointer;
  transition: all 0.2s;
}
.angle-card:hover {
  border-color: var(--color-primary-light);
}
.angle-card.selected {
  border-color: var(--color-primary);
  background: var(--color-primary-bg);
  box-shadow: 0 2px 8px rgba(255, 36, 66, 0.08);
}
.angle-card-check {
  flex-shrink: 0;
  padding-top: 2px;
}
.angle-card-checkbox {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  border: 2px solid #d9d9d9;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.angle-card-checkbox svg {
  width: 13px;
  height: 13px;
  color: #fff;
}
.angle-card.selected .angle-card-checkbox {
  background: var(--color-primary);
  border-color: var(--color-primary);
}
.angle-card-body {
  flex: 1;
  min-width: 0;
}
.angle-card-text {
  font-size: 15px;
  line-height: 1.6;
  color: var(--color-text-primary);
  word-break: break-word;
}
.angle-card-actions {
  margin-top: 10px;
}
.angle-card-edit-btn {
  font-size: 13px;
  color: var(--color-primary);
  background: transparent;
  border: 1px solid var(--color-primary-light);
  border-radius: 8px;
  padding: 5px 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.angle-card-edit-btn:hover {
  background: var(--color-primary-bg);
}
.mobile-angle-summary {
  margin-top: 20px;
  padding: 14px;
  background: var(--color-primary-bg);
  border: 1px solid var(--color-primary-light);
  border-radius: 14px;
}
.mobile-angle-summary-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary);
  margin-bottom: 10px;
}
.mobile-angle-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.mobile-angle-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  padding: 6px 10px;
  background: #fff;
  border: 1px solid var(--color-primary-light);
  border-radius: 20px;
  font-size: 13px;
  color: var(--color-text-primary);
}
.mobile-angle-chip-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 220px;
}
.mobile-angle-chip-remove {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: all 0.2s;
}
.mobile-angle-chip-remove:hover {
  background: var(--color-primary-bg);
  color: var(--color-primary);
}
.mobile-angle-chip-remove svg {
  width: 12px;
  height: 12px;
}
.word-presets {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}
.word-slider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.word-count {
  min-width: 60px;
  text-align: right;
  font-weight: 600;
  color: #FF2442;
}
.word-limit-tip {
  font-size: 13px;
  color: #8c8c8c;
}
.prompt-market-tip {
  margin-bottom: 16px;
}
.prompt-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-top: 12px;
  min-height: 200px;
}
.prompt-card-meta-dot {
  color: #d9d9d9;
  font-weight: 700;
}
.favorite-offline {
  opacity: 0.7;
}
.favorite-status-badge {
  font-size: 12px;
  color: #8c8c8c;
}
.favorite-status-badge.offline {
  color: #ff4d4f;
  font-weight: 500;
}
.prompt-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  margin-top: 12px;
}
.prompt-pagination :deep(.ant-pagination-item-active) {
  background: var(--color-primary);
  border-color: var(--color-primary);
}
.prompt-pagination :deep(.ant-pagination-item-active a) {
  color: #fff;
}
.prompt-pagination :deep(.ant-pagination-item:hover) {
  border-color: var(--color-primary);
}
.prompt-pagination :deep(.ant-pagination-item:hover a) {
  color: var(--color-primary);
}
.prompt-pagination :deep(.ant-pagination-prev:hover .ant-pagination-item-link),
.prompt-pagination :deep(.ant-pagination-next:hover .ant-pagination-item-link) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.prompt-empty {
  padding: 24px;
  text-align: center;
  color: #8c8c8c;
  font-size: 14px;
}
.template-tabs {
  display: flex;
  gap: 8px;
  padding: 0 0 14px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
  overflow-x: auto;
}
.template-tab {
  padding: 6px 14px;
  border-radius: 16px;
  border: 1px solid #d9d9d9;
  background: #fff;
  color: #595959;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}
.template-tab.active {
  border-color: #ff2442;
  background: #fff0f2;
  color: #ff2442;
  font-weight: 600;
}
.template-body {
  display: flex;
  gap: 16px;
}
.template-preview-pane {
  flex: 0 0 360px;
  background: #f5f5f5;
  border-radius: 10px;
  overflow: hidden;
  height: 420px;
  box-shadow: inset 0 0 0 1px rgba(0,0,0,0.05);
}
.template-list-pane {
  flex: 1;
  min-width: 0;
  height: 420px;
  overflow-y: auto;
  padding-right: 4px;
}
.template-row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 8px;
}
.template-row:hover {
  border-color: #ff2442;
  background: #fff0f2;
}
.template-row.selected {
  border-color: #ff2442;
  background: #fff0f2;
  box-shadow: 0 0 0 2px rgba(255, 36, 66, 0.25);
}
.template-row.locked {
  cursor: not-allowed;
  background: #f5f5f5;
}
.template-row.locked:hover {
  border-color: #e8e8e8;
  background: #f5f5f5;
}
.template-row.locked .template-row-name,
.template-row.locked .template-row-desc {
  opacity: 0.55;
}
.template-row-badge {
  position: absolute;
  top: 4px;
  right: 6px;
  z-index: 1;
  padding: 1px 6px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
  line-height: 1.4;
  color: #fff;
  background: linear-gradient(135deg, #ff9a4d, #ff2442);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12);
  pointer-events: none;
}
.template-row-name {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 14px;
  margin-bottom: 2px;
}
.template-row-desc {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.5;
}
.flow-footer {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-shrink: 0;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}
:deep(.ant-btn-primary) {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
}
:deep(.ant-btn-primary:hover),
:deep(.ant-btn-primary:focus) {
  background-color: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}
:deep(.ant-btn-primary:disabled) {
  background-color: rgba(255, 36, 66, 0.35);
  border-color: rgba(255, 36, 66, 0.35);
  color: #fff;
}
:deep(.ant-spin-dot-item) {
  background-color: var(--color-primary);
}
:deep(.ant-slider-track) {
  background-color: var(--color-primary);
}
:deep(.ant-slider:hover .ant-slider-track) {
  background-color: var(--color-primary-hover);
}
:deep(.ant-slider-handle) {
  border-color: var(--color-primary);
}
:deep(.ant-slider-handle:hover),
:deep(.ant-slider-handle:focus),
:deep(.ant-slider-handle:active),
:deep(.ant-slider-handle.ant-tooltip-open) {
  border-color: var(--color-primary-hover);
}
:deep(.ant-slider-handle::after) {
  background-color: var(--color-primary);
  box-shadow: 0 0 0 2px var(--color-primary);
}
:deep(.ant-slider-handle:hover::after),
:deep(.ant-slider-handle:focus::after),
:deep(.ant-slider-handle:active::after),
:deep(.ant-slider-handle.ant-tooltip-open::after) {
  background-color: var(--color-primary-hover);
  box-shadow: 0 0 0 2px var(--color-primary-hover);
}
:deep(.ant-slider-dot-active) {
  border-color: var(--color-primary);
}
:deep(.ant-checkbox-checked .ant-checkbox-inner),
:deep(.ant-checkbox-indeterminate .ant-checkbox-inner) {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
}
:deep(.ant-checkbox-wrapper:hover .ant-checkbox-inner),
:deep(.ant-checkbox:hover .ant-checkbox-inner),
:deep(.ant-checkbox-input:focus + .ant-checkbox-inner) {
  border-color: var(--color-primary);
}
:deep(.ant-tabs .ant-tabs-tab.ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: var(--color-primary);
}
:deep(.ant-tabs .ant-tabs-ink-bar) {
  background: var(--color-primary);
}
:deep(.ant-tabs .ant-tabs-tab:hover) {
  color: var(--color-primary-hover);
}
:deep(.ant-alert-info) {
  background-color: var(--color-primary-bg);
  border: 1px solid var(--color-primary-light);
}
:deep(.ant-alert-info .ant-alert-icon) {
  color: var(--color-primary);
}
:deep(.ant-alert-info .ant-alert-message) {
  color: var(--color-primary);
}
@media (max-width: 768px) {
  .template-body {
    flex-direction: column;
    gap: 12px;
  }
  .template-preview-pane {
    flex: none;
    width: 100%;
    height: 240px;
    order: 2;
  }
  .template-list-pane {
    flex: none;
    display: flex;
    gap: 10px;
    height: auto;
    max-height: 120px;
    overflow-x: auto;
    overflow-y: hidden;
    padding-right: 0;
    padding-bottom: 4px;
    order: 1;
    scrollbar-width: none;
  }
  .template-list-pane::-webkit-scrollbar {
    display: none;
  }
  .template-row {
    flex: 0 0 128px;
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
    padding: 12px;
    margin-bottom: 0;
    border-radius: 12px;
  }
  .word-presets {
    flex-direction: column;
  }
  .prompt-grid {
    grid-template-columns: 1fr;
  }

  /* 页面模式各步骤移动端优化 */
  .flow-panel--topics .topic-options {
    gap: 10px;
  }
  .flow-panel--topics .topic-option {
    padding: 14px;
    border-radius: 14px;
    border-width: 1.5px;
  }
  .flow-panel--topics .topic-option-title {
    font-size: 15px;
    margin-bottom: 10px;
  }
  .flow-panel--topics .topic-option-meta {
    gap: 8px;
    margin-bottom: 8px;
  }
  .flow-panel--topics .topic-option-angle {
    font-size: 12px;
    color: var(--color-text-secondary);
  }

  .flow-panel--words .word-presets {
    flex-direction: column;
    gap: 10px;
  }
  .flow-panel--words .word-presets .ant-btn {
    width: 100%;
    height: 44px;
    border-radius: 10px;
    font-size: 14px;
  }
  .flow-panel--words .word-slider {
    gap: 10px;
    margin-top: 16px;
  }
  .flow-panel--words .word-slider span:first-child {
    font-size: 13px;
    white-space: nowrap;
  }

  .flow-panel--prompts .prompt-market-tip {
    margin-bottom: 10px;
    padding: 8px 10px;
    font-size: 12px;
  }
  .flow-panel--prompts .prompt-market-tip .ant-alert-message {
    font-size: 12px;
  }
  .flow-panel--prompts .prompt-grid {
    gap: 10px;
    margin-top: 8px;
    min-height: auto;
  }
  .flow-panel--prompts .prompt-pagination {
    margin-top: 8px;
    padding-top: 8px;
  }
  .flow-panel--prompts :deep(.skill-card--compact) {
    padding: 10px 12px;
    border-radius: 12px;
    min-height: auto;
  }
  .flow-panel--prompts :deep(.skill-card__head) {
    margin-bottom: 6px;
  }
  .flow-panel--prompts :deep(.skill-card__avatar) {
    width: 32px;
    height: 32px;
    font-size: 13px;
    border-radius: 8px;
  }
  .flow-panel--prompts :deep(.skill-card__title) {
    font-size: 14px;
  }
  .flow-panel--prompts :deep(.skill-card__meta) {
    font-size: 11px;
  }
  .flow-panel--prompts :deep(.skill-card__prompt) {
    font-size: 12px;
    line-height: 1.5;
    margin-bottom: 6px;
    -webkit-line-clamp: 2;
    flex: 0 0 auto;
    max-height: 3em;
  }
  .flow-panel--prompts :deep(.skill-card__action-btn) {
    padding: 3px 8px;
    font-size: 11px;
    border-radius: 6px;
  }

  .flow-panel--templates .template-tabs {
    margin-bottom: 10px;
    padding-bottom: 8px;
  }
  .flow-panel--templates .template-preview-pane {
    height: calc(100vh - 400px);
    min-height: 180px;
    max-height: 520px;
    border-radius: 12px;
  }
  .flow-panel--templates .template-list-pane {
    max-height: 96px;
    gap: 8px;
  }
  .flow-panel--templates .template-row {
    flex: 0 0 116px;
    padding: 10px;
  }
}

/* 页面模式 */
.create-flow-page {
  min-height: 100%;
  background: var(--color-bg-page);
  display: flex;
  flex-direction: column;
}
.create-flow-page-header {
  display: none;
}
.create-flow-page-body {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

@media (max-width: 768px) {
  .create-flow-page {
    background: var(--color-bg-card);
  }
  .create-flow-page-header {
    display: flex;
    align-items: center;
    position: relative;
    height: 48px;
    padding: 0 12px;
    background: var(--color-bg-card);
    border-bottom: 1px solid var(--color-border-light);
    flex-shrink: 0;
  }
  .create-flow-page-back {
    position: absolute;
    left: 12px;
    top: 50%;
    transform: translateY(-50%);
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 14px;
    color: var(--color-text-secondary);
    cursor: pointer;
  }
  .create-flow-page-back svg {
    width: 20px;
    height: 20px;
  }
  .create-flow-page-title {
    flex: 1;
    text-align: center;
    font-size: 16px;
    font-weight: 600;
    color: var(--color-text-primary);
  }
  .create-flow-page-body {
    display: flex;
    flex-direction: column;
    padding: 12px 12px calc(12px + env(safe-area-inset-bottom));
    overflow: hidden;
  }
  .create-flow.create-flow--page {
    display: flex;
    flex-direction: column;
    flex: 1;
    min-height: 0;
    overflow: hidden;
    padding: 0;
  }
  .create-flow--page .flow-steps {
    flex-shrink: 0;
    margin-bottom: 12px;
    padding-bottom: 8px;
    background: var(--color-bg-card);
  }
  .create-flow--page .flow-panel {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    padding-right: 2px;
    padding-bottom: 8px;
  }
  .create-flow--page .flow-footer {
    flex-shrink: 0;
    display: flex;
    gap: 12px;
    padding: 12px 0 0;
    margin-top: 12px;
    background: var(--color-bg-card);
    border-top: 1px solid var(--color-border-light);
  }
  .create-flow--page .flow-footer .ant-btn {
    flex: 1;
    height: 44px;
    border-radius: 10px;
    font-size: 15px;
  }
  .create-flow--page .panel-title {
    font-size: 17px;
    margin-bottom: 6px;
  }
  .create-flow--page .panel-desc {
    font-size: 13px;
    margin-bottom: 14px;
  }
}
</style>

<style>
/* 提示词详情弹框 teleport 到 body，需非 scoped 全局覆盖 */
.skill-prompt-modal .ant-modal-body {
  max-height: 70vh;
  overflow-y: auto;
}

.skill-prompt-body {
  padding: 8px 0 0;
}

.skill-prompt-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 12px;
}

.skill-prompt-scope-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.skill-prompt-scope {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-primary);
  background: #fff5f7;
  border: 1px solid #ffd1d9;
  padding: 3px 10px;
  border-radius: 6px;
}

.skill-prompt-scope::before {
  content: '#';
  opacity: 0.8;
}

.skill-prompt-text {
  font-size: 14px;
  color: #262626;
  line-height: 1.8;
  background: #fafafa;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
  white-space: pre-line;
  max-height: 360px;
  overflow-y: auto;
}

.skill-prompt-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.skill-prompt-use-btn {
  padding: 8px 20px;
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: 8px;
  font-size: 14px;
  color: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.skill-prompt-use-btn:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.skill-prompt-close-btn {
  padding: 8px 20px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.skill-prompt-close-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-bg);
}

@media (max-width: 768px) {
  .skill-prompt-modal .ant-modal {
    width: 100% !important;
    max-width: 100%;
    margin: 0;
    top: auto !important;
    bottom: 0 !important;
    transform: none !important;
    padding: 0;
  }

  .skill-prompt-modal .ant-modal-content {
    border-radius: 20px 20px 0 0;
    height: 82vh;
    display: flex;
    flex-direction: column;
  }

  .skill-prompt-modal .ant-modal-header {
    flex-shrink: 0;
    border-bottom: 1px solid #f0f0f0;
    padding: 14px 18px;
    border-radius: 20px 20px 0 0;
  }

  .skill-prompt-modal .ant-modal-body {
    flex: 1;
    max-height: none;
    overflow: hidden;
    padding: 16px 18px calc(16px + env(safe-area-inset-bottom));
  }

  .skill-prompt-body {
    height: 100%;
    display: flex;
    flex-direction: column;
    padding: 0;
  }

  .skill-prompt-text {
    flex: 1;
    max-height: none;
    overflow-y: auto;
    margin-bottom: 16px;
  }

  .skill-prompt-actions {
    padding-top: 12px;
  }

  .skill-prompt-use-btn,
  .skill-prompt-close-btn {
    flex: 1;
    padding: 12px 20px;
    border-radius: 12px;
  }
}

/* 观点编辑弹框 */
.angle-edit-modal.ant-modal {
  max-width: 560px;
}

.angle-edit-modal .ant-modal-body {
  max-height: 70vh;
  overflow-y: auto;
  padding: 16px 20px 20px;
}

.angle-edit-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.angle-edit-actions {
  display: flex;
  gap: 12px;
}

.angle-edit-actions .ant-btn {
  flex: 1;
  border-radius: 10px;
}

@media (max-width: 768px) {
  .angle-edit-modal.ant-modal {
    width: 100% !important;
    max-width: 100%;
    margin: 0;
    top: auto !important;
    bottom: 0 !important;
    transform: none !important;
    padding: 0;
  }

  .angle-edit-modal .ant-modal-content {
    border-radius: 20px 20px 0 0;
    height: auto;
    max-height: 82vh;
    display: flex;
    flex-direction: column;
  }

  .angle-edit-modal .ant-modal-header {
    flex-shrink: 0;
    border-bottom: 1px solid #f0f0f0;
    padding: 14px 18px;
    border-radius: 20px 20px 0 0;
  }

  .angle-edit-modal .ant-modal-body {
    flex: 1;
    max-height: none;
    overflow: hidden;
    padding: 16px 18px calc(16px + env(safe-area-inset-bottom));
  }

  .angle-edit-body {
    height: 100%;
    padding: 0;
  }

  .angle-edit-actions {
    padding-top: 4px;
  }

  .angle-edit-actions .ant-btn {
    padding: 12px 20px;
    border-radius: 12px;
    height: auto;
  }
}

body[data-theme="dark"] .skill-prompt-modal .ant-modal-content,
body[data-theme="dark"] .skill-prompt-modal .ant-modal-header {
  background: #1f1f1f !important;
  border-color: #303030 !important;
}

body[data-theme="dark"] .skill-prompt-modal .ant-modal-title {
  color: #f0f0f0 !important;
}

body[data-theme="dark"] .skill-prompt-modal .ant-modal-close-x {
  color: #a6a6a6 !important;
}

body[data-theme="dark"] .skill-prompt-modal .ant-modal-close:hover {
  background: #2a2a2a !important;
  color: #f0f0f0 !important;
}

body[data-theme="dark"] .skill-prompt-meta {
  color: #a6a6a6;
}

body[data-theme="dark"] .skill-prompt-scope {
  background: rgba(255, 36, 66, 0.12);
  border-color: rgba(255, 36, 66, 0.25);
  color: #ff6b81;
}

body[data-theme="dark"] .skill-prompt-text {
  background: #141414;
  color: #d9d9d9;
  border: 1px solid #303030;
}

body[data-theme="dark"] .skill-prompt-actions {
  border-top-color: #303030;
}

body[data-theme="dark"] .skill-prompt-use-btn {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

body[data-theme="dark"] .skill-prompt-use-btn:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

body[data-theme="dark"] .skill-prompt-close-btn {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .skill-prompt-close-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .angle-edit-modal .ant-modal-content,
body[data-theme="dark"] .angle-edit-modal .ant-modal-header {
  background: #1f1f1f !important;
  border-color: #303030 !important;
}

body[data-theme="dark"] .angle-edit-modal .ant-modal-title {
  color: #f0f0f0 !important;
}

body[data-theme="dark"] .angle-edit-modal .ant-modal-close-x {
  color: #a6a6a6 !important;
}

body[data-theme="dark"] .angle-edit-modal .ant-modal-close:hover {
  background: #2a2a2a !important;
  color: #f0f0f0 !important;
}

body[data-theme="dark"] .mobile-angle-chip {
  background: #2a2a2a;
  border-color: rgba(255, 36, 66, 0.25);
  color: #d9d9d9;
}
</style>
