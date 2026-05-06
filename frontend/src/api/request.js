import axios from 'axios'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 8000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number' && body.code !== 200) {
      const message = body.message || '请求失败'
      const error = new Error(message)
      error.response = response
      error.normalizedMessage = message
      return Promise.reject(error)
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '请求失败'

    if (status === 401) {
      clearLoginState()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }

    if (status === 403) {
      error.normalizedMessage = '没有权限执行该操作'
    } else if (status >= 500) {
      error.normalizedMessage = '服务暂时不可用'
    } else {
      error.normalizedMessage = message
    }

    return Promise.reject(error)
  }
)

function clearLoginState() {
  localStorage.removeItem('token')
  localStorage.removeItem('tokenType')
  localStorage.removeItem('role')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  localStorage.removeItem('relatedId')
}

export default request
