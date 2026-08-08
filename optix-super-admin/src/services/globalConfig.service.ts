import axios from 'axios';

const API_URL = 'https://api.optixapp.in/api/v1/super-admin';
const auth = () => ({ headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });

export const globalConfigService = {
  getConfig: async () => {
    const { data } = await axios.get(`${API_URL}/global-config`, auth());
    return data;
  },
  updateConfig: async (body: any) => {
    const { data } = await axios.patch(`${API_URL}/global-config`, body, auth());
    return data;
  },
  getLiveStatus: async () => {
    const { data } = await axios.get(`${API_URL}/live-status`, auth());
    return data;
  },
};
