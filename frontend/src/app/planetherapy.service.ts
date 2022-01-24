import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Profile } from './model/profile';

@Injectable({
  providedIn: 'root'
})
export class PlanetherapyService {

  private baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) { }

  getProfile(profile: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/profiles/${profile}`);
  }

  updateProfile(profile: Profile): Observable<Object> {
    return this.http.put(`${this.baseUrl}/profiles`, profile);
  }

  saveProfile(profile: Profile): Observable<Object> {
    return this.http.put(`${this.baseUrl}/profiles/apply`, profile);
  }

  openReferenceImage(rootFolder: string): Observable<Object> {
    return this.http.put(`${this.baseUrl}/reference/open`,rootFolder);
  }

  saveReferenceImage(rootFolder: string): Observable<Object> {
    return this.http.put(`${this.baseUrl}/reference/save`,rootFolder);
  }
}
