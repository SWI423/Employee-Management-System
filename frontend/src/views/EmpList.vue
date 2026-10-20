<template>
  <div class="page-card">
    <!-- 查询条件区 -->
    <el-form :inline="true" :model="query" class="query-form">
      <el-form-item label="姓名">
        <el-input v-model="query.name" placeholder="按姓名模糊查询" clearable />
      </el-form-item>
      <el-form-item label="部门">
        <el-select v-model="query.deptId" placeholder="全部部门" clearable style="width: 160px">
          <el-option
            v-for="d in deptList"
            :key="d.id"
            :label="d.deptName"
            :value="d.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="岗位">
        <el-input v-model="query.job" placeholder="按岗位模糊查询" clearable />
      </el-form-item>
      <el-form-item label="性别">
        <el-select v-model="query.gender" placeholder="全部" clearable style="width: 100px">
          <el-option label="男" value="男" />
          <el-option label="女" value="女" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮区（权限码控制可见性） -->
    <div class="toolbar">
      <el-button
        v-if="userStore.hasPerm('emp:add')"
        type="primary"
        :icon="Plus"
        @click="handleAdd"
      >
        新增员工
      </el-button>
      <el-button
        v-if="userStore.hasPerm('emp:delete')"
        type="danger"
        :icon="Delete"
        :disabled="!selectedIds.length"
        @click="handleBatchDelete"
      >
        批量删除<span v-if="selectedIds.length">（{{ selectedIds.length }}）</span>
      </el-button>
    </div>

    <!-- 数据表格 -->
    <el-table
      :data="tableData"
      border
      stripe
      v-loading="loading"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="gender" label="性别" width="70" />
      <el-table-column prop="age" label="年龄" width="70" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="deptName" label="部门" width="120">
        <template #default="{ row }">{{ row.deptName || '未分配' }}</template>
      </el-table-column>
      <el-table-column prop="job" label="岗位" min-width="150" />
      <el-table-column prop="entryDate" label="入职日期" width="120" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="userStore.hasPerm('emp:edit')"
            type="primary" text size="small"
            @click="handleEdit(row)"
          >编辑</el-button>
          <el-button
            v-if="userStore.hasPerm('emp:delete')"
            type="danger" text size="small"
            @click="handleDelete(row)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      class="pagination"
      v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize"
      :total="total"
      :page-sizes="[5, 10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="loadData"
      @current-change="loadData"
    />

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editing.id ? '编辑员工' : '新增员工'"
      width="500px"
    >
      <el-form ref="formRef" :model="editing" :rules="formRules" label-width="80px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="editing.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="editing.age" :min="18" :max="60" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="editing.gender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editing.phone" placeholder="11位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="部门" prop="deptId">
          <el-select v-model="editing.deptId" placeholder="请选择部门" style="width: 100%">
            <el-option
              v-for="d in deptList"
              :key="d.id"
              :label="d.deptName"
              :value="d.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位" prop="job">
          <el-input v-model="editing.job" placeholder="请输入岗位" />
        </el-form-item>
        <el-form-item label="入职日期" prop="entryDate">
          <el-date-picker
            v-model="editing.entryDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
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
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import {
  getEmpPage, addEmp, updateEmp, deleteEmp, batchDeleteEmp
} from '@/api/emp'
import { getDeptList } from '@/api/dept'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()

// ---------- 查询 ----------
const query = reactive({
  pageNum: 1, pageSize: 10,
  name: '', deptId: null, job: '', gender: ''
})
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const deptList = ref([])
const selectedIds = ref([])

async function loadData() {
  loading.value = true
  try {
    const res = await getEmpPage(query)
    tableData.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadDepts() {
  const res = await getDeptList()
  deptList.value = res.data
}

function handleSearch() {
  query.pageNum = 1 // 查询时回到第1页，避免停在不存在的结果页
  loadData()
}

function handleReset() {
  Object.assign(query, { pageNum: 1, pageSize: 10, name: '', deptId: null, job: '', gender: '' })
  loadData()
}

function handleSelectionChange(rows) {
  selectedIds.value = rows.map((r) => r.id)
}

// ---------- 新增/编辑 ----------
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
const editing = reactive({
  id: null, name: '', age: undefined, gender: '男',
  phone: '', deptId: null, job: '', entryDate: ''
})

const formRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  age: [{ required: true, message: '请输入年龄', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  deptId: [{ required: true, message: '请选择部门', trigger: 'change' }],
  job: [{ required: true, message: '请输入岗位', trigger: 'blur' }]
}

function handleAdd() {
  Object.assign(editing, {
    id: null, name: '', age: undefined, gender: '男',
    phone: '', deptId: null, job: '', entryDate: ''
  })
  dialogVisible.value = true
}

function handleEdit(row) {
  Object.assign(editing, {
    id: row.id,
    name: row.name,
    age: row.age,
    gender: row.gender,
    phone: row.phone,
    deptId: row.deptId,
    job: row.job,
    entryDate: row.entryDate
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (editing.id) {
      await updateEmp(editing.id, editing)
      ElMessage.success('修改成功')
    } else {
      await addEmp(editing)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

// ---------- 删除 ----------
async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除员工「${row.name}」吗？`, '删除确认', {
    type: 'warning'
  })
  await deleteEmp(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(
    `确定删除选中的 ${selectedIds.value.length} 名员工吗？`, '批量删除确认',
    { type: 'warning' }
  )
  await batchDeleteEmp(selectedIds.value)
  ElMessage.success('批量删除成功')
  loadData()
}

onMounted(() => {
  loadDepts()
  loadData()
})
</script>

<style scoped>
.page-card {
  background: #fff;
  border-radius: 6px;
  padding: 20px;
}
.query-form { margin-bottom: 8px; }
.toolbar { margin-bottom: 16px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
