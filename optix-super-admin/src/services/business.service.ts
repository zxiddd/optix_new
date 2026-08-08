import axios from 'axios';

const API_URL = 'https://api.optixapp.in/api/v1/super-admin';

// Reusing existing token from localStorage (to be implemented in Auth milestone)
const getAuthHeader = () => ({
  headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
});

export const businessService = {
  getBusinesses: async (params: any) => {
    const { data } = await axios.get(`${API_URL}/businesses`, {
      params,
      ...getAuthHeader()
    });
    return data;
  },

  getBusinessDetail: async (id: string) => {
    const { data } = await axios.get(`${API_URL}/businesses/${id}`, getAuthHeader());
    return data;
  },

  updateStatus: async (id: string, status: string) => {
    const { data } = await axios.patch(`${API_URL}/businesses/${id}/status`, { status }, getAuthHeader());
    return data;
  },

  resetTrial: async (id: string) => {
    const { data } = await axios.post(`${API_URL}/businesses/${id}/reset-trial`, {}, getAuthHeader());
    return data;
  }
};
