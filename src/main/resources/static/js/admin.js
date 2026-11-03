/**
 * LifeDrop - Professional Admin Panel Controller
 * Interacts with Spring Boot Admin REST APIs, validates JWT & ADMIN role,
 * controls Dashboard, Donors, Blood Requests, and Blood Stock Management.
 */

const AdminApp = {
    state: {
        user: null,
        token: null,
        currentView: 'dashboard',
        stats: null,
        donors: [],
        requests: [],
        inventory: [],
        requestFilter: 'all',
        donorSearchQuery: '',
        donorBloodGroupFilter: '',
        donorStatusFilter: ''
    },

    // Initialization
    async init() {
        this.state.token = Api.getToken();
        this.state.user = Api.getUser();

        // Check authentication & ADMIN role
        if (!this.state.token || !this.state.user) {
            this.showLoginScreen();
            return;
        }

        if (this.state.user.role !== 'ADMIN') {
            this.showAccessDeniedScreen(this.state.user.role);
            return;
        }

        // Authenticated as ADMIN: Show main panel
        this.showAdminPanel();
        this.updateAdminProfileUI();
        await this.loadView('dashboard');
    },

    // Screen display state management
    showLoginScreen() {
        document.getElementById('admin-login-screen').style.display = 'flex';
        document.getElementById('admin-layout').style.display = 'none';
        document.getElementById('access-denied-message').style.display = 'none';
    },

    showAccessDeniedScreen(currentRole) {
        document.getElementById('admin-login-screen').style.display = 'flex';
        document.getElementById('admin-layout').style.display = 'none';
        const alertBox = document.getElementById('login-alert-box');
        alertBox.className = 'login-alert error';
        alertBox.innerHTML = `
            <strong>🚫 Access Denied:</strong> Your account role is <code>${escapeHtml(currentRole)}</code>.
            Only accounts with <code>ADMIN</code> privileges are authorized to enter this portal.
            <div style="margin-top: 8px;">
                <button type="button" class="btn btn-secondary btn-sm" onclick="AdminApp.handleLogout()">Switch Account</button>
                <a href="index.html" class="btn btn-primary btn-sm" style="margin-left: 6px;">Return to Site</a>
            </div>
        `;
        alertBox.style.display = 'block';
    },

    showAdminPanel() {
        document.getElementById('admin-login-screen').style.display = 'none';
        document.getElementById('admin-layout').style.display = 'flex';
    },

    updateAdminProfileUI() {
        if (!this.state.user) return;
        const name = this.state.user.name || 'Administrator';
        const initials = name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase() || 'AD';
        
        const avatarEl = document.getElementById('admin-user-avatar');
        if (avatarEl) avatarEl.textContent = initials;
        
        const nameEl = document.getElementById('admin-user-name');
        if (nameEl) nameEl.textContent = name;
        
        const roleEl = document.getElementById('admin-user-role');
        if (roleEl) roleEl.textContent = this.state.user.role || 'ADMIN';
    },

    // Navigation between views
    async loadView(viewName) {
        this.state.currentView = viewName;

        // Update sidebar active classes
        document.querySelectorAll('.sidebar-nav-item').forEach(item => {
            if (item.dataset.view === viewName) {
                item.classList.add('active');
            } else {
                item.classList.remove('active');
            }
        });

        // Hide all views, show selected
        document.querySelectorAll('.admin-view').forEach(view => view.classList.remove('active'));
        const targetView = document.getElementById(`view-${viewName}`);
        if (targetView) targetView.classList.add('active');

        // Update Topbar Title
        const titleEl = document.getElementById('topbar-page-title');
        const descEl = document.getElementById('topbar-page-desc');
        switch (viewName) {
            case 'dashboard':
                if (titleEl) titleEl.textContent = 'Dashboard Overview';
                if (descEl) descEl.textContent = 'Real-time database analytics, donor activity, and inventory thresholds.';
                await this.refreshDashboard();
                break;
            case 'donors':
                if (titleEl) titleEl.textContent = 'Donor Management';
                if (descEl) descEl.textContent = 'View, search, inspect, and toggle availability for registered voluntary donors.';
                await this.refreshDonors();
                break;
            case 'requests':
                if (titleEl) titleEl.textContent = 'Blood Requests';
                if (descEl) descEl.textContent = 'Hospital and patient blood requirements. Review, approve, reject, or fulfill.';
                await this.refreshRequests();
                break;
            case 'stock':
                if (titleEl) titleEl.textContent = 'Blood Stock Management';
                if (descEl) descEl.textContent = 'Inspect and update available units across all 8 ABO/Rh blood groups.';
                await this.refreshStock();
                break;
        }

        // Close mobile sidebar if open
        this.toggleSidebar(false);
    },

    // Toggle Mobile Sidebar
    toggleSidebar(forceState = null) {
        const sidebar = document.getElementById('admin-sidebar');
        if (!sidebar) return;
        if (forceState !== null) {
            if (forceState) sidebar.classList.add('open');
            else sidebar.classList.remove('open');
        } else {
            sidebar.classList.toggle('open');
        }
    },

    // ==========================================
    // 1. Dashboard View
    // ==========================================
    async refreshDashboard() {
        try {
            // Load stats and inventory concurrently
            const [statsRes, invRes, reqRes] = await Promise.all([
                Api.admin.getDashboardStats().catch(err => {
                    console.error('Failed to fetch admin stats:', err);
                    return null;
                }),
                Api.inventory.getAll().catch(err => {
                    console.error('Failed to fetch inventory:', err);
                    return null;
                }),
                Api.admin.getAllRequests().catch(err => {
                    console.error('Failed to fetch requests:', err);
                    return null;
                })
            ]);

            if (statsRes && statsRes.data) {
                this.state.stats = statsRes.data;
                this.renderDashboardMetrics(statsRes.data);
            }

            if (invRes && invRes.data) {
                this.state.inventory = invRes.data;
                this.renderDashboardStockOverview(invRes.data);
            }

            if (reqRes && reqRes.data) {
                this.state.requests = reqRes.data;
                this.renderDashboardRecentRequests(reqRes.data);
            }
        } catch (err) {
            this.showToast('Error loading dashboard statistics: ' + err.message, 'error');
        }
    },

    renderDashboardMetrics(stats) {
        // Total Donors
        const elDonors = document.getElementById('stat-total-donors');
        if (elDonors) elDonors.textContent = (stats.totalDonors !== undefined ? stats.totalDonors : 0).toLocaleString();

        // Total Requests
        const elRequests = document.getElementById('stat-total-requests');
        if (elRequests) elRequests.textContent = (stats.totalRequests !== undefined ? stats.totalRequests : (stats.activeRequests || 0)).toLocaleString();

        // Pending Requests
        const elPending = document.getElementById('stat-pending-requests');
        if (elPending) elPending.textContent = (stats.pendingRequests !== undefined ? stats.pendingRequests : (stats.activeRequests || 0)).toLocaleString();

        // Approved / Fulfilled Requests
        const elApproved = document.getElementById('stat-approved-requests');
        if (elApproved) elApproved.textContent = (stats.approvedRequests !== undefined ? stats.approvedRequests : (stats.successfulDonations || 0)).toLocaleString();

        // Rejected Requests
        const elRejected = document.getElementById('stat-rejected-requests');
        if (elRejected) elRejected.textContent = (stats.rejectedRequests !== undefined ? stats.rejectedRequests : 0).toLocaleString();

        // Total Blood Stock Units
        const elStock = document.getElementById('stat-total-stock');
        if (elStock) elStock.textContent = (stats.totalInventoryUnits !== undefined ? stats.totalInventoryUnits : 0).toLocaleString();
    },

    renderDashboardStockOverview(inventoryList) {
        const container = document.getElementById('dashboard-stock-overview-grid');
        if (!container) return;

        if (!inventoryList || inventoryList.length === 0) {
            container.innerHTML = '<div class="empty-state"><p>No blood inventory data available.</p></div>';
            return;
        }

        container.innerHTML = inventoryList.map(item => {
            const bg = item.bloodGroup || 'O+';
            const available = item.availableUnits !== undefined ? item.availableUnits : 0;
            const min = item.minimumRequiredUnits || 15;
            const demand = (item.demandLevel || 'MODERATE').toLowerCase();
            const badgeClass = demand === 'critical' ? 'critical' : (demand === 'high' ? 'high' : (demand === 'surplus' ? 'surplus' : 'moderate'));
            const percent = Math.min(Math.round((available / (min * 2)) * 100), 100);

            return `
                <div class="stock-card" style="padding: 16px;">
                    <div class="stock-card-top" style="margin-bottom: 10px;">
                        <div class="stock-blood-group" style="font-size: 1.6rem;">
                            <span class="drop-icon">🩸</span> ${escapeHtml(bg)}
                        </div>
                        <span class="demand-badge ${badgeClass}">${escapeHtml(item.urgencyBadge || item.demandLevel || 'Normal')}</span>
                    </div>
                    <div class="stock-units-display" style="margin-bottom: 10px;">
                        <div class="stock-units-number" style="font-size: 1.5rem;">
                            ${available} <span class="stock-units-label">units</span>
                        </div>
                    </div>
                    <div class="stock-progress-bar" style="margin-bottom: 10px;">
                        <div class="stock-progress-fill ${badgeClass}" style="width: ${percent}%;"></div>
                    </div>
                    <div class="stock-meta-row" style="margin-bottom: 0;">
                        <span>Target: ${min} units</span>
                        <a href="javascript:void(0)" onclick="AdminApp.openStockModal('${escapeHtml(bg)}', ${available}, ${min})" style="color: var(--primary); font-weight: 600; text-decoration: none;">Update &rarr;</a>
                    </div>
                </div>
            `;
        }).join('');
    },

    renderDashboardRecentRequests(requestsList) {
        const tbody = document.getElementById('dashboard-recent-requests-body');
        if (!tbody) return;

        if (!requestsList || requestsList.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 24px;">No blood requests recorded in database.</td></tr>';
            return;
        }

        // Show recent 5 requests
        const recent = requestsList.slice(0, 5);
        tbody.innerHTML = recent.map(req => {
            const urgencyClass = (req.urgency || 'HIGH').toLowerCase();
            const statusClass = (req.status || 'ACTIVE').toLowerCase();
            const isPending = req.status === 'PENDING' || req.status === 'ACTIVE';

            return `
                <tr>
                    <td><strong>#${req.id}</strong></td>
                    <td>
                        <div style="font-weight: 600; color: #fff;">${escapeHtml(req.hospitalName || 'Medical Facility')}</div>
                        <div style="font-size: 0.76rem; color: var(--text-muted);">${escapeHtml(req.hospitalLocation || 'N/A')}</div>
                    </td>
                    <td><span class="blood-badge">${escapeHtml(req.bloodGroup)}</span></td>
                    <td><strong>${req.requiredUnits || 1}</strong> unit(s)</td>
                    <td><span class="urgency-pill ${urgencyClass}">${escapeHtml(req.urgency || 'HIGH')}</span></td>
                    <td><span class="status-pill ${statusClass}">${escapeHtml(req.status)}</span></td>
                    <td>
                        ${isPending ? `
                            <button class="btn btn-primary btn-sm" onclick="AdminApp.updateRequestStatus(${req.id}, 'APPROVED')" style="padding: 4px 8px; font-size: 0.75rem;">✓ Approve</button>
                            <button class="btn btn-secondary btn-sm" onclick="AdminApp.updateRequestStatus(${req.id}, 'REJECTED')" style="padding: 4px 8px; font-size: 0.75rem; border-color: rgba(225,29,72,0.4); color: #fda4af;">✕ Reject</button>
                        ` : `
                            <span style="font-size: 0.78rem; color: var(--text-muted);">Processed</span>
                        `}
                    </td>
                </tr>
            `;
        }).join('');
    },

    // ==========================================
    // 2. Manage Donors View
    // ==========================================
    async refreshDonors() {
        const tbody = document.getElementById('donors-table-body');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 30px;">Loading registered donors from database...</td></tr>';
        }

        try {
            const res = await Api.admin.getAllDonors();
            if (res && res.data) {
                this.state.donors = res.data;
                this.applyDonorFilters();
            } else {
                this.state.donors = [];
                this.applyDonorFilters();
            }
        } catch (err) {
            if (tbody) {
                tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: #fda4af; padding: 24px;">Failed to load donors: ${escapeHtml(err.message)}</td></tr>`;
            }
            this.showToast('Error loading donors: ' + err.message, 'error');
        }
    },

    filterDonors() {
        const searchInput = document.getElementById('donor-search-input');
        const bgSelect = document.getElementById('donor-blood-group-filter');
        const statusSelect = document.getElementById('donor-status-filter');

        this.state.donorSearchQuery = searchInput ? searchInput.value.toLowerCase().trim() : '';
        this.state.donorBloodGroupFilter = bgSelect ? bgSelect.value : '';
        this.state.donorStatusFilter = statusSelect ? statusSelect.value : '';

        this.applyDonorFilters();
    },

    applyDonorFilters() {
        const tbody = document.getElementById('donors-table-body');
        if (!tbody) return;

        let filtered = this.state.donors || [];

        // Search by name, blood group, city or address
        if (this.state.donorSearchQuery) {
            const q = this.state.donorSearchQuery;
            filtered = filtered.filter(d => 
                (d.name && d.name.toLowerCase().includes(q)) ||
                (d.bloodGroup && d.bloodGroup.toLowerCase().includes(q)) ||
                (d.city && d.city.toLowerCase().includes(q)) ||
                (d.address && d.address.toLowerCase().includes(q)) ||
                (d.email && d.email.toLowerCase().includes(q)) ||
                (d.phone && d.phone.toLowerCase().includes(q))
            );
        }

        // Filter by Blood Group
        if (this.state.donorBloodGroupFilter) {
            filtered = filtered.filter(d => d.bloodGroup === this.state.donorBloodGroupFilter);
        }

        // Filter by Status (Available vs Inactive)
        if (this.state.donorStatusFilter === 'available') {
            filtered = filtered.filter(d => d.availabilityStatus === true);
        } else if (this.state.donorStatusFilter === 'inactive') {
            filtered = filtered.filter(d => d.availabilityStatus === false);
        }

        const countEl = document.getElementById('donors-count-badge');
        if (countEl) countEl.textContent = `${filtered.length} donor(s)`;

        if (filtered.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 36px;">No donors match your search and filter criteria.</td></tr>';
            return;
        }

        tbody.innerHTML = filtered.map(d => {
            const isAvail = d.availabilityStatus === true;
            const badgeIcon = d.badgeIcon || '🩸';
            const badgeTitle = d.badgeTitle || (d.badge ? d.badge.replace('_', ' ') : 'Voluntary Donor');

            return `
                <tr>
                    <td>
                        <div style="font-weight: 700; color: #fff; display: flex; align-items: center; gap: 6px;">
                            ${escapeHtml(d.name || 'Anonymous')}
                            <span title="${escapeHtml(badgeTitle)}" style="font-size: 1rem; cursor: help;">${badgeIcon}</span>
                        </div>
                        <div style="font-size: 0.74rem; color: var(--text-muted);">${escapeHtml(d.email || 'N/A')}</div>
                    </td>
                    <td><span class="blood-badge">${escapeHtml(d.bloodGroup || 'O+')}</span></td>
                    <td>${d.age || '—'} / ${escapeHtml(d.gender || '—')}</td>
                    <td>
                        <div style="font-weight: 500;">${escapeHtml(d.city || 'Chennai')}</div>
                    </td>
                    <td>
                        <div style="font-size: 0.85rem; color: #fff;">${escapeHtml(d.phone || 'N/A')}</div>
                    </td>
                    <td style="text-align: center;">
                        <span style="font-weight: 700; color: var(--primary); font-size: 1.05rem;">${d.totalDonations || 0}</span>
                    </td>
                    <td>
                        <span class="status-pill ${isAvail ? 'available' : 'inactive'}">
                            ${isAvail ? '● Active' : '○ Inactive'}
                        </span>
                    </td>
                    <td>
                        <div style="display: flex; gap: 6px;">
                            <button class="btn btn-secondary btn-sm" onclick="AdminApp.viewDonorDetails(${d.id})" title="View complete donor profile" style="padding: 4px 10px; font-size: 0.78rem;">
                                👁️ Details
                            </button>
                            <button class="btn ${isAvail ? 'btn-secondary' : 'btn-primary'} btn-sm" onclick="AdminApp.toggleDonorStatus(${d.id}, ${!isAvail})" title="${isAvail ? 'Deactivate donor availability' : 'Activate donor availability'}" style="padding: 4px 10px; font-size: 0.78rem; ${isAvail ? 'color: #fda4af; border-color: rgba(225,29,72,0.3);' : ''}">
                                ${isAvail ? 'Deactivate' : 'Activate'}
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    },

    viewDonorDetails(donorId) {
        const donor = (this.state.donors || []).find(d => d.id === donorId);
        if (!donor) {
            this.showToast('Donor profile not found', 'error');
            return;
        }

        const modal = document.getElementById('donor-details-modal');
        const container = document.getElementById('donor-modal-content');
        if (!modal || !container) return;

        const badgeIcon = donor.badgeIcon || '🩸';
        const badgeTitle = donor.badgeTitle || (donor.badge ? donor.badge.replace('_', ' ') : 'Voluntary Donor');

        container.innerHTML = `
            <div style="display: flex; align-items: center; gap: 16px; padding: 14px; background: rgba(255,255,255,0.03); border-radius: var(--radius-sm); border: 1px solid var(--border-glass); margin-bottom: 16px;">
                <div style="width: 52px; height: 52px; border-radius: 50%; background: linear-gradient(135deg, var(--primary), #9f1239); display: flex; align-items: center; justify-content: center; font-size: 1.6rem; color: #fff;">
                    ${badgeIcon}
                </div>
                <div>
                    <h4 style="margin: 0; font-size: 1.2rem; color: #fff;">${escapeHtml(donor.name || 'Anonymous')}</h4>
                    <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 2px;">
                        <span>Rank: <strong>${escapeHtml(badgeTitle)}</strong></span> • 
                        <span class="blood-badge" style="font-size: 0.76rem; padding: 1px 6px;">${escapeHtml(donor.bloodGroup || 'O+')}</span>
                    </div>
                </div>
            </div>

            <div class="donor-detail-grid">
                <div class="donor-detail-item">
                    <div class="donor-detail-label">Email Address</div>
                    <div class="donor-detail-value">${escapeHtml(donor.email || 'N/A')}</div>
                </div>
                <div class="donor-detail-item">
                    <div class="donor-detail-label">Phone Number</div>
                    <div class="donor-detail-value">${escapeHtml(donor.phone || 'N/A')}</div>
                </div>
                <div class="donor-detail-item">
                    <div class="donor-detail-label">Age & Gender</div>
                    <div class="donor-detail-value">${donor.age || '—'} yrs (${escapeHtml(donor.gender || '—')})</div>
                </div>
                <div class="donor-detail-item">
                    <div class="donor-detail-label">City</div>
                    <div class="donor-detail-value">${escapeHtml(donor.city || 'Chennai')}</div>
                </div>
                <div class="donor-detail-item" style="grid-column: span 2;">
                    <div class="donor-detail-label">Address</div>
                    <div class="donor-detail-value">${escapeHtml(donor.address || 'N/A')}</div>
                </div>
                <div class="donor-detail-item">
                    <div class="donor-detail-label">Total Verified Donations</div>
                    <div class="donor-detail-value" style="color: var(--primary);">${donor.totalDonations || 0} times</div>
                </div>
                <div class="donor-detail-item">
                    <div class="donor-detail-label">Last Donation Date</div>
                    <div class="donor-detail-value">${donor.lastDonationDate || 'First-time Donor'}</div>
                </div>
                <div class="donor-detail-item">
                    <div class="donor-detail-label">Clinical Eligibility</div>
                    <div class="donor-detail-value" style="color: ${donor.eligible ? '#34d399' : '#fb7185'};">
                        ${donor.eligible ? '✓ Clinically Eligible' : '✕ In Waiting Period'}
                    </div>
                </div>
                <div class="donor-detail-item">
                    <div class="donor-detail-label">Availability Status</div>
                    <div class="donor-detail-value" style="color: ${donor.availabilityStatus ? '#34d399' : '#94a3b8'};">
                        ${donor.availabilityStatus ? '● Active Voluntary Donor' : '○ Temporarily Inactive'}
                    </div>
                </div>
                <div class="donor-detail-item">
                    <div class="donor-detail-label">Community Stars</div>
                    <div class="donor-detail-value">⭐ ${donor.stars || 0} stars</div>
                </div>
                <div class="donor-detail-item">
                    <div class="donor-detail-label">Donor Rating</div>
                    <div class="donor-detail-value">★ ${donor.rating ? donor.rating.toFixed(1) : '5.0'} / 5.0</div>
                </div>
            </div>
        `;

        modal.classList.add('active');
    },

    closeDonorModal() {
        const modal = document.getElementById('donor-details-modal');
        if (modal) modal.classList.remove('active');
    },

    async toggleDonorStatus(donorId, newStatus) {
        try {
            const res = await Api.admin.toggleDonorAvailability(donorId, newStatus);
            this.showToast(newStatus ? 'Donor account set to Active' : 'Donor account deactivated', 'success');
            await this.refreshDonors();
            await this.refreshDashboard();
        } catch (err) {
            this.showToast('Failed to update donor status: ' + err.message, 'error');
        }
    },

    // ==========================================
    // 3. Manage Blood Requests View
    // ==========================================
    async refreshRequests() {
        const tbody = document.getElementById('requests-table-body');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 30px;">Loading hospital blood requests from database...</td></tr>';
        }

        try {
            const res = await Api.admin.getAllRequests();
            if (res && res.data) {
                this.state.requests = res.data;
                this.applyRequestFilters();
            } else {
                this.state.requests = [];
                this.applyRequestFilters();
            }
        } catch (err) {
            if (tbody) {
                tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: #fda4af; padding: 24px;">Failed to load requests: ${escapeHtml(err.message)}</td></tr>`;
            }
            this.showToast('Error loading requests: ' + err.message, 'error');
        }
    },

    setRequestFilter(filterName) {
        this.state.requestFilter = filterName;

        document.querySelectorAll('.request-tab-btn').forEach(btn => {
            if (btn.dataset.status === filterName) {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        });

        this.applyRequestFilters();
    },

    filterRequests() {
        this.applyRequestFilters();
    },

    applyRequestFilters() {
        const tbody = document.getElementById('requests-table-body');
        if (!tbody) return;

        let filtered = this.state.requests || [];

        // Filter tabs
        const statusTab = this.state.requestFilter;
        if (statusTab === 'pending') {
            filtered = filtered.filter(r => r.status === 'PENDING' || r.status === 'ACTIVE');
        } else if (statusTab === 'approved') {
            filtered = filtered.filter(r => r.status === 'APPROVED');
        } else if (statusTab === 'rejected') {
            filtered = filtered.filter(r => r.status === 'REJECTED' || r.status === 'CANCELLED');
        } else if (statusTab === 'fulfilled') {
            filtered = filtered.filter(r => r.status === 'FULFILLED');
        }

        // Search filter input
        const searchInput = document.getElementById('request-search-input');
        if (searchInput && searchInput.value.trim()) {
            const q = searchInput.value.toLowerCase().trim();
            filtered = filtered.filter(r =>
                (r.hospitalName && r.hospitalName.toLowerCase().includes(q)) ||
                (r.hospitalLocation && r.hospitalLocation.toLowerCase().includes(q)) ||
                (r.bloodGroup && r.bloodGroup.toLowerCase().includes(q)) ||
                (r.contactPhone && r.contactPhone.toLowerCase().includes(q)) ||
                (r.notes && r.notes.toLowerCase().includes(q)) ||
                (String(r.id).includes(q))
            );
        }

        // Blood Group Filter
        const bgFilter = document.getElementById('request-blood-group-filter');
        if (bgFilter && bgFilter.value) {
            filtered = filtered.filter(r => r.bloodGroup === bgFilter.value);
        }

        const countEl = document.getElementById('requests-count-badge');
        if (countEl) countEl.textContent = `${filtered.length} request(s)`;

        if (filtered.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 36px;">No blood requests match this filter.</td></tr>';
            return;
        }

        tbody.innerHTML = filtered.map(req => {
            const urgencyClass = (req.urgency || 'HIGH').toLowerCase();
            const statusClass = (req.status || 'ACTIVE').toLowerCase();
            const isPending = req.status === 'PENDING' || req.status === 'ACTIVE';
            const isApproved = req.status === 'APPROVED';

            return `
                <tr>
                    <td><strong>#${req.id}</strong></td>
                    <td>
                        <div style="font-weight: 700; color: #fff;">${escapeHtml(req.hospitalName || 'Hospital')}</div>
                        <div style="font-size: 0.74rem; color: var(--text-muted);">${escapeHtml(req.hospitalLocation || 'N/A')}</div>
                    </td>
                    <td><span class="blood-badge">${escapeHtml(req.bloodGroup)}</span></td>
                    <td><strong>${req.requiredUnits || 1}</strong> unit(s)</td>
                    <td><span class="urgency-pill ${urgencyClass}">${escapeHtml(req.urgency || 'HIGH')}</span></td>
                    <td>
                        <div>${req.requiredDate || 'Urgent'}</div>
                        <div style="font-size: 0.74rem; color: var(--text-muted);">${escapeHtml(req.contactPhone || 'N/A')}</div>
                    </td>
                    <td><span class="status-pill ${statusClass}">${escapeHtml(req.status)}</span></td>
                    <td>
                        <div style="display: flex; gap: 6px; flex-wrap: wrap;">
                            ${isPending ? `
                                <button class="btn btn-primary btn-sm" onclick="AdminApp.updateRequestStatus(${req.id}, 'APPROVED')" title="Approve blood request" style="padding: 4px 8px; font-size: 0.75rem;">
                                    ✓ Approve
                                </button>
                                <button class="btn btn-secondary btn-sm" onclick="AdminApp.updateRequestStatus(${req.id}, 'REJECTED')" title="Reject blood request" style="padding: 4px 8px; font-size: 0.75rem; border-color: rgba(225,29,72,0.4); color: #fda4af;">
                                    ✕ Reject
                                </button>
                                <button class="btn btn-secondary btn-sm" onclick="AdminApp.updateRequestStatus(${req.id}, 'FULFILLED')" title="Directly mark fulfilled" style="padding: 4px 8px; font-size: 0.75rem;">
                                    ⚡ Fulfill
                                </button>
                            ` : (isApproved ? `
                                <button class="btn btn-primary btn-sm" onclick="AdminApp.updateRequestStatus(${req.id}, 'FULFILLED')" title="Mark request fulfilled" style="padding: 4px 8px; font-size: 0.75rem;">
                                    ⚡ Fulfill
                                </button>
                                <button class="btn btn-secondary btn-sm" onclick="AdminApp.updateRequestStatus(${req.id}, 'CANCELLED')" title="Cancel request" style="padding: 4px 8px; font-size: 0.75rem; color: #fda4af;">
                                    Cancel
                                </button>
                            ` : `
                                <span style="font-size: 0.78rem; color: var(--text-muted);">No actions</span>
                            `)}
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    },

    async updateRequestStatus(requestId, newStatus) {
        try {
            await Api.admin.updateRequestStatus(requestId, newStatus);
            this.showToast(`Blood request #${requestId} status set to ${newStatus}`, 'success');
            await this.refreshRequests();
            await this.refreshDashboard();
        } catch (err) {
            this.showToast('Failed to update request: ' + err.message, 'error');
        }
    },

    // ==========================================
    // 4. Blood Stock Management View
    // ==========================================
    async refreshStock() {
        const grid = document.getElementById('stock-cards-grid');
        if (grid) {
            grid.innerHTML = '<div class="empty-state" style="grid-column: 1 / -1;"><p>Loading blood stock levels from inventory database...</p></div>';
        }

        try {
            const res = await Api.inventory.getAll();
            if (res && res.data) {
                this.state.inventory = res.data;
                this.renderStockCards(res.data);
            }
        } catch (err) {
            if (grid) {
                grid.innerHTML = `<div class="empty-state" style="grid-column: 1 / -1; color: #fda4af;"><p>Failed to load blood inventory: ${escapeHtml(err.message)}</p></div>`;
            }
            this.showToast('Error loading blood stock: ' + err.message, 'error');
        }
    },

    renderStockCards(inventoryList) {
        const grid = document.getElementById('stock-cards-grid');
        if (!grid) return;

        // Ensure all 8 blood groups are represented
        const allGroups = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
        
        // Map existing inventory by bloodGroup
        const invMap = {};
        (inventoryList || []).forEach(item => {
            if (item.bloodGroup) invMap[item.bloodGroup] = item;
        });

        grid.innerHTML = allGroups.map(bg => {
            const item = invMap[bg] || {
                bloodGroup: bg,
                availableUnits: 0,
                reservedUnits: 0,
                minimumRequiredUnits: 15,
                demandLevel: 'HIGH',
                urgencyBadge: 'High Demand'
            };

            const available = item.availableUnits !== undefined ? item.availableUnits : 0;
            const reserved = item.reservedUnits !== undefined ? item.reservedUnits : 0;
            const min = item.minimumRequiredUnits || 15;
            const demand = (item.demandLevel || 'MODERATE').toLowerCase();
            const badgeClass = demand === 'critical' ? 'critical' : (demand === 'high' ? 'high' : (demand === 'surplus' ? 'surplus' : 'moderate'));
            const percent = Math.min(Math.round((available / (min * 2)) * 100), 100);

            return `
                <div class="stock-card">
                    <div class="stock-card-top">
                        <div class="stock-blood-group">
                            <span class="drop-icon">🩸</span> ${escapeHtml(bg)}
                        </div>
                        <span class="demand-badge ${badgeClass}">${escapeHtml(item.urgencyBadge || item.demandLevel || 'Moderate')}</span>
                    </div>

                    <div class="stock-units-display">
                        <div class="stock-units-number">
                            ${available} <span class="stock-units-label">available units</span>
                        </div>
                    </div>

                    <div class="stock-meta-row">
                        <span>Safe Threshold: ${min} units</span>
                        <span>Reserved: ${reserved}</span>
                    </div>

                    <div class="stock-progress-bar">
                        <div class="stock-progress-fill ${badgeClass}" style="width: ${percent}%;"></div>
                    </div>

                    <button class="btn btn-primary btn-sm admin-btn-block" onclick="AdminApp.openStockModal('${escapeHtml(bg)}', ${available}, ${min})">
                        ✏️ Update Stock Units
                    </button>
                </div>
            `;
        }).join('');
    },

    openStockModal(bloodGroup, currentUnits, currentMin) {
        const modal = document.getElementById('stock-update-modal');
        if (!modal) return;

        document.getElementById('stock-modal-blood-group').textContent = bloodGroup;
        document.getElementById('stock-modal-bg-input').value = bloodGroup;
        
        const unitsInput = document.getElementById('stock-modal-units-input');
        if (unitsInput) {
            unitsInput.value = currentUnits;
            unitsInput.focus();
        }

        const minInput = document.getElementById('stock-modal-min-input');
        if (minInput) minInput.value = currentMin || 15;

        modal.classList.add('active');
    },

    closeStockModal() {
        const modal = document.getElementById('stock-update-modal');
        if (modal) modal.classList.remove('active');
    },

    async handleStockUpdateSubmit(event) {
        event.preventDefault();

        const bloodGroup = document.getElementById('stock-modal-bg-input').value;
        const unitsInput = document.getElementById('stock-modal-units-input');
        const minInput = document.getElementById('stock-modal-min-input');

        const availableUnits = parseInt(unitsInput.value, 10);
        const minimumRequiredUnits = minInput ? parseInt(minInput.value, 10) : 15;

        // Front-end validation: Prevent negative values
        if (isNaN(availableUnits) || availableUnits < 0) {
            this.showToast('Validation Error: Available units cannot be negative', 'error');
            unitsInput.focus();
            return;
        }

        if (isNaN(minimumRequiredUnits) || minimumRequiredUnits < 0) {
            this.showToast('Validation Error: Minimum required units cannot be negative', 'error');
            minInput.focus();
            return;
        }

        const submitBtn = document.getElementById('stock-modal-submit-btn');
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.textContent = 'Saving...';
        }

        try {
            await Api.admin.updateInventory(bloodGroup, {
                availableUnits: availableUnits,
                minimumRequiredUnits: minimumRequiredUnits
            });

            this.showToast(`Blood inventory stock for ${bloodGroup} updated to ${availableUnits} units!`, 'success');
            this.closeStockModal();
            await this.refreshStock();
            await this.refreshDashboard();
        } catch (err) {
            this.showToast('Failed to update stock: ' + err.message, 'error');
        } finally {
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.textContent = 'Save Changes';
            }
        }
    },

    // ==========================================
    // 5. Admin Authentication
    // ==========================================
    async handleAdminLogin(event) {
        event.preventDefault();

        const emailInput = document.getElementById('admin-login-email');
        const passInput = document.getElementById('admin-login-password');
        const alertBox = document.getElementById('login-alert-box');
        const loginBtn = document.getElementById('admin-login-btn');

        const email = emailInput ? emailInput.value.trim() : '';
        const password = passInput ? passInput.value : '';

        if (!email || !password) {
            if (alertBox) {
                alertBox.className = 'login-alert error';
                alertBox.textContent = 'Please enter both administrator email and password.';
                alertBox.style.display = 'block';
            }
            return;
        }

        if (loginBtn) {
            loginBtn.disabled = true;
            loginBtn.textContent = 'Authenticating...';
        }

        if (alertBox) alertBox.style.display = 'none';

        try {
            const res = await Api.auth.login(email, password);
            if (!res || !res.data || !res.data.token) {
                throw new Error('Invalid authentication response from server.');
            }

            const user = res.data.user || res.data;

            // Security check: Only allow ADMIN role
            if (!user || user.role !== 'ADMIN') {
                Api.logout();
                if (alertBox) {
                    alertBox.className = 'login-alert error';
                    alertBox.innerHTML = `
                        <strong>Access Denied:</strong> This account has role <code>${escapeHtml(user ? user.role : 'UNKNOWN')}</code>.
                        Only <code>ADMIN</code> accounts can access the Admin Panel.
                    `;
                    alertBox.style.display = 'block';
                }
                return;
            }

            // Save credentials
            Api.setToken(res.data.token);
            Api.setUser(user);

            this.state.token = res.data.token;
            this.state.user = user;

            this.showToast('Welcome back, Administrator!', 'success');
            this.showAdminPanel();
            this.updateAdminProfileUI();
            await this.loadView('dashboard');
        } catch (err) {
            if (alertBox) {
                alertBox.className = 'login-alert error';
                alertBox.textContent = err.message || 'Login failed. Please verify admin credentials.';
                alertBox.style.display = 'block';
            }
        } finally {
            if (loginBtn) {
                loginBtn.disabled = false;
                loginBtn.textContent = 'Sign In to Admin Panel';
            }
        }
    },

    handleLogout() {
        Api.logout();
        this.state.token = null;
        this.state.user = null;
        this.showLoginScreen();
        const alertBox = document.getElementById('login-alert-box');
        if (alertBox) {
            alertBox.className = 'login-alert info';
            alertBox.textContent = 'You have been safely signed out.';
            alertBox.style.display = 'block';
        }
    },

    // Toast Notifications
    showToast(message, type = 'info') {
        const container = document.getElementById('admin-toast-container');
        if (!container) return;

        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.style.cssText = `
            background: ${type === 'error' ? 'rgba(225, 29, 72, 0.95)' : (type === 'success' ? 'rgba(16, 185, 129, 0.95)' : 'rgba(30, 41, 59, 0.95)')};
            color: #fff;
            padding: 12px 18px;
            border-radius: var(--radius-sm);
            margin-bottom: 8px;
            font-size: 0.88rem;
            box-shadow: 0 8px 24px rgba(0,0,0,0.4);
            border: 1px solid rgba(255,255,255,0.15);
            display: flex;
            align-items: center;
            gap: 10px;
            animation: fadeIn 0.2s ease-out;
        `;
        toast.innerHTML = `<span>${type === 'error' ? '⚠️' : (type === 'success' ? '✅' : 'ℹ️')}</span> <span>${escapeHtml(message)}</span>`;

        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(10px)';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    }
};

// Helper: Escape HTML strings to prevent XSS
function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

window.AdminApp = AdminApp;

// Auto-initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    AdminApp.init();
});
