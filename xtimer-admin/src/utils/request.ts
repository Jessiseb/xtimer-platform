import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types/response'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE,
  timeout: 10000,
})

service.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse<unknown>
    if (res.code === 0) {
      return response
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    ElMessage.error(error.message || '网络异常，请确认网关 :10001 与 xtimer 服务已启动')
    return Promise.reject(error)
  },
)

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await service(config)
  return (response.data as ApiResponse<T>).data
}
