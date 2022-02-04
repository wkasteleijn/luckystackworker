import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Profile } from './model/profile';

@Injectable({
  providedIn: 'root',
})
export class PlanetherapyService {
  private baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  getProfile(profile: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/profiles/${profile}`);
  }

  getStatus(): Observable<any> {
    return this.http.get(`${this.baseUrl}/profiles/status`);
  }

  updateProfile(profile: Profile): Observable<Object> {
    return this.http.put(`${this.baseUrl}/profiles`, profile);
  }

  applyProfile(profile: Profile): Observable<Object> {
    return this.http.put(`${this.baseUrl}/profiles/apply`, profile);
  }

  exit(): Observable<Object> {
    return this.http.put(`${this.baseUrl}/profiles/exit`, null);
  }

  openReferenceImage(rootFolder: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/reference/open?path=${rootFolder}`);
  }

  selectRootFolder(): Observable<any> {
    return this.http.get(`${this.baseUrl}/reference/rootfolder`);
  }

  saveReferenceImage(rootFolder: string): Observable<Object> {
    return this.http.put(`${this.baseUrl}/reference/save`, rootFolder);
  }
}
