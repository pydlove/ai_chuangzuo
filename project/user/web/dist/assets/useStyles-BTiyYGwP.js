import{l as u}from"./index-CyXCbNQA.js";const l=[{name:"年度总结",desc:"回顾、复盘、展望",promptSummary:`语气：回顾性、感恩 + 数据自省
结构：成绩 + 反思 + 明年目标
长度：1500-2500 字，带小标题分章`,prompt:"你是一位擅长年度复盘与展望的写手。文章语气应回顾性、感恩且带数据自省。结构分为：成绩回顾、深度反思、明年目标。长度 1500-2500 字，使用小标题分章。"},{name:"产品评测",desc:"客观、数据驱动、多角度对比",promptSummary:`语气：客观中立、有理有据
结构：外观 + 性能 + 体验 + 总结
要素：必带参数对比表 + 优缺点`,prompt:"你是一位客观中立的产品评测作者。文章需数据驱动、多角度对比，结构分为外观、性能、体验、总结，必须包含参数对比表和优缺点分析。"},{name:"情感散文",desc:"细腻、共情、个人化表达",promptSummary:`语气：细腻、温暖、第一人称
修辞：善用比喻、意象、留白
结构：场景 + 情绪 + 升华`,prompt:"你擅长写情感散文。使用细腻温暖的第一人称，善用比喻、意象和留白。结构为：场景描写、情绪铺陈、主题升华。"},{name:"职场干货",desc:"实操性强、结构清晰",promptSummary:`语气：专业务实、老板视角
结构：痛点 + 方案 + 步骤 + 案例
要素：可执行的 checklist`,prompt:"你是一位专业务实的职场作者。从老板视角出发，结构为痛点、方案、步骤、案例，必须提供可执行的 checklist。"},{name:"热点评论",desc:"观点鲜明、论据紧凑",promptSummary:`语气：犀利、有态度
结构：事件概述 + 观点 + 论据 + 结论
要素：引用数据或权威观点`,prompt:"你是一位观点鲜明的热点评论员。语气犀利有态度，结构为事件概述、核心观点、论据支撑、结论，需引用数据或权威观点。"},{name:"知识科普",desc:"深入浅出、逻辑清晰",promptSummary:`语气：亲和、易懂
结构：问题 + 原理 + 案例 + 总结
要素：避免术语堆砌，善用类比`,prompt:"你是一位知识科普作者。语气亲和易懂，结构为提出问题、解释原理、给出案例、总结要点。避免术语堆砌，善用类比。"},{name:"营销转化",desc:"引导行动、强说服",promptSummary:`语气：紧迫感 + 利益点突出
结构：痛点共鸣 + 方案 + 案例 + CTA
要素：必带限时/优惠/倒计时`,prompt:"你是一位营销转化写手。语气紧迫、利益点突出，结构为痛点共鸣、解决方案、案例证明、行动号召（CTA），必须包含限时/优惠/倒计时要素。"},{name:"故事叙事",desc:"沉浸感、有冲突与转折",promptSummary:`语气：克制、文学化
结构：起承转合 + 人物对话
要素：场景细节 + 心理活动`,prompt:"你是一位故事叙事作者。语气克制文学化，结构为起承转合，包含人物对话，注重场景细节和心理活动描写。"}],o=u([]),a=u(l[0]),T=e=>{a.value=e},E=e=>{o.value.push({name:e.name.trim(),desc:e.desc||"自定义风格",prompt:e.prompt.trim(),scope:(e.scope||"").trim(),count:0})},H=(e,t)=>{const r=o.value.findIndex(n=>n.name===e);r>-1&&(o.value[r]={...o.value[r],name:t.name.trim(),desc:t.desc||"自定义风格",prompt:t.prompt.trim(),scope:(t.scope||"").trim()},a.value&&a.value.name===e&&(a.value=o.value[r]))},I=e=>{const t=o.value.findIndex(r=>r.name===e);t>-1&&o.value.splice(t,1),a.value&&a.value.name===e&&(a.value=l[0])},b=(e,t=null)=>{const r=e.trim().toLowerCase();if(!r||t&&r===t.trim().toLowerCase())return!1;const n=l.some(i=>i.name.trim().toLowerCase()===r),m=o.value.some(i=>i.name.trim().toLowerCase()===r);return n||m},S="aichuangzuo_learned_styles";function x(){try{const e=localStorage.getItem(S);return e?JSON.parse(e):[]}catch{return[]}}function f(){localStorage.setItem(S,JSON.stringify(s.value))}async function L(e){const t=e.slice(0,1e3)+"|"+e.length,r=new TextEncoder().encode(t),n=await crypto.subtle.digest("SHA-1",r);return Array.from(new Uint8Array(n)).map(i=>i.toString(16).padStart(2,"0")).join("").slice(0,16)}const s=u(x()),d=u(!1);function O(e){return new Promise((t,r)=>{const n=new FileReader;n.onload=m=>t(m.target.result),n.onerror=()=>r(new Error("文件读取失败")),n.readAsText(e)})}async function R(e){if(!window.mammoth)throw new Error("mammoth.js 未加载");const t=await e.arrayBuffer();return(await window.mammoth.extractRawText({arrayBuffer:t})).value}async function _(e,t){var r,n,m;d.value=!0;try{const i=await L(e),p=e.split(/\n\s*\n/).filter(c=>c.trim().length>20),y=((r=p[0])==null?void 0:r.trim())||"",v=((n=p[Math.floor(p.length/2)])==null?void 0:n.trim())||"",h=((m=e.split(/[。！？\n]/).filter(c=>c.trim().length>10).sort((c,g)=>g.length-c.length)[0])==null?void 0:m.trim().slice(0,80))||"",w=`你是一位中文写手，请模仿以下参考文章的写作风格：

【语气】克制、文学化，善用短句与留白
【词汇】避免网络用语，偏书面表达
【句式】长短句交替，节奏感强
【结构】起承转合清晰，结尾有余味

请在生成新内容时参考以下片段的风格特征。`;return await new Promise(c=>setTimeout(c,1500)),{sourceType:t.sourceType,excerpt1:(y||v).slice(0,120),excerpt2:h,prompt:w,scope:"",fileHash:i,createdAt:new Date().toISOString()}}finally{d.value=!1}}function j(e,t=null){const r=e.trim().toLowerCase();return!r||t&&r===t.trim().toLowerCase()?!1:s.value.some(n=>n.name.trim().toLowerCase()===r)}function k(e){s.value.unshift({name:e.name.trim(),sourceType:e.sourceType,excerpt1:e.excerpt1,excerpt2:e.excerpt2,prompt:e.prompt.trim(),scope:(e.scope||"").trim(),fileHash:e.fileHash,createdAt:e.createdAt}),f()}function B(e){const t=s.value.findIndex(r=>r.name===e);t>-1&&s.value.splice(t,1),f()}function D(e,t){const r=s.value.findIndex(n=>n.name===e);if(r>-1){const n={...s.value[r],name:t.name.trim(),prompt:t.prompt.trim(),scope:(t.scope||"").trim()};s.value[r]=n,a.value&&a.value.name===e&&(a.value=n),f()}}function z(e){return s.value.find(t=>t.fileHash===e)}export{T as a,E as b,a as c,d,B as e,R as f,O as g,j as h,b as i,D as j,k,s as l,o as m,_ as n,z as o,I as r,l as s,H as u};
