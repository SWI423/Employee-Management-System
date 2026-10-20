<template>
  <div class="page-card">
    <div class="toolbar">
      <el-radio-group v-model="role" @change="loadData">
        <el-radio-button value="admin">管理员（admin）</el-radio-button>
        <el-radio-button value="user">普通用户（user）</el-radio-button>
      </el-radio-group>
      <span class="tip">权限数据驱动 @RequirePermission 注解鉴权，接口越权返回 403</span>
    </div>

    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="role" label="角色" width="140">
        <template #default="{ row }">
          <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">
            {{ row.role === 'admin' ? '管理员' : '普通用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="permCode" label="权限码" width="160">
        <template #default="{ row }">
          <code class="perm-code">{{ row.permCode }}</code>
        </template>
      </el-table-column>
      <el-table-column prop="permName" label="权限名称" min-width="200" />
    </el-table>

    <el-descriptions class="mt-16" :column="1" border size="small">
      <el-descriptions-item label="当前账号">
        {{ userInfo?.username }}（{{ userInfo?.role === 'admin' ? '管理员' : '普通用户' }}）
      </el-descriptions-item>
      <el-descriptions-item label="我的权限">
        <el-tag
          v-for="p in (userInfo?.permissions || [])"
          :key="p"
          size="small"
          class="perm-tag"
        >{{ p }}</el-tag>
        <span v-if="!userInfo?.permissions?.length">无</span>
      </el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getPermList } from '@/api/dept'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const userInfo = ref(userStore.userInfo)
const role = ref('admin')
const tableData = ref([])
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    const res = await getPermList(role.value)
    tableData.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-card {
  background: #fff;
  border-radius: 6px;
  padding: 20px;
}
.toolbar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
}
.tip { font-size: 12px; color: #909399; }
.perm-code {
  background: #f4f4f5;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
  color: #409eff;
}
.perm-tag { margin-right: 6px; }
.mt-16 { margin-top: 16px; }
</style>
