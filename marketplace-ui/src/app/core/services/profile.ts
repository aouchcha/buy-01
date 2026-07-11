import { HttpClient } from "@angular/common/http";
import { User } from "../models/user";
import { inject, Injectable } from "@angular/core";


@Injectable({
  providedIn: 'root',
})
export class ProfileService {
  private readonly http = inject(HttpClient);

  getMe() {
    return this.http.get<User>(
      'http://localhost:8080/api/users/me'
    );
  }

  updateMe(data: User) {
    return this.http.put<User>(
      'http://localhost:8080/api/users/me',
      data
    );
  }
}