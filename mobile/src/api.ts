import axios from "axios";
import { API_URL } from "./config";

let token: string | null = null;
export const setAuthToken = (t: string | null) => {
  token = t;
};

export const api = axios.create({ baseURL: API_URL, timeout: 10000 });

api.interceptors.request.use((config) => {
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export const errorMessage = (e: any): string =>
  e.response?.data?.message ??
  (e.response ? `Cerere respinsă (${e.response.status})` : "Nu pot contacta serverul");