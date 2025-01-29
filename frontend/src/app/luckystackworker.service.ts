import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Profile } from './model/profile';

@Injectable({
  providedIn: 'root',
})
export class LuckyStackWorkerService {
  private baseUrl = 'http://localhost:36469/api';

  constructor(private http: HttpClient) {}

  getProfile(profile: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/profiles/${profile}`);
  }

  loadProfile(): Observable<any> {
    return this.http.get(`${this.baseUrl}/profiles/load`);
  }

  getSelectedProfile(): Observable<any> {
    return this.http.get(`${this.baseUrl}/profiles/selected`);
  }

  getStatus(): Observable<any> {
    return this.http.get(`${this.baseUrl}/profiles/status`);
  }

  updateProfile(profile: Profile, operation: string): Observable<any> {
    let url = `${this.baseUrl}/profiles`;
    if (operation) {
      url += `?operation=${operation}`;
    }
    return this.http.put(url, profile);
  }

  applyProfile(profile: Profile): Observable<Object> {
    return this.http.put(`${this.baseUrl}/profiles/apply`, profile);
  }

  exit(): Observable<Object> {
    return this.http.put(`${this.baseUrl}/profiles/exit`, null);
  }

  stop(): Observable<Object> {
    return this.http.put(`${this.baseUrl}/profiles/stop`, null);
  }

  openReferenceImage(scale: number, openImageMode: string): Observable<any> {
    return this.http.get(
      `${this.baseUrl}/reference/open?scale=${scale}&openImageMode=${openImageMode}`
    );
  }

  scale(profile: Profile): Observable<any> {
    return this.http.put(`${this.baseUrl}/profiles/scale`, profile);
  }

  rotate(profile: Profile): Observable<any> {
    return this.http.put(`${this.baseUrl}/profiles/rotate`, profile);
  }

  selectRootFolder(): Observable<any> {
    return this.http.get(`${this.baseUrl}/reference/rootfolder`);
  }

  saveReferenceImage(profile: Profile): Observable<any> {
    return this.http.put(`${this.baseUrl}/reference/save`, profile);
  }

  zoomIn(): Observable<Object> {
    return this.http.put(`${this.baseUrl}/reference/zoomin`, null);
  }

  zoomOut(): Observable<Object> {
    return this.http.put(`${this.baseUrl}/reference/zoomout`, null);
  }

  maximize(): Observable<Object> {
    return this.http.put(`${this.baseUrl}/reference/maximize`, null);
  }

  cropSelectionChanged(): Observable<Object> {
    return this.http.put(`${this.baseUrl}/reference/crop`, null);
  }

  nightModeChanged(nightMode: boolean): Observable<Object> {
    return this.http.put(
      `${this.baseUrl}/reference/night?on=${nightMode}`,
      null
    );
  }

  histogramSelectionChanged(): Observable<Object> {
    return this.http.put(`${this.baseUrl}/reference/histogram`, null);
  }

  getLatestVersion(): Observable<any> {
    return this.http.get(`${this.baseUrl}/profiles/version`);
  }

  realtimeChanged(realtime: boolean): Observable<any> {
    return this.http.put(`${this.baseUrl}/reference/realtime`, realtime);
  }

  channelChanged(channel: string) {
    return this.http.put(`${this.baseUrl}/reference/channel`, channel);
  }

  loadCustomPSF(): Observable<any> {
    return this.http.get(`${this.baseUrl}/profiles/custom-psf`);
  }
}
