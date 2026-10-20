<template>
  <div class="page-card">
    <div class="toolbar">
      <el-button
        v-if="userStore.hasPerm('dept:add')"
        type="primary"
        :icon="Plus"
        @click="handleAdd"
      >
        新增部门
      </el-button>
      <span class="cache-tip">
        <el-icon><InfoFilled /></el-icon>
        热点数据已接入 Redis 缓存（30分钟TTL + 主动失效）
      </span>
    </div>

    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="deptName" label="部门名称" width="200" />
      <el-table-column prop="deptDesc" label="部门描述" min-width="250" />
      <el-table-column prop="empCount" label="员工数" width="100">
        <template #default="{ row }">
          <el-tag :type="row.empCount > 0 ? 'warning' : 'info'" size="small">
            {{ row.empCount || 0 }} 人
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="userStore.hasPerm('dept:edit')"
            type="primary" text size="small"
            @click="handleEdit(row)"
          >编辑</el-button>
          <el-button
            v-if="userStore.hasPerm('dept:delete')"
            type="danger" text size="small"
            @click="handleDelete(row)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editing.id ? '编辑部门' : '新增部门'"
      width="450px"
    >
      <el-form ref="formRef" :model="editing" :rules="formRules" label-width="90px">
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="editing.deptName" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="部门描述" prop="deptDesc">
          <el-input
            v-model="editing.deptDesc"
            type="textarea"
            :rows="3"
            placeholder="请输入部门描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Plus, InfoFilled } from '@element-plus/icons-vue'
import { getDeptList, addDept, updateDept, deleteDept } from '@/api/dept'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const tableData = ref([])
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    const res = await getDeptList()
    tableData.value = res.data
  } finally {
    loading.value = false
  }
}

// ---------- 新增/编辑 ----------
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
const editing = reactive({ id: null, deptName: '', deptDesc: '' })

const formRules = {
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

function handleAdd() {
  Object.assign(editing, { id: null, deptName: '', deptDesc: '' })
  dialogVisible.value = true
}

function handleEdit(row) {
  Object.assign(editing, {
    id: row.id,
    deptName: row.deptName,
    deptDesc: row.deptDesc
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (editing.id) {
      await updateDept(editing.id, editing)
      ElMessage.success('修改成功')
    } else {
      await addDept(editing)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

// ---------- 删除（后端会拦截"部门下有员工"的场景） ----------
async function handleDelete(row) {
  await ElMessageBox.confirm(
    `确定删除部门「${row.deptName}」吗？${row.empCount ? '该部门下还有 ' + row.empCount + ' 名员工，将无法删除。' : ''}`,
    '删除确认',
    { type: 'warning' }
  )
  await deleteDept(row.id)
  ElMessage.success('删除成功')
  loadData()
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
.cache-tip {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
