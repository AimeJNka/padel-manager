export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  matricule: string;
  role: string;
}

// Backend returns the same AuthResponseDTO for both /api/auth/login and /api/auth/register
export type RegisterResponse = LoginResponse;

export interface AdminLoginResponse {
  accessToken: string;
  idAdmin: number;
  role: string;
}

export interface RegisterRequest {
  nom: string;
  prenom: string;
  email: string;
  telephone: string;
  motDePasse: string;
  idType: number;
  idSite?: number;
}

export interface RefreshResponse {
  accessToken: string;
  role: string;
}
