import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import StudentLayout from '../views/StudentLayout.vue'
import CourseListView from '../views/CourseListView.vue'
import MyTimetableView from '../views/MyTimetableView.vue'
import StudentProfileView from '../views/StudentProfileView.vue'
import TeacherCoursesView from '../views/TeacherCoursesView.vue'
import TeacherStudentsView from '../views/TeacherStudentsView.vue'
import AdminLayout from '../views/AdminLayout.vue'
import AdminCourseView from '../views/AdminCourseView.vue'
import AdminTeacherView from '../views/AdminTeacherView.vue'
import AdminStudentView from '../views/AdminStudentView.vue'
import AdminEnrollmentView from '../views/AdminEnrollmentView.vue'
import ForbiddenView from '../views/ForbiddenView.vue'

const routes = [
  { path: '/', redirect: () => defaultHome() },
  { path: '/login', component: LoginView, meta: { guest: true } },
  { path: '/register', component: RegisterView, meta: { guest: true } },
  {
    path: '/student',
    component: StudentLayout,
    meta: { roles: ['STUDENT'] },
    redirect: '/student/courses',
    children: [
      { path: 'courses', component: CourseListView, meta: { roles: ['STUDENT'] } },
      { path: 'timetable', component: MyTimetableView, meta: { roles: ['STUDENT'] } },
      { path: 'profile', component: StudentProfileView, meta: { roles: ['STUDENT'] } }
    ]
  },
  { path: '/teacher/courses', component: TeacherCoursesView, meta: { roles: ['TEACHER'] } },
  { path: '/teacher/students', component: TeacherStudentsView, meta: { roles: ['TEACHER'] } },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { roles: ['ADMIN'] },
    redirect: '/admin/courses',
    children: [
      { path: 'courses', component: AdminCourseView, meta: { roles: ['ADMIN'] } },
      { path: 'teachers', component: AdminTeacherView, meta: { roles: ['ADMIN'] } },
      { path: 'students', component: AdminStudentView, meta: { roles: ['ADMIN'] } },
      { path: 'enrollments', component: AdminEnrollmentView, meta: { roles: ['ADMIN'] } }
    ]
  },
  { path: '/403', component: ForbiddenView },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')

  if (to.meta.guest && token) {
    return defaultHome()
  }

  if (!to.meta.guest && to.path !== '/403' && !token) {
    return '/login'
  }

  if (to.meta.roles && !to.meta.roles.includes(role)) {
    return '/403'
  }

  return true
})

function defaultHome() {
  const role = localStorage.getItem('role')
  if (role === 'ADMIN') return '/admin'
  if (role === 'TEACHER') return '/teacher/courses'
  if (role === 'STUDENT') return '/student/courses'
  return '/login'
}

export default router
