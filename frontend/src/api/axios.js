import axios from 'axios'
import { setupAuthInterceptor } from './authInterceptor'

const apiClient = axios.create({
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

setupAuthInterceptor(apiClient)

export default apiClient
