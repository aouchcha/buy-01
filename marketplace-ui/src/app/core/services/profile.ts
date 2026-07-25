import { HttpClient } from "@angular/common/http";
import { User } from "../models/user";
import { inject, Injectable } from "@angular/core";
import { environment } from "../../../environments/environment";
import { Observable } from "rxjs";

@Injectable({
  providedIn: 'root',
})
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/users/me`;

  getMe(): Observable<User> {
    return this.http.get<User>(this.apiUrl);
  }

  updateMe(data: User): Observable<User> {
    return this.http.put<User>(this.apiUrl, data);
  }
}