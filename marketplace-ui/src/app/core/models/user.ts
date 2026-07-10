export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: 'CLIENT' | 'SELLER';
  profilePictureUrl?: string;
}