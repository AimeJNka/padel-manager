export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  matricule: string;
  role: string;
}

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
