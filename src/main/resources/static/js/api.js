/**
 * LifeDrop - Centralized REST API Service Client
 * Hospital-to-Donor Blood Bank Platform
 * Supports JWT Stateless Authentication, Role-based operations, and centralized error handling.
 */

const API_BASE_URL = (() => {
    if (typeof window === 'undefined') return '';
    if (window.LIFEDROP_API_URL) return window.LIFEDROP_API_URL.replace(/\/+$/, '');
    if (window.location.protocol === 'file:') return 'https://lifedrop-blood-bank-3ypa.onrender.com';
    if (window.location.origin.includes('localhost') || window.location.origin.includes('127.0.0.1')) {
        return window.location.port === '8080' ? '' : 'http://localhost:8080';
    }
    return '';
})();

const API_PREFIX = `${API_BASE_URL}/api`;

const Api = {
    // JWT Token Storage Helper
    getToken() {
        return localStorage.getItem('lifedrop_jwt_token');
    },

    setToken(token) {
        if (token) {
            localStorage.setItem('lifedrop_jwt_token', token);
        } else {
            localStorage.removeItem('lifedrop_jwt_token');
        }
    },

    getUser() {
        try {
            const userStr = localStorage.getItem('lifedrop_user');
            return userStr ? JSON.parse(userStr) : null;
        } catch (e) {
            return null;
        }
    },

    setUser(user) {
        if (user) {
            localStorage.setItem('lifedrop_user', JSON.stringify(user));
        } else {
            localStorage.removeItem('lifedrop_user');
        }
    },

    logout() {
        localStorage.removeItem('lifedrop_jwt_token');
        localStorage.removeItem('lifedrop_user');
    },

    // Generic Fetch Wrapper with AbortController timeout & actionable error messages
    async request(endpoint, options = {}) {
        const url = `${API_PREFIX}${endpoint}`;
        const headers = {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        };

        const token = this.getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const timeoutMs = options.timeout || 35000;
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

        try {
            const response = await fetch(url, {
                ...options,
                headers,
                signal: options.signal || controller.signal
            });
            clearTimeout(timeoutId);

            const data = await response.json().catch(() => null);

            if (!response.ok) {
                const errorMessage = data && data.message ? data.message : `Request failed with status ${response.status}`;
                throw new Error(errorMessage);
            }

            return data;
        } catch (error) {
            clearTimeout(timeoutId);
            if (error.name === 'AbortError') {
                throw new Error('Connection timed out. The server may be waking up from sleep, please try again in a few moments.');
            }
            if (error.message === 'Failed to fetch' || error.message.includes('NetworkError')) {
                throw new Error('Network error: Unable to reach LifeDrop backend server. Please verify your connection.');
            }
            console.error(`API Error on [${options.method || 'GET'} ${endpoint}]:`, error);
            throw error;
        }
    },

    // ==========================================
    // 1. Authentication APIs
    // ==========================================
    auth: {
        async login(email, password) {
            return Api.request('/auth/login', {
                method: 'POST',
                body: JSON.stringify({ email, password })
            });
        },

        async register(formData) {
            return Api.request('/auth/register', {
                method: 'POST',
                body: JSON.stringify(formData)
            });
        },

        async getMe() {
            return Api.request('/auth/me');
        }
    },

    // ==========================================
    // 2. Statistics APIs (Live Database-Calculated)
    // ==========================================
    statistics: {
        async getLiveStats() {
            return Api.request('/statistics');
        }
    },

    // ==========================================
    // 3. Blood Inventory APIs
    // ==========================================
    inventory: {
        async getAll() {
            return Api.request('/inventory');
        },

        async getByBloodGroup(bloodGroup) {
            return Api.request(`/inventory/${encodeURIComponent(bloodGroup)}`);
        },

        async updateStock(bloodGroup, data) {
            return Api.request(`/inventory/${encodeURIComponent(bloodGroup)}`, {
                method: 'PUT',
                body: JSON.stringify(data)
            });
        }
    },

    // ==========================================
    // 4. Donor APIs
    // ==========================================
    donors: {
        async search(params = {}) {
            const queryParams = new URLSearchParams();
            if (params.query) queryParams.append('query', params.query);
            if (params.bloodGroup && params.bloodGroup !== 'ALL') queryParams.append('bloodGroup', params.bloodGroup);
            if (params.city) queryParams.append('city', params.city);
            if (params.available !== undefined && params.available !== null) queryParams.append('available', params.available);
            if (params.eligible !== undefined && params.eligible !== null) queryParams.append('eligible', params.eligible);

            const qs = queryParams.toString();
            return Api.request(`/donors${qs ? '?' + qs : ''}`);
        },

        async findCompatible(bloodGroup, city = '') {
            const queryParams = new URLSearchParams({ bloodGroup });
            if (city) queryParams.append('city', city);
            return Api.request(`/donors/compatible?${queryParams.toString()}`);
        },

        async getById(id) {
            return Api.request(`/donors/${id}`);
        },

        async getMyProfile() {
            return Api.request('/donors/my-profile');
        },

        async createOrUpdateProfile(profileData) {
            return Api.request('/donors', {
                method: 'POST',
                body: JSON.stringify(profileData)
            });
        },

        async updateAvailability(id, available) {
            return Api.request(`/donors/${id}/availability`, {
                method: 'PUT',
                body: JSON.stringify({ available })
            });
        },

        async checkEligibility(id) {
            return Api.request(`/donors/${id}/eligibility`);
        }
    },

    // ==========================================
    // 5. Hospital APIs
    // ==========================================
    hospitals: {
        async getAll(params = {}) {
            const queryParams = new URLSearchParams();
            if (params.query) queryParams.append('query', params.query);
            if (params.city) queryParams.append('city', params.city);
            const qs = queryParams.toString();
            return Api.request(`/hospitals${qs ? '?' + qs : ''}`);
        },

        async getById(id) {
            return Api.request(`/hospitals/${id}`);
        },

        async getMyProfile() {
            return Api.request('/hospitals/my-profile');
        },

        async createOrUpdateProfile(profileData) {
            return Api.request('/hospitals', {
                method: 'POST',
                body: JSON.stringify(profileData)
            });
        }
    },

    // ==========================================
    // 6. Blood Requests APIs (Hospital-to-Donor)
    // ==========================================
    bloodRequests: {
        async create(requestData) {
            return Api.request('/blood-requests', {
                method: 'POST',
                body: JSON.stringify(requestData)
            });
        },

        async getAll(params = {}) {
            const queryParams = new URLSearchParams();
            if (params.status) queryParams.append('status', params.status);
            if (params.urgency) queryParams.append('urgency', params.urgency);
            if (params.bloodGroup) queryParams.append('bloodGroup', params.bloodGroup);

            const qs = queryParams.toString();
            return Api.request(`/blood-requests${qs ? '?' + qs : ''}`);
        },

        async getMyRequests() {
            return Api.request('/blood-requests/my-requests');
        },

        async getIncomingForDonor() {
            return Api.request('/blood-requests/incoming');
        },

        async getHighlyDemanded() {
            return Api.request('/blood-requests/demanded-groups');
        },

        async sendToDonor(requestId, donorId, notes = '') {
            return Api.request(`/blood-requests/${requestId}/send-to-donor/${donorId}`, {
                method: 'POST',
                body: JSON.stringify({ requestId, donorId, notes })
            });
        },

        async respondToRequest(requestId, status, scheduledDate = null, scheduledTime = null, notes = '') {
            return Api.request(`/blood-requests/${requestId}/respond`, {
                method: 'PUT',
                body: JSON.stringify({ status, scheduledDate, scheduledTime, notes })
            });
        },

        async updateStatus(id, status) {
            return Api.request(`/blood-requests/${id}/status`, {
                method: 'PUT',
                body: JSON.stringify({ status })
            });
        },

        async delete(id) {
            return Api.request(`/blood-requests/${id}`, {
                method: 'DELETE'
            });
        }
    },

    // ==========================================
    // 7. Donations APIs
    // ==========================================
    donations: {
        async record(data) {
            return Api.request('/donations', {
                method: 'POST',
                body: JSON.stringify(data)
            });
        },

        async getAll() {
            return Api.request('/donations');
        },

        async getByDonor(donorId) {
            return Api.request(`/donations/donor/${donorId}`);
        },

        async updateStatus(id, status) {
            return Api.request(`/donations/${id}/status`, {
                method: 'PUT',
                body: JSON.stringify({ status })
            });
        }
    },

    // ==========================================
    // 8. Referral APIs
    // ==========================================
    referrals: {
        async generate() {
            return Api.request('/referrals/generate', { method: 'POST' });
        },

        async getMyReferrals() {
            return Api.request('/referrals/my-referrals');
        }
    },

    // ==========================================
    // 9. Admin Dashboard APIs
    // ==========================================
    admin: {
        async getDashboardStats() {
            return Api.request('/admin/stats');
        },

        async getAllUsers() {
            return Api.request('/admin/users');
        },

        async changeUserRole(userId, role) {
            return Api.request(`/admin/users/${userId}/role?role=${role}`, { method: 'PUT' });
        },

        async getAllDonors() {
            return Api.request('/admin/donors');
        },

        async toggleDonorAvailability(donorId, available) {
            return Api.request(`/admin/donors/${donorId}/availability`, {
                method: 'PUT',
                body: JSON.stringify({ available })
            });
        },

        async getAllHospitals() {
            return Api.request('/admin/hospitals');
        },

        async getAllRequests() {
            return Api.request('/admin/requests');
        },

        async updateRequestStatus(id, status) {
            return Api.request(`/admin/requests/${id}/status`, {
                method: 'PUT',
                body: JSON.stringify({ status })
            });
        },

        async getAllDonations() {
            return Api.request('/admin/donations');
        },

        async verifyDonation(id) {
            return Api.request(`/admin/donations/${id}/verify`, { method: 'PUT' });
        },

        async updateDonationStatus(id, status) {
            return Api.request(`/admin/donations/${id}/status`, {
                method: 'PUT',
                body: JSON.stringify({ status })
            });
        },

        async getAllReferrals() {
            return Api.request('/admin/referrals');
        },

        async updateInventory(bloodGroup, data) {
            return Api.request(`/admin/inventory/${encodeURIComponent(bloodGroup)}`, {
                method: 'PUT',
                body: JSON.stringify(data)
            });
        }
    }
};

window.Api = Api;
