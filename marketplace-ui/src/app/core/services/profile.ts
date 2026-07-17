import { HttpClient } from "@angular/common/http";
import { User } from "../models/user";
import { inject, Injectable } from "@angular/core";
import { environment } from "../../../environments/environment";

@Injectable({
  providedIn: 'root',
})
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/users/me`;

  getMe() {
    return this.http.get<User>(this.apiUrl);
  }

  updateMe(data: User, file?: File) {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify({ firstName: data.firstName, lastName: data.lastName })], { type: 'application/json' }));
    if (file) {
      formData.append('file', file);
    }
    return this.http.put<User>(this.apiUrl, formData);
  }
}