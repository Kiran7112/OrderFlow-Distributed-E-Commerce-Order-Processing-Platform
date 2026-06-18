import { createSlice, type PayloadAction } from '@reduxjs/toolkit';
import type { AuthState } from '../types';

const loadAuth = (): AuthState => {
  const token = localStorage.getItem('token');
  const userId = localStorage.getItem('userId');
  const role = localStorage.getItem('role');
  return {
    token,
    userId,
    role,
    isAuthenticated: !!token,
  };
};

const initialState: AuthState = loadAuth();

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials: (
      state,
      action: PayloadAction<{ token: string; userId: string; role: string }>
    ) => {
      state.token = action.payload.token;
      state.userId = action.payload.userId;
      state.role = action.payload.role;
      state.isAuthenticated = true;
      localStorage.setItem('token', action.payload.token);
      localStorage.setItem('userId', action.payload.userId);
      localStorage.setItem('role', action.payload.role);
    },
    logout: (state) => {
      state.token = null;
      state.userId = null;
      state.role = null;
      state.isAuthenticated = false;
      localStorage.removeItem('token');
      localStorage.removeItem('userId');
      localStorage.removeItem('role');
    },
  },
});

export const { setCredentials, logout } = authSlice.actions;
export default authSlice.reducer;
