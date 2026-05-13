import request from './request'

export function enroll(data) {
  return request.post('/api/enrollments', data)
}

export function dropEnrollment(enrollmentId) {
  return request.delete(`/api/enrollments/${enrollmentId}`)
}

export function listStudentEnrollments(studentId) {
  return request.get(`/api/enrollments/students/${studentId}`)
}

export function getStudentTimetable(studentId) {
  return request.get(`/api/enrollments/students/${studentId}/timetable`)
}

export function listEnrollments(params) {
  return request.get('/api/enrollments', { params })
}

export function listTeacherCourseStudents(teacherId, courseId) {
  return request.get(`/api/enrollments/teachers/${teacherId}/courses/${courseId}/students`)
}
