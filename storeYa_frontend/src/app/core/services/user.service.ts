import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { apiUrl } from '../../../environment/api.config';

const BASE_URL = apiUrl.BASE_URL + '/users';

export interface User {
    id?: number;
    username?: string;
    firstName?: string;
    lastName?: string;
    email?: string;
    phone?: string;
    address?: string;
}

@Injectable({
    providedIn: 'root'
})
export class UserService {
    private apiUrl = '/api/users';

    constructor(private http: HttpClient) { }

    getCurrentUser(): Observable<User> {
        return this.http.get<User>(`${BASE_URL}/me`);
    }

    updateCurrentUser(data: User): Observable<User> {
        return this.http.put<User>(`${BASE_URL}/me`, data);
    }
}
