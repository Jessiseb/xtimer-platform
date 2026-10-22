export interface ApiResponse<T> {
  code: number
  message: string
  datetime: string
  data: T
}

export interface PageResult<T> {
  list: T[]
  total: number
}
