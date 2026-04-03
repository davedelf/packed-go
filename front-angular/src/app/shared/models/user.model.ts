// Auth User (from auth-service)
export interface AuthUser {
  id: number;
  username: string;
  email: string;
  document?: number;
  role: string;
  loginType: string;
  isEmailVerified?: boolean;
  lastLogin?: string;
}

// Login Requests
export interface AdminLoginRequest {
  email: string;
  password: string;
}

export interface CustomerLoginRequest {
  document: number;
  password: string;
}

// Login Response
export interface LoginResponse {
  token: string;
  refreshToken: string;
  user: AuthUser;
  permissions: string[];
  expiresIn: number;
}

// Registration Requests
export interface AdminRegistrationRequest {
  username: string;
  email: string;
  password: string;
  authorizationCode: string;
}

export interface CustomerRegistrationRequest {
  // Datos de autenticación
  username: string;
  email: string;
  document: number;
  password: string;
  // Datos del perfil
  name: string;
  lastName: string;
  bornDate: string; // LocalDate en formato ISO (YYYY-MM-DD)
  telephone: number;
  gender: 'MALE' | 'FEMALE' | 'OTHER' | 'PREFER_NOT_TO_SAY';
}

// User Profile (from users-service)
export interface UserProfile {
  authUserId: number;
  name: string;
  lastName: string;
  gender: string;
  document: number;
  bornDate: string; // LocalDate en formato ISO
  telephone: number;
  profileImageUrl?: string;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

// Update User Profile Request
export interface UpdateUserProfileRequest {
  name: string;
  lastName: string;
  gender: string;
  document: number;
  bornDate: string; // LocalDate en formato ISO (YYYY-MM-DD)
  telephone: number;
  profileImageUrl?: string;
}

