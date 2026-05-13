import request from './request'

export function listCourses(params) {
  return request.get('/api/courses', { params })
}

export function getCourse(courseId) {
  return request.get(`/api/courses/${courseId}`)
}

export function createCourse(data) {
  return request.post('/api/courses', data)
}

export function updateCourse(courseId, data) {
  return request.put(`/api/courses/${courseId}`, data)
}

export function deleteCourse(courseId) {
  return request.delete(`/api/courses/${courseId}`)
}

export function addCourseSchedule(courseId, data) {
  return request.post(`/api/courses/${courseId}/schedules`, data)
}

export function listCourseSchedules(courseId) {
  return request.get(`/api/courses/${courseId}/schedules`)
}
