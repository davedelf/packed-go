import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserProfile, UpdateUserProfileRequest } from '../../shared/models/user.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private http = inject(HttpClient);
  private apiUrl = environment.usersServiceUrl;

  // Get user profile by authUserId
  getUserProfile(authUserId: number): Observable<UserProfile> {
    return this.http.get<UserProfile>(
      `${this.apiUrl}/user-profiles/by-auth-user/${authUserId}`
    );
  }

  // Get current logged user profile
  getMyProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/user-profiles/me`);
  }

  // Update user profile
  updateUserProfile(authUserId: number, profile: UpdateUserProfileRequest): Observable<UserProfile> {
    return this.http.put<UserProfile>(
      `${this.apiUrl}/user-profiles/by-auth-user/${authUserId}`,
      profile
    );
  }

  // Update my profile
  updateMyProfile(profile: UpdateUserProfileRequest): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.apiUrl}/user-profiles/me`, profile);
  }

  // Get all users (admin only)
  getAllUsers(): Observable<UserProfile[]> {
    return this.http.get<UserProfile[]>(`${this.apiUrl}/user-profiles`);
  }
}
