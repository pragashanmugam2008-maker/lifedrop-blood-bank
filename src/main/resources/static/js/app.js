/**
 * LifeDrop - Main Application Controller
 * Hospital-to-Donor Blood Bank Platform
 * Handles live UI data binding, reactive searches, hospital and donor dashboards, and session management.
 */

document.addEventListener('DOMContentLoaded', () => {
    App.init();
});

// Centralized Red Blood Cell Compatibility Matrix (Universal Donor O-, Universal Recipient AB+)
const BloodCompatibility = {
    compatibilityMap: {
        'O-': ['O-', 'O+', 'A-', 'A+', 'B-', 'B+', 'AB-', 'AB+'],
        'O+': ['O+', 'A+', 'B+', 'AB+'],
        'A-': ['A-', 'A+', 'AB-', 'AB+'],
        'A+': ['A+', 'AB+'],
        'B-': ['B-', 'B+', 'AB-', 'AB+'],
        'B+': ['B+', 'AB+'],
        'AB-': ['AB-', 'AB+'],
        'AB+': ['AB+']
    },
    canDonateTo(donorBg, recipientBg) {
        if (!donorBg || !recipientBg) return false;
        const normDonor = donorBg.trim().toUpperCase();
        const normRecipient = recipientBg.trim().toUpperCase();
        const recipients = this.compatibilityMap[normDonor];
        return recipients ? recipients.includes(normRecipient) : false;
    },
    getCompatibleRecipients(donorBg) {
        if (!donorBg) return [];
        return this.compatibilityMap[donorBg.trim().toUpperCase()] || [];
    },
    getCompatibleDonors(recipientBg) {
        if (!recipientBg) return [];
        const norm = recipientBg.trim().toUpperCase();
        return Object.keys(this.compatibilityMap).filter(donorBg =>
            this.compatibilityMap[donorBg].includes(norm)
        );
    }
};

const App = {
    state: {
        user: null,
        selectedBloodGroup: 'ALL',
        donors: [],
        inventory: [],
        hospitalRequests: [],
        stats: null
    },

    init() {
        this.checkAuth();
        this.loadStatistics();
        this.loadInventory();
        this.loadHospitalRequests();
        this.loadDonors();
        this.setupEventListeners();
    },

    // ==========================================
    // 1. Authentication & Session Management
    // ==========================================
    checkAuth() {
        this.state.user = Api.getUser();
        this.renderAuthUI();

        if (Api.getToken()) {
            Api.auth.getMe().then(res => {
                if (res && res.data) {
                    this.state.user = res.data;
                    Api.setUser(res.data);
                    this.renderAuthUI();
                }
            }).catch(() => {
                Api.logout();
                this.state.user = null;
                this.renderAuthUI();
            });
        }
    },

    renderAuthUI() {
        const authContainer = document.getElementById('nav-auth-container');
        if (!authContainer) return;

        if (this.state.user) {
            const role = this.state.user.role || 'DONOR';
            const roleClass = role.toLowerCase();
            const displayName = this.state.user.hospitalName || this.state.user.name;

            authContainer.innerHTML = `
                <div class="user-badge">
                    <span class="user-name">${role === 'HOSPITAL' ? '🏥' : (role === 'DONOR' ? '🩸' : '👑')} ${displayName}</span>
                    <span class="role-tag ${roleClass}">${role}</span>
                </div>
                ${role === 'ADMIN' ? `
                    <button class="btn btn-secondary btn-sm" onclick="App.openAdminModal()">
                        ⚙️ Admin Dashboard
                    </button>
                ` : ''}
                ${role === 'HOSPITAL' ? `
                    <button class="btn btn-primary btn-sm" onclick="App.openHospitalDashboard()">
                        🏥 Hospital Dashboard
                    </button>
                    <button class="btn btn-outline-danger btn-sm" onclick="App.openBloodRequestModal()">
                        🚨 Request Blood
                    </button>
                ` : ''}
                ${role === 'DONOR' ? `
                    <button class="btn btn-primary btn-sm" onclick="App.openDonorDashboard()">
                        🩸 Donor Dashboard
                    </button>
                ` : ''}
                <button class="btn btn-secondary btn-sm" onclick="App.logout()">
                    Logout
                </button>
            `;

            // Update referral card if present
            if (this.state.user.referralCode) {
                const refCodeDisplay = document.getElementById('referral-code-display');
                if (refCodeDisplay) refCodeDisplay.innerText = this.state.user.referralCode;
            }
        } else {
            authContainer.innerHTML = `
                <button class="btn btn-secondary btn-sm" onclick="App.openAuthModal('login')">
                    Login
                </button>
                <button class="btn btn-primary btn-sm" onclick="App.openAuthModal('register')">
                    Register
                </button>
            `;
        }

        this.updateRoleVisibility();
        this.loadInventory();
    },

    updateRoleVisibility() {
        const role = this.state.user ? this.state.user.role : null;
        
        // Nav items
        const findDonorNav = document.getElementById('nav-item-find-donor');
        const hospitalsNav = document.getElementById('nav-item-hospitals');
        const donorsNav = document.getElementById('nav-item-donors');
        
        // Page sections
        const hospitalsSection = document.getElementById('hospitals');
        const donorsSection = document.getElementById('donors');
        
        // Hero CTA container
        const heroCta = document.getElementById('hero-cta-container');

        if (role === 'DONOR') {
            // Modification 3 & 6: Remove Find a Donor, Hospitals section, and Donors list from Donor view
            if (findDonorNav) findDonorNav.style.display = 'none';
            if (hospitalsNav) hospitalsNav.style.display = 'none';
            if (donorsNav) donorsNav.style.display = 'none';
            if (hospitalsSection) hospitalsSection.style.display = 'none';
            if (donorsSection) donorsSection.style.display = 'none';
            
            if (heroCta) {
                heroCta.innerHTML = `
                    <button class="btn btn-primary btn-lg" onclick="App.openDonorDashboard()">
                        🩸 Open Donor Dashboard
                    </button>
                    <button class="btn btn-secondary btn-lg" onclick="App.viewMyReferrals()">
                        ⭐ My Referral Code
                    </button>
                `;
            }
        } else if (role === 'HOSPITAL') {
            // Hospital view: Find donor, Hospital dashboard, request blood
            if (findDonorNav) findDonorNav.style.display = 'block';
            if (hospitalsNav) hospitalsNav.style.display = 'block';
            if (donorsNav) donorsNav.style.display = 'block';
            if (hospitalsSection) hospitalsSection.style.display = 'block';
            if (donorsSection) donorsSection.style.display = 'block';
            
            if (heroCta) {
                heroCta.innerHTML = `
                    <button class="btn btn-primary btn-lg" onclick="App.openHospitalDashboard()">
                        🏥 Hospital Dashboard
                    </button>
                    <button class="btn btn-outline-danger btn-lg" onclick="App.openBloodRequestModal()">
                        🚨 Request Blood
                    </button>
                    <button class="btn btn-secondary btn-lg" onclick="App.openFindDonorModal()">
                        🔍 Find Compatible Donors
                    </button>
                `;
            }
        } else if (role === 'ADMIN') {
            if (findDonorNav) findDonorNav.style.display = 'block';
            if (hospitalsNav) hospitalsNav.style.display = 'block';
            if (donorsNav) donorsNav.style.display = 'block';
            if (hospitalsSection) hospitalsSection.style.display = 'block';
            if (donorsSection) donorsSection.style.display = 'block';
            
            if (heroCta) {
                heroCta.innerHTML = `
                    <button class="btn btn-secondary btn-lg" onclick="App.openAdminModal()">
                        ⚙️ Admin Dashboard
                    </button>
                    <button class="btn btn-primary btn-lg" onclick="App.openHospitalDashboard()">
                        🏥 Hospital Dashboard
                    </button>
                    <button class="btn btn-outline-danger btn-lg" onclick="App.openFindDonorModal()">
                        🔍 Find Compatible Donors
                    </button>
                `;
            }
        } else {
            // Guest view
            if (findDonorNav) findDonorNav.style.display = 'block';
            if (hospitalsNav) hospitalsNav.style.display = 'block';
            if (donorsNav) donorsNav.style.display = 'block';
            if (hospitalsSection) hospitalsSection.style.display = 'block';
            if (donorsSection) donorsSection.style.display = 'block';
            
            if (heroCta) {
                heroCta.innerHTML = `
                    <button class="btn btn-primary btn-lg" onclick="App.openFindDonorModal()">
                        🔍 Find Compatible Donors
                    </button>
                    <button class="btn btn-secondary btn-lg" onclick="App.openBecomeDonorModal()">
                        🩸 Become a Donor
                    </button>
                    <button class="btn btn-outline-danger btn-lg" onclick="App.openBloodRequestModal()">
                        🏥 Request Blood (Hospital)
                    </button>
                `;
            }
        }
    },

    logout() {
        Api.logout();
        this.state.user = null;
        this.renderAuthUI();
        this.showToast('Logged out successfully', 'info');
    },

    toggleRegRoleFields() {
        const role = document.getElementById('reg-role').value;
        const donorFields = document.getElementById('reg-donor-fields');
        const hospitalFields = document.getElementById('reg-hospital-fields');
        const nameLabel = document.getElementById('reg-name-label');

        if (role === 'HOSPITAL') {
            if (donorFields) donorFields.style.display = 'none';
            if (hospitalFields) hospitalFields.style.display = 'block';
            if (nameLabel) nameLabel.innerText = 'Hospital / Blood Bank Name';
        } else {
            if (donorFields) donorFields.style.display = 'block';
            if (hospitalFields) hospitalFields.style.display = 'none';
            if (nameLabel) nameLabel.innerText = 'Full Name';
        }
    },

    // ==========================================
    // 2. Live Database-Calculated Statistics
    // ==========================================
    async loadStatistics() {
        try {
            const res = await Api.statistics.getLiveStats();
            if (res && res.success && res.data) {
                this.state.stats = res.data;
                this.renderStatistics(res.data);
            }
        } catch (e) {
            console.error('Failed to load live statistics:', e);
        }
    },

    renderStatistics(stats) {
        this.animateCounter('stat-donors', stats.registeredDonors || 0);
        this.animateCounter('stat-donations', stats.successfulDonations || 0);
        this.animateCounter('stat-lives', stats.livesHelped || 0);
        this.animateCounter('stat-emergency', stats.emergencyResponsePercentage || 98, '%');
    },

    animateCounter(id, target, suffix = '') {
        const el = document.getElementById(id);
        if (!el) return;
        let count = 0;
        const speed = Math.max(1, Math.floor(target / 30));
        const timer = setInterval(() => {
            count += speed;
            if (count >= target) {
                el.innerText = target.toLocaleString() + (suffix ? suffix : '+');
                clearInterval(timer);
            } else {
                el.innerText = count.toLocaleString() + suffix;
            }
        }, 25);
    },

    // ==========================================
    // 3. Blood Inventory (Blood Needed Section)
    // ==========================================
    async loadInventory() {
        const grid = document.getElementById('blood-inventory-grid');
        if (!grid) return;

        const isDonor = this.state.user && this.state.user.role === 'DONOR';
        const tagEl = document.getElementById('blood-needed-tag');
        const titleEl = document.getElementById('blood-needed-title');
        const descEl = document.getElementById('blood-needed-desc');

        if (isDonor) {
            // Modification 2: Donor sees only HIGHLY DEMANDED BLOOD GROUPS calculated from active requests
            if (tagEl) tagEl.innerText = 'High Demand Groups';
            if (titleEl) titleEl.innerText = 'HIGHLY DEMANDED BLOOD GROUPS';
            if (descEl) descEl.innerText = 'Blood groups with the highest current demand based on active hospital blood requests.';

            try {
                const res = await Api.bloodRequests.getHighlyDemanded();
                if (res && res.success && res.data) {
                    const demanded = res.data;
                    if (demanded.length === 0) {
                        grid.innerHTML = `
                            <div style="grid-column: 1/-1; text-align: center; padding: 40px; background: var(--bg-card); border-radius: var(--radius-md); border: 1px dashed var(--border-glass);">
                                <p style="color: var(--text-muted);">No high-demand blood requirements recorded at the moment.</p>
                            </div>
                        `;
                    } else {
                        grid.innerHTML = demanded.map(item => {
                            const urgencyClass = (item.urgencyLevel || 'HIGH').toLowerCase();
                            return `
                                <div class="blood-card" style="border-top: 4px solid #f43f5e;">
                                    <div class="blood-card-header">
                                        <div class="blood-group-badge">🔴 ${item.bloodGroup}</div>
                                        <span class="demand-badge ${urgencyClass}">${item.urgencyLevel}</span>
                                    </div>
                                    <div class="blood-card-body">
                                        <div class="stock-label">
                                            <span>Units Needed</span>
                                            <strong style="color: #ffffff; font-size: 1.2rem;">${item.unitsNeeded} units</strong>
                                        </div>
                                        <div class="stock-label" style="font-size: 0.85rem; margin-top: 10px;">
                                            <span>Active Hospital Requests</span>
                                            <span style="color: var(--primary); font-weight: 700;">${item.activeRequestCount} Requests</span>
                                        </div>
                                    </div>
                                    <button class="btn btn-primary btn-sm" style="width: 100%;" onclick="App.openDonorDashboard()">
                                        🩸 View Matching in Dashboard
                                    </button>
                                </div>
                            `;
                        }).join('');
                    }
                }
            } catch (e) {
                console.error('Failed to load highly demanded blood groups:', e);
            }
        } else {
            if (tagEl) tagEl.innerText = 'Current Inventory';
            if (titleEl) titleEl.innerText = 'Blood Availability & Demand';
            if (descEl) descEl.innerText = 'Live stock levels and dynamically calculated urgency thresholds across all 8 ABO/Rh blood groups.';

            try {
                const res = await Api.inventory.getAll();
                if (res && res.success && res.data) {
                    this.state.inventory = res.data;
                    this.renderInventory(res.data);
                }
            } catch (e) {
                console.error('Failed to load blood inventory:', e);
            }
        }
    },

    renderInventory(items) {
        const grid = document.getElementById('blood-inventory-grid');
        if (!grid) return;

        grid.innerHTML = items.map(item => {
            const statusClass = (item.status || 'NORMAL').toLowerCase();
            const fillPct = Math.min(100, Math.round(((item.availableUnits || 0) / (item.minimumRequiredUnits || 15)) * 100));

            return `
                <div class="blood-card">
                    <div class="blood-card-header">
                        <div class="blood-group-badge">${item.bloodGroup}</div>
                        <span class="demand-badge ${statusClass}">${item.demandBadge || 'NORMAL'}</span>
                    </div>
                    <div class="blood-card-body">
                        <div class="stock-label">
                            <span>Available Units</span>
                            <strong style="color: #ffffff;">${item.availableUnits} units</strong>
                        </div>
                        <div class="stock-meter">
                            <div class="stock-meter-fill ${statusClass}" style="width: ${fillPct}%;"></div>
                        </div>
                        <div class="stock-label" style="font-size: 0.78rem;">
                            <span>Target Minimum</span>
                            <span>${item.minimumRequiredUnits || 15} units</span>
                        </div>
                    </div>
                    <button class="btn btn-secondary btn-sm" style="width: 100%;" onclick="App.openRequestForBlood('${item.bloodGroup}')">
                        🩸 Request ${item.bloodGroup}
                    </button>
                </div>
            `;
        }).join('');
    },

    // ==========================================
    // 4. Active Hospital Blood Requests Section
    // ==========================================
    async loadHospitalRequests() {
        const grid = document.getElementById('hospital-requests-grid');
        if (!grid) return;

        try {
            const res = await Api.bloodRequests.getAll({ status: 'ACTIVE' });
            if (res && res.success && res.data) {
                this.state.hospitalRequests = res.data;
                this.renderHospitalRequests(res.data);
            }
        } catch (e) {
            console.error('Failed to load hospital requests:', e);
        }
    },

    renderHospitalRequests(requests) {
        const grid = document.getElementById('hospital-requests-grid');
        if (!grid) return;

        if (requests.length === 0) {
            grid.innerHTML = `
                <div style="grid-column: 1/-1; text-align: center; padding: 40px; background: var(--bg-card); border-radius: var(--radius-md); border: 1px dashed var(--border-glass);">
                    <p style="color: var(--text-muted);">No active emergency blood requests right now. Blood inventory is stable.</p>
                </div>
            `;
            return;
        }

        grid.innerHTML = requests.map(req => {
            const urgencyClass = (req.urgency || 'HIGH').toLowerCase();

            return `
                <div class="request-card ${urgencyClass}">
                    <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px;">
                        <div>
                            <h4 style="font-size: 1.15rem; color: #ffffff;">🏥 ${req.hospitalName}</h4>
                            <span style="font-size: 0.85rem; color: var(--text-muted);">📍 ${req.hospitalLocation || 'Chennai'}</span>
                        </div>
                        <span class="demand-badge ${urgencyClass === 'critical' ? 'critical' : 'low'}">${req.urgency}</span>
                    </div>

                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; background: rgba(15, 23, 42, 0.6); padding: 10px 14px; border-radius: var(--radius-sm);">
                        <div>
                            <span style="font-size: 0.8rem; color: var(--text-muted);">Blood Required:</span>
                            <div style="font-size: 1.25rem; font-weight: 800; color: #fb7185;">${req.bloodGroup}</div>
                        </div>
                        <div style="text-align: right;">
                            <span style="font-size: 0.8rem; color: var(--text-muted);">Units Needed:</span>
                            <div style="font-size: 1.25rem; font-weight: 800; color: #ffffff;">${req.requiredUnits} Units</div>
                        </div>
                    </div>

                    <p style="font-size: 0.88rem; color: var(--text-muted); margin-bottom: 14px; line-height: 1.4;">
                        ${req.notes || 'Emergency requirement for hospitalized patient.'}
                    </p>

                    <div style="display: flex; gap: 8px;">
                        <button class="btn btn-primary btn-sm" style="flex: 1;" onclick="App.respondToHospitalRequest(${req.id}, '${req.hospitalName}', '${req.bloodGroup}')">
                            🩸 Donate to Hospital
                        </button>
                        <button class="btn btn-secondary btn-sm" onclick="App.findCompatibleForRequest('${req.bloodGroup}')">
                            🔍 Matched (${req.matchedDonorsCount || 0})
                        </button>
                    </div>
                </div>
            `;
        }).join('');
    },

    // ==========================================
    // 5. Donor Database & Search
    // ==========================================
    async loadDonors() {
        const grid = document.getElementById('donors-grid');
        if (!grid) return;

        grid.innerHTML = `
            <div style="grid-column: 1/-1; text-align: center; padding: 40px; color: var(--text-muted);">
                <p>Loading verified voluntary donors from database...</p>
            </div>
        `;

        try {
            const searchInput = document.getElementById('donor-search-input');
            const cityInput = document.getElementById('donor-city-input');
            const availableOnly = document.getElementById('filter-available-only');
            const eligibleOnly = document.getElementById('filter-eligible-only');

            const params = {
                query: searchInput ? searchInput.value.trim() : '',
                city: cityInput ? cityInput.value.trim() : '',
                bloodGroup: this.state.selectedBloodGroup,
                available: availableOnly && availableOnly.checked ? true : null,
                eligible: eligibleOnly && eligibleOnly.checked ? true : null
            };

            const res = await Api.donors.search(params);
            if (res && res.success && res.data) {
                this.state.donors = res.data;
                this.renderDonors(res.data);
            }
        } catch (e) {
            console.error('Failed to load donors:', e);
            grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: var(--primary);"><p>Failed to load donors from backend.</p></div>`;
        }
    },

    renderDonors(donors) {
        const grid = document.getElementById('donors-grid');
        if (!grid) return;

        if (donors.length === 0) {
            grid.innerHTML = `
                <div style="grid-column: 1/-1; text-align: center; padding: 50px; background: var(--bg-card); border-radius: var(--radius-md); border: 1px dashed var(--border-glass);">
                    <h4 style="margin-bottom: 8px;">No donors found</h4>
                    <p style="color: var(--text-muted); font-size: 0.95rem;">Try adjusting your blood group filter or search keywords.</p>
                </div>
            `;
            return;
        }

        grid.innerHTML = donors.map(donor => {
            const badgeIcon = donor.badgeIcon || '🌱';
            const badgeTitle = donor.badgeTitle || 'New Donor';
            const initials = donor.name ? donor.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase() : 'LD';
            const isEligible = donor.eligible;

            return `
                <div class="donor-card">
                    <div class="donor-card-top">
                        <div class="donor-avatar-name">
                            <div class="donor-avatar">${initials}</div>
                            <div>
                                <h4 class="donor-name">${donor.name}</h4>
                                <span class="donor-location">📍 ${donor.city || 'Chennai'}</span>
                            </div>
                        </div>
                        <div class="donor-blood-badge">${donor.bloodGroup}</div>
                    </div>

                    <div class="donor-details-grid">
                        <div class="detail-item">
                            <span class="label">Age / Gender</span>
                            <span class="val">${donor.age || 25} yrs • ${donor.gender || 'M'}</span>
                        </div>
                        <div class="detail-item">
                            <span class="label">Total Donations</span>
                            <span class="val">${donor.totalDonations || 0} times</span>
                        </div>
                        <div class="detail-item">
                            <span class="label">Status</span>
                            <span class="val" style="color: ${donor.availabilityStatus ? '#34d399' : '#f87171'};">
                                ${donor.availabilityStatus ? '● Available' : '○ Unavailable'}
                            </span>
                        </div>
                        <div class="detail-item">
                            <span class="label">Eligibility</span>
                            <span class="val" style="color: ${isEligible ? '#34d399' : '#fbbf24'};">
                                ${isEligible ? '✓ Eligible' : '⏱ Cool-down'}
                            </span>
                        </div>
                    </div>

                    <div class="donor-card-footer">
                        <div class="badge-tag">
                            <span>${badgeIcon}</span>
                            <span>${badgeTitle}</span>
                        </div>
                        <div class="rating-stars">
                            ⭐ ${donor.rating ? donor.rating.toFixed(1) : '5.0'} (${donor.stars || 0}★)
                        </div>
                    </div>

                    <div style="margin-top: 14px; display: flex; gap: 8px;">
                        <button class="btn btn-secondary btn-sm" style="flex: 1;" onclick="App.checkDonorEligibility(${donor.id})">
                            🔍 Check Eligibility
                        </button>
                        <button class="btn btn-primary btn-sm" style="flex: 1;" onclick="App.openRequestDirectToDonor(${donor.id}, '${donor.name}', '${donor.bloodGroup}')">
                            Request
                        </button>
                    </div>
                </div>
            `;
        }).join('');
    },

    setBloodGroupFilter(bloodGroup, btnEl) {
        this.state.selectedBloodGroup = bloodGroup;
        document.querySelectorAll('.filter-btn').forEach(btn => btn.classList.remove('active'));
        if (btnEl) btnEl.classList.add('active');
        this.loadDonors();
    },

    // ==========================================
    // 6. Compatible Donor Matching
    // ==========================================
    findCompatibleForRequest(bloodGroup) {
        const select = document.getElementById('find-blood-group');
        if (select) select.value = bloodGroup;
        this.openFindDonorModal();
    },

    async findCompatibleDonors() {
        const bloodGroupSelect = document.getElementById('find-blood-group');
        const cityInput = document.getElementById('find-city');
        const resultsContainer = document.getElementById('compatible-results-container');

        if (!bloodGroupSelect || !resultsContainer) return;

        const bloodGroup = bloodGroupSelect.value;
        const city = cityInput ? cityInput.value.trim() : '';

        resultsContainer.innerHTML = `<p style="text-align: center; color: var(--text-muted); padding: 20px;">Finding compatible, available, and eligible donors for ${bloodGroup}...</p>`;

        try {
            const res = await Api.donors.findCompatible(bloodGroup, city);
            if (res && res.success && res.data) {
                const donors = res.data;
                if (donors.length === 0) {
                    resultsContainer.innerHTML = `
                        <div style="text-align: center; padding: 24px; background: rgba(15, 23, 42, 0.6); border-radius: var(--radius-sm);">
                            <h5>No compatible donors found</h5>
                            <p style="color: var(--text-muted); font-size: 0.88rem; margin-top: 6px;">
                                There are currently no available and eligible donors compatible with <strong>${bloodGroup}</strong> in the database.
                            </p>
                        </div>
                    `;
                } else {
                    resultsContainer.innerHTML = `
                        <div style="margin-bottom: 12px; font-size: 0.88rem; color: #34d399;">
                            ✓ Found <strong>${donors.length}</strong> compatible & eligible donors who can donate to <strong>${bloodGroup}</strong>:
                        </div>
                        <div style="display: flex; flex-direction: column; gap: 10px; max-height: 300px; overflow-y: auto;">
                            ${donors.map(d => `
                                <div style="display: flex; justify-content: space-between; align-items: center; background: rgba(15, 23, 42, 0.8); padding: 12px 16px; border-radius: var(--radius-sm); border: 1px solid var(--border-glass);">
                                    <div>
                                        <strong style="color: #ffffff;">${d.name}</strong> (${d.bloodGroup})
                                        <div style="font-size: 0.8rem; color: var(--text-muted);">📍 ${d.city || 'City'} • Total: ${d.totalDonations || 0} donations • ⭐ ${d.rating}</div>
                                    </div>
                                    <button class="btn btn-primary btn-sm" onclick="App.openRequestDirectToDonor(${d.id}, '${d.name}', '${d.bloodGroup}')">
                                        Send Request
                                    </button>
                                </div>
                            `).join('')}
                        </div>
                    `;
                }
            }
        } catch (e) {
            resultsContainer.innerHTML = `<p style="color: var(--primary); text-align: center;">Error finding compatible donors: ${e.message}</p>`;
        }
    },

    // ==========================================
    // 7. Dedicated Hospital Dashboard
    // ==========================================
    async openHospitalDashboard() {
        if (!this.state.user || (this.state.user.role !== 'HOSPITAL' && this.state.user.role !== 'ADMIN')) {
            this.showToast('Access denied: Hospital account required', 'error');
            return;
        }
        this.openModal('hospital-modal');
        this.loadHospitalTab('hosp-requests');
    },

    async loadHospitalTab(tabName) {
        document.querySelectorAll('.hospital-tab-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.tab === tabName);
        });

        const content = document.getElementById('hospital-tab-content');
        if (!content) return;

        content.innerHTML = `<p style="text-align: center; padding: 30px; color: var(--text-muted);">Loading hospital data...</p>`;

        try {
            if (tabName === 'hosp-requests') {
                const res = await Api.bloodRequests.getMyRequests();
                if (res && res.data) {
                    const reqs = res.data;
                    if (reqs.length === 0) {
                        content.innerHTML = `
                            <div style="text-align: center; padding: 40px;">
                                <p style="color: var(--text-muted); margin-bottom: 16px;">You have not posted any active blood requests yet.</p>
                                <button class="btn btn-primary btn-sm" onclick="App.loadHospitalTab('hosp-create')">➕ Create Blood Request</button>
                            </div>
                        `;
                    } else {
                        content.innerHTML = `
                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
                                <h4>My Hospital Blood Requests</h4>
                                <button class="btn btn-primary btn-sm" onclick="App.loadHospitalTab('hosp-create')">➕ Post New Request</button>
                            </div>
                            <div class="data-table-wrapper">
                                <table class="data-table">
                                    <thead>
                                        <tr>
                                            <th>Blood Group</th>
                                            <th>Units</th>
                                            <th>Urgency</th>
                                            <th>Date</th>
                                            <th>Matched Donors</th>
                                            <th>Status</th>
                                            <th>Action</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        ${reqs.map(r => `
                                            <tr>
                                                <td><span class="role-tag donor">${r.bloodGroup}</span></td>
                                                <td><strong>${r.requiredUnits}</strong></td>
                                                <td><span class="demand-badge ${r.urgency === 'CRITICAL' ? 'critical' : 'low'}">${r.urgency}</span></td>
                                                <td>${r.requiredDate || 'Immediate'}</td>
                                                <td><strong style="color: #34d399;">${r.matchedDonorsCount} Donors</strong></td>
                                                <td><span class="role-tag ${r.status === 'FULFILLED' ? 'donor' : 'hospital'}">${r.status}</span></td>
                                                <td>
                                                    ${r.status === 'ACTIVE' ? `
                                                        <button class="btn btn-secondary btn-sm" onclick="App.findCompatibleForRequest('${r.bloodGroup}')">Match Donors</button>
                                                        <button class="btn btn-primary btn-sm" onclick="App.fulfillHospitalRequest(${r.id})">Fulfill</button>
                                                    ` : 'Completed ✓'}
                                                </td>
                                            </tr>
                                        `).join('')}
                                    </tbody>
                                </table>
                            </div>
                        `;
                    }
                }
            } else if (tabName === 'hosp-create') {
                content.innerHTML = `
                    <form id="hosp-dash-create-form" onsubmit="App.handleHospitalCreateRequest(event)">
                        <div class="form-row">
                            <div class="form-group">
                                <label class="form-label">Blood Group Required</label>
                                <select id="dash-req-blood" class="form-select" required>
                                    <option value="O+">O+</option>
                                    <option value="O-">O-</option>
                                    <option value="A+">A+</option>
                                    <option value="A-">A-</option>
                                    <option value="B+">B+</option>
                                    <option value="B-">B-</option>
                                    <option value="AB+">AB+</option>
                                    <option value="AB-">AB-</option>
                                </select>
                            </div>
                            <div class="form-group">
                                <label class="form-label">Units Needed</label>
                                <input type="number" id="dash-req-units" class="form-input" min="1" max="20" value="2" required>
                            </div>
                        </div>
                        <div class="form-row">
                            <div class="form-group">
                                <label class="form-label">Urgency Level</label>
                                <select id="dash-req-urgency" class="form-select">
                                    <option value="CRITICAL">Critical (Immediate)</option>
                                    <option value="HIGH" selected>High (Within 24h)</option>
                                    <option value="MEDIUM">Medium (Within 48h)</option>
                                    <option value="LOW">Low (Scheduled)</option>
                                </select>
                            </div>
                            <div class="form-group">
                                <label class="form-label">Required Date</label>
                                <input type="date" id="dash-req-date" class="form-input">
                            </div>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Clinical Notes / Department</label>
                            <input type="text" id="dash-req-notes" class="form-input" placeholder="e.g. Critical requirement for orthopedic / trauma unit">
                        </div>
                        <button type="submit" class="btn btn-primary" style="width: 100%;">
                            🚨 Dispatch Blood Request to Compatible Donors
                        </button>
                    </form>
                `;
            } else if (tabName === 'hosp-donors') {
                content.innerHTML = `
                    <div class="form-row" style="margin-bottom: 16px;">
                        <div class="form-group">
                            <label class="form-label">Recipient Blood Group Needed</label>
                            <select id="hosp-donor-match-bg" class="form-select" onchange="App.loadHospitalMatchedDonors()">
                                <option value="O+">O+</option>
                                <option value="O-">O-</option>
                                <option value="A+">A+</option>
                                <option value="A-">A-</option>
                                <option value="B+">B+</option>
                                <option value="B-">B-</option>
                                <option value="AB+">AB+</option>
                                <option value="AB-">AB-</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Filter by City</label>
                            <input type="text" id="hosp-donor-match-city" class="form-input" placeholder="e.g. Chennai" oninput="App.loadHospitalMatchedDonors()">
                        </div>
                    </div>
                    <div id="hosp-matched-donors-list">
                        <!-- Loaded dynamically -->
                    </div>
                `;
                this.loadHospitalMatchedDonors();
            } else if (tabName === 'hosp-profile') {
                const res = await Api.hospitals.getMyProfile().catch(() => null);
                const hosp = res ? res.data : this.state.user;

                content.innerHTML = `
                    <form onsubmit="App.handleSaveHospitalProfile(event)">
                        <div class="form-group">
                            <label class="form-label">Hospital Name</label>
                            <input type="text" id="prof-hosp-name" class="form-input" value="${(hosp && hosp.hospitalName) || this.state.user.name}" required>
                        </div>
                        <div class="form-row">
                            <div class="form-group">
                                <label class="form-label">License / Registration Number</label>
                                <input type="text" id="prof-hosp-license" class="form-input" value="${(hosp && hosp.licenseNumber) || 'LIC-CHE-2024'}">
                            </div>
                            <div class="form-group">
                                <label class="form-label">Emergency Helpline</label>
                                <input type="tel" id="prof-hosp-helpline" class="form-input" value="${(hosp && hosp.emergencyHelpline) || this.state.user.phone}">
                            </div>
                        </div>
                        <div class="form-row">
                            <div class="form-group">
                                <label class="form-label">City</label>
                                <input type="text" id="prof-hosp-city" class="form-input" value="${(hosp && hosp.city) || 'Chennai'}" required>
                            </div>
                            <div class="form-group">
                                <label class="form-label">Contact Person</label>
                                <input type="text" id="prof-hosp-contact" class="form-input" value="${(hosp && hosp.contactPerson) || this.state.user.name}">
                            </div>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Address</label>
                            <input type="text" id="prof-hosp-address" class="form-input" value="${(hosp && hosp.address) || 'Greams Road, Thousand Lights, Chennai'}">
                        </div>
                        <button type="submit" class="btn btn-primary" style="width: 100%;">
                            Save Hospital Profile
                        </button>
                    </form>
                `;
            }
        } catch (e) {
            content.innerHTML = `<p style="color: var(--primary); text-align: center;">Error: ${e.message}</p>`;
        }
    },

    async loadHospitalMatchedDonors() {
        const bgSelect = document.getElementById('hosp-donor-match-bg');
        const cityInput = document.getElementById('hosp-donor-match-city');
        const list = document.getElementById('hosp-matched-donors-list');
        if (!bgSelect || !list) return;

        const bloodGroup = bgSelect.value;
        const city = cityInput ? cityInput.value.trim() : '';

        try {
            const res = await Api.donors.findCompatible(bloodGroup, city);
            if (res && res.data) {
                if (res.data.length === 0) {
                    list.innerHTML = `<p style="text-align: center; color: var(--text-muted); padding: 20px;">No available & eligible compatible donors found for ${bloodGroup}.</p>`;
                } else {
                    list.innerHTML = `
                        <div style="margin-bottom: 10px; font-size: 0.88rem; color: #34d399;">
                            ✓ <strong>${res.data.length}</strong> verified compatible donors available:
                        </div>
                        <div class="data-table-wrapper">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Donor Name</th>
                                        <th>Blood Group</th>
                                        <th>City</th>
                                        <th>Donations</th>
                                        <th>Badge</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${res.data.map(d => `
                                        <tr>
                                            <td><strong>${d.name}</strong></td>
                                            <td><span class="role-tag donor">${d.bloodGroup}</span></td>
                                            <td>${d.city}</td>
                                            <td>${d.totalDonations || 0} times</td>
                                            <td>${d.badgeIcon || ''} ${d.badgeTitle || ''}</td>
                                            <td>
                                                <button class="btn btn-primary btn-sm" onclick="App.hospitalSendDirectRequest(${d.id}, '${d.name}', '${bloodGroup}')">
                                                    📩 Send Request
                                                </button>
                                            </td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                        </div>
                    `;
                }
            }
        } catch (e) {
            list.innerHTML = `<p style="color: var(--primary);">Error: ${e.message}</p>`;
        }
    },

    async handleHospitalCreateRequest(e) {
        e.preventDefault();
        const data = {
            hospitalName: this.state.user.hospitalName || this.state.user.name,
            bloodGroup: document.getElementById('dash-req-blood').value,
            requiredUnits: parseInt(document.getElementById('dash-req-units').value),
            urgency: document.getElementById('dash-req-urgency').value,
            requiredDate: document.getElementById('dash-req-date').value || null,
            hospitalLocation: this.state.user.city || 'Chennai',
            notes: document.getElementById('dash-req-notes').value.trim()
        };

        try {
            await Api.bloodRequests.create(data);
            this.showToast('Blood requirement posted successfully! Matched compatible donors notified.', 'success');
            this.loadHospitalTab('hosp-requests');
            this.loadHospitalRequests();
            this.loadStatistics();
        } catch (err) {
            this.showToast(err.message, 'error');
        }
    },

    async handleSaveHospitalProfile(e) {
        e.preventDefault();
        const data = {
            hospitalName: document.getElementById('prof-hosp-name').value.trim(),
            licenseNumber: document.getElementById('prof-hosp-license').value.trim(),
            city: document.getElementById('prof-hosp-city').value.trim(),
            address: document.getElementById('prof-hosp-address').value.trim(),
            emergencyHelpline: document.getElementById('prof-hosp-helpline').value.trim(),
            contactPerson: document.getElementById('prof-hosp-contact').value.trim()
        };

        try {
            await Api.hospitals.createOrUpdateProfile(data);
            this.showToast('Hospital profile updated successfully', 'success');
            this.loadHospitalTab('hosp-profile');
        } catch (err) {
            this.showToast(err.message, 'error');
        }
    },

    async fulfillHospitalRequest(requestId) {
        try {
            await Api.bloodRequests.updateStatus(requestId, 'FULFILLED');
            this.showToast('Hospital blood request fulfilled! Thank you to all voluntary donors.', 'success');
            this.loadHospitalTab('hosp-requests');
            this.loadHospitalRequests();
            this.loadStatistics();
        } catch (e) {
            this.showToast(e.message, 'error');
        }
    },

    async hospitalSendDirectRequest(donorId, donorName, bloodGroup) {
        try {
            // Pick or create active request for this specific required blood group
            const reqs = await Api.bloodRequests.getMyRequests();
            let reqId = null;
            if (reqs && reqs.data && reqs.data.length > 0) {
                const active = reqs.data.find(r => r.status === 'ACTIVE' && r.bloodGroup === bloodGroup);
                if (active) reqId = active.id;
            }

            if (!reqId) {
                const created = await Api.bloodRequests.create({
                    hospitalName: this.state.user.hospitalName || this.state.user.name,
                    bloodGroup: bloodGroup,
                    requiredUnits: 1,
                    hospitalLocation: this.state.user.city || 'Chennai',
                    urgency: 'HIGH',
                    notes: `Direct donation request sent to voluntary donor ${donorName}`
                });
                reqId = created.data.id;
            }

            await Api.bloodRequests.sendToDonor(reqId, donorId, 'Hospital donation request');
            this.showToast(`Donation request sent directly to donor ${donorName}! 🩸`, 'success');
        } catch (e) {
            this.showToast(e.message, 'error');
        }
    },

    // ==========================================
    // 8. Dedicated Donor Dashboard
    // ==========================================
    async openDonorDashboard() {
        if (!this.state.user || (this.state.user.role !== 'DONOR' && this.state.user.role !== 'ADMIN')) {
            this.showToast('Access denied: Donor account required', 'error');
            return;
        }
        this.openModal('donor-dashboard-modal');
        this.loadDonorTab('donor-incoming');
    },

    async loadDonorTab(tabName) {
        document.querySelectorAll('.donor-tab-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.tab === tabName);
        });

        const content = document.getElementById('donor-tab-content');
        if (!content) return;

        content.innerHTML = `<p style="text-align: center; padding: 30px; color: var(--text-muted);">Loading donor data...</p>`;

        try {
            if (tabName === 'donor-incoming') {
                // 1. Fetch donor profile for name and blood group
                const donorRes = await Api.donors.getMyProfile().catch(() => null);
                const donor = donorRes ? donorRes.data : null;
                const donorName = (donor && donor.name) || (this.state.user && (this.state.user.hospitalName || this.state.user.name)) || 'Donor';
                const donorBloodGroup = (donor && donor.bloodGroup) || (this.state.user && this.state.user.bloodGroup) || 'O+';

                // 2. Fetch demanded blood groups from active requests
                const demandRes = await Api.bloodRequests.getHighlyDemanded().catch(() => ({ data: [] }));
                const demanded = (demandRes && demandRes.data) ? demandRes.data : [];

                // 3. Fetch matching incoming requests for this donor (compatible only)
                const reqRes = await Api.bloodRequests.getIncomingForDonor().catch(() => ({ data: [] }));
                const requests = (reqRes && reqRes.data) ? reqRes.data : [];

                content.innerHTML = `
                    <!-- Welcome Banner -->
                    <div style="background: rgba(15, 23, 42, 0.85); padding: 20px 24px; border-radius: var(--radius-md); border: 1px solid var(--border-glass); margin-bottom: 24px; display: flex; justify-content: space-between; align-items: center;">
                        <div>
                            <div style="font-size: 0.8rem; letter-spacing: 0.05em; color: var(--text-muted); font-weight: 700;">DONOR DASHBOARD</div>
                            <h2 style="color: #ffffff; margin-top: 4px;">Welcome, ${donorName}</h2>
                        </div>
                        <div style="text-align: right; background: rgba(244, 63, 94, 0.15); padding: 8px 18px; border-radius: var(--radius-sm); border: 1px solid rgba(244, 63, 94, 0.3);">
                            <span style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase;">Blood Group</span>
                            <div style="font-size: 1.6rem; font-weight: 800; color: #fb7185;">${donorBloodGroup}</div>
                        </div>
                    </div>

                    <!-- Section 1: HIGHLY DEMANDED BLOOD GROUPS -->
                    <div style="margin-bottom: 28px;">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; border-bottom: 1px solid var(--border-glass); padding-bottom: 8px;">
                            <h4 style="color: #ffffff; font-size: 1.1rem; display: flex; align-items: center; gap: 8px;">
                                🔥 HIGHLY DEMANDED BLOOD GROUPS
                            </h4>
                            <span style="font-size: 0.78rem; color: var(--text-muted);">From active hospital requests</span>
                        </div>
                        ${demanded.length === 0 ? `
                            <div style="padding: 20px; background: rgba(15, 23, 42, 0.5); border-radius: var(--radius-sm); text-align: center; color: var(--text-muted);">
                                No urgent blood demands recorded in the database.
                            </div>
                        ` : `
                            <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px;">
                                ${demanded.map(dg => `
                                    <div style="background: rgba(15, 23, 42, 0.7); padding: 14px 16px; border-radius: var(--radius-sm); border-left: 4px solid #f43f5e;">
                                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                                            <span style="font-size: 1.25rem; font-weight: 800; color: #fb7185;">🔴 ${dg.bloodGroup}</span>
                                            <span class="demand-badge ${(dg.urgencyLevel || 'HIGH').toLowerCase()}">${dg.urgencyLevel}</span>
                                        </div>
                                        <div style="font-size: 0.82rem; color: var(--text-muted);">
                                            <strong>${dg.unitsNeeded}</strong> Units (${dg.activeRequestCount} requests)
                                        </div>
                                    </div>
                                `).join('')}
                            </div>
                        `}
                    </div>

                    <!-- Section 2: MATCHING BLOOD REQUESTS -->
                    <div>
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; border-bottom: 1px solid var(--border-glass); padding-bottom: 8px;">
                            <h4 style="color: #ffffff; font-size: 1.1rem; display: flex; align-items: center; gap: 8px;">
                                🩸 MATCHING BLOOD REQUESTS
                            </h4>
                            <span style="font-size: 0.78rem; color: #34d399; font-weight: 600;">
                                Compatible with your blood group (${donorBloodGroup})
                            </span>
                        </div>
                        ${requests.length === 0 ? `
                            <div style="text-align: center; padding: 40px; background: rgba(15, 23, 42, 0.5); border-radius: var(--radius-sm);">
                                <div style="font-size: 2.5rem; margin-bottom: 10px;">✅</div>
                                <h4>No matching hospital requests right now</h4>
                                <p style="color: var(--text-muted); font-size: 0.9rem; margin-top: 6px;">
                                    When a hospital creates a request compatible with your <strong>${donorBloodGroup}</strong> blood group, it will be listed here.
                                </p>
                            </div>
                        ` : `
                            <div style="display: flex; flex-direction: column; gap: 16px;">
                                ${requests.map(r => `
                                    <div class="request-card ${(r.urgency || 'HIGH').toLowerCase() === 'critical' ? 'critical' : 'high'}" id="request-card-${r.id}">
                                        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px;">
                                            <div>
                                                <h4 style="color: #ffffff; font-size: 1.15rem; margin-bottom: 4px;">🏥 ${r.hospitalName}</h4>
                                                <span style="font-size: 0.82rem; color: var(--text-muted);">📍 ${r.hospitalLocation || 'Chennai'}</span>
                                            </div>
                                            <span class="demand-badge ${(r.urgency || 'HIGH').toLowerCase() === 'critical' ? 'critical' : 'low'}">${r.urgency} NEED</span>
                                        </div>

                                        <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; margin-bottom: 12px; background: rgba(15, 23, 42, 0.6); padding: 10px 14px; border-radius: var(--radius-sm);">
                                            <div>
                                                <span style="font-size: 0.75rem; color: var(--text-muted);">Required Blood Group:</span>
                                                <div style="font-size: 1.15rem; font-weight: 800; color: #fb7185;">${r.bloodGroup}</div>
                                            </div>
                                            <div>
                                                <span style="font-size: 0.75rem; color: var(--text-muted);">Units Required:</span>
                                                <div style="font-size: 1.15rem; font-weight: 800; color: #ffffff;">${r.requiredUnits} Units</div>
                                            </div>
                                            <div>
                                                <span style="font-size: 0.75rem; color: var(--text-muted);">Deadline:</span>
                                                <div style="font-size: 1rem; font-weight: 700; color: #fbbf24;">${r.requiredDate || 'Immediate'}</div>
                                            </div>
                                        </div>

                                        <p style="font-size: 0.86rem; color: var(--text-muted); margin-bottom: 14px; line-height: 1.4;">
                                            ${r.notes || 'Emergency requirement for hospitalized patient.'}
                                        </p>

                                        <div id="request-btn-box-${r.id}" style="display: flex; gap: 10px;">
                                            <button class="btn btn-primary btn-sm" style="flex: 1;" onclick="App.showScheduleForm(${r.id}, '${r.hospitalName}', '${r.bloodGroup}', ${r.requiredUnits}, '${r.requiredDate || ''}')">
                                                Accept Request
                                            </button>
                                            <button class="btn btn-secondary btn-sm" onclick="App.donorRespondRequest(${r.id}, 'REJECTED')">
                                                Decline
                                            </button>
                                        </div>

                                        <!-- Inline Schedule Donation Box -->
                                        <div id="schedule-box-${r.id}" style="display: none; margin-top: 14px; background: rgba(15, 23, 42, 0.95); padding: 18px; border-radius: var(--radius-sm); border: 1px solid var(--border-glass);">
                                            <h4 style="color: #34d399; font-size: 1rem; margin-bottom: 6px;">🗓 SCHEDULE DONATION</h4>
                                            <p style="font-size: 0.82rem; color: var(--text-muted); margin-bottom: 14px;">
                                                Selected donation date must be before or on request deadline: <strong style="color: #fbbf24;">${r.requiredDate || 'Immediate'}</strong>
                                            </p>
                                            <div class="form-row" style="margin-bottom: 12px;">
                                                <div class="form-group" style="margin-bottom: 0;">
                                                    <label class="form-label" style="font-size: 0.8rem;">Select Date</label>
                                                    <input type="date" id="sched-date-${r.id}" class="form-input" max="${r.requiredDate || ''}">
                                                </div>
                                                <div class="form-group" style="margin-bottom: 0;">
                                                    <label class="form-label" style="font-size: 0.8rem;">Select Time</label>
                                                    <input type="time" id="sched-time-${r.id}" class="form-input" value="10:00">
                                                </div>
                                            </div>
                                            <div id="sched-err-${r.id}" style="color: #f87171; font-size: 0.85rem; margin-bottom: 10px; display: none;"></div>
                                            <div style="display: flex; gap: 10px;">
                                                <button class="btn btn-primary btn-sm" style="flex: 1;" onclick="App.confirmScheduleDonation(${r.id}, '${r.requiredDate || ''}')">
                                                    Confirm Schedule
                                                </button>
                                                <button class="btn btn-secondary btn-sm" onclick="App.hideScheduleForm(${r.id})">
                                                    Cancel
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                `).join('')}
                            </div>
                        `}
                    </div>
                `;
            } else if (tabName === 'donor-status') {
                const donorRes = await Api.donors.getMyProfile().catch(() => null);
                const donor = donorRes ? donorRes.data : null;

                if (!donor) {
                    content.innerHTML = `<p style="text-align: center; color: var(--text-muted); padding: 20px;">Please complete your donor profile.</p>`;
                    return;
                }

                const eligRes = await Api.donors.checkEligibility(donor.id);
                const elig = eligRes ? eligRes.data : null;

                content.innerHTML = `
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px;">
                        <div style="background: rgba(15, 23, 42, 0.8); padding: 20px; border-radius: var(--radius-md); border: 1px solid var(--border-glass);">
                            <div style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 6px;">Current Availability</div>
                            <div style="display: flex; align-items: center; justify-content: space-between;">
                                <h3 style="color: ${donor.availabilityStatus ? '#34d399' : '#f87171'};">
                                    ${donor.availabilityStatus ? '● Available for Donation' : '○ Marked Unavailable'}
                                </h3>
                                <button class="btn btn-secondary btn-sm" onclick="App.toggleMyAvailability(${donor.id}, ${!donor.availabilityStatus})">
                                    ${donor.availabilityStatus ? 'Turn Off' : 'Turn On'}
                                </button>
                            </div>
                        </div>

                        <div style="background: rgba(15, 23, 42, 0.8); padding: 20px; border-radius: var(--radius-md); border: 1px solid var(--border-glass);">
                            <div style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 6px;">Eligibility Status</div>
                            <h3 style="color: ${(elig && elig.eligible) ? '#34d399' : '#fbbf24'};">
                                ${(elig && elig.eligible) ? '✓ Eligible to Donate' : '⏱ Interval Cool-down'}
                            </h3>
                            <p style="font-size: 0.82rem; color: var(--text-muted); margin-top: 4px;">
                                ${(elig && elig.reason) || '90-day whole blood interval rule applies'}
                            </p>
                        </div>
                    </div>

                    <div style="background: rgba(15, 23, 42, 0.6); padding: 20px; border-radius: var(--radius-md); border: 1px solid var(--border-glass);">
                        <h4 style="margin-bottom: 12px;">Donor Recognition & Badges</h4>
                        <div style="display: flex; align-items: center; justify-content: space-between;">
                            <div style="display: flex; align-items: center; gap: 14px;">
                                <div style="font-size: 2.5rem;">${donor.badgeIcon || '🌱'}</div>
                                <div>
                                    <h4 style="color: #ffffff;">${donor.badgeTitle || 'New Donor'}</h4>
                                    <span style="font-size: 0.85rem; color: var(--text-muted);">${donor.totalDonations || 0} verified donations completed</span>
                                </div>
                            </div>
                            <div style="text-align: right;">
                                <span style="font-size: 0.85rem; color: var(--text-muted);">Community Stars:</span>
                                <div style="font-size: 1.5rem; font-weight: 800; color: #fbbf24;">${donor.stars || 0} ★</div>
                            </div>
                        </div>
                    </div>
                `;
            } else if (tabName === 'donor-history') {
                const donorRes = await Api.donors.getMyProfile().catch(() => null);
                const donor = donorRes ? donorRes.data : null;

                if (!donor) {
                    content.innerHTML = `<p style="text-align: center; color: var(--text-muted); padding: 20px;">No donation records found.</p>`;
                    return;
                }

                const donRes = await Api.donations.getByDonor(donor.id);
                const list = donRes ? donRes.data : [];

                if (list.length === 0) {
                    content.innerHTML = `<p style="text-align: center; color: var(--text-muted); padding: 30px;">No donation history recorded yet. Accept incoming hospital requests to build your life-saving record!</p>`;
                } else {
                    content.innerHTML = `
                        <div class="data-table-wrapper">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Date</th>
                                        <th>Hospital / Center</th>
                                        <th>Units</th>
                                        <th>Status</th>
                                        <th>Notes</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${list.map(d => `
                                        <tr>
                                            <td>${d.donationDate}</td>
                                            <td><strong>${d.hospitalName || 'LifeDrop Center'}</strong></td>
                                            <td>${d.units} Unit</td>
                                            <td><span class="role-tag ${d.status === 'COMPLETED' ? 'donor' : 'hospital'}">${d.status}</span></td>
                                            <td>${d.notes || 'Voluntary blood donation'}</td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                        </div>
                    `;
                }
            } else if (tabName === 'donor-profile') {
                const donorRes = await Api.donors.getMyProfile().catch(() => null);
                const donor = donorRes ? donorRes.data : null;

                content.innerHTML = `
                    <form onsubmit="App.handleSaveDonorProfile(event)">
                        <div class="form-row">
                            <div class="form-group">
                                <label class="form-label">Blood Group</label>
                                <select id="dash-prof-bg" class="form-select">
                                    <option value="O+" ${donor && donor.bloodGroup === 'O+' ? 'selected' : ''}>O+</option>
                                    <option value="O-" ${donor && donor.bloodGroup === 'O-' ? 'selected' : ''}>O-</option>
                                    <option value="A+" ${donor && donor.bloodGroup === 'A+' ? 'selected' : ''}>A+</option>
                                    <option value="A-" ${donor && donor.bloodGroup === 'A-' ? 'selected' : ''}>A-</option>
                                    <option value="B+" ${donor && donor.bloodGroup === 'B+' ? 'selected' : ''}>B+</option>
                                    <option value="B-" ${donor && donor.bloodGroup === 'B-' ? 'selected' : ''}>B-</option>
                                    <option value="AB+" ${donor && donor.bloodGroup === 'AB+' ? 'selected' : ''}>AB+</option>
                                    <option value="AB-" ${donor && donor.bloodGroup === 'AB-' ? 'selected' : ''}>AB-</option>
                                </select>
                            </div>
                            <div class="form-group">
                                <label class="form-label">Age (18-65)</label>
                                <input type="number" id="dash-prof-age" class="form-input" min="18" max="65" value="${(donor && donor.age) || 26}" required>
                            </div>
                        </div>
                        <div class="form-row">
                            <div class="form-group">
                                <label class="form-label">Gender</label>
                                <select id="dash-prof-gender" class="form-select">
                                    <option value="Male">Male</option>
                                    <option value="Female">Female</option>
                                    <option value="Other">Other</option>
                                </select>
                            </div>
                            <div class="form-group">
                                <label class="form-label">City</label>
                                <input type="text" id="dash-prof-city" class="form-input" value="${(donor && donor.city) || 'Chennai'}" required>
                            </div>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Address</label>
                            <input type="text" id="dash-prof-address" class="form-input" value="${(donor && donor.address) || '12 Anna Salai, Chennai'}">
                        </div>
                        <button type="submit" class="btn btn-primary" style="width: 100%;">
                            Update Donor Profile
                        </button>
                    </form>
                `;
            }
        } catch (e) {
            content.innerHTML = `<p style="color: var(--primary);">Error: ${e.message}</p>`;
        }
    },

    showScheduleForm(requestId, hospitalName, bloodGroup, units, requiredDate) {
        const box = document.getElementById(`schedule-box-${requestId}`);
        const btnBox = document.getElementById(`request-btn-box-${requestId}`);
        const dateInput = document.getElementById(`sched-date-${requestId}`);
        
        if (box) box.style.display = 'block';
        if (btnBox) btnBox.style.display = 'none';
        
        if (dateInput && !dateInput.value) {
            const today = new Date().toISOString().split('T')[0];
            dateInput.value = today;
        }
    },

    hideScheduleForm(requestId) {
        const box = document.getElementById(`schedule-box-${requestId}`);
        const btnBox = document.getElementById(`request-btn-box-${requestId}`);
        const errBox = document.getElementById(`sched-err-${requestId}`);
        
        if (box) box.style.display = 'none';
        if (btnBox) btnBox.style.display = 'flex';
        if (errBox) errBox.style.display = 'none';
    },

    async confirmScheduleDonation(requestId, requiredDate) {
        const dateInput = document.getElementById(`sched-date-${requestId}`);
        const timeInput = document.getElementById(`sched-time-${requestId}`);
        const errBox = document.getElementById(`sched-err-${requestId}`);
        
        const selectedDate = dateInput ? dateInput.value : '';
        const selectedTime = timeInput ? timeInput.value : '';
        
        if (!selectedDate) {
            if (errBox) {
                errBox.innerText = 'Please select a donation date.';
                errBox.style.display = 'block';
            }
            return;
        }
        
        // Client-side validation: must be before or on deadline
        if (requiredDate && selectedDate > requiredDate) {
            const msg = 'Donation schedule must be before the request deadline.';
            if (errBox) {
                errBox.innerText = msg;
                errBox.style.display = 'block';
            }
            this.showToast(msg, 'error');
            return;
        }
        
        try {
            await Api.bloodRequests.respondToRequest(
                requestId,
                'ACCEPTED',
                selectedDate,
                selectedTime,
                'Voluntary donation scheduled via LifeDrop Donor Dashboard'
            );
            this.showToast('Donation scheduled successfully! Thank you for saving a life. 🩸', 'success');
            this.loadDonorTab('donor-incoming');
        } catch (err) {
            if (errBox) {
                errBox.innerText = err.message;
                errBox.style.display = 'block';
            }
            this.showToast(err.message, 'error');
        }
    },

    async donorRespondRequest(requestId, status) {
        try {
            await Api.bloodRequests.respondToRequest(requestId, status, null, null, 'Donor response from dashboard');
            if (status === 'ACCEPTED') {
                this.showToast('Hospital blood request ACCEPTED! Donation is now scheduled.', 'success');
            } else {
                this.showToast('Hospital blood request declined.', 'info');
            }
            this.loadDonorTab('donor-incoming');
        } catch (e) {
            this.showToast(e.message, 'error');
        }
    },

    async toggleMyAvailability(donorId, available) {
        try {
            await Api.donors.updateAvailability(donorId, available);
            this.showToast(`Availability updated to ${available ? 'AVAILABLE' : 'UNAVAILABLE'}`, 'success');
            this.loadDonorTab('donor-status');
            this.loadDonors();
        } catch (e) {
            this.showToast(e.message, 'error');
        }
    },

    async handleSaveDonorProfile(e) {
        e.preventDefault();
        const data = {
            bloodGroup: document.getElementById('dash-prof-bg').value,
            age: parseInt(document.getElementById('dash-prof-age').value),
            gender: document.getElementById('dash-prof-gender').value,
            city: document.getElementById('dash-prof-city').value.trim(),
            address: document.getElementById('dash-prof-address').value.trim(),
            availabilityStatus: true
        };

        try {
            await Api.donors.createOrUpdateProfile(data);
            this.showToast('Donor profile updated successfully', 'success');
            this.loadDonorTab('donor-profile');
            this.loadDonors();
        } catch (err) {
            this.showToast(err.message, 'error');
        }
    },

    // ==========================================
    // 9. Eligibility Inspector Modal
    // ==========================================
    async checkDonorEligibility(donorId) {
        try {
            const res = await Api.donors.checkEligibility(donorId);
            if (res && res.success && res.data) {
                const data = res.data;
                const modalBody = document.getElementById('eligibility-modal-body');
                if (modalBody) {
                    modalBody.innerHTML = `
                        <div style="text-align: center; margin-bottom: 20px;">
                            <div style="font-size: 3rem; margin-bottom: 10px;">
                                ${data.eligible ? '✅' : '⏳'}
                            </div>
                            <h3 style="color: ${data.eligible ? '#34d399' : '#fbbf24'}; margin-bottom: 8px;">
                                ${data.eligible ? 'Eligible to Donate Blood' : 'Currently Ineligible'}
                            </h3>
                            <p style="color: var(--text-muted); font-size: 0.95rem;">${data.reason}</p>
                        </div>

                        <div style="background: rgba(15, 23, 42, 0.6); padding: 18px; border-radius: var(--radius-sm); border: 1px solid var(--border-glass);">
                            <div style="display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 0.9rem;">
                                <span style="color: var(--text-muted);">Minimum Interval Rule:</span>
                                <strong>90 days between donations</strong>
                            </div>
                            <div style="display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 0.9rem;">
                                <span style="color: var(--text-muted);">Eligible Age Range:</span>
                                <strong>18 – 65 years</strong>
                            </div>
                            ${data.nextEligibleDate ? `
                                <div style="display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 0.9rem;">
                                    <span style="color: var(--text-muted);">Next Eligible Date:</span>
                                    <strong style="color: #fbbf24;">${data.nextEligibleDate}</strong>
                                </div>
                                <div style="display: flex; justify-content: space-between; font-size: 0.9rem;">
                                    <span style="color: var(--text-muted);">Days Remaining:</span>
                                    <strong style="color: #fbbf24;">${data.daysRemaining} days</strong>
                                </div>
                            ` : ''}
                        </div>
                        
                        <div style="margin-top: 20px; font-size: 0.8rem; color: var(--text-muted); text-align: center;">
                            * Note: Eligibility calculations are project rules implemented for this platform.
                        </div>
                    `;
                }
                this.openModal('eligibility-modal');
            }
        } catch (e) {
            this.showToast('Failed to check eligibility: ' + e.message, 'error');
        }
    },

    // ==========================================
    // 10. Referral Activity Logic
    // ==========================================
    copyReferralCode() {
        const code = (this.state.user && this.state.user.referralCode) || 'PRAGA123';
        navigator.clipboard.writeText(code).then(() => {
            this.showToast(`Referral code ${code} copied to clipboard! 🌟`, 'success');
        }).catch(() => {
            this.showToast(`Referral Code: ${code}`, 'info');
        });
    },

    async viewMyReferrals() {
        if (!this.state.user) {
            this.openAuthModal('login');
            return;
        }

        try {
            const res = await Api.referrals.getMyReferrals();
            if (res && res.success && res.data) {
                const list = res.data;
                const container = document.getElementById('my-referrals-container');
                if (container) {
                    if (list.length === 0) {
                        container.innerHTML = `<p style="text-align: center; color: var(--text-muted); padding: 20px;">No referral signups yet. Share your code to earn +10 stars when referred donors complete verified donations!</p>`;
                    } else {
                        container.innerHTML = `
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Code</th>
                                        <th>Referred Donor</th>
                                        <th>Status</th>
                                        <th>Stars</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${list.map(r => `
                                        <tr>
                                            <td><code>${r.referralCode}</code></td>
                                            <td>${r.referredUserName || 'Pending Registration'}</td>
                                            <td><span class="role-tag ${r.status === 'COMPLETED' ? 'donor' : 'hospital'}">${r.status}</span></td>
                                            <td style="color: #fbbf24; font-weight: 700;">+${r.starsEarned} ★</td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                        `;
                    }
                }
                this.openModal('referrals-modal');
            }
        } catch (e) {
            this.showToast('Failed to load referrals: ' + e.message, 'error');
        }
    },

    // ==========================================
    // 11. Admin Dashboard
    // ==========================================
    async openAdminModal() {
        if (!this.state.user || this.state.user.role !== 'ADMIN') {
            this.showToast('Access denied: Admin permissions required', 'error');
            return;
        }
        this.openModal('admin-modal');
        this.loadAdminTab('stats');
    },

    async loadAdminTab(tabName) {
        document.querySelectorAll('.admin-tab-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.tab === tabName);
        });

        const content = document.getElementById('admin-tab-content');
        if (!content) return;

        content.innerHTML = `<p style="text-align: center; padding: 30px; color: var(--text-muted);">Loading admin data...</p>`;

        try {
            if (tabName === 'stats') {
                const res = await Api.admin.getDashboardStats();
                if (res && res.data) {
                    const s = res.data;
                    content.innerHTML = `
                        <div class="stats-grid" style="margin-bottom: 20px;">
                            <div class="stat-card"><div class="stat-info"><div class="stat-value">${s.totalUsers}</div><div class="stat-label">Total Users</div></div></div>
                            <div class="stat-card"><div class="stat-info"><div class="stat-value">${s.totalDonors}</div><div class="stat-label">Registered Donors</div></div></div>
                            <div class="stat-card"><div class="stat-info"><div class="stat-value">${s.totalHospitals}</div><div class="stat-label">Registered Hospitals</div></div></div>
                            <div class="stat-card"><div class="stat-info"><div class="stat-value">${s.totalDonations}</div><div class="stat-label">Total Donations</div></div></div>
                            <div class="stat-card"><div class="stat-info"><div class="stat-value">${s.successfulDonations}</div><div class="stat-label">Successful Donations</div></div></div>
                            <div class="stat-card"><div class="stat-info"><div class="stat-value">${s.activeRequests}</div><div class="stat-label">Active Requests</div></div></div>
                            <div class="stat-card"><div class="stat-info"><div class="stat-value">${s.totalInventoryUnits}</div><div class="stat-label">Total Blood Units</div></div></div>
                            <div class="stat-card"><div class="stat-info"><div class="stat-value">${s.referralRewards} ★</div><div class="stat-label">Stars Distributed</div></div></div>
                        </div>
                    `;
                }
            } else if (tabName === 'donors') {
                const res = await Api.admin.getAllDonors();
                if (res && res.data) {
                    content.innerHTML = `
                        <div class="data-table-wrapper">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Name</th>
                                        <th>Blood Group</th>
                                        <th>City</th>
                                        <th>Donations</th>
                                        <th>Badge</th>
                                        <th>Available</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${res.data.map(d => `
                                        <tr>
                                            <td><strong>${d.name}</strong></td>
                                            <td><span class="role-tag donor">${d.bloodGroup}</span></td>
                                            <td>${d.city}</td>
                                            <td>${d.totalDonations || 0}</td>
                                            <td>${d.badgeIcon || ''} ${d.badgeTitle || ''}</td>
                                            <td>${d.availabilityStatus ? '✓ Available' : '✗ Unavailable'}</td>
                                            <td>
                                                <button class="btn btn-secondary btn-sm" onclick="App.adminToggleDonor(${d.id}, ${!d.availabilityStatus})">
                                                    ${d.availabilityStatus ? 'Deactivate' : 'Activate'}
                                                </button>
                                            </td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                        </div>
                    `;
                }
            } else if (tabName === 'hospitals') {
                const res = await Api.admin.getAllHospitals();
                if (res && res.data) {
                    content.innerHTML = `
                        <div class="data-table-wrapper">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Hospital Name</th>
                                        <th>License Number</th>
                                        <th>City</th>
                                        <th>Emergency Helpline</th>
                                        <th>Contact Person</th>
                                        <th>Status</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${res.data.map(h => `
                                        <tr>
                                            <td><strong>🏥 ${h.hospitalName}</strong></td>
                                            <td><code>${h.licenseNumber || 'N/A'}</code></td>
                                            <td>${h.city}</td>
                                            <td>${h.emergencyHelpline || h.phone}</td>
                                            <td>${h.contactPerson || h.name}</td>
                                            <td><span class="role-tag donor">VERIFIED</span></td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                        </div>
                    `;
                }
            } else if (tabName === 'inventory') {
                const res = await Api.inventory.getAll();
                if (res && res.data) {
                    content.innerHTML = `
                        <div class="data-table-wrapper">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Blood Group</th>
                                        <th>Available Units</th>
                                        <th>Minimum Target</th>
                                        <th>Status</th>
                                        <th>Update Units</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${res.data.map(i => `
                                        <tr>
                                            <td><strong>${i.bloodGroup}</strong></td>
                                            <td><input type="number" id="inv-units-${i.bloodGroup.replace('+', 'P').replace('-', 'N')}" value="${i.availableUnits}" style="width: 70px; padding: 4px;" class="form-input"></td>
                                            <td>${i.minimumRequiredUnits}</td>
                                            <td><span class="demand-badge ${i.status.toLowerCase()}">${i.demandBadge}</span></td>
                                            <td>
                                                <button class="btn btn-primary btn-sm" onclick="App.adminUpdateInventory('${i.bloodGroup}')">
                                                    Save
                                                </button>
                                            </td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                        </div>
                    `;
                }
            } else if (tabName === 'requests') {
                const res = await Api.admin.getAllRequests();
                if (res && res.data) {
                    content.innerHTML = `
                        <div class="data-table-wrapper">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Hospital</th>
                                        <th>Blood Group</th>
                                        <th>Units</th>
                                        <th>Location</th>
                                        <th>Urgency</th>
                                        <th>Status</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${res.data.map(r => `
                                        <tr>
                                            <td><strong>🏥 ${r.hospitalName}</strong></td>
                                            <td><span class="role-tag donor">${r.bloodGroup}</span></td>
                                            <td>${r.requiredUnits}</td>
                                            <td>${r.hospitalLocation}</td>
                                            <td><span class="demand-badge ${r.urgency === 'CRITICAL' ? 'critical' : 'low'}">${r.urgency}</span></td>
                                            <td>${r.status}</td>
                                            <td>
                                                ${r.status === 'ACTIVE' ? `
                                                    <button class="btn btn-primary btn-sm" onclick="App.adminFulfillRequest(${r.id})">Fulfill</button>
                                                ` : 'Resolved'}
                                            </td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                        </div>
                    `;
                }
            } else if (tabName === 'donations') {
                const res = await Api.admin.getAllDonations();
                if (res && res.data) {
                    content.innerHTML = `
                        <div class="data-table-wrapper">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Donor</th>
                                        <th>Blood Group</th>
                                        <th>Date</th>
                                        <th>Units</th>
                                        <th>Status</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${res.data.map(d => `
                                        <tr>
                                            <td><strong>${d.donorName}</strong></td>
                                            <td>${d.bloodGroup}</td>
                                            <td>${d.donationDate}</td>
                                            <td>${d.units}</td>
                                            <td><span class="role-tag ${d.status === 'COMPLETED' ? 'donor' : 'hospital'}">${d.status}</span></td>
                                            <td>
                                                ${d.status !== 'COMPLETED' ? `
                                                    <button class="btn btn-primary btn-sm" onclick="App.adminVerifyDonation(${d.id})">Verify</button>
                                                ` : 'Verified ✓'}
                                            </td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                        </div>
                    `;
                }
            } else if (tabName === 'referrals') {
                const res = await Api.admin.getAllReferrals();
                if (res && res.data) {
                    content.innerHTML = `
                        <div class="data-table-wrapper">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Code</th>
                                        <th>Referrer</th>
                                        <th>Referred User</th>
                                        <th>Status</th>
                                        <th>Stars</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${res.data.map(r => `
                                        <tr>
                                            <td><code>${r.referralCode}</code></td>
                                            <td><strong>${r.referrerName}</strong></td>
                                            <td>${r.referredUserName || 'Pending Registration'}</td>
                                            <td><span class="role-tag ${r.status === 'COMPLETED' ? 'donor' : 'hospital'}">${r.status}</span></td>
                                            <td style="color: #fbbf24; font-weight: 700;">+${r.starsEarned} ★</td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                        </div>
                    `;
                }
            }
        } catch (e) {
            content.innerHTML = `<p style="color: var(--primary); text-align: center;">Error: ${e.message}</p>`;
        }
    },

    async adminToggleDonor(id, available) {
        try {
            await Api.admin.toggleDonorAvailability(id, available);
            this.showToast('Donor availability updated', 'success');
            this.loadAdminTab('donors');
            this.loadDonors();
        } catch (e) {
            this.showToast(e.message, 'error');
        }
    },

    async adminUpdateInventory(bloodGroup) {
        const inputId = `inv-units-${bloodGroup.replace('+', 'P').replace('-', 'N')}`;
        const input = document.getElementById(inputId);
        if (!input) return;

        try {
            await Api.admin.updateInventory(bloodGroup, { availableUnits: parseInt(input.value) });
            this.showToast(`Inventory updated for ${bloodGroup}`, 'success');
            this.loadInventory();
            this.loadAdminTab('inventory');
        } catch (e) {
            this.showToast(e.message, 'error');
        }
    },

    async adminFulfillRequest(id) {
        try {
            await Api.admin.updateRequestStatus(id, 'FULFILLED');
            this.showToast('Request marked as FULFILLED', 'success');
            this.loadAdminTab('requests');
            this.loadStatistics();
            this.loadHospitalRequests();
        } catch (e) {
            this.showToast(e.message, 'error');
        }
    },

    async adminVerifyDonation(id) {
        try {
            await Api.admin.verifyDonation(id);
            this.showToast('Donation verified! Stock updated & referral rewarded if applicable.', 'success');
            this.loadAdminTab('donations');
            this.loadStatistics();
            this.loadInventory();
        } catch (e) {
            this.showToast(e.message, 'error');
        }
    },

    // ==========================================
    // 12. Modals Helper & Events
    // ==========================================
    openModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.add('active');
            document.body.style.overflow = 'hidden';
        }
    },

    closeModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.remove('active');
            document.body.style.overflow = 'auto';
        }
    },

    openAuthModal(tab = 'login') {
        this.switchAuthTab(tab);
        this.openModal('auth-modal');
    },

    switchAuthTab(tab) {
        document.querySelectorAll('#auth-modal .modal-tab').forEach(t => {
            t.classList.toggle('active', t.dataset.tab === tab);
        });
        document.getElementById('login-form-container').style.display = tab === 'login' ? 'block' : 'none';
        document.getElementById('register-form-container').style.display = tab === 'register' ? 'block' : 'none';
    },

    openFindDonorModal() {
        if (this.state.user && this.state.user.role === 'DONOR') {
            this.showToast('Find a Donor is restricted to Hospital accounts.', 'error');
            return;
        }
        this.openModal('find-donor-modal');
        this.findCompatibleDonors();
    },

    openBecomeDonorModal() {
        if (!this.state.user) {
            this.openAuthModal('register');
            return;
        }
        if (this.state.user.role === 'DONOR') {
            this.openDonorDashboard();
        } else {
            this.showToast('You are logged in as ' + this.state.user.role, 'info');
        }
    },

    openBloodRequestModal() {
        if (!this.state.user) {
            this.openAuthModal('login');
            return;
        }
        if (this.state.user.role === 'DONOR') {
            this.showToast('Blood requests can only be created by Hospitals.', 'error');
            return;
        }
        if (this.state.user.role === 'HOSPITAL' || this.state.user.role === 'ADMIN') {
            const hospNameInput = document.getElementById('req-hospital-name');
            if (hospNameInput && this.state.user.hospitalName) {
                hospNameInput.value = this.state.user.hospitalName;
            }
            this.openModal('blood-request-modal');
        } else {
            this.showToast('Blood requests can be created by Hospitals. Log in with a Hospital account.', 'info');
        }
    },

    openRequestForBlood(bloodGroup) {
        const select = document.getElementById('req-blood-group');
        if (select) select.value = bloodGroup;
        this.openBloodRequestModal();
    },

    openRequestDirectToDonor(donorId, donorName, bloodGroup) {
        if (this.state.user && (this.state.user.role === 'HOSPITAL' || this.state.user.role === 'ADMIN')) {
            this.hospitalSendDirectRequest(donorId, donorName, bloodGroup);
        } else {
            this.findCompatibleForRequest(bloodGroup);
        }
    },

    async respondToHospitalRequest(requestId, hospitalName, bloodGroup) {
        if (!this.state.user) {
            this.openAuthModal('login');
            return;
        }
        if (this.state.user.role === 'DONOR') {
            try {
                const donorRes = await Api.donors.getMyProfile().catch(() => null);
                if (donorRes && donorRes.data && donorRes.data.bloodGroup) {
                    const donorBg = donorRes.data.bloodGroup;
                    if (!BloodCompatibility.canDonateTo(donorBg, bloodGroup)) {
                        this.showToast(`Incompatible blood group: Your blood group (${donorBg}) cannot donate to requested blood group (${bloodGroup})`, 'error');
                        return;
                    }
                }
            } catch (err) {
                // Backend will enforce validation if profile lookup fails
            }
            this.donorRespondRequest(requestId, 'ACCEPTED');
        } else {
            this.showToast('Log in with a Donor account to accept hospital blood requests', 'info');
        }
    },

    showToast(message, type = 'info') {
        const container = document.getElementById('toast-container');
        if (!container) return;

        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        const icon = type === 'success' ? '✓' : (type === 'error' ? '✕' : 'ℹ');
        toast.innerHTML = `<span>${icon}</span><span>${message}</span>`;
        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(10px)';
            setTimeout(() => toast.remove(), 300);
        }, 3500);
    },

    setupEventListeners() {
        // Search inputs
        const searchInput = document.getElementById('donor-search-input');
        const cityInput = document.getElementById('donor-city-input');
        if (searchInput) searchInput.addEventListener('input', () => this.debounce(() => this.loadDonors(), 300)());
        if (cityInput) cityInput.addEventListener('input', () => this.debounce(() => this.loadDonors(), 300)());

        // Checkbox filters
        const availableOnly = document.getElementById('filter-available-only');
        const eligibleOnly = document.getElementById('filter-eligible-only');
        if (availableOnly) availableOnly.addEventListener('change', () => this.loadDonors());
        if (eligibleOnly) eligibleOnly.addEventListener('change', () => this.loadDonors());

        // Login Form Submission
        const loginForm = document.getElementById('login-form');
        const loginSubmitBtn = document.getElementById('login-submit-btn');
        if (loginForm) {
            loginForm.addEventListener('submit', async (e) => {
                e.preventDefault();
                if (this._isLoggingIn) return;

                const email = document.getElementById('login-email').value.trim();
                const password = document.getElementById('login-password').value;

                if (!email || !password) {
                    this.showToast('Please enter both email and password', 'error');
                    return;
                }

                this._isLoggingIn = true;
                const submitBtn = loginSubmitBtn || loginForm.querySelector('button[type="submit"]');
                const origBtnHtml = submitBtn ? submitBtn.innerHTML : '';
                if (submitBtn) {
                    submitBtn.disabled = true;
                    submitBtn.innerHTML = '<span class="btn-spinner"></span> Authenticating...';
                }

                try {
                    const res = await Api.auth.login(email, password);
                    if (res && res.success && res.data) {
                        Api.setToken(res.data.token);
                        Api.setUser(res.data);
                        this.state.user = res.data;
                        this.renderAuthUI();
                        this.closeModal('auth-modal');
                        this.showToast(`Welcome back, ${res.data.hospitalName || res.data.name}!`, 'success');
                    } else {
                        throw new Error((res && res.message) ? res.message : 'Login failed');
                    }
                } catch (err) {
                    const userMsg = err.message || 'Login failed. Please check your credentials and try again.';
                    this.showToast(userMsg, 'error');
                } finally {
                    this._isLoggingIn = false;
                    if (submitBtn) {
                        submitBtn.disabled = false;
                        submitBtn.innerHTML = origBtnHtml;
                    }
                }
            });
        }

        // Register Form Submission
        const regForm = document.getElementById('register-form');
        const regSubmitBtn = document.getElementById('register-submit-btn');
        if (regForm) {
            regForm.addEventListener('submit', async (e) => {
                e.preventDefault();
                if (this._isRegistering) return;

                const role = document.getElementById('reg-role').value;
                const formData = {
                    role: role,
                    name: document.getElementById('reg-name').value.trim(),
                    email: document.getElementById('reg-email').value.trim(),
                    password: document.getElementById('reg-password').value,
                    phone: document.getElementById('reg-phone').value.trim(),
                    city: document.getElementById('reg-city').value.trim() || 'Chennai',
                    address: document.getElementById('reg-address').value.trim() || ''
                };

                if (!formData.name || !formData.email || !formData.password || !formData.phone) {
                    this.showToast('Please fill in all required fields', 'error');
                    return;
                }

                if (formData.password.length < 6) {
                    this.showToast('Password must be at least 6 characters long', 'error');
                    return;
                }

                if (role === 'DONOR') {
                    formData.bloodGroup = document.getElementById('reg-blood-group').value;
                    formData.age = parseInt(document.getElementById('reg-age').value) || 25;
                    formData.referralCode = document.getElementById('reg-referral-code').value.trim() || null;
                } else if (role === 'HOSPITAL') {
                    formData.hospitalName = formData.name;
                    formData.licenseNumber = document.getElementById('reg-license').value.trim() || '';
                    formData.emergencyHelpline = document.getElementById('reg-helpline').value.trim() || formData.phone;
                }

                this._isRegistering = true;
                const submitBtn = regSubmitBtn || regForm.querySelector('button[type="submit"]');
                const origBtnHtml = submitBtn ? submitBtn.innerHTML : '';
                if (submitBtn) {
                    submitBtn.disabled = true;
                    submitBtn.innerHTML = '<span class="btn-spinner"></span> Creating Account...';
                }

                try {
                    const res = await Api.auth.register(formData);
                    if (res && res.success && res.data) {
                        Api.setToken(res.data.token);
                        Api.setUser(res.data);
                        this.state.user = res.data;
                        this.renderAuthUI();
                        this.closeModal('auth-modal');
                        this.showToast(`Registration successful! Welcome to LifeDrop.`, 'success');
                        setTimeout(() => {
                            this.loadStatistics();
                            this.loadDonors();
                        }, 50);
                    } else {
                        throw new Error((res && res.message) ? res.message : 'Registration failed');
                    }
                } catch (err) {
                    const userMsg = err.message || 'Registration failed. Please check your information and try again.';
                    this.showToast(userMsg, 'error');
                } finally {
                    this._isRegistering = false;
                    if (submitBtn) {
                        submitBtn.disabled = false;
                        submitBtn.innerHTML = origBtnHtml;
                    }
                }
            });
        }

        // Blood Request Form Submission
        const reqForm = document.getElementById('blood-request-form');
        if (reqForm) {
            reqForm.addEventListener('submit', async (e) => {
                e.preventDefault();
                const data = {
                    hospitalName: document.getElementById('req-hospital-name').value.trim(),
                    bloodGroup: document.getElementById('req-blood-group').value,
                    requiredUnits: parseInt(document.getElementById('req-units').value),
                    urgency: document.getElementById('req-urgency').value,
                    hospitalLocation: document.getElementById('req-hospital-location').value.trim(),
                    requiredDate: document.getElementById('req-date').value || null,
                    contactPhone: document.getElementById('req-phone').value.trim() || '',
                    notes: document.getElementById('req-notes').value.trim()
                };

                try {
                    await Api.bloodRequests.create(data);
                    this.showToast('Hospital blood request posted! Matching compatible donors notified.', 'success');
                    this.closeModal('blood-request-modal');
                    this.loadStatistics();
                    this.loadHospitalRequests();
                } catch (err) {
                    this.showToast(err.message, 'error');
                }
            });
        }
    },

    debounce(func, wait) {
        let timeout;
        return function(...args) {
            clearTimeout(timeout);
            timeout = setTimeout(() => func.apply(this, args), wait);
        };
    }
};

window.App = App;
