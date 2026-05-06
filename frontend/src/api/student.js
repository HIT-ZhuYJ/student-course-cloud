import request from './request'

export function getStudent(studentId) {
  return request.get(`/api/students/${studentId}`)
}

export function updateStudent(studentId, data) {
  return request.put(`/api/students/${studentId}`, data)
}

export function listStudents(params) {
  return request.get('/api/students', { params })
}
