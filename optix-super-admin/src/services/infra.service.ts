import axios from 'axios';

const API_URL = 'https://api.optixapp.in/api/v1/super-admin/infra';
const auth = () => ({ headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });

export const infraService = {
  getOverview: async () => {
    const { data } = await axios.get(`${API_URL}/overview`, auth());
    return data;
  },
  getServerHealth: async () => {
    const { data } = await axios.get(`${API_URL}/server-health`, auth());
    return data;
  },
  getDbMonitor: async () => {
    const { data } = await axios.get(`${API_URL}/db-monitor`, auth());
    return data;
  },
  getWebSocketMonitor: async () => {
    const { data } = await axios.get(`${API_URL}/websocket-monitor`, auth());
    return data;
  },
  getApiMonitor: async () => {
    const { data } = await axios.get(`${API_URL}/api-monitor`, auth());
    return data;
  },
  getBackgroundServices: async () => {
    const { data } = await axios.get(`${API_URL}/background-services`, auth());
    return data;
  },
  getContainers: async () => {
    const { data } = await axios.get(`${API_URL}/containers`, auth());
    return data;
  },
  restartContainer: async (id: string) => {
    const { data } = await axios.post(`${API_URL}/containers/${id}/restart`, {}, auth());
    return data;
  },
  getRealtimeLogs: async (params: { filter?: string; search?: string; limit?: number } = {}) => {
    const { data } = await axios.get(`${API_URL}/logs`, { params, ...auth() });
    return data;
  },
  getErrorTracking: async () => {
    const { data } = await axios.get(`${API_URL}/errors`, auth());
    return data;
  },
  getBackups: async () => {
    const { data } = await axios.get(`${API_URL}/backups`, auth());
    return data;
  },
  createBackup: async () => {
    const { data } = await axios.post(`${API_URL}/backups/create`, {}, auth());
    return data;
  },
  getStorageStats: async () => {
    const { data } = await axios.get(`${API_URL}/storage`, auth());
    return data;
  },
  cleanupStorage: async () => {
    const { data } = await axios.post(`${API_URL}/storage/cleanup`, {}, auth());
    return data;
  },
  freeRam: async () => {
    const { data } = await axios.post(`${API_URL}/vps/free-ram`, {}, auth());
    return data;
  },
  cleanDisk: async () => {
    const { data } = await axios.post(`${API_URL}/vps/clean-disk`, {}, auth());
    return data;
  },

  getSecurityStats: async () => {
    const { data } = await axios.get(`${API_URL}/security`, auth());
    return data;
  },
  getDeployments: async () => {
    const { data } = await axios.get(`${API_URL}/deployments`, auth());
    return data;
  },
  getAlerts: async () => {
    const { data } = await axios.get(`${API_URL}/alerts`, auth());
    return data;
  },
  getLiveFeed: async () => {
    const { data } = await axios.get(`${API_URL}/live-feed`, auth());
    return data;
  },
};
