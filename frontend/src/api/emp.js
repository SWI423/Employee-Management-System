import request from './request'

// 条件分页查询员工
export function getEmpPage(params) {
  return request.get('/emp/page', { params })
}

// 查询全部员工（下拉选择等场景）
export function getEmpList() {
  return request.get('/emp/list')
}

// 新增员工
export function addEmp(data) {
  return request.post('/emp/add', data)
}

// 修改员工
export function updateEmp(id, data) {
  return request.put(`/emp/${id}`, data)
}

// 删除员工
export function deleteEmp(id) {
  return request.delete(`/emp/${id}`)
}

// 批量删除员工
export function batchDeleteEmp(ids) {
  return request.delete('/emp/batch', { data: ids })
}
