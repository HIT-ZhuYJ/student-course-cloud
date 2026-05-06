import request from './request'

export function listTeachers(params) {
  return request.get('/api/teachers', { params })
}

export function createTeacher(data) {
  return request.post('/api/teachers', data)
}

export function updateTeacher(teacherId, data) {
  return request.put(`/api/teachers/${teacherId}`, data)
}

export function deleteTeacher(teacherId) {
  return request.delete(`/api/teachers/${teacherId}`)
}

export function assignTeacherCourse(teacherId, courseId) {
  return request.post(`/api/teachers/${teacherId}/courses/${courseId}`)
}

export function cancelTeacherCourse(teacherId, courseId) {
  return request.delete(`/api/teachers/${teacherId}/courses/${courseId}`)
}

export function listTeacherCourses(teacherId) {
  return request.get(`/api/teachers/${teacherId}/courses`)
}
