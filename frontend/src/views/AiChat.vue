<template>
  <div class="ai-chat">
    <!-- 对话头部 -->
    <div class="chat-header">
      <el-icon :size="20"><ChatDotRound /></el-icon>
      <span class="title">AI 智能助手</span>
      <el-tag size="small" type="success">自然语言查询员工数据</el-tag>
    </div>

    <!-- 消息列表 -->
    <div class="msg-list" ref="listRef">
      <!-- 空状态：引导 + 示例问题 -->
      <div v-if="messages.length === 0" class="empty">
        <el-empty description="问我员工数据问题，例如：统计各部门人数、帮我查研发部的女员工" />
        <div class="quick-questions">
          <el-button
            v-for="q in quickQuestions"
            :key="q"
            size="small"
            type="primary"
            plain
            round
            :disabled="loading"
            @click="send(q)"
          >{{ q }}</el-button>
        </div>
      </div>

      <!-- 对话气泡 -->
      <div v-for="(m, i) in messages" :key="i" :class="['msg-row', m.role]">
        <div class="avatar">
          <el-icon><User v-if="m.role === 'user'" /><MagicStick v-else /></el-icon>
        </div>
        <div class="bubble">{{ m.content }}</div>
      </div>

      <!-- 思考中 -->
      <div v-if="loading" class="msg-row ai">
        <div class="avatar"><el-icon><MagicStick /></el-icon></div>
        <div class="bubble thinking">AI 正在查询数据库…</div>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="input-area">
      <el-input
        v-model="input"
        placeholder="输入自然语言问题，如：统计各部门人数"
        :disabled="loading"
        clearable
        @keyup.enter="send()"
      />
      <el-button type="primary" :loading="loading" :disabled="loading" @click="send()">发送</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { aiChat } from '@/api/ai'

const input = ref('')
const loading = ref(false)
const messages = ref([])
const listRef = ref(null)

const quickQuestions = ['统计各部门人数', '帮我查研发部的女员工', '研发部有哪些员工']

async function send(text) {
  const msg = (text ?? input.value).trim()
  if (!msg || loading.value) return
  input.value = ''
  messages.value.push({ role: 'user', content: msg })
  loading.value = true
  scrollBottom()
  try {
    const res = await aiChat(msg)
    messages.value.push({ role: 'ai', content: res.data })
  } catch (e) {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
    scrollBottom()
  }
}

function scrollBottom() {
  nextTick(() => {
    if (listRef.value) listRef.value.scrollTop = listRef.value.scrollHeight
  })
}
</script>

<style scoped>
.ai-chat { display: flex; flex-direction: column; height: calc(100vh - 96px); }

.chat-header {
  display: flex; align-items: center; gap: 8px;
  padding: 0 16px; height: 48px;
  background: #fff; border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06);
}
.chat-header .title { font-size: 15px; font-weight: 600; color: #303133; margin-right: 4px; }

.msg-list {
  flex: 1; overflow-y: auto;
  padding: 16px; margin: 12px 0;
  background: #fff; border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06);
}

.empty { padding: 24px 0 8px; }
.quick-questions { display: flex; justify-content: center; gap: 8px; flex-wrap: wrap; }

.msg-row { display: flex; gap: 10px; margin-bottom: 14px; }
.msg-row.user { justify-content: flex-end; }
.msg-row.ai { justify-content: flex-start; }

.avatar {
  width: 32px; height: 32px; flex-shrink: 0;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 16px;
}
.msg-row.user .avatar { background: #409eff; order: 2; }
.msg-row.ai .avatar { background: #67c23a; }

.bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px; line-height: 1.6;
  white-space: pre-wrap; word-break: break-word;
}
.msg-row.user .bubble { background: #409eff; color: #fff; }
.msg-row.ai .bubble { background: #f4f4f5; color: #303133; }
.bubble.thinking { color: #909399; }

.input-area { display: flex; gap: 10px; }
.input-area .el-input { flex: 1; }
</style>
