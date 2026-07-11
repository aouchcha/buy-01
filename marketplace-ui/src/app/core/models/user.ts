export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
  profilePictureUrl?: string;
}

export enum Role {
  CLIENT,
  SELLER
}