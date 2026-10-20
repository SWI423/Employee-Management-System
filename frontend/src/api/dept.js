import request from './request'

// 查询全部部门（带员工数）
export function getDeptList() {
  return request.get('/dept/list')
}

// 新增部门
export function addDept(data) {
  return request.post('/dept/add', data)
}

// 修改部门
export function updateDept(id, data) {
  return request.put(`/dept/${id}`, data)
}

// 删除部门
export function deleteDept(id) {
  return request.delete(`/dept/${id}`)
}

// 查询角色权限
export function getPermList(role = 'admin') {
  return request.get('/perm/list', { params: { role } })
}
