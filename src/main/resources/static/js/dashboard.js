(function () {
    "use strict";

    const api = window.CollabNexusApi.api;
    const ensureCsrf = window.CollabNexusApi.ensureCsrf;
    const toast = (message) => window.CollabNexus && window.CollabNexus.toast(message);

    const state = {
        session: null,
        view: "overview",
        campaigns: {page: 0, totalPages: 1, status: "", q: "", editingId: null, loading: false},
        applications: {
            brand: {page: 0, totalPages: 1, status: "", q: "", campaignId: null,
                campaignTitle: null, loading: false},
            creator: {page: 0, totalPages: 1, status: "", q: "", loading: false}
        },
        collaborations: {page: 0, totalPages: 1, status: "", loading: false},
        deliverables: {
            page: 0, totalPages: 1, status: "", deadline: "", collaborationId: null,
            collaborationTitle: null, loading: false, editingId: null, uploadId: null
        },
        contracts: {page: 0, totalPages: 1, loading: false, selectedId: null, editingId: null},
        payments: {page: 0, totalPages: 1, status: "", q: "", loading: false},
        analytics: {loading: false, charts: {}},
        messages: {conversations: [], activeId: null, page: 0, totalPages: 1, loading: false},
        reviews: {eligible: [], mine: [], loading: false},
        notifications: {page: 0, totalPages: 1, loading: false},
        admin: {
            users: {page: 0, totalPages: 1, role: "", q: "", loading: false},
            campaigns: {page: 0, totalPages: 1, status: "", q: "", loading: false},
            reports: {page: 0, totalPages: 1, status: "", loading: false},
            reviews: {page: 0, totalPages: 1, status: "", loading: false},
            audit: {page: 0, totalPages: 1, loading: false}
        }
    };

    const els = {
        sidebar: document.getElementById("appSidebar"),
        nav: document.querySelector("[data-side-nav]"),
        sideRole: document.querySelector("[data-side-role]"),
        userName: document.querySelector("[data-user-name]"),
        userEmail: document.querySelector("[data-user-email]"),
        avatar: document.querySelector("[data-avatar]"),
        pageTitle: document.querySelector("[data-page-title]"),
        pageKicker: document.querySelector("[data-page-kicker]"),
        roleEyebrow: document.querySelector("[data-role-eyebrow]"),
        greeting: document.querySelector("[data-greeting]"),
        heroCopy: document.querySelector("[data-hero-copy]"),
        completionRing: document.querySelector("[data-completion-ring]"),
        completionValue: document.querySelector("[data-completion-value]"),
        completionNote: document.querySelector("[data-completion-note]"),
        statGrid: document.querySelector("[data-stat-grid]"),
        accountDetails: document.querySelector("[data-account-details]"),
        checklist: document.querySelector("[data-checklist]"),
        statusPill: document.querySelector("[data-status-pill]"),
        roleBadge: document.querySelector("[data-role-badge]"),
        adminPanel: document.querySelector("[data-admin-panel]"),
        adminUsers: document.querySelector("[data-admin-users]"),
        adminCount: document.querySelector("[data-admin-count]"),
        profileForm: document.querySelector("[data-profile-form]"),
        profileTitle: document.querySelector("[data-profile-title]"),
        profileSubtitle: document.querySelector("[data-profile-subtitle]"),
        profileComplete: document.querySelector("[data-profile-complete]"),
        profileAlert: document.querySelector("[data-profile-alert]"),
        appAlert: document.querySelector("[data-app-alert]"),
        securityDetails: document.querySelector("[data-security-details]"),
        campaignList: document.querySelector("[data-campaign-list]"),
        campaignEmpty: document.querySelector("[data-campaign-empty]"),
        campaignAlert: document.querySelector("[data-campaign-alert]"),
        campaignAlertText: document.querySelector("[data-campaign-alert-text]"),
        campaignStatus: document.querySelector("[data-campaign-status]"),
        campaignSearch: document.querySelector("[data-campaign-search]"),
        campaignPagination: document.querySelector("[data-campaign-pagination]"),
        campaignPage: document.querySelector("[data-campaign-page]"),
        campaignPrev: document.querySelector("[data-campaign-prev]"),
        campaignNext: document.querySelector("[data-campaign-next]"),
        campaignEditor: document.querySelector("[data-campaign-editor]"),
        campaignForm: document.querySelector("[data-campaign-form]"),
        campaignFormTitle: document.querySelector("[data-campaign-form-title]"),
        campaignFormNote: document.querySelector("[data-campaign-form-note]"),
        campaignFormStatus: document.querySelector("[data-campaign-form-status]"),
        campaignFormAlert: document.querySelector("[data-campaign-form-alert]"),
        campaignFormAlertText: document.querySelector("[data-campaign-form-alert-text]"),
        campaignSave: document.querySelector("[data-campaign-save]"),
        brandApplicationsPanel: document.querySelector("[data-brand-applications-panel]"),
        creatorApplicationsPanel: document.querySelector("[data-creator-applications-panel]"),
        brandAppList: document.querySelector("[data-brand-app-list]"),
        brandAppEmpty: document.querySelector("[data-brand-app-empty]"),
        brandAppAlert: document.querySelector("[data-brand-app-alert]"),
        brandAppAlertText: document.querySelector("[data-brand-app-alert-text]"),
        brandAppStatus: document.querySelector("[data-brand-app-status]"),
        brandAppSearch: document.querySelector("[data-brand-app-search]"),
        brandAppCount: document.querySelector("[data-brand-app-count]"),
        brandAppPagination: document.querySelector("[data-brand-app-pagination]"),
        brandAppPage: document.querySelector("[data-brand-app-page]"),
        brandAppPrev: document.querySelector("[data-brand-app-prev]"),
        brandAppNext: document.querySelector("[data-brand-app-next]"),
        brandAppCampaignFilter: document.querySelector("[data-brand-app-campaign-filter]"),
        brandAppCampaignTitle: document.querySelector("[data-brand-app-campaign-title]"),
        creatorAppList: document.querySelector("[data-creator-app-list]"),
        creatorAppEmpty: document.querySelector("[data-creator-app-empty]"),
        creatorAppAlert: document.querySelector("[data-creator-app-alert]"),
        creatorAppAlertText: document.querySelector("[data-creator-app-alert-text]"),
        creatorAppStatus: document.querySelector("[data-creator-app-status]"),
        creatorAppSearch: document.querySelector("[data-creator-app-search]"),
        creatorAppCount: document.querySelector("[data-creator-app-count]"),
        creatorAppPagination: document.querySelector("[data-creator-app-pagination]"),
        creatorAppPage: document.querySelector("[data-creator-app-page]"),
        creatorAppPrev: document.querySelector("[data-creator-app-prev]"),
        creatorAppNext: document.querySelector("[data-creator-app-next]"),
        collabList: document.querySelector("[data-collab-list]"),
        collabEmpty: document.querySelector("[data-collab-empty]"),
        collabAlert: document.querySelector("[data-collab-alert]"),
        collabAlertText: document.querySelector("[data-collab-alert-text]"),
        collabStatus: document.querySelector("[data-collab-status]"),
        collabCount: document.querySelector("[data-collab-count]"),
        collabPagination: document.querySelector("[data-collab-pagination]"),
        collabPage: document.querySelector("[data-collab-page]"),
        collabPrev: document.querySelector("[data-collab-prev]"),
        collabNext: document.querySelector("[data-collab-next]"),
        notificationBell: document.querySelector("[data-notification-bell]"),
        notificationBadge: document.querySelector("[data-notification-badge]"),
        deliverableUpcoming: document.querySelector("[data-deliverable-upcoming]"),
        deliverableUpcomingCount: document.querySelector("[data-deliverable-upcoming-count]"),
        deliverableOverdue: document.querySelector("[data-deliverable-overdue]"),
        deliverableOverdueCount: document.querySelector("[data-deliverable-overdue-count]"),
        deliverableList: document.querySelector("[data-deliverable-list]"),
        deliverableEmpty: document.querySelector("[data-deliverable-empty]"),
        deliverableCount: document.querySelector("[data-deliverable-count]"),
        deliverableStatus: document.querySelector("[data-deliverable-status]"),
        deliverableDeadline: document.querySelector("[data-deliverable-deadline]"),
        deliverableAlert: document.querySelector("[data-deliverable-alert]"),
        deliverableAlertText: document.querySelector("[data-deliverable-alert-text]"),
        deliverablePagination: document.querySelector("[data-deliverable-pagination]"),
        deliverablePage: document.querySelector("[data-deliverable-page]"),
        deliverablePrev: document.querySelector("[data-deliverable-prev]"),
        deliverableNext: document.querySelector("[data-deliverable-next]"),
        deliverableNew: document.querySelector("[data-deliverable-new]"),
        deliverableEditor: document.querySelector("[data-deliverable-editor]"),
        deliverableForm: document.querySelector("[data-deliverable-form]"),
        deliverableFormTitle: document.querySelector("[data-deliverable-form-title]"),
        deliverableFormMode: document.querySelector("[data-deliverable-form-mode]"),
        deliverableFormAlert: document.querySelector("[data-deliverable-form-alert]"),
        deliverableFormAlertText: document.querySelector("[data-deliverable-form-alert-text]"),
        deliverableSave: document.querySelector("[data-deliverable-save]"),
        deliverableCollabFilter: document.querySelector("[data-deliverable-collab-filter]"),
        deliverableCollabTitle: document.querySelector("[data-deliverable-collab-title]"),
        submissionEditor: document.querySelector("[data-submission-editor]"),
        submissionForm: document.querySelector("[data-submission-form]"),
        submissionTitle: document.querySelector("[data-submission-title]"),
        submissionAlert: document.querySelector("[data-submission-alert]"),
        submissionAlertText: document.querySelector("[data-submission-alert-text]"),
        submissionSave: document.querySelector("[data-submission-save]"),
        contractList: document.querySelector("[data-contract-list]"),
        contractEmpty: document.querySelector("[data-contract-empty]"),
        contractCount: document.querySelector("[data-contract-count]"),
        contractAlert: document.querySelector("[data-contract-alert]"),
        contractAlertText: document.querySelector("[data-contract-alert-text]"),
        contractPagination: document.querySelector("[data-contract-pagination]"),
        contractPage: document.querySelector("[data-contract-page]"),
        contractPrev: document.querySelector("[data-contract-prev]"),
        contractNext: document.querySelector("[data-contract-next]"),
        contractDetail: document.querySelector("[data-contract-detail]"),
        contractReference: document.querySelector("[data-contract-reference]"),
        contractCampaign: document.querySelector("[data-contract-campaign]"),
        contractStatus: document.querySelector("[data-contract-status]"),
        contractFacts: document.querySelector("[data-contract-facts]"),
        contractTerms: document.querySelector("[data-contract-terms]"),
        contractAck: document.querySelector("[data-contract-ack]"),
        contractActions: document.querySelector("[data-contract-actions]"),
        contractHistory: document.querySelector("[data-contract-history]"),
        contractEditor: document.querySelector("[data-contract-editor]"),
        contractForm: document.querySelector("[data-contract-form]"),
        contractFormAlert: document.querySelector("[data-contract-form-alert]"),
        contractFormAlertText: document.querySelector("[data-contract-form-alert-text]"),
        contractEditorVersion: document.querySelector("[data-contract-editor-version]"),
        contractSave: document.querySelector("[data-contract-save]"),
        paymentSummary: document.querySelector("[data-payment-summary]"),
        paymentCreate: document.querySelector("[data-payment-create]"),
        paymentForm: document.querySelector("[data-payment-form]"),
        paymentAlert: document.querySelector("[data-payment-alert]"),
        paymentAlertText: document.querySelector("[data-payment-alert-text]"),
        paymentList: document.querySelector("[data-payment-list]"),
        paymentEmpty: document.querySelector("[data-payment-empty]"),
        paymentCount: document.querySelector("[data-payment-count]"),
        paymentStatus: document.querySelector("[data-payment-status]"),
        paymentSearch: document.querySelector("[data-payment-search]"),
        paymentListAlert: document.querySelector("[data-payment-list-alert]"),
        paymentListAlertText: document.querySelector("[data-payment-list-alert-text]"),
        paymentPagination: document.querySelector("[data-payment-pagination]"),
        paymentPage: document.querySelector("[data-payment-page]"),
        paymentPrev: document.querySelector("[data-payment-prev]"),
        paymentNext: document.querySelector("[data-payment-next]"),
        analyticsStats: document.querySelector("[data-analytics-stats]"),
        conversationList: document.querySelector("[data-conversation-list]"),
        conversationEmpty: document.querySelector("[data-conversation-empty]"),
        conversationAlert: document.querySelector("[data-conversation-alert]"),
        conversationAlertText: document.querySelector("[data-conversation-alert-text]"),
        threadTitle: document.querySelector("[data-thread-title]"),
        threadSubtitle: document.querySelector("[data-thread-subtitle]"),
        threadUnread: document.querySelector("[data-thread-unread]"),
        messageList: document.querySelector("[data-message-list]"),
        messageForm: document.querySelector("[data-message-form]"),
        messageBody: document.querySelector("#messageBody"),
        messageSend: document.querySelector("[data-message-send]"),
        reviewEligible: document.querySelector("[data-review-eligible]"),
        reviewEligibleEmpty: document.querySelector("[data-review-eligible-empty]"),
        reviewEligibleCount: document.querySelector("[data-review-eligible-count]"),
        reviewMine: document.querySelector("[data-review-mine]"),
        reviewReceived: document.querySelector("[data-review-received]"),
        reviewAverage: document.querySelector("[data-review-average]"),
        reviewAlert: document.querySelector("[data-review-alert]"),
        reviewAlertText: document.querySelector("[data-review-alert-text]"),
        reportForm: document.querySelector("[data-report-form]"),
        reportAlert: document.querySelector("[data-report-alert]"),
        reportAlertText: document.querySelector("[data-report-alert-text]"),
        notificationList: document.querySelector("[data-notification-list]"),
        notificationEmpty: document.querySelector("[data-notification-empty]"),
        notificationCount: document.querySelector("[data-notification-count]"),
        notificationPage: document.querySelector("[data-notification-page]"),
        notificationPagination: document.querySelector("[data-notification-pagination]"),
        notificationPrev: document.querySelector("[data-notification-prev]"),
        notificationNext: document.querySelector("[data-notification-next]"),
        notificationAlert: document.querySelector("[data-notification-alert]"),
        notificationAlertText: document.querySelector("[data-notification-alert-text]"),
        adminUserList: document.querySelector("[data-admin-user-list]"),
        adminUserCount: document.querySelector("[data-admin-user-count]"),
        adminUserRole: document.querySelector("[data-admin-user-role]"),
        adminUserSearch: document.querySelector("[data-admin-user-search]"),
        adminUserAlert: document.querySelector("[data-admin-user-alert]"),
        adminUserAlertText: document.querySelector("[data-admin-user-alert-text]"),
        adminUserPagination: document.querySelector("[data-admin-user-pagination]"),
        adminUserPage: document.querySelector("[data-admin-user-page]"),
        adminUserPrev: document.querySelector("[data-admin-user-prev]"),
        adminUserNext: document.querySelector("[data-admin-user-next]"),
        adminCampaignList: document.querySelector("[data-admin-campaign-list]"),
        adminCampaignEmpty: document.querySelector("[data-admin-campaign-empty]"),
        adminCampaignCount: document.querySelector("[data-admin-campaign-count]"),
        adminCampaignStatus: document.querySelector("[data-admin-campaign-status]"),
        adminCampaignSearch: document.querySelector("[data-admin-campaign-search]"),
        adminCampaignAlert: document.querySelector("[data-admin-campaign-alert]"),
        adminCampaignAlertText: document.querySelector("[data-admin-campaign-alert-text]"),
        adminCampaignPagination: document.querySelector("[data-admin-campaign-pagination]"),
        adminCampaignPage: document.querySelector("[data-admin-campaign-page]"),
        adminCampaignPrev: document.querySelector("[data-admin-campaign-prev]"),
        adminCampaignNext: document.querySelector("[data-admin-campaign-next]"),
        adminReportList: document.querySelector("[data-admin-report-list]"),
        adminReportEmpty: document.querySelector("[data-admin-report-empty]"),
        adminReportCount: document.querySelector("[data-admin-report-count]"),
        adminReportStatus: document.querySelector("[data-admin-report-status]"),
        adminReportAlert: document.querySelector("[data-admin-report-alert]"),
        adminReportAlertText: document.querySelector("[data-admin-report-alert-text]"),
        adminReportPagination: document.querySelector("[data-admin-report-pagination]"),
        adminReportPage: document.querySelector("[data-admin-report-page]"),
        adminReportPrev: document.querySelector("[data-admin-report-prev]"),
        adminReportNext: document.querySelector("[data-admin-report-next]"),
        adminReviewList: document.querySelector("[data-admin-review-list]"),
        adminReviewEmpty: document.querySelector("[data-admin-review-empty]"),
        adminReviewCount: document.querySelector("[data-admin-review-count]"),
        adminReviewStatus: document.querySelector("[data-admin-review-status]"),
        adminReviewAlert: document.querySelector("[data-admin-review-alert]"),
        adminReviewAlertText: document.querySelector("[data-admin-review-alert-text]"),
        adminReviewPagination: document.querySelector("[data-admin-review-pagination]"),
        adminReviewPage: document.querySelector("[data-admin-review-page]"),
        adminReviewPrev: document.querySelector("[data-admin-review-prev]"),
        adminReviewNext: document.querySelector("[data-admin-review-next]"),
        adminAuditList: document.querySelector("[data-admin-audit-list]"),
        adminAuditCount: document.querySelector("[data-admin-audit-count]"),
        adminAuditAlert: document.querySelector("[data-admin-audit-alert]"),
        adminAuditAlertText: document.querySelector("[data-admin-audit-alert-text]"),
        adminAuditPagination: document.querySelector("[data-admin-audit-pagination]"),
        adminAuditPage: document.querySelector("[data-admin-audit-page]"),
        adminAuditPrev: document.querySelector("[data-admin-audit-prev]"),
        adminAuditNext: document.querySelector("[data-admin-audit-next]"),
        statusDialog: document.querySelector("[data-status-dialog]"),
        statusForm: document.querySelector("[data-status-form]"),
        statusDialogNote: document.querySelector("[data-status-dialog-note]"),
        statusDialogAlert: document.querySelector("[data-status-dialog-alert]"),
        statusDialogAlertText: document.querySelector("[data-status-dialog-alert-text]"),
        statusDialogSave: document.querySelector("[data-status-dialog-save]")
    };

    const dateFormatter = new Intl.DateTimeFormat(undefined, {
        day: "2-digit", month: "short", year: "numeric"
    });

    function text(value, fallback) {
        return value === null || value === undefined || value === "" ? (fallback || "Not set") : value;
    }

    function initials(name) {
        return (name || "CN").split(/\s+/).filter(Boolean).slice(0, 2)
            .map((part) => part.charAt(0).toUpperCase()).join("") || "CN";
    }

    function formatDate(value) {
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? "Unknown" : dateFormatter.format(date);
    }

    function metaList(entries) {
        const list = document.createElement("dl");
        list.className = "campaign-meta";
        entries.forEach(([term, value]) => {
            const group = document.createElement("div");
            const dt = document.createElement("dt");
            dt.textContent = term;
            const dd = document.createElement("dd");
            dd.textContent = value;
            group.append(dt, dd);
            list.appendChild(group);
        });
        return list;
    }

    function formatMoneyAmount(value, currency) {
        if (value === null || value === undefined || value === "") {
            return "Not stated";
        }
        try {
            return new Intl.NumberFormat(undefined, {
                style: "currency", currency: currency || "USD", maximumFractionDigits: 0
            }).format(Number(value));
        } catch (error) {
            return String(value);
        }
    }

    function titleCase(value) {
        return String(value || "").toLowerCase().replace(/(^|[_\s])([a-z])/g, (m, p1, p2) => p1 + p2.toUpperCase())
            .replace(/_/g, " ");
    }

    function roleName() {
        return state.session.user.role;
    }

    function profile() {
        return roleName() === "BRAND" ? state.session.brand : state.session.creator;
    }

    function showAppAlert(message) {
        if (!els.appAlert) {
            return;
        }
        els.appAlert.hidden = false;
        els.appAlert.querySelector("[data-app-alert-text]").textContent = message;
    }

    function navItems() {
        const role = roleName();
        if (role === "ADMIN") {
            return [
                {id: "overview", label: "Overview", kicker: "Platform overview"},
                {id: "admin-users", label: "Users", kicker: "Account administration"},
                {id: "admin-campaigns", label: "Campaigns", kicker: "Campaign oversight"},
                {id: "admin-reports", label: "Reports & disputes", kicker: "Moderation queue"},
                {id: "admin-reviews", label: "Reviews", kicker: "Review moderation"},
                {id: "admin-audit", label: "Audit log", kicker: "Administrative history"},
                {id: "analytics", label: "Analytics", kicker: "Platform analytics"},
                {id: "notifications", label: "Notifications", kicker: "Recent activity"},
                {id: "security", label: "Account security", kicker: "Account"}
            ];
        }
        return [
            {id: "overview", label: "Overview", kicker: role === "BRAND" ? "Brand workspace" : "Creator workspace"},
            ...(role === "BRAND"
                ? [{id: "campaigns", label: "Campaigns", kicker: "Campaign workspace"}]
                : []),
            {id: "applications", label: role === "BRAND" ? "Applications" : "My applications",
                kicker: role === "BRAND" ? "Application review" : "Application history"},
            {id: "collaborations", label: "Collaborations", kicker: "Active partnerships"},
            {id: "deliverables", label: "Deliverables", kicker: "Deadlines and submissions"},
            {id: "contracts", label: "Contracts", kicker: "Terms and acknowledgement"},
            {id: "payments", label: role === "BRAND" ? "Payments" : "Earnings",
                kicker: role === "BRAND" ? "Payment records" : "Recorded earnings"},
            {id: "analytics", label: "Analytics", kicker: "Performance metrics"},
            {id: "messages", label: "Messages", kicker: "Collaboration conversations"},
            {id: "reviews", label: "Reviews", kicker: "Ratings and reports"},
            {id: "notifications", label: "Notifications", kicker: "Recent activity"},
            {id: "profile", label: role === "BRAND" ? "Company profile" : "Creator profile", kicker: "Profile"},
            {id: "security", label: "Account security", kicker: "Security"}
        ];
    }

    function renderNavigation() {
        els.nav.innerHTML = "";
        navItems().forEach((item) => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "side-link" + (item.id === state.view ? " is-active" : "");
            button.dataset.viewTarget = item.id;

            const icon = document.createElement("span");
            icon.className = "side-icon";
            icon.setAttribute("aria-hidden", "true");
            icon.innerHTML = iconFor(item.id);
            const label = document.createElement("span");
            label.textContent = item.label;
            button.append(icon, label);
            button.addEventListener("click", () => showView(item.id));
            els.nav.appendChild(button);
        });

        const marketplace = document.createElement("a");
        marketplace.className = "side-link side-link-muted";
        marketplace.href = role === "BRAND" ? "/creators.html" : "/campaigns.html";
        marketplace.innerHTML = '<span class="side-icon" aria-hidden="true">'
            + '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M4 7h16M4 12h10M4 17h7"/></svg>'
            + '</span>';
        const marketplaceLabel = document.createElement("span");
        marketplaceLabel.textContent = role === "BRAND" ? "Discover creators" : "Discover campaigns";
        marketplace.appendChild(marketplaceLabel);
        els.nav.appendChild(marketplace);
    }

    function iconFor(id) {
        if (id === "campaigns") {
            return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M4 6h16v12H4z"/><path d="M8 10h8M8 14h5"/></svg>';
        }
        if (id === "applications") {
            return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M7 3h7l4 4v14H7z"/><path d="M14 3v4h4M10 12h5M10 16h5"/></svg>';
        }
        if (id === "collaborations") {
            return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M8 12l3-3a3 3 0 014 0l1 1"/><path d="M16 12l-3 3a3 3 0 01-4 0l-1-1"/><path d="M5 8l-2 2 2 2M19 8l2 2-2 2"/></svg>';
        }
        if (id === "profile") {
            return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><circle cx="12" cy="8" r="3.2"/><path d="M5 20c0-3.6 3.1-6 7-6s7 2.4 7 6"/></svg>';
        }
        if (id === "security") {
            return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M12 3l7 3v6c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6l7-3z"/><path d="M9.5 12l1.8 1.8 3.4-3.6"/></svg>';
        }
        if (id === "deliverables") {
            return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M5 4h9l5 5v11H5z"/><path d="M14 4v5h5M8 13h8M8 17h5"/></svg>';
        }
        if (id === "contracts") {
            return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M7 3h10v18H7z"/><path d="M10 8h4M10 12h4M10 16h2"/></svg>';
        }
        if (id === "payments") {
            return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><rect x="3" y="6" width="18" height="12" rx="2"/><path d="M3 10h18M7 15h3"/></svg>';
        }
        if (id === "analytics") {
            return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/></svg>';
        }
        if (id === "messages") {
            return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M4 5h16v11H9l-5 4z"/><path d="M8 9h8M8 12h5"/></svg>';
        }
        if (id === "reviews") {
            return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M12 3l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.4 6.8 19l1-5.8L3.5 9.2l5.9-.9z"/></svg>';
        }
        if (id === "notifications") {
            return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M6 9a6 6 0 0112 0c0 5 2 6 2 6H4s2-1 2-6z"/><path d="M10 19a2 2 0 004 0"/></svg>';
        }
        if (id === "admin-users") {
            return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><circle cx="9" cy="8" r="3"/><path d="M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6"/><path d="M16 5a3 3 0 010 6M18 14c2 1 3 3 3 6"/></svg>';
        }
        if (id === "admin-campaigns") {
            return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M4 6h16v12H4z"/><path d="M8 10h8M8 14h5"/><path d="M9 20h6"/></svg>';
        }
        if (id === "admin-reports") {
            return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M12 3l9 16H3z"/><path d="M12 9v4M12 16h.01"/></svg>';
        }
        if (id === "admin-reviews") {
            return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M12 3l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.4 6.8 19l1-5.8L3.5 9.2l5.9-.9z"/><path d="M8 21h8"/></svg>';
        }
        if (id === "admin-audit") {
            return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M6 4h12v16H6z"/><path d="M9 8h6M9 12h6M9 16h3"/><circle cx="17" cy="17" r="3"/></svg>';
        }
        return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><rect x="4" y="4" width="7" height="7" rx="1.4"/><rect x="13" y="4" width="7" height="7" rx="1.4"/><rect x="4" y="13" width="7" height="7" rx="1.4"/><rect x="13" y="13" width="7" height="7" rx="1.4"/></svg>';
    }

    function showView(id) {
        state.view = id;
        document.querySelectorAll("[data-view]").forEach((section) => {
            section.hidden = section.getAttribute("data-view") !== id;
        });
        const item = navItems().find((entry) => entry.id === id);
        if (item) {
            els.pageTitle.textContent = item.label;
            els.pageKicker.textContent = item.kicker;
        }
        els.nav.querySelectorAll("[data-view-target]").forEach((button) => {
            button.classList.toggle("is-active", button.dataset.viewTarget === id);
        });
        closeSidebar();
        if (id === "profile") {
            fillProfileForm();
        }
        if (id === "campaigns") {
            loadCampaigns();
        }
        if (id === "applications") {
            loadApplications();
        }
        if (id === "collaborations") {
            loadCollaborations();
        }
        if (id === "deliverables") {
            loadDeliverables();
        }
        if (id === "contracts") {
            loadContracts();
        }
        if (id === "payments") {
            loadPayments();
        }
        if (id === "analytics") {
            loadAnalytics();
        }
        if (id === "messages") {
            loadConversations();
        }
        if (id === "reviews") {
            loadReviews();
        }
        if (id === "notifications") {
            loadNotifications();
        }
        if (id === "admin-users") {
            loadAdminUsers();
        }
        if (id === "admin-campaigns") {
            loadAdminCampaigns();
        }
        if (id === "admin-reports") {
            loadAdminReports();
        }
        if (id === "admin-reviews") {
            loadAdminReviews();
        }
        if (id === "admin-audit") {
            loadAdminAudit();
        }
    }

    function closeSidebar() {
        els.sidebar.classList.remove("is-open");
        const toggle = document.querySelector("[data-sidebar-toggle]");
        if (toggle) {
            toggle.setAttribute("aria-expanded", "false");
        }
    }

    function greeting() {
        const hour = new Date().getHours();
        const part = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
        const first = (state.session.user.fullName || "there").split(" ")[0];
        return `${part}, ${first}.`;
    }

    function completeness() {
        const role = roleName();
        const data = profile() || {};
        let fields;
        if (role === "BRAND") {
            fields = [
                ["Company name", data.companyName],
                ["Primary contact", state.session.user.fullName],
                ["Company website", data.website],
                ["Industry", data.industry],
                ["Country", data.country],
                ["Company description", data.description],
                ["Logo URL", data.logoUrl]
            ];
        } else if (role === "CREATOR") {
            fields = [
                ["Creator name", data.displayName],
                ["Headline", data.headline],
                ["Biography", data.bio],
                ["Primary niche", data.niche],
                ["Primary platform", data.primaryPlatform],
                ["Country", data.country],
                ["Languages", data.languages],
                ["Content formats", data.contentFormats],
                ["Indicative rate", data.baseRate],
                ["Portfolio URL", data.portfolioUrl]
            ];
        } else {
            fields = [["Administrator account", state.session.user.email]];
        }
        const complete = fields.filter((entry) => entry[1] !== null && entry[1] !== undefined && entry[1] !== "");
        return {
            fields,
            complete: complete.length,
            total: fields.length,
            percent: Math.round((complete.length / fields.length) * 100)
        };
    }

    function renderAccount() {
        const user = state.session.user;
        els.userName.textContent = user.fullName;
        els.userEmail.textContent = user.email;
        els.avatar.textContent = initials(user.fullName);
        els.sideRole.textContent = roleName() === "ADMIN" ? "Administration"
            : roleName() === "BRAND" ? "Brand workspace" : "Creator workspace";
        els.roleBadge.textContent = titleCase(roleName());
        els.roleEyebrow.textContent = roleName() === "ADMIN" ? "Platform administration"
            : roleName() === "BRAND" ? "Brand workspace" : "Creator workspace";

        els.statusPill.innerHTML = "";
        const dot = document.createElement("span");
        dot.className = "status-dot " + (user.status === "ACTIVE" ? "is-up" : "is-down");
        dot.setAttribute("aria-hidden", "true");
        els.statusPill.append(dot, document.createTextNode(
            user.status === "ACTIVE" ? "Account active" : "Account suspended"));
    }

    function renderOverview() {
        const role = roleName();
        els.greeting.textContent = greeting();
        if (role === "BRAND") {
            els.heroCopy.textContent = "Your company profile anchors every campaign brief. Complete it once and reuse it across your marketplace activity.";
        } else if (role === "CREATOR") {
            els.heroCopy.textContent = "A clear creator profile helps brands understand your niche, formats, availability, and indicative rates before they reach out.";
        } else {
            els.heroCopy.textContent = "You are signed in to platform administration. Account data is read-only in this phase and no moderation actions are simulated.";
        }

        const progress = completeness();
        els.completionRing.style.setProperty("--progress", progress.percent);
        els.completionValue.textContent = progress.percent + "%";
        els.completionNote.textContent = progress.percent === 100
            ? "Every profile field is complete."
            : `${progress.complete} of ${progress.total} fields complete. Add the remaining details.`;
        els.profileComplete.textContent = progress.percent + "% complete";

        renderStats(progress);
        renderAccountDetails();
        renderChecklist(progress);
        els.adminPanel.hidden = role !== "ADMIN";
        if (role === "ADMIN") {
            loadAdminData();
        } else {
            loadActivityCounts();
        }
    }

    function statCard(label, value, note, key) {
        const card = document.createElement("article");
        card.className = "stat-card";
        if (key) {
            card.dataset.stat = key;
        }
        const labelEl = document.createElement("p");
        labelEl.className = "stat-label";
        labelEl.textContent = label;
        const valueEl = document.createElement("strong");
        valueEl.className = "stat-value";
        valueEl.textContent = value;
        const noteEl = document.createElement("span");
        noteEl.className = "stat-note";
        noteEl.textContent = note;
        card.append(labelEl, valueEl, noteEl);
        return card;
    }

    function updateStat(key, value) {
        const el = els.statGrid.querySelector("[data-stat='" + key + "'] .stat-value");
        if (el) {
            el.textContent = String(value);
        }
    }

    async function loadActivityCounts() {
        const role = roleName();
        if (role === "ADMIN") {
            return;
        }
        const applicationsEndpoint = role === "BRAND"
            ? "/api/brand/applications?size=1"
            : "/api/creator/applications?size=1";
        try {
            const [applications, collaborations] = await Promise.all([
                api(applicationsEndpoint),
                api("/api/collaborations?size=1")
            ]);
            updateStat("applications", applications.data.totalElements);
            updateStat("collaborations", collaborations.data.totalElements);
        } catch (error) {
            updateStat("applications", "—");
            updateStat("collaborations", "—");
        }
    }

    function renderStats(progress) {
        els.statGrid.innerHTML = "";
        const role = roleName();
        const data = profile() || {};
        if (role === "ADMIN") {
            els.statGrid.append(
                statCard("Total accounts", "…", "Loading"),
                statCard("Brands", "…", "Loading"),
                statCard("Creators", "…", "Loading"),
                statCard("Suspended", "…", "Loading")
            );
            return;
        }
        if (role === "BRAND") {
            els.statGrid.append(
                statCard("Profile completeness", progress.percent + "%", `${progress.complete} of ${progress.total} fields`),
                statCard("Applications", "…", "Loading", "applications"),
                statCard("Collaborations", "…", "Loading", "collaborations"),
                statCard("Account status", "Active", "Verified by the server"),
                statCard("Company website", data.website ? "Added" : "Pending", data.website ? safeHost(data.website) : "Not provided"),
                statCard("Member since", formatDate(state.session.user.createdAt), "Account creation date")
            );
            return;
        }
        const rate = data.baseRate
            ? new Intl.NumberFormat(undefined, {style: "currency", currency: data.currency || "USD", maximumFractionDigits: 0}).format(data.baseRate)
            : "Not set";
        els.statGrid.append(
            statCard("Profile completeness", progress.percent + "%", `${progress.complete} of ${progress.total} fields`),
            statCard("Applications", "…", "Loading", "applications"),
            statCard("Collaborations", "…", "Loading", "collaborations"),
            statCard("Availability", titleCase(data.availability || "UNAVAILABLE"), "Set by you"),
            statCard("Verification", data.verificationStatus === "IDENTITY_VERIFIED" ? "Identity verified" : "Unverified", "Not a platform endorsement"),
            statCard("Indicative rate", rate, data.currency || "USD")
        );
    }

    function safeHost(url) {
        try {
            return new URL(url).host;
        } catch (error) {
            return url;
        }
    }

    function detailRow(term, value) {
        const row = document.createElement("div");
        row.className = "detail-row";
        const dt = document.createElement("dt");
        dt.textContent = term;
        const dd = document.createElement("dd");
        dd.textContent = value;
        row.append(dt, dd);
        return row;
    }

    function renderAccountDetails() {
        const user = state.session.user;
        els.accountDetails.innerHTML = "";
        els.accountDetails.append(
            detailRow("Email address", user.email),
            detailRow("Account role", titleCase(user.role)),
            detailRow("Account status", titleCase(user.status)),
            detailRow("Member since", formatDate(user.createdAt)),
            detailRow("Profile reference", profile() && profile().id ? "CN-" + profile().id : "Not applicable")
        );

        if (els.securityDetails) {
            els.securityDetails.innerHTML = "";
            els.securityDetails.append(
                detailRow("Email address", user.email),
                detailRow("Session", "Server-side, HTTP-only cookie"),
                detailRow("CSRF protection", "Enabled for state-changing requests"),
                detailRow("Password storage", "BCrypt hash, never returned by the API"),
                detailRow("Account status", titleCase(user.status))
            );
        }
    }

    function renderChecklist(progress) {
        els.checklist.innerHTML = "";
        if (roleName() === "ADMIN") {
            const item = document.createElement("li");
            item.textContent = "Administrator accounts cannot be self-registered.";
            els.checklist.appendChild(item);
            return;
        }
        progress.fields.forEach(([label, value]) => {
            const done = value !== null && value !== undefined && value !== "";
            const item = document.createElement("li");
            item.className = done ? "is-done" : "is-pending";
            const marker = document.createElement("span");
            marker.className = "check-marker";
            marker.setAttribute("aria-hidden", "true");
            marker.textContent = done ? "✓" : "";
            const copy = document.createElement("span");
            copy.textContent = done ? label + " added" : "Add " + label.toLowerCase();
            item.append(marker, copy);
            els.checklist.appendChild(item);
        });
    }

    function fillProfileForm() {
        const role = roleName();
        const form = els.profileForm;
        if (!form || role === "ADMIN") {
            return;
        }
        const data = profile() || {};
        form.querySelectorAll("[data-profile-group]").forEach((group) => {
            group.hidden = group.getAttribute("data-profile-group") !== role.toLowerCase();
        });
        els.profileTitle.textContent = role === "BRAND" ? "Company profile" : "Creator profile";
        els.profileSubtitle.textContent = role === "BRAND"
            ? "This information appears on your campaign briefs and company record."
            : "This information powers creator discovery and appears on your public preview.";

        const set = (name, value) => {
            const field = form.querySelector("[name='" + name + "']");
            if (field) {
                field.value = value === null || value === undefined ? "" : value;
            }
        };

        if (role === "BRAND") {
            set("companyName", data.companyName);
            set("contactName", state.session.user.fullName);
            set("companyWebsite", data.website);
            set("industry", data.industry);
            set("country", data.country);
            set("description", data.description);
            set("logoUrl", data.logoUrl);
        } else {
            set("displayName", data.displayName);
            set("headline", data.headline);
            set("bio", data.bio);
            set("niche", data.niche);
            set("primaryPlatform", data.primaryPlatform);
            set("creatorCountry", data.country);
            set("languages", data.languages);
            set("contentFormats", data.contentFormats);
            set("baseRate", data.baseRate);
            set("currency", data.currency || "USD");
            set("availability", data.availability || "AVAILABLE");
            set("portfolioUrl", data.portfolioUrl);
        }
        clearProfileAlert();
    }

    function clearProfileAlert() {
        if (els.profileAlert) {
            els.profileAlert.hidden = true;
        }
    }

    function profilePayload() {
        const form = els.profileForm;
        const value = (name) => {
            const field = form.querySelector("[name='" + name + "']");
            return field && field.value.trim() !== "" ? field.value.trim() : null;
        };
        if (roleName() === "BRAND") {
            return {
                companyName: value("companyName"),
                contactName: value("contactName"),
                companyWebsite: value("companyWebsite"),
                industry: value("industry"),
                country: value("country"),
                description: value("description"),
                logoUrl: value("logoUrl")
            };
        }
        const baseRate = value("baseRate");
        return {
            displayName: value("displayName"),
            headline: value("headline"),
            bio: value("bio"),
            niche: value("niche"),
            primaryPlatform: value("primaryPlatform"),
            country: value("creatorCountry"),
            languages: value("languages"),
            contentFormats: value("contentFormats"),
            baseRate: baseRate === null ? null : Number(baseRate),
            currency: value("currency"),
            availability: value("availability"),
            portfolioUrl: value("portfolioUrl")
        };
    }

    function setupProfileForm() {
        const form = els.profileForm;
        if (!form) {
            return;
        }

        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            clearProfileAlert();
            if (window.CollabNexus && window.CollabNexus.validateForm
                && !window.CollabNexus.validateForm(form, "profile")) {
                toast("Please review the highlighted fields.");
                return;
            }
            const submit = form.querySelector("[data-profile-submit]");
            submit.disabled = true;
            submit.textContent = "Saving...";
            try {
                const endpoint = roleName() === "BRAND" ? "/api/brand/profile" : "/api/creator/profile";
                const response = await api(endpoint, {method: "PUT", body: JSON.stringify(profilePayload())});
                if (roleName() === "BRAND") {
                    state.session.brand = response.data;
                } else {
                    state.session.creator = response.data;
                    state.session.user.fullName = response.data.displayName;
                }
                const me = await api("/api/auth/me");
                state.session = me.data;
                renderAccount();
                renderOverview();
                fillProfileForm();
                showProfileSuccess("Profile saved. Your workspace is up to date.");
                toast("Profile saved.");
            } catch (error) {
                showProfileError(error.message);
                toast("Profile could not be saved.");
            } finally {
                submit.disabled = false;
                submit.textContent = "Save profile";
            }
        });

        const reset = form.querySelector("[data-profile-reset]");
        if (reset) {
            reset.addEventListener("click", () => {
                fillProfileForm();
                toast("Unsaved changes reset.");
            });
        }
    }

    function showProfileSuccess(message) {
        els.profileAlert.hidden = false;
        els.profileAlert.className = "form-alert is-success";
        els.profileAlert.querySelector("[data-profile-alert-text]").textContent = message;
    }

    function showProfileError(message) {
        els.profileAlert.hidden = false;
        els.profileAlert.className = "form-alert is-error";
        els.profileAlert.querySelector("[data-profile-alert-text]").textContent = message;
    }

    async function loadAdminData() {
        try {
            const [overview, users] = await Promise.all([
                api("/api/admin/overview"),
                api("/api/admin/users?size=8")
            ]);
            els.statGrid.innerHTML = "";
            els.statGrid.append(
                statCard("Total accounts", String(overview.data.totalUsers), "Registered users"),
                statCard("Brands", String(overview.data.brands), "Brand workspaces"),
                statCard("Creators", String(overview.data.creators), "Creator profiles"),
                statCard("Suspended", String(overview.data.suspended), "Restricted accounts")
            );
            els.adminCount.textContent = users.data.totalElements + (users.data.totalElements === 1 ? " account" : " accounts");
            els.adminUsers.innerHTML = "";
            users.data.content.forEach((user) => {
                const row = document.createElement("tr");
                const account = document.createElement("td");
                const name = document.createElement("strong");
                name.textContent = user.fullName;
                const email = document.createElement("span");
                email.className = "table-sub";
                email.textContent = user.email;
                const wrapper = document.createElement("span");
                wrapper.className = "table-stack";
                wrapper.append(name, email);
                account.appendChild(wrapper);

                const role = document.createElement("td");
                const roleBadge = document.createElement("span");
                roleBadge.className = "badge badge-soft";
                roleBadge.textContent = titleCase(user.role);
                role.appendChild(roleBadge);

                const status = document.createElement("td");
                const statusBadge = document.createElement("span");
                statusBadge.className = "badge " + (user.status === "ACTIVE" ? "badge-success" : "badge-danger");
                statusBadge.textContent = titleCase(user.status);
                status.appendChild(statusBadge);

                const joined = document.createElement("td");
                joined.textContent = formatDate(user.createdAt);

                row.append(account, role, status, joined);
                els.adminUsers.appendChild(row);
            });
        } catch (error) {
            showAppAlert("Administrator data could not be loaded: " + error.message);
        }
    }

    function applicationBadge(status) {
        const badge = document.createElement("span");
        badge.className = status === "ACCEPTED"
            ? "badge badge-success"
            : status === "REJECTED" || status === "WITHDRAWN"
                ? "badge badge-danger"
                : "badge badge-soft";
        badge.textContent = titleCase(status);
        return badge;
    }

    function collaborationBadge(status) {
        const badge = document.createElement("span");
        badge.className = status === "COMPLETED" || status === "ACTIVE"
            ? "badge badge-success"
            : status === "CANCELLED"
                ? "badge badge-danger"
                : "badge badge-soft";
        badge.textContent = titleCase(status);
        return badge;
    }

    function externalLink(url, label) {
        const anchor = document.createElement("a");
        anchor.href = url;
        anchor.textContent = label;
        anchor.className = "text-link";
        anchor.target = "_blank";
        anchor.rel = "noopener noreferrer";
        return anchor;
    }

    function showBrandAppAlert(message) {
        if (!els.brandAppAlert) {
            return;
        }
        els.brandAppAlert.hidden = false;
        els.brandAppAlertText.textContent = message;
    }

    function clearBrandAppAlert() {
        if (els.brandAppAlert) {
            els.brandAppAlert.hidden = true;
        }
    }

    function showCreatorAppAlert(message) {
        if (!els.creatorAppAlert) {
            return;
        }
        els.creatorAppAlert.hidden = false;
        els.creatorAppAlertText.textContent = message;
    }

    function clearCreatorAppAlert() {
        if (els.creatorAppAlert) {
            els.creatorAppAlert.hidden = true;
        }
    }

    function showCollabAlert(message) {
        if (!els.collabAlert) {
            return;
        }
        els.collabAlert.hidden = false;
        els.collabAlertText.textContent = message;
    }

    function clearCollabAlert() {
        if (els.collabAlert) {
            els.collabAlert.hidden = true;
        }
    }

    function brandApplicationCard(application) {
        const card = document.createElement("article");
        card.className = "application-card";

        const head = document.createElement("div");
        head.className = "application-head";
        const copy = document.createElement("div");
        const title = document.createElement("h4");
        title.textContent = application.creatorName;
        const sub = document.createElement("p");
        sub.textContent = [application.creatorHeadline, application.creatorNiche]
            .filter(Boolean).join(" · ") || "Creator profile";
        copy.append(title, sub);
        head.append(copy, applicationBadge(application.status));

        const proposal = document.createElement("p");
        proposal.className = "application-proposal";
        proposal.textContent = application.proposal;

        const facts = metaList([
            ["Campaign", application.campaignTitle],
            ["Proposed price", formatMoneyAmount(application.proposedPrice, application.currency)],
            ["Available from", application.availableFrom ? formatDate(application.availableFrom) : "Not stated"],
            ["Platform", application.creatorPrimaryPlatform || "Not stated"],
            ["Submitted", formatDate(application.createdAt)]
        ]);

        const foot = document.createElement("div");
        foot.className = "application-foot";
        const availability = document.createElement("span");
        availability.className = "badge badge-soft";
        availability.textContent = "Availability: " + titleCase(application.creatorAvailability || "UNKNOWN");
        foot.appendChild(availability);
        if (application.portfolioUrl) {
            foot.appendChild(externalLink(application.portfolioUrl, "View portfolio"));
        }

        const actions = document.createElement("div");
        actions.className = "application-actions";
        if (application.status === "PENDING") {
            actions.appendChild(actionButton("Shortlist", "button button-ghost button-sm",
                () => brandApplicationAction(application, "shortlist", "Application shortlisted.")));
        }
        if (application.status === "PENDING" || application.status === "SHORTLISTED") {
            actions.appendChild(actionButton("Accept", "button button-navy button-sm",
                () => brandApplicationAction(application, "accept",
                    "Application accepted. A collaboration has been created.")));
            actions.appendChild(actionButton("Reject", "button button-ghost button-sm is-danger",
                () => brandApplicationAction(application, "reject", "Application rejected.")));
        }

        card.append(head, proposal, facts, foot, actions);
        return card;
    }

    function creatorApplicationCard(application) {
        const card = document.createElement("article");
        card.className = "application-card";

        const head = document.createElement("div");
        head.className = "application-head";
        const copy = document.createElement("div");
        const title = document.createElement("h4");
        title.textContent = application.campaignTitle;
        const sub = document.createElement("p");
        sub.textContent = application.brandName;
        copy.append(title, sub);
        head.append(copy, applicationBadge(application.status));

        const proposal = document.createElement("p");
        proposal.className = "application-proposal";
        proposal.textContent = application.proposal;

        const facts = metaList([
            ["Proposed price", formatMoneyAmount(application.proposedPrice, application.currency)],
            ["Available from", application.availableFrom ? formatDate(application.availableFrom) : "Not stated"],
            ["Application deadline", formatDate(application.campaignApplicationDeadline)],
            ["Submitted", formatDate(application.createdAt)]
        ]);

        const foot = document.createElement("div");
        foot.className = "application-foot";
        const niche = document.createElement("span");
        niche.className = "badge badge-soft";
        niche.textContent = application.creatorNiche || "Creator";
        foot.appendChild(niche);
        if (application.portfolioUrl) {
            foot.appendChild(externalLink(application.portfolioUrl, "Your portfolio"));
        }

        const actions = document.createElement("div");
        actions.className = "application-actions";
        if (application.status === "PENDING" || application.status === "SHORTLISTED") {
            actions.appendChild(actionButton("Withdraw", "button button-ghost button-sm is-danger",
                () => withdrawApplication(application)));
        }

        card.append(head, proposal, facts, foot, actions);
        return card;
    }

    function collaborationCard(collaboration) {
        const card = document.createElement("article");
        card.className = "collaboration-card";

        const head = document.createElement("div");
        head.className = "application-head";
        const copy = document.createElement("div");
        const title = document.createElement("h4");
        title.textContent = collaboration.campaignTitle;
        const sub = document.createElement("p");
        sub.textContent = "Campaign collaboration";
        copy.append(title, sub);
        head.append(copy, collaborationBadge(collaboration.status));

        const counterpart = roleName() === "BRAND"
            ? collaboration.creatorName
            : collaboration.brandName;

        const facts = metaList([
            [roleName() === "BRAND" ? "Creator" : "Brand", counterpart],
            ["Agreed amount", formatMoneyAmount(collaboration.agreedAmount, collaboration.currency)],
            ["Campaign window", formatDate(collaboration.campaignStartDate) + " – "
                + formatDate(collaboration.campaignEndDate)],
            ["Niche", collaboration.creatorNiche || "Not stated"],
            ["Updated", formatDate(collaboration.updatedAt)]
        ]);

        const actions = document.createElement("div");
        actions.className = "application-actions";
        if (collaboration.status === "ACTIVE") {
            actions.appendChild(actionButton("Start work", "button button-navy button-sm",
                () => collaborationAction(collaboration, "start", "Collaboration moved into progress.")));
        }
        if (collaboration.status === "IN_PROGRESS") {
            actions.appendChild(actionButton("Mark complete", "button button-navy button-sm",
                () => collaborationAction(collaboration, "complete", "Collaboration marked complete.")));
        }
        if (collaboration.status === "ACTIVE" || collaboration.status === "IN_PROGRESS") {
            actions.appendChild(actionButton("Cancel", "button button-ghost button-sm is-danger",
                () => collaborationAction(collaboration, "cancel", "Collaboration cancelled.")));
        }

        const note = document.createElement("p");
        note.className = "fine-line";
        note.textContent = "Contract terms, deliverables, and payment records are managed from this collaboration.";

        card.append(head, facts, actions, note);
        return card;
    }

    function loadApplications() {
        const role = roleName();
        if (!els.brandApplicationsPanel) {
            return;
        }
        els.brandApplicationsPanel.hidden = role !== "BRAND";
        els.creatorApplicationsPanel.hidden = role !== "CREATOR";
        if (role === "BRAND") {
            loadBrandApplications();
        } else if (role === "CREATOR") {
            loadCreatorApplications();
        }
    }

    async function loadBrandApplications() {
        const view = state.applications.brand;
        if (!els.brandAppList || view.loading) {
            return;
        }
        view.loading = true;
        clearBrandAppAlert();
        els.brandAppList.innerHTML = "";
        try {
            const params = new URLSearchParams({page: String(view.page), size: "6"});
            if (view.status) {
                params.set("status", view.status);
            }
            if (view.q) {
                params.set("q", view.q);
            }
            if (view.campaignId) {
                params.set("campaignId", String(view.campaignId));
            }
            const response = await api("/api/brand/applications?" + params.toString());
            const pageData = response.data;
            view.totalPages = Math.max(1, pageData.totalPages);
            pageData.content.forEach((application) =>
                els.brandAppList.appendChild(brandApplicationCard(application)));
            els.brandAppEmpty.hidden = pageData.content.length !== 0;
            els.brandAppCount.textContent = pageData.totalElements
                + (pageData.totalElements === 1 ? " application" : " applications");
            els.brandAppPagination.hidden = view.totalPages <= 1;
            els.brandAppPage.textContent = "Page " + (view.page + 1) + " of " + view.totalPages;
            els.brandAppPrev.disabled = view.page <= 0;
            els.brandAppNext.disabled = view.page >= view.totalPages - 1;
            els.brandAppCampaignFilter.hidden = !view.campaignId;
            if (view.campaignId) {
                els.brandAppCampaignTitle.textContent = view.campaignTitle || "Selected campaign";
            }
        } catch (error) {
            showBrandAppAlert("Applications could not be loaded: " + error.message);
            els.brandAppEmpty.hidden = true;
        } finally {
            view.loading = false;
        }
    }

    async function loadCreatorApplications() {
        const view = state.applications.creator;
        if (!els.creatorAppList || view.loading) {
            return;
        }
        view.loading = true;
        clearCreatorAppAlert();
        els.creatorAppList.innerHTML = "";
        try {
            const params = new URLSearchParams({page: String(view.page), size: "6"});
            if (view.status) {
                params.set("status", view.status);
            }
            if (view.q) {
                params.set("q", view.q);
            }
            const response = await api("/api/creator/applications?" + params.toString());
            const pageData = response.data;
            view.totalPages = Math.max(1, pageData.totalPages);
            pageData.content.forEach((application) =>
                els.creatorAppList.appendChild(creatorApplicationCard(application)));
            els.creatorAppEmpty.hidden = pageData.content.length !== 0;
            els.creatorAppCount.textContent = pageData.totalElements
                + (pageData.totalElements === 1 ? " application" : " applications");
            els.creatorAppPagination.hidden = view.totalPages <= 1;
            els.creatorAppPage.textContent = "Page " + (view.page + 1) + " of " + view.totalPages;
            els.creatorAppPrev.disabled = view.page <= 0;
            els.creatorAppNext.disabled = view.page >= view.totalPages - 1;
        } catch (error) {
            showCreatorAppAlert("Applications could not be loaded: " + error.message);
            els.creatorAppEmpty.hidden = true;
        } finally {
            view.loading = false;
        }
    }

    async function loadCollaborations() {
        const view = state.collaborations;
        if (!els.collabList || view.loading || roleName() === "ADMIN") {
            return;
        }
        view.loading = true;
        clearCollabAlert();
        els.collabList.innerHTML = "";
        try {
            const params = new URLSearchParams({page: String(view.page), size: "6"});
            if (view.status) {
                params.set("status", view.status);
            }
            const response = await api("/api/collaborations?" + params.toString());
            const pageData = response.data;
            view.totalPages = Math.max(1, pageData.totalPages);
            pageData.content.forEach((collaboration) =>
                els.collabList.appendChild(collaborationCard(collaboration)));
            els.collabEmpty.hidden = pageData.content.length !== 0;
            els.collabCount.textContent = pageData.totalElements
                + (pageData.totalElements === 1 ? " collaboration" : " collaborations");
            els.collabPagination.hidden = view.totalPages <= 1;
            els.collabPage.textContent = "Page " + (view.page + 1) + " of " + view.totalPages;
            els.collabPrev.disabled = view.page <= 0;
            els.collabNext.disabled = view.page >= view.totalPages - 1;
        } catch (error) {
            showCollabAlert("Collaborations could not be loaded: " + error.message);
            els.collabEmpty.hidden = true;
        } finally {
            view.loading = false;
        }
    }

    function openApplicationsForCampaign(campaign) {
        const view = state.applications.brand;
        view.campaignId = campaign.id;
        view.campaignTitle = campaign.title;
        view.page = 0;
        showView("applications");
    }

    async function brandApplicationAction(application, action, successMessage) {
        if (action === "accept" && !window.confirm(
            "Accept " + application.creatorName + " for \"" + application.campaignTitle
            + "\"? This creates a collaboration and counts against the campaign capacity.")) {
            return;
        }
        if (action === "reject" && !window.confirm(
            "Reject " + application.creatorName + "'s application for \""
            + application.campaignTitle + "\"?")) {
            return;
        }
        try {
            await api("/api/brand/applications/" + application.id + "/" + action, {method: "POST"});
            toast(successMessage);
            await loadBrandApplications();
            await loadActivityCounts();
            if (state.view === "campaigns") {
                await loadCampaigns();
            }
        } catch (error) {
            showBrandAppAlert(error.message);
            toast("The application action failed.");
        }
    }

    async function withdrawApplication(application) {
        if (!window.confirm("Withdraw your application for \"" + application.campaignTitle + "\"?")) {
            return;
        }
        try {
            await api("/api/creator/applications/" + application.id + "/withdraw", {method: "POST"});
            toast("Application withdrawn.");
            await loadCreatorApplications();
            await loadActivityCounts();
        } catch (error) {
            showCreatorAppAlert(error.message);
            toast("The application could not be withdrawn.");
        }
    }

    async function collaborationAction(collaboration, action, successMessage) {
        const prompt = action === "cancel"
            ? "Cancel the collaboration for \"" + collaboration.campaignTitle + "\"? This cannot be undone."
            : action === "complete"
                ? "Mark the collaboration for \"" + collaboration.campaignTitle + "\" complete?"
                : null;
        if (prompt && !window.confirm(prompt)) {
            return;
        }
        try {
            await api("/api/collaborations/" + collaboration.id + "/" + action, {method: "POST"});
            toast(successMessage);
            await loadCollaborations();
            await loadActivityCounts();
        } catch (error) {
            showCollabAlert(error.message);
            toast("The collaboration action failed.");
        }
    }

    function setupApplications() {
        if (!els.brandAppStatus) {
            return;
        }
        els.brandAppStatus.addEventListener("change", () => {
            state.applications.brand.status = els.brandAppStatus.value;
            state.applications.brand.page = 0;
            loadBrandApplications();
        });
        els.creatorAppStatus.addEventListener("change", () => {
            state.applications.creator.status = els.creatorAppStatus.value;
            state.applications.creator.page = 0;
            loadCreatorApplications();
        });

        let brandTimer = null;
        els.brandAppSearch.addEventListener("input", () => {
            window.clearTimeout(brandTimer);
            brandTimer = window.setTimeout(() => {
                state.applications.brand.q = els.brandAppSearch.value.trim();
                state.applications.brand.page = 0;
                loadBrandApplications();
            }, 350);
        });
        let creatorTimer = null;
        els.creatorAppSearch.addEventListener("input", () => {
            window.clearTimeout(creatorTimer);
            creatorTimer = window.setTimeout(() => {
                state.applications.creator.q = els.creatorAppSearch.value.trim();
                state.applications.creator.page = 0;
                loadCreatorApplications();
            }, 350);
        });

        document.querySelectorAll("[data-brand-app-refresh]").forEach((button) => {
            button.addEventListener("click", loadBrandApplications);
        });
        document.querySelectorAll("[data-creator-app-refresh]").forEach((button) => {
            button.addEventListener("click", loadCreatorApplications);
        });
        document.querySelectorAll("[data-brand-app-clear-campaign]").forEach((button) => {
            button.addEventListener("click", () => {
                state.applications.brand.campaignId = null;
                state.applications.brand.campaignTitle = null;
                state.applications.brand.page = 0;
                loadBrandApplications();
            });
        });

        els.brandAppPrev.addEventListener("click", () => {
            if (state.applications.brand.page > 0) {
                state.applications.brand.page -= 1;
                loadBrandApplications();
            }
        });
        els.brandAppNext.addEventListener("click", () => {
            if (state.applications.brand.page < state.applications.brand.totalPages - 1) {
                state.applications.brand.page += 1;
                loadBrandApplications();
            }
        });
        els.creatorAppPrev.addEventListener("click", () => {
            if (state.applications.creator.page > 0) {
                state.applications.creator.page -= 1;
                loadCreatorApplications();
            }
        });
        els.creatorAppNext.addEventListener("click", () => {
            if (state.applications.creator.page < state.applications.creator.totalPages - 1) {
                state.applications.creator.page += 1;
                loadCreatorApplications();
            }
        });
    }

    function setupCollaborations() {
        if (!els.collabStatus) {
            return;
        }
        els.collabStatus.addEventListener("change", () => {
            state.collaborations.status = els.collabStatus.value;
            state.collaborations.page = 0;
            loadCollaborations();
        });
        document.querySelectorAll("[data-collab-refresh]").forEach((button) => {
            button.addEventListener("click", loadCollaborations);
        });
        els.collabPrev.addEventListener("click", () => {
            if (state.collaborations.page > 0) {
                state.collaborations.page -= 1;
                loadCollaborations();
            }
        });
        els.collabNext.addEventListener("click", () => {
            if (state.collaborations.page < state.collaborations.totalPages - 1) {
                state.collaborations.page += 1;
                loadCollaborations();
            }
        });
    }

    function showCampaignAlert(message) {
        if (!els.campaignAlert) {
            return;
        }
        els.campaignAlert.hidden = false;
        els.campaignAlertText.textContent = message;
    }

    function clearCampaignAlert() {
        if (els.campaignAlert) {
            els.campaignAlert.hidden = true;
        }
    }

    function statusBadgeClass(status) {
        if (status === "PUBLISHED") {
            return "badge badge-success";
        }
        if (status === "CLOSED" || status === "PAUSED") {
            return "badge badge-danger";
        }
        return "badge badge-soft";
    }

    function campaignCard(campaign) {
        const card = document.createElement("article");
        card.className = "campaign-manager-card";

        const head = document.createElement("div");
        head.className = "campaign-manager-head";
        const copy = document.createElement("div");
        const title = document.createElement("h4");
        title.textContent = campaign.title;
        const sub = document.createElement("p");
        sub.textContent = titleCase(campaign.status) + " · Updated " + formatDate(campaign.updatedAt);
        copy.append(title, sub);
        const badge = document.createElement("span");
        badge.className = statusBadgeClass(campaign.status);
        badge.textContent = titleCase(campaign.status);
        head.append(copy, badge);

        const description = document.createElement("p");
        description.className = "campaign-manager-copy";
        description.textContent = campaign.description;

        const facts = document.createElement("dl");
        facts.className = "campaign-meta";
        [
            ["Budget", formatMoneyRange(campaign)],
            ["Creators", String(campaign.creatorsRequired)],
            ["Deadline", formatDate(campaign.applicationDeadline)],
            ["Applications", String(campaign.applicationCount || 0)]
        ].forEach(([term, value]) => {
            const group = document.createElement("div");
            const dt = document.createElement("dt");
            dt.textContent = term;
            const dd = document.createElement("dd");
            dd.textContent = value;
            group.append(dt, dd);
            facts.appendChild(group);
        });

        const actions = document.createElement("div");
        actions.className = "campaign-manager-actions";

        actions.appendChild(actionButton(
            "Applications (" + (campaign.applicationCount || 0) + ")",
            "button button-ghost button-sm",
            () => openApplicationsForCampaign(campaign)
        ));

        if (campaign.status === "DRAFT") {
            const edit = document.createElement("button");
            edit.type = "button";
            edit.className = "button button-ghost button-sm";
            edit.textContent = "Edit draft";
            edit.addEventListener("click", () => openCampaignEditor(campaign));
            actions.appendChild(edit);
        }

        if (campaign.status === "DRAFT" || campaign.status === "PAUSED") {
            actions.appendChild(actionButton("Publish", "button button-navy button-sm",
                () => campaignAction(campaign, "publish", "Campaign published.")));
        }
        if (campaign.status === "PUBLISHED") {
            actions.appendChild(actionButton("Pause", "button button-ghost button-sm",
                () => campaignAction(campaign, "pause", "Campaign paused. It is hidden from public discovery.")));
        }
        if (campaign.status !== "CLOSED") {
            actions.appendChild(actionButton("Close", "button button-ghost button-sm is-danger",
                () => campaignAction(campaign, "close", "Campaign closed.")));
        }

        card.append(head, description, facts, actions);
        return card;
    }

    function actionButton(label, className, handler) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = className;
        button.textContent = label;
        button.addEventListener("click", handler);
        return button;
    }

    function formatMoneyRange(campaign) {
        const currency = campaign.currency || "USD";
        const formatter = (value) => {
            try {
                return new Intl.NumberFormat(undefined, {
                    style: "currency", currency, maximumFractionDigits: 0
                }).format(value);
            } catch (error) {
                return String(value);
            }
        };
        return formatter(campaign.minBudget) + " – " + formatter(campaign.maxBudget);
    }

    async function loadCampaigns() {
        if (!els.campaignList || state.campaigns.loading) {
            return;
        }
        state.campaigns.loading = true;
        clearCampaignAlert();
        els.campaignList.innerHTML = "";
        try {
            const params = new URLSearchParams({
                page: String(state.campaigns.page),
                size: "8"
            });
            if (state.campaigns.status) {
                params.set("status", state.campaigns.status);
            }
            if (state.campaigns.q) {
                params.set("q", state.campaigns.q);
            }
            const response = await api("/api/brand/campaigns?" + params.toString());
            const pageData = response.data;
            state.campaigns.totalPages = Math.max(1, pageData.totalPages);
            pageData.content.forEach((campaign) => els.campaignList.appendChild(campaignCard(campaign)));
            els.campaignEmpty.hidden = pageData.content.length !== 0;
            els.campaignPagination.hidden = state.campaigns.totalPages <= 1;
            els.campaignPage.textContent = "Page " + (state.campaigns.page + 1)
                + " of " + state.campaigns.totalPages;
            els.campaignPrev.disabled = state.campaigns.page <= 0;
            els.campaignNext.disabled = state.campaigns.page >= state.campaigns.totalPages - 1;
        } catch (error) {
            showCampaignAlert("Campaigns could not be loaded: " + error.message);
            els.campaignEmpty.hidden = true;
        } finally {
            state.campaigns.loading = false;
        }
    }

    async function campaignAction(campaign, action, successMessage) {
        if (action === "close" && !window.confirm(
            "Close \"" + campaign.title + "\"? Applications and campaign edits will stop.")) {
            return;
        }
        try {
            await api("/api/brand/campaigns/" + campaign.id + "/" + action, {method: "POST"});
            toast(successMessage);
            closeCampaignEditor();
            await loadCampaigns();
        } catch (error) {
            showCampaignAlert(error.message);
            toast("The campaign action failed.");
        }
    }

    function setCampaignFormAlert(message, success) {
        if (!els.campaignFormAlert) {
            return;
        }
        els.campaignFormAlert.hidden = false;
        els.campaignFormAlert.className = "form-alert " + (success ? "is-success" : "is-error");
        els.campaignFormAlertText.textContent = message;
    }

    function clearCampaignFormAlert() {
        if (els.campaignFormAlert) {
            els.campaignFormAlert.hidden = true;
        }
    }

    function isoDate(offsetDays) {
        const date = new Date();
        date.setDate(date.getDate() + offsetDays);
        return date.toISOString().slice(0, 10);
    }

    function openCampaignEditor(campaign) {
        if (!els.campaignForm) {
            return;
        }
        state.campaigns.editingId = campaign ? campaign.id : null;
        clearCampaignFormAlert();
        els.campaignForm.reset();
        els.campaignFormTitle.textContent = campaign ? campaign.title : "New campaign";
        els.campaignFormNote.textContent = campaign
            ? "Published campaign terms are locked. Only drafts can be edited."
            : "Campaigns start as a private draft. You can edit every detail before publishing.";
        els.campaignFormStatus.textContent = campaign ? titleCase(campaign.status) : "Draft";
        els.campaignSave.textContent = campaign ? "Save changes" : "Save draft";

        if (campaign) {
            setField("title", campaign.title);
            setField("description", campaign.description);
            setField("objectives", campaign.objectives);
            setField("productDetails", campaign.productDetails);
            setField("targetAudience", campaign.targetAudience);
            setField("category", campaign.category);
            setField("platforms", campaign.platforms);
            setField("niches", campaign.niches);
            setField("contentFormats", campaign.contentFormats);
            setField("startDate", campaign.startDate);
            setField("endDate", campaign.endDate);
            setField("applicationDeadline", campaign.applicationDeadline);
            setField("minBudget", campaign.minBudget);
            setField("maxBudget", campaign.maxBudget);
            setField("currency", campaign.currency || "USD");
            setField("compensationModel", campaign.compensationModel || "FIXED_FEE");
            setField("creatorsRequired", campaign.creatorsRequired || 1);
            setField("deliverableRequirements", campaign.deliverableRequirements);
            setField("reviewRules", campaign.reviewRules);
        } else {
            setField("applicationDeadline", isoDate(14));
            setField("startDate", isoDate(21));
            setField("endDate", isoDate(75));
            setField("minBudget", "");
            setField("maxBudget", "");
            setField("creatorsRequired", 1);
            setField("currency", "USD");
            setField("compensationModel", "FIXED_FEE");
        }

        els.campaignEditor.hidden = false;
        els.campaignEditor.scrollIntoView({behavior: "smooth", block: "start"});
        const first = els.campaignForm.querySelector("[name='title']");
        if (first && !campaign) {
            first.focus({preventScroll: true});
        }
    }

    function setField(name, value) {
        const field = els.campaignForm.querySelector("[name='" + name + "']");
        if (field) {
            field.value = value === null || value === undefined ? "" : value;
        }
    }

    function closeCampaignEditor() {
        if (!els.campaignEditor) {
            return;
        }
        state.campaigns.editingId = null;
        els.campaignEditor.hidden = true;
        clearCampaignFormAlert();
    }

    function campaignPayload() {
        const value = (name) => {
            const field = els.campaignForm.querySelector("[name='" + name + "']");
            return field && field.value.trim() !== "" ? field.value.trim() : null;
        };
        return {
            title: value("title"),
            description: value("description"),
            objectives: value("objectives"),
            productDetails: value("productDetails"),
            targetAudience: value("targetAudience"),
            platforms: value("platforms"),
            niches: value("niches"),
            contentFormats: value("contentFormats"),
            startDate: value("startDate"),
            endDate: value("endDate"),
            applicationDeadline: value("applicationDeadline"),
            minBudget: value("minBudget") === null ? null : Number(value("minBudget")),
            maxBudget: value("maxBudget") === null ? null : Number(value("maxBudget")),
            currency: value("currency"),
            compensationModel: value("compensationModel"),
            creatorsRequired: value("creatorsRequired") === null ? null : Number(value("creatorsRequired")),
            deliverableRequirements: value("deliverableRequirements"),
            reviewRules: value("reviewRules"),
            category: value("category")
        };
    }

    function setupCampaigns() {
        if (!els.campaignForm) {
            return;
        }

        document.querySelectorAll("[data-campaign-new]").forEach((button) => {
            button.addEventListener("click", () => openCampaignEditor(null));
        });
        document.querySelectorAll("[data-campaign-refresh]").forEach((button) => {
            button.addEventListener("click", () => loadCampaigns());
        });
        document.querySelectorAll("[data-campaign-cancel]").forEach((button) => {
            button.addEventListener("click", closeCampaignEditor);
        });

        els.campaignStatus.addEventListener("change", () => {
            state.campaigns.status = els.campaignStatus.value;
            state.campaigns.page = 0;
            loadCampaigns();
        });

        let searchTimer = null;
        els.campaignSearch.addEventListener("input", () => {
            window.clearTimeout(searchTimer);
            searchTimer = window.setTimeout(() => {
                state.campaigns.q = els.campaignSearch.value.trim();
                state.campaigns.page = 0;
                loadCampaigns();
            }, 350);
        });

        els.campaignPrev.addEventListener("click", () => {
            if (state.campaigns.page > 0) {
                state.campaigns.page -= 1;
                loadCampaigns();
            }
        });
        els.campaignNext.addEventListener("click", () => {
            if (state.campaigns.page < state.campaigns.totalPages - 1) {
                state.campaigns.page += 1;
                loadCampaigns();
            }
        });

        els.campaignForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            clearCampaignFormAlert();
            if (window.CollabNexus && window.CollabNexus.validateForm
                && !window.CollabNexus.validateForm(els.campaignForm, "campaign")) {
                setCampaignFormAlert("Review the highlighted fields before saving.", false);
                return;
            }
            const payload = campaignPayload();
            if (payload.startDate && payload.endDate && payload.endDate < payload.startDate) {
                setCampaignFormAlert("The campaign end date cannot be before the start date.", false);
                return;
            }
            if (payload.endDate && payload.applicationDeadline
                && payload.applicationDeadline > payload.endDate) {
                setCampaignFormAlert("The application deadline cannot be after the campaign end date.", false);
                return;
            }
            if (payload.minBudget !== null && payload.maxBudget !== null
                && payload.minBudget > payload.maxBudget) {
                setCampaignFormAlert("The minimum budget cannot be greater than the maximum budget.", false);
                return;
            }

            const editingId = state.campaigns.editingId;
            els.campaignSave.disabled = true;
            els.campaignSave.textContent = "Saving...";
            try {
                await api(editingId ? "/api/brand/campaigns/" + editingId : "/api/brand/campaigns", {
                    method: editingId ? "PUT" : "POST",
                    body: JSON.stringify(payload)
                });
                toast(editingId ? "Campaign draft updated." : "Campaign draft created.");
                closeCampaignEditor();
                state.campaigns.page = 0;
                await loadCampaigns();
            } catch (error) {
                setCampaignFormAlert(error.message, false);
                toast("The campaign could not be saved.");
            } finally {
                els.campaignSave.disabled = false;
                els.campaignSave.textContent = editingId ? "Save changes" : "Save draft";
            }
        });
    }


    /* ------------------------------------------------------------------
       Deliverables
       ------------------------------------------------------------------ */

    function deliverableBadge(status) {
        const badge = document.createElement("span");
        badge.className = status === "APPROVED"
            ? "badge badge-success"
            : status === "OVERDUE" || status === "REJECTED"
                ? "badge badge-danger"
                : status === "SUBMITTED" || status === "REVISION_REQUESTED"
                    ? "badge badge-warning"
                    : "badge badge-soft";
        badge.textContent = titleCase(status);
        return badge;
    }

    function contractBadge(status) {
        const badge = document.createElement("span");
        badge.className = status === "ACKNOWLEDGED"
            ? "badge badge-success"
            : status === "PENDING_ACKNOWLEDGEMENT"
                ? "badge badge-warning"
                : status === "VOID"
                    ? "badge badge-danger"
                    : "badge badge-soft";
        badge.textContent = titleCase(status);
        return badge;
    }

    function paymentBadge(status) {
        const badge = document.createElement("span");
        badge.className = status === "PAID"
            ? "badge badge-success"
            : status === "FAILED" || status === "DISPUTED"
                ? "badge badge-danger"
                : status === "DUE" || status === "PROCESSING"
                    ? "badge badge-warning"
                    : "badge badge-soft";
        badge.textContent = titleCase(status);
        return badge;
    }

    function showDeliverableAlert(message) {
        if (!els.deliverableAlert) {
            return;
        }
        els.deliverableAlert.hidden = false;
        els.deliverableAlertText.textContent = message;
    }

    function setDeliverableFormAlert(message, success) {
        if (!els.deliverableFormAlert) {
            return;
        }
        els.deliverableFormAlert.hidden = false;
        els.deliverableFormAlert.className = "form-alert " + (success ? "is-success" : "is-error");
        els.deliverableFormAlertText.textContent = message;
    }

    function setSubmissionAlert(message, success) {
        if (!els.submissionAlert) {
            return;
        }
        els.submissionAlert.hidden = false;
        els.submissionAlert.className = "form-alert " + (success ? "is-success" : "is-error");
        els.submissionAlertText.textContent = message;
    }

    function submissionHistory(deliverable) {
        const list = document.createElement("ul");
        list.className = "submission-history";
        const submissions = deliverable.submissions || [];
        const reviews = deliverable.reviews || [];
        if (submissions.length === 0) {
            const item = document.createElement("li");
            item.textContent = "No submissions yet.";
            list.appendChild(item);
        }
        submissions.forEach((submission) => {
            const item = document.createElement("li");
            const head = document.createElement("strong");
            head.textContent = "Version " + submission.versionNumber + " · "
                + formatDate(submission.createdAt);
            item.appendChild(head);
            if (submission.submissionUrl) {
                item.appendChild(document.createTextNode(" · "));
                item.appendChild(externalLink(submission.submissionUrl, "Open submission"));
            }
            if (submission.hasFile) {
                const download = document.createElement("a");
                download.className = "text-link";
                download.href = "/api/deliverables/" + deliverable.id + "/submissions/"
                    + submission.id + "/file";
                download.textContent = "Download " + (submission.originalFileName || "file");
                item.appendChild(document.createTextNode(" · "));
                item.appendChild(download);
            }
            if (submission.notes) {
                const notes = document.createElement("p");
                notes.className = "fine-line";
                notes.textContent = submission.notes;
                item.appendChild(notes);
            }
            list.appendChild(item);
        });
        reviews.forEach((review) => {
            const item = document.createElement("li");
            const head = document.createElement("strong");
            head.textContent = titleCase(review.action) + " · " + formatDate(review.createdAt);
            item.appendChild(head);
            if (review.comments) {
                const notes = document.createElement("p");
                notes.className = "fine-line";
                notes.textContent = review.comments;
                item.appendChild(notes);
            }
            list.appendChild(item);
        });
        return list;
    }

    function deliverableCard(deliverable) {
        const card = document.createElement("article");
        card.className = "deliverable-card";

        const head = document.createElement("div");
        head.className = "deliverable-head";
        const copy = document.createElement("div");
        const title = document.createElement("h4");
        title.textContent = deliverable.title;
        const sub = document.createElement("p");
        sub.textContent = deliverable.campaignTitle + " · " + deliverable.brandName;
        copy.append(title, sub);
        head.append(copy, deliverableBadge(deliverable.status));

        const facts = metaList([
            ["Platform", deliverable.platform || "Not set"],
            ["Format", deliverable.contentFormat || "Not set"],
            ["Due date", formatDate(deliverable.dueDate)],
            ["Revision", "v" + (deliverable.revisionNumber || 0)],
            [roleName() === "BRAND" ? "Creator" : "Brand",
                roleName() === "BRAND" ? deliverable.creatorName : deliverable.brandName]
        ]);

        card.append(head, facts);
        if (deliverable.description) {
            const description = document.createElement("p");
            description.className = "fine-line";
            description.textContent = deliverable.description;
            card.appendChild(description);
        }
        if (deliverable.reviewComments) {
            const comments = document.createElement("p");
            comments.className = "fine-line";
            comments.textContent = "Latest review: " + deliverable.reviewComments;
            card.appendChild(comments);
        }
        card.appendChild(submissionHistory(deliverable));

        const actions = document.createElement("div");
        actions.className = "deliverable-actions";
        if (roleName() === "CREATOR") {
            if (deliverable.workflowStatus === "PENDING") {
                actions.appendChild(actionButton("Start work", "button button-navy button-sm",
                    () => deliverableAction(deliverable, "start", "Deliverable moved into progress.")));
            }
            if (deliverable.workflowStatus === "IN_PROGRESS"
                || deliverable.workflowStatus === "REVISION_REQUESTED") {
                actions.appendChild(actionButton("Submit work", "button button-navy button-sm",
                    () => openSubmission(deliverable)));
            }
        } else if (roleName() === "BRAND") {
            if (deliverable.workflowStatus === "PENDING") {
                actions.appendChild(actionButton("Edit terms", "button button-ghost button-sm",
                    () => openDeliverableEditor(deliverable)));
            }
            if (deliverable.workflowStatus === "SUBMITTED") {
                actions.appendChild(actionButton("Approve", "button button-navy button-sm",
                    () => reviewDeliverable(deliverable, "approve", false)));
                actions.appendChild(actionButton("Request revision", "button button-ghost button-sm",
                    () => reviewDeliverable(deliverable, "request-revision", true)));
                actions.appendChild(actionButton("Reject", "button button-ghost button-sm is-danger",
                    () => reviewDeliverable(deliverable, "reject", true)));
            }
        }
        actions.appendChild(actionButton("Message", "button button-ghost button-sm",
            () => startConversation(deliverable.collaborationId)));
        card.appendChild(actions);
        return card;
    }

    async function loadDeliverables() {
        if (!els.deliverableList || state.deliverables.loading) {
            return;
        }
        state.deliverables.loading = true;
        els.deliverableList.innerHTML = "";
        if (els.deliverableAlert) {
            els.deliverableAlert.hidden = true;
        }
        try {
            const view = state.deliverables;
            const params = new URLSearchParams({page: String(view.page), size: "6"});
            if (view.status) {
                params.set("status", view.status);
            }
            if (view.deadline === "overdue") {
                params.set("overdue", "true");
            } else if (view.deadline === "upcoming") {
                params.set("upcomingDays", "30");
            }
            if (view.collaborationId) {
                params.set("collaborationId", String(view.collaborationId));
            }
            const response = await api("/api/deliverables?" + params.toString());
            const pageData = response.data;
            view.totalPages = Math.max(1, pageData.totalPages);
            pageData.content.forEach((item) => els.deliverableList.appendChild(deliverableCard(item)));
            els.deliverableEmpty.hidden = pageData.content.length !== 0;
            els.deliverableCount.textContent = pageData.totalElements
                + (pageData.totalElements === 1 ? " deliverable" : " deliverables");
            els.deliverablePagination.hidden = view.totalPages <= 1;
            els.deliverablePage.textContent = "Page " + (view.page + 1) + " of " + view.totalPages;
            els.deliverablePrev.disabled = view.page <= 0;
            els.deliverableNext.disabled = view.page >= view.totalPages - 1;
            els.deliverableCollabFilter.hidden = !view.collaborationId;
            if (view.collaborationId) {
                els.deliverableCollabTitle.textContent = view.collaborationTitle || "Selected collaboration";
            }
            if (els.deliverableNew) {
                els.deliverableNew.hidden = roleName() !== "BRAND";
            }
            loadDeliverableHighlights();
            if (roleName() === "BRAND") {
                loadDeliverableCollaborations();
            }
        } catch (error) {
            showDeliverableAlert("Deliverables could not be loaded: " + error.message);
            els.deliverableEmpty.hidden = true;
        } finally {
            state.deliverables.loading = false;
        }
    }

    function deadlineItem(deliverable) {
        const item = document.createElement("div");
        item.className = "deadline-item";
        const copy = document.createElement("div");
        const title = document.createElement("strong");
        title.textContent = deliverable.title;
        const sub = document.createElement("span");
        sub.textContent = deliverable.campaignTitle + " · due " + formatDate(deliverable.dueDate);
        copy.append(title, sub);
        item.append(copy, deliverableBadge(deliverable.status));
        item.addEventListener("click", () => {
            state.deliverables.page = 0;
            state.deliverables.collaborationId = deliverable.collaborationId;
            state.deliverables.collaborationTitle = deliverable.campaignTitle;
            loadDeliverables();
        });
        return item;
    }

    async function loadDeliverableHighlights() {
        if (!els.deliverableUpcoming || roleName() === "ADMIN") {
            return;
        }
        try {
            const [upcoming, overdue] = await Promise.all([
                api("/api/deliverables?upcomingDays=30&size=5"),
                api("/api/deliverables?overdue=true&size=5")
            ]);
            els.deliverableUpcoming.innerHTML = "";
            els.deliverableOverdue.innerHTML = "";
            upcoming.data.content.forEach((item) =>
                els.deliverableUpcoming.appendChild(deadlineItem(item)));
            overdue.data.content.forEach((item) =>
                els.deliverableOverdue.appendChild(deadlineItem(item)));
            els.deliverableUpcomingCount.textContent = String(upcoming.data.totalElements);
            els.deliverableOverdueCount.textContent = String(overdue.data.totalElements);
            if (upcoming.data.content.length === 0) {
                els.deliverableUpcoming.innerHTML = '<p class="fine-line">Nothing due in the next 30 days.</p>';
            }
            if (overdue.data.content.length === 0) {
                els.deliverableOverdue.innerHTML = '<p class="fine-line">No overdue deliverables.</p>';
            }
        } catch (error) {
            els.deliverableUpcoming.innerHTML = '<p class="fine-line">Deadlines could not be loaded.</p>';
            els.deliverableOverdue.innerHTML = "";
        }
    }

    async function loadDeliverableCollaborations() {
        if (!els.deliverableForm || !els.deliverableCollabFilter) {
            return;
        }
        const select = els.deliverableForm.querySelector("[name='collaborationId']");
        const paymentSelect = els.paymentForm
            ? els.paymentForm.querySelector("[name='collaborationId']") : null;
        try {
            const response = await api("/api/collaborations?size=50");
            const options = response.data.content.filter((item) =>
                item.status === "ACTIVE" || item.status === "IN_PROGRESS");
            [select, paymentSelect].forEach((target) => {
                if (!target) {
                    return;
                }
                const current = target.value;
                target.innerHTML = "";
                const placeholder = document.createElement("option");
                placeholder.value = "";
                placeholder.textContent = options.length ? "Choose a collaboration" : "No open collaborations";
                target.appendChild(placeholder);
                options.forEach((item) => {
                    const option = document.createElement("option");
                    option.value = String(item.id);
                    option.textContent = item.campaignTitle + " · "
                        + (roleName() === "BRAND" ? item.creatorName : item.brandName);
                    target.appendChild(option);
                });
                if (current && options.some((item) => String(item.id) === current)) {
                    target.value = current;
                }
            });
        } catch (error) {
            // The deliverable form still shows a clear empty state.
        }
    }

    function openDeliverableEditor(deliverable) {
        if (!els.deliverableForm) {
            return;
        }
        state.deliverables.editingId = deliverable ? deliverable.id : null;
        els.deliverableForm.reset();
        if (els.deliverableFormAlert) {
            els.deliverableFormAlert.hidden = true;
        }
        els.deliverableFormTitle.textContent = deliverable ? "Edit deliverable terms" : "New deliverable";
        els.deliverableFormMode.textContent = deliverable ? "Editing" : "New";
        els.deliverableSave.textContent = deliverable ? "Save terms" : "Add deliverable";
        const collaborationField = els.deliverableForm.querySelector("[data-deliverable-collaboration-field]");
        if (collaborationField) {
            collaborationField.hidden = Boolean(deliverable);
        }
        if (deliverable) {
            setDeliverableField("title", deliverable.title);
            setDeliverableField("dueDate", deliverable.dueDate);
            setDeliverableField("platform", deliverable.platform);
            setDeliverableField("contentFormat", deliverable.contentFormat);
            setDeliverableField("description", deliverable.description);
        } else {
            const collaboration = state.deliverables.collaborationId;
            if (collaboration) {
                setDeliverableField("collaborationId", collaboration);
            }
            setDeliverableField("dueDate", isoDate(14));
        }
        els.deliverableEditor.hidden = false;
        els.deliverableEditor.scrollIntoView({behavior: "smooth", block: "start"});
    }

    function setDeliverableField(name, value) {
        const field = els.deliverableForm.querySelector("[name='" + name + "']");
        if (field) {
            field.value = value === null || value === undefined ? "" : value;
        }
    }

    function closeDeliverableEditor() {
        if (!els.deliverableEditor) {
            return;
        }
        state.deliverables.editingId = null;
        els.deliverableEditor.hidden = true;
    }

    async function deliverableAction(deliverable, action, message) {
        try {
            await api("/api/deliverables/" + deliverable.id + "/" + action, {method: "POST"});
            toast(message);
            await loadDeliverables();
        } catch (error) {
            showDeliverableAlert(error.message);
            toast("The deliverable action failed.");
        }
    }

    async function reviewDeliverable(deliverable, action, requireComment) {
        let comments = null;
        if (requireComment) {
            comments = window.prompt("Add review comments for the creator:");
            if (comments === null) {
                return;
            }
            if (!comments.trim()) {
                toast("Review comments are required for this action.");
                return;
            }
        }
        const prompt = action === "approve"
            ? "Approve \"" + deliverable.title + "\"?"
            : "Reject \"" + deliverable.title + "\"?";
        if (!window.confirm(prompt)) {
            return;
        }
        try {
            await api("/api/brand/deliverables/" + deliverable.id + "/" + action, {
                method: "POST",
                body: JSON.stringify({comments: comments})
            });
            toast("Deliverable review recorded.");
            await loadDeliverables();
        } catch (error) {
            showDeliverableAlert(error.message);
            toast("The review could not be recorded.");
        }
    }

    function openSubmission(deliverable) {
        if (!els.submissionForm) {
            return;
        }
        state.deliverables.uploadId = deliverable.id;
        els.submissionForm.reset();
        if (els.submissionAlert) {
            els.submissionAlert.hidden = true;
        }
        els.submissionTitle.textContent = "Submit: " + deliverable.title;
        els.submissionEditor.hidden = false;
        els.submissionEditor.scrollIntoView({behavior: "smooth", block: "start"});
    }

    function closeSubmission() {
        if (!els.submissionEditor) {
            return;
        }
        state.deliverables.uploadId = null;
        els.submissionEditor.hidden = true;
    }

    function setupDeliverables() {
        if (!els.deliverableForm) {
            return;
        }
        if (els.deliverableNew) {
            els.deliverableNew.addEventListener("click", () => openDeliverableEditor(null));
        }
        document.querySelectorAll("[data-deliverable-refresh]").forEach((button) => {
            button.addEventListener("click", loadDeliverables);
        });
        document.querySelectorAll("[data-deliverable-cancel]").forEach((button) => {
            button.addEventListener("click", closeDeliverableEditor);
        });
        document.querySelectorAll("[data-submission-cancel]").forEach((button) => {
            button.addEventListener("click", closeSubmission);
        });
        document.querySelectorAll("[data-deliverable-clear-collab]").forEach((button) => {
            button.addEventListener("click", () => {
                state.deliverables.collaborationId = null;
                state.deliverables.collaborationTitle = null;
                state.deliverables.page = 0;
                loadDeliverables();
            });
        });
        els.deliverableStatus.addEventListener("change", () => {
            state.deliverables.status = els.deliverableStatus.value;
            state.deliverables.page = 0;
            loadDeliverables();
        });
        els.deliverableDeadline.addEventListener("change", () => {
            state.deliverables.deadline = els.deliverableDeadline.value;
            state.deliverables.page = 0;
            loadDeliverables();
        });
        els.deliverablePrev.addEventListener("click", () => {
            if (state.deliverables.page > 0) {
                state.deliverables.page -= 1;
                loadDeliverables();
            }
        });
        els.deliverableNext.addEventListener("click", () => {
            if (state.deliverables.page < state.deliverables.totalPages - 1) {
                state.deliverables.page += 1;
                loadDeliverables();
            }
        });

        els.deliverableForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            if (window.CollabNexus && window.CollabNexus.validateForm
                && !window.CollabNexus.validateForm(els.deliverableForm, "deliverable")) {
                setDeliverableFormAlert("Review the highlighted fields.", false);
                return;
            }
            const value = (name) => {
                const field = els.deliverableForm.querySelector("[name='" + name + "']");
                return field && field.value.trim() !== "" ? field.value.trim() : null;
            };
            const editingId = state.deliverables.editingId;
            const collaborationId = value("collaborationId");
            if (!editingId && !collaborationId) {
                setDeliverableFormAlert("Choose a collaboration first.", false);
                return;
            }
            const payload = {
                title: value("title"),
                description: value("description"),
                platform: value("platform"),
                contentFormat: value("contentFormat"),
                dueDate: value("dueDate")
            };
            els.deliverableSave.disabled = true;
            els.deliverableSave.textContent = "Saving...";
            try {
                await api(editingId
                    ? "/api/brand/deliverables/" + editingId
                    : "/api/brand/collaborations/" + collaborationId + "/deliverables", {
                    method: editingId ? "PUT" : "POST",
                    body: JSON.stringify(payload)
                });
                toast(editingId ? "Deliverable terms updated." : "Deliverable added.");
                closeDeliverableEditor();
                await loadDeliverables();
            } catch (error) {
                setDeliverableFormAlert(error.message, false);
                toast("The deliverable could not be saved.");
            } finally {
                els.deliverableSave.disabled = false;
                els.deliverableSave.textContent = editingId ? "Save terms" : "Add deliverable";
            }
        });

        els.submissionForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            if (!state.deliverables.uploadId) {
                return;
            }
            const formData = new FormData(els.submissionForm);
            if (!formData.get("submissionUrl") && !(formData.get("file") && formData.get("file").size)) {
                setSubmissionAlert("Provide a submission URL, a supporting file, or both.", false);
                return;
            }
            els.submissionSave.disabled = true;
            els.submissionSave.textContent = "Submitting...";
            try {
                await api("/api/deliverables/" + state.deliverables.uploadId + "/submissions", {
                    method: "POST",
                    body: formData
                });
                toast("Deliverable submitted for review.");
                closeSubmission();
                await loadDeliverables();
            } catch (error) {
                setSubmissionAlert(error.message, false);
                toast("The deliverable could not be submitted.");
            } finally {
                els.submissionSave.disabled = false;
                els.submissionSave.textContent = "Submit for review";
            }
        });
    }

    /* ------------------------------------------------------------------
       Contracts
       ------------------------------------------------------------------ */

    function showContractAlert(message) {
        if (!els.contractAlert) {
            return;
        }
        els.contractAlert.hidden = false;
        els.contractAlertText.textContent = message;
    }

    function contractCard(contract) {
        const card = document.createElement("article");
        card.className = "contract-card";
        const head = document.createElement("div");
        head.className = "contract-head";
        const copy = document.createElement("div");
        const title = document.createElement("h4");
        title.textContent = contract.referenceNumber;
        const sub = document.createElement("p");
        sub.textContent = contract.campaignTitle + " · " + contract.brandName
            + " / " + contract.creatorName;
        copy.append(title, sub);
        head.append(copy, contractBadge(contract.status));

        const facts = metaList([
            ["Compensation", formatMoneyAmount(contract.agreedCompensation, contract.currency)],
            ["Version", "v" + contract.versionNumber],
            ["Brand acknowledgement", contract.brandAcknowledged ? "Recorded" : "Pending"],
            ["Creator acknowledgement", contract.creatorAcknowledged ? "Recorded" : "Pending"],
            ["Updated", formatDate(contract.updatedAt)]
        ]);

        const actions = document.createElement("div");
        actions.className = "contract-actions";
        actions.appendChild(actionButton("View summary", "button button-navy button-sm",
            () => openContract(contract.id)));
        if (roleName() === "BRAND" && contract.status !== "ACKNOWLEDGED" && contract.status !== "VOID") {
            actions.appendChild(actionButton("Edit terms", "button button-ghost button-sm",
                () => openContractEditor(contract)));
        }
        card.append(head, facts, actions);
        return card;
    }

    async function loadContracts() {
        if (!els.contractList || state.contracts.loading) {
            return;
        }
        state.contracts.loading = true;
        els.contractList.innerHTML = "";
        if (els.contractAlert) {
            els.contractAlert.hidden = true;
        }
        try {
            const view = state.contracts;
            const response = await api("/api/contracts?page=" + view.page + "&size=8");
            const pageData = response.data;
            view.totalPages = Math.max(1, pageData.totalPages);
            pageData.content.forEach((contract) =>
                els.contractList.appendChild(contractCard(contract)));
            els.contractEmpty.hidden = pageData.content.length !== 0;
            els.contractCount.textContent = pageData.totalElements
                + (pageData.totalElements === 1 ? " contract" : " contracts");
            els.contractPagination.hidden = view.totalPages <= 1;
            els.contractPage.textContent = "Page " + (view.page + 1) + " of " + view.totalPages;
            els.contractPrev.disabled = view.page <= 0;
            els.contractNext.disabled = view.page >= view.totalPages - 1;
        } catch (error) {
            showContractAlert("Contracts could not be loaded: " + error.message);
            els.contractEmpty.hidden = true;
        } finally {
            state.contracts.loading = false;
        }
    }

    function contractTerm(title, value) {
        const block = document.createElement("div");
        block.className = "contract-term";
        const heading = document.createElement("h4");
        heading.textContent = title;
        const body = document.createElement("p");
        body.textContent = value || "Not stated";
        block.append(heading, body);
        return block;
    }

    async function openContract(contractId) {
        try {
            const response = await api("/api/contracts/" + contractId);
            const contract = response.data;
            state.contracts.selectedId = contract.id;
            els.contractReference.textContent = contract.referenceNumber;
            els.contractCampaign.textContent = contract.campaignTitle + " · "
                + contract.brandName + " / " + contract.creatorName;
            els.contractStatus.className = contractBadge(contract.status).className;
            els.contractStatus.textContent = titleCase(contract.status);

            els.contractFacts.innerHTML = "";
            [
                ["Agreed compensation", formatMoneyAmount(contract.agreedCompensation, contract.currency)],
                ["Completion deadline", contract.completionDeadline
                    ? formatDate(contract.completionDeadline) : "Not stated"],
                ["Revision allowance", String(contract.revisionAllowance)],
                ["Version", "v" + contract.versionNumber],
                ["Created", formatDate(contract.createdAt)],
                ["Updated", formatDate(contract.updatedAt)]
            ].forEach(([term, value]) => els.contractFacts.appendChild(detailRow(term, value)));

            els.contractTerms.innerHTML = "";
            [
                ["Deliverables and deadlines", contract.deliverablesSummary],
                ["Usage rights and licensing", contract.usageRights],
                ["Cancellation terms", contract.cancellationTerms],
                ["Payment schedule", contract.paymentSchedule]
            ].forEach(([term, value]) => els.contractTerms.appendChild(contractTerm(term, value)));

            els.contractAck.innerHTML = "";
            const brandAck = document.createElement("span");
            brandAck.className = contract.brandAcknowledged ? "badge badge-success" : "badge badge-soft";
            brandAck.textContent = "Brand: "
                + (contract.brandAcknowledged ? "acknowledged " + formatDate(contract.brandAcknowledgedAt) : "pending");
            const creatorAck = document.createElement("span");
            creatorAck.className = contract.creatorAcknowledged ? "badge badge-success" : "badge badge-soft";
            creatorAck.textContent = "Creator: "
                + (contract.creatorAcknowledged ? "acknowledged " + formatDate(contract.creatorAcknowledgedAt) : "pending");
            els.contractAck.append(brandAck, creatorAck);

            els.contractActions.innerHTML = "";
            const mineAcknowledged = roleName() === "BRAND"
                ? contract.brandAcknowledged : contract.creatorAcknowledged;
            if (roleName() !== "ADMIN" && contract.status === "PENDING_ACKNOWLEDGEMENT" && !mineAcknowledged) {
                els.contractActions.appendChild(actionButton("Acknowledge terms",
                    "button button-navy button-sm", () => acknowledgeContract(contract.id)));
            }
            if (roleName() === "BRAND" && contract.status === "DRAFT") {
                els.contractActions.appendChild(actionButton("Send for acknowledgement",
                    "button button-navy button-sm", () => sendContract(contract.id)));
                els.contractActions.appendChild(actionButton("Edit terms",
                    "button button-ghost button-sm", () => openContractEditor(contract)));
            }

            els.contractHistory.innerHTML = "";
            if (contract.versions && contract.versions.length) {
                const wrapper = document.createElement("details");
                const summary = document.createElement("summary");
                summary.textContent = "Version history (" + contract.versions.length + ")";
                const list = document.createElement("ul");
                contract.versions.forEach((version) => {
                    const item = document.createElement("li");
                    item.textContent = "v" + version.versionNumber + " · "
                        + formatMoneyAmount(version.agreedCompensation, version.currency)
                        + " · saved " + formatDate(version.createdAt)
                        + " by " + version.createdByName;
                    list.appendChild(item);
                });
                wrapper.append(summary, list);
                els.contractHistory.appendChild(wrapper);
            }
            if (contract.acceptances && contract.acceptances.length) {
                const wrapper = document.createElement("details");
                const summary = document.createElement("summary");
                summary.textContent = "Acknowledgement events (" + contract.acceptances.length + ")";
                const list = document.createElement("ul");
                contract.acceptances.forEach((acceptance) => {
                    const item = document.createElement("li");
                    item.textContent = (acceptance.acceptedByName || "Participant") + " · v"
                        + acceptance.versionNumber + " · " + formatDate(acceptance.acceptedAt);
                    list.appendChild(item);
                });
                wrapper.append(summary, list);
                els.contractHistory.appendChild(wrapper);
            }

            els.contractDetail.hidden = false;
            els.contractDetail.scrollIntoView({behavior: "smooth", block: "start"});
        } catch (error) {
            showContractAlert("Contract details could not be loaded: " + error.message);
        }
    }

    async function acknowledgeContract(contractId) {
        if (!window.confirm("Record your acknowledgement of this contract version? This is not a claim of legal electronic signature.")) {
            return;
        }
        try {
            const response = await api("/api/contracts/" + contractId + "/acknowledge", {method: "POST"});
            toast("Contract acknowledgement recorded.");
            await loadContracts();
            await openContract(response.data.id);
        } catch (error) {
            showContractAlert(error.message);
            toast("The acknowledgement could not be recorded.");
        }
    }

    async function sendContract(contractId) {
        if (!window.confirm("Send this contract version to both parties for acknowledgement?")) {
            return;
        }
        try {
            await api("/api/brand/contracts/" + contractId + "/send", {method: "POST"});
            toast("Contract sent for acknowledgement.");
            await loadContracts();
            await openContract(contractId);
        } catch (error) {
            showContractAlert(error.message);
            toast("The contract could not be sent.");
        }
    }

    function openContractEditor(contract) {
        if (!els.contractForm) {
            return;
        }
        state.contracts.editingId = contract.id;
        els.contractForm.reset();
        if (els.contractFormAlert) {
            els.contractFormAlert.hidden = true;
        }
        els.contractEditorVersion.textContent = "Version " + (contract.versionNumber + 1);
        const set = (name, value) => {
            const field = els.contractForm.querySelector("[name='" + name + "']");
            if (field) {
                field.value = value === null || value === undefined ? "" : value;
            }
        };
        set("agreedCompensation", contract.agreedCompensation);
        set("currency", contract.currency || "USD");
        set("completionDeadline", contract.completionDeadline);
        set("revisionAllowance", contract.revisionAllowance);
        set("deliverablesSummary", contract.deliverablesSummary);
        set("usageRights", contract.usageRights);
        set("cancellationTerms", contract.cancellationTerms);
        set("paymentSchedule", contract.paymentSchedule);
        els.contractEditor.hidden = false;
        els.contractEditor.scrollIntoView({behavior: "smooth", block: "start"});
    }

    function closeContractEditor() {
        if (!els.contractEditor) {
            return;
        }
        state.contracts.editingId = null;
        els.contractEditor.hidden = true;
    }

    function printContract(contractId) {
        const source = els.contractDetail;
        if (!source) {
            return;
        }
        const printWindow = window.open("", "_blank");
        if (!printWindow) {
            toast("Allow pop-ups to print the contract summary.");
            return;
        }
        const doc = printWindow.document;
        const title = doc.createElement("title");
        title.textContent = "CollabNexus contract summary";
        const style = doc.createElement("style");
        style.textContent = "body{font-family:Georgia,serif;max-width:760px;margin:40px auto;line-height:1.6;color:#102033}"
            + "h1{font-size:1.6rem}dt{font-weight:bold;margin-top:10px}dd{margin:0 0 6px}"
            + ".contract-term{border:1px solid #dfe4ea;padding:12px;margin:8px 0}"
            + ".badge{border:1px solid #dfe4ea;padding:2px 8px;border-radius:10px;margin-right:6px;font-size:.8rem}";
        doc.head.append(title, style);
        const heading = doc.createElement("h1");
        heading.textContent = "CollabNexus contract summary";
        const note = doc.createElement("p");
        note.textContent = "Printable record of acknowledged terms. This is not a legal electronic signature and does not prove an external payment.";
        doc.body.append(heading, note);
        source.cloneNode(true).querySelectorAll("button, script").forEach((node) => node.remove());
        const clone = source.cloneNode(true);
        clone.querySelectorAll("button, script").forEach((node) => node.remove());
        doc.body.appendChild(clone);
        printWindow.focus();
        printWindow.print();
    }

    function setupContracts() {
        if (!els.contractList) {
            return;
        }
        document.querySelectorAll("[data-contract-refresh]").forEach((button) => {
            button.addEventListener("click", loadContracts);
        });
        const print = document.querySelector("[data-contract-print]");
        if (print) {
            print.addEventListener("click", () => printContract(state.contracts.selectedId));
        }
        const cancel = els.contractForm
            ? els.contractForm.querySelector("[data-contract-cancel]") : null;
        if (cancel) {
            cancel.addEventListener("click", closeContractEditor);
        }
        els.contractPrev.addEventListener("click", () => {
            if (state.contracts.page > 0) {
                state.contracts.page -= 1;
                loadContracts();
            }
        });
        els.contractNext.addEventListener("click", () => {
            if (state.contracts.page < state.contracts.totalPages - 1) {
                state.contracts.page += 1;
                loadContracts();
            }
        });
        if (!els.contractForm) {
            return;
        }
        els.contractForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            if (!state.contracts.editingId) {
                return;
            }
            const value = (name) => {
                const field = els.contractForm.querySelector("[name='" + name + "']");
                return field && field.value.trim() !== "" ? field.value.trim() : null;
            };
            const payload = {
                agreedCompensation: value("agreedCompensation") === null
                    ? null : Number(value("agreedCompensation")),
                currency: value("currency"),
                deliverablesSummary: value("deliverablesSummary"),
                completionDeadline: value("completionDeadline"),
                usageRights: value("usageRights"),
                revisionAllowance: value("revisionAllowance") === null
                    ? 0 : Number(value("revisionAllowance")),
                cancellationTerms: value("cancellationTerms"),
                paymentSchedule: value("paymentSchedule")
            };
            if (!payload.agreedCompensation || !payload.currency) {
                setContractFormAlert("Enter compensation and a three-letter currency.", false);
                return;
            }
            els.contractSave.disabled = true;
            els.contractSave.textContent = "Saving...";
            try {
                const response = await api("/api/brand/contracts/" + state.contracts.editingId, {
                    method: "PUT",
                    body: JSON.stringify(payload)
                });
                toast("Contract terms saved as a new version.");
                closeContractEditor();
                await loadContracts();
                await openContract(response.data.id);
            } catch (error) {
                setContractFormAlert(error.message, false);
                toast("The contract could not be saved.");
            } finally {
                els.contractSave.disabled = false;
                els.contractSave.textContent = "Save new version";
            }
        });
    }

    function setContractFormAlert(message, success) {
        if (!els.contractFormAlert) {
            return;
        }
        els.contractFormAlert.hidden = false;
        els.contractFormAlert.className = "form-alert " + (success ? "is-success" : "is-error");
        els.contractFormAlertText.textContent = message;
    }

    /* ------------------------------------------------------------------
       Payments
       ------------------------------------------------------------------ */

    function showPaymentListAlert(message) {
        if (!els.paymentListAlert) {
            return;
        }
        els.paymentListAlert.hidden = false;
        els.paymentListAlertText.textContent = message;
    }

    function paymentCard(payment) {
        const card = document.createElement("article");
        card.className = "payment-card";
        const head = document.createElement("div");
        head.className = "payment-head";
        const copy = document.createElement("div");
        const title = document.createElement("h4");
        title.textContent = payment.milestone;
        const sub = document.createElement("p");
        sub.textContent = payment.campaignTitle + " · "
            + (roleName() === "BRAND" ? payment.creatorName : payment.brandName);
        copy.append(title, sub);
        const amount = document.createElement("strong");
        amount.className = "payment-amount";
        amount.textContent = formatMoneyAmount(payment.amount, payment.currency);
        head.append(copy, paymentBadge(payment.status));

        const facts = metaList([
            ["Paid", formatMoneyAmount(payment.paidAmount, payment.currency)],
            ["Remaining", formatMoneyAmount(payment.remainingAmount, payment.currency)],
            ["Due date", payment.dueDate ? formatDate(payment.dueDate) : "Not set"],
            ["External reference", payment.externalReference || "None recorded"],
            ["Recorded", formatDate(payment.recordedAt)]
        ]);

        const progress = document.createElement("div");
        progress.className = "payment-progress";
        const fill = document.createElement("span");
        const percent = Number(payment.amount) > 0
            ? Math.min(100, Math.round((Number(payment.paidAmount) / Number(payment.amount)) * 100)) : 0;
        fill.style.width = percent + "%";
        progress.appendChild(fill);

        const actions = document.createElement("div");
        actions.className = "payment-actions";
        if (roleName() === "BRAND" && payment.status !== "REFUNDED") {
            actions.appendChild(actionButton("Update status", "button button-navy button-sm",
                () => openStatusDialog(payment)));
        }
        if (roleName() === "CREATOR" && payment.status !== "DISPUTED" && payment.status !== "REFUNDED") {
            actions.appendChild(actionButton("Dispute", "button button-ghost button-sm is-danger",
                () => disputePayment(payment)));
        }
        if (payment.notes) {
            const notes = document.createElement("p");
            notes.className = "fine-line";
            notes.textContent = payment.notes;
            card.appendChild(notes);
        }

        card.append(head, facts, progress, actions);
        return card;
    }

    async function loadPayments() {
        if (!els.paymentList || state.payments.loading) {
            return;
        }
        state.payments.loading = true;
        els.paymentList.innerHTML = "";
        if (els.paymentListAlert) {
            els.paymentListAlert.hidden = true;
        }
        if (els.paymentCreate) {
            els.paymentCreate.hidden = roleName() !== "BRAND";
        }
        try {
            const view = state.payments;
            const params = new URLSearchParams({page: String(view.page), size: "8"});
            if (view.status) {
                params.set("status", view.status);
            }
            const [summary, list] = await Promise.all([
                api("/api/payments/summary"),
                api("/api/payments?" + params.toString())
            ]);
            renderPaymentSummary(summary.data);
            const pageData = list.data;
            view.totalPages = Math.max(1, pageData.totalPages);
            const query = view.q.toLowerCase();
            const filtered = pageData.content.filter((payment) => !query
                || [payment.milestone, payment.campaignTitle, payment.brandName, payment.creatorName]
                    .filter(Boolean).some((value) => value.toLowerCase().includes(query)));
            filtered.forEach((payment) => els.paymentList.appendChild(paymentCard(payment)));
            els.paymentEmpty.hidden = filtered.length !== 0;
            els.paymentCount.textContent = pageData.totalElements
                + (pageData.totalElements === 1 ? " record" : " records");
            els.paymentPagination.hidden = view.totalPages <= 1;
            els.paymentPage.textContent = "Page " + (view.page + 1) + " of " + view.totalPages;
            els.paymentPrev.disabled = view.page <= 0;
            els.paymentNext.disabled = view.page >= view.totalPages - 1;
            if (roleName() === "BRAND") {
                loadDeliverableCollaborations();
            }
        } catch (error) {
            showPaymentListAlert("Payment records could not be loaded: " + error.message);
            els.paymentEmpty.hidden = true;
        } finally {
            state.payments.loading = false;
        }
    }

    function renderPaymentSummary(summary) {
        if (!els.paymentSummary) {
            return;
        }
        els.paymentSummary.innerHTML = "";
        const currency = summary.currency || "USD";
        els.paymentSummary.append(
            statCard("Scheduled", formatMoneyAmount(summary.scheduledAmount, currency), "Recorded milestones"),
            statCard("Paid", formatMoneyAmount(summary.paidAmount, currency), "Status records, not verified transfers"),
            statCard("Outstanding", formatMoneyAmount(summary.outstandingAmount, currency), "Scheduled minus paid"),
            statCard("Records", String(summary.recordCount), summary.paidCount + " marked paid")
        );
    }

    function openStatusDialog(payment) {
        if (!els.statusDialog || !els.statusForm) {
            return;
        }
        els.statusDialog.dataset.paymentId = String(payment.id);
        els.statusDialog.dataset.paymentAmount = String(payment.amount);
        els.statusDialogNote.textContent = payment.milestone + " · "
            + formatMoneyAmount(payment.amount, payment.currency)
            + ". This records a status change; it does not transfer money.";
        els.statusForm.reset();
        els.statusForm.querySelector("[name='status']").value = payment.status;
        els.statusForm.querySelector("[name='paidAmount']").value = payment.paidAmount || "";
        els.statusForm.querySelector("[name='externalReference']").value = payment.externalReference || "";
        els.statusForm.querySelector("[name='notes']").value = payment.notes || "";
        if (els.statusDialogAlert) {
            els.statusDialogAlert.hidden = true;
        }
        if (typeof els.statusDialog.showModal === "function") {
            els.statusDialog.showModal();
        } else {
            els.statusDialog.setAttribute("open", "open");
        }
    }

    function closeStatusDialog() {
        if (!els.statusDialog) {
            return;
        }
        if (typeof els.statusDialog.close === "function") {
            els.statusDialog.close();
        } else {
            els.statusDialog.removeAttribute("open");
        }
    }

    async function disputePayment(payment) {
        const notes = window.prompt("Explain why this payment record is disputed:");
        if (!notes || !notes.trim()) {
            return;
        }
        try {
            await api("/api/payments/" + payment.id + "/status", {
                method: "POST",
                body: JSON.stringify({status: "DISPUTED", notes: notes.trim()})
            });
            toast("Payment dispute recorded.");
            await loadPayments();
        } catch (error) {
            showPaymentListAlert(error.message);
            toast("The dispute could not be recorded.");
        }
    }

    function setupPayments() {
        if (!els.paymentList) {
            return;
        }
        document.querySelectorAll("[data-payment-refresh]").forEach((button) => {
            button.addEventListener("click", loadPayments);
        });
        els.paymentStatus.addEventListener("change", () => {
            state.payments.status = els.paymentStatus.value;
            state.payments.page = 0;
            loadPayments();
        });
        let searchTimer = null;
        els.paymentSearch.addEventListener("input", () => {
            window.clearTimeout(searchTimer);
            searchTimer = window.setTimeout(() => {
                state.payments.q = els.paymentSearch.value.trim();
                loadPayments();
            }, 300);
        });
        els.paymentPrev.addEventListener("click", () => {
            if (state.payments.page > 0) {
                state.payments.page -= 1;
                loadPayments();
            }
        });
        els.paymentNext.addEventListener("click", () => {
            if (state.payments.page < state.payments.totalPages - 1) {
                state.payments.page += 1;
                loadPayments();
            }
        });

        if (els.paymentForm) {
            els.paymentForm.addEventListener("submit", async (event) => {
                event.preventDefault();
                const value = (name) => {
                    const field = els.paymentForm.querySelector("[name='" + name + "']");
                    return field && field.value.trim() !== "" ? field.value.trim() : null;
                };
                const collaborationId = value("collaborationId");
                const milestone = value("milestone");
                const amount = value("amount");
                if (!collaborationId || !milestone || !amount) {
                    showPaymentFormAlert("Choose a collaboration, then enter a milestone and amount.", false);
                    return;
                }
                try {
                    await api("/api/brand/collaborations/" + collaborationId + "/payments", {
                        method: "POST",
                        body: JSON.stringify({
                            milestone: milestone,
                            amount: Number(amount),
                            currency: value("currency"),
                            dueDate: value("dueDate"),
                            notes: value("notes")
                        })
                    });
                    toast("Payment milestone recorded.");
                    els.paymentForm.reset();
                    showPaymentFormAlert("Milestone recorded. No money was transferred.", true);
                    await loadPayments();
                } catch (error) {
                    showPaymentFormAlert(error.message, false);
                    toast("The milestone could not be recorded.");
                }
            });
        }

        if (els.statusForm) {
            const cancel = els.statusDialog.querySelector("[data-status-dialog-cancel]");
            if (cancel) {
                cancel.addEventListener("click", closeStatusDialog);
            }
            els.statusForm.addEventListener("submit", async (event) => {
                event.preventDefault();
                const paymentId = els.statusDialog.dataset.paymentId;
                const value = (name) => {
                    const field = els.statusForm.querySelector("[name='" + name + "']");
                    return field && field.value.trim() !== "" ? field.value.trim() : null;
                };
                const paidAmount = value("paidAmount");
                els.statusDialogSave.disabled = true;
                els.statusDialogSave.textContent = "Saving...";
                try {
                    await api("/api/payments/" + paymentId + "/status", {
                        method: "POST",
                        body: JSON.stringify({
                            status: value("status"),
                            paidAmount: paidAmount === null ? null : Number(paidAmount),
                            externalReference: value("externalReference"),
                            notes: value("notes")
                        })
                    });
                    toast("Payment status recorded.");
                    closeStatusDialog();
                    await loadPayments();
                } catch (error) {
                    els.statusDialogAlert.hidden = false;
                    els.statusDialogAlertText.textContent = error.message;
                } finally {
                    els.statusDialogSave.disabled = false;
                    els.statusDialogSave.textContent = "Save status";
                }
            });
        }
    }

    function showPaymentFormAlert(message, success) {
        if (!els.paymentAlert) {
            return;
        }
        els.paymentAlert.hidden = false;
        els.paymentAlert.className = "form-alert " + (success ? "is-success" : "is-error");
        els.paymentAlertText.textContent = message;
    }

    /* ------------------------------------------------------------------
       Analytics
       ------------------------------------------------------------------ */

    function shortMonth(key) {
        const parts = String(key).split("-");
        if (parts.length !== 2) {
            return key;
        }
        const date = new Date(Number(parts[0]), Number(parts[1]) - 1, 1);
        return new Intl.DateTimeFormat(undefined, {month: "short"}).format(date);
    }

    function analyticsChart(name) {
        return document.querySelector("[data-chart='" + name + "']");
    }

    function destroyCharts() {
        Object.values(state.analytics.charts).forEach((chart) => {
            if (chart && typeof chart.destroy === "function") {
                chart.destroy();
            }
        });
        state.analytics.charts = {};
    }

    function drawChart(name, type, labels, datasets, options) {
        const canvas = analyticsChart(name);
        if (!canvas || !window.Chart) {
            return;
        }
        state.analytics.charts[name] = new window.Chart(canvas, {
            type: type,
            data: {labels: labels, datasets: datasets},
            options: Object.assign({
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {display: datasets.length > 1, labels: {usePointStyle: true, boxWidth: 8}},
                    tooltip: {intersect: false, mode: "index"}
                },
                scales: {
                    y: {beginAtZero: true, ticks: {precision: 0}, grid: {color: "rgba(16, 32, 51, 0.08)"}},
                    x: {grid: {display: false}}
                }
            }, options || {})
        });
    }

    async function loadAnalytics() {
        if (!els.analyticsStats || state.analytics.loading) {
            return;
        }
        state.analytics.loading = true;
        destroyCharts();
        els.analyticsStats.innerHTML = "";
        try {
            const role = roleName();
            const endpoint = role === "ADMIN"
                ? "/api/admin/analytics/overview"
                : role === "BRAND"
                    ? "/api/brand/analytics/overview"
                    : "/api/creator/analytics/overview";
            const response = await api(endpoint);
            const data = response.data;
            renderAnalyticsStats(role, data);
            renderAnalyticsCharts(role, data);
        } catch (error) {
            els.analyticsStats.appendChild(statCard("Analytics unavailable", "—", error.message));
        } finally {
            state.analytics.loading = false;
        }
    }

    function renderAnalyticsStats(role, data) {
        const cards = [];
        if (role === "BRAND") {
            cards.push(
                statCard("Campaigns", String(data.totalCampaigns), data.activeCampaigns + " published"),
                statCard("Applications", String(data.totalApplications), data.shortlistedApplications + " shortlisted"),
                statCard("Accepted", String(data.acceptedApplications), data.totalCollaborations + " collaborations"),
                statCard("Completed", String(data.completedCollaborations), "Completed collaborations"),
                statCard("Planned budget", formatMoneyAmount(data.plannedBudget, data.currency), "Sum of maximum budgets"),
                statCard("Recorded spending", formatMoneyAmount(data.recordedSpending, data.currency), "Recorded payments marked paid"),
                statCard("Awaiting review", String(data.deliverablesAwaitingReview), "Submitted deliverables"),
                statCard("Completion rate", data.deliverableCompletionRate + "%", "Approved ÷ all deliverables")
            );
        } else if (role === "CREATOR") {
            cards.push(
                statCard("Applications", String(data.totalApplications), data.pendingApplications + " pending"),
                statCard("Accepted", String(data.acceptedApplications), data.totalCollaborations + " collaborations"),
                statCard("Active collaborations", String(data.activeCollaborations), data.completedCollaborations + " completed"),
                statCard("Active deliverables", String(data.activeDeliverables), "Open work"),
                statCard("Upcoming deadlines", String(data.upcomingDeadlines), "Next 30 days"),
                statCard("Recorded earnings", formatMoneyAmount(data.recordedEarnings, data.currency), "Marked paid records"),
                statCard("Outstanding", String(data.outstandingPaymentRecords), "Payment records not paid"),
                statCard("Response rate", data.responseRate + "%", "Accepted ÷ decided applications")
            );
        } else {
            cards.push(
                statCard("Users", String(data.totalUsers), data.brands + " brands · " + data.creators + " creators"),
                statCard("Campaigns", String(data.totalCampaigns), data.publishedCampaigns + " published"),
                statCard("Applications", String(data.totalApplications), "Across the platform"),
                statCard("Collaborations", String(data.totalCollaborations), data.completedCollaborations + " completed"),
                statCard("Open reports", String(data.openReports), "Moderation queue"),
                statCard("Reviews under review", String(data.reviewsUnderReview), "Awaiting moderation"),
                statCard("Scheduled payments", formatMoneyAmount(data.scheduledPayments, "USD"), "Recorded milestones"),
                statCard("Paid payments", formatMoneyAmount(data.paidPayments, "USD"), "Recorded status only")
            );
        }
        cards.forEach((card) => els.analyticsStats.appendChild(card));
    }

    function renderAnalyticsCharts(role, data) {
        if (!window.Chart) {
            return;
        }
        if (role === "BRAND") {
            const months = Object.keys(data.monthlyApplications || {});
            const labels = months.map(shortMonth);
            drawChart("applications", "line", labels, [{
                label: "Applications",
                data: months.map((key) => data.monthlyApplications[key]),
                borderColor: "#17395c",
                backgroundColor: "rgba(23, 57, 92, 0.12)",
                fill: true,
                tension: 0.35,
                pointRadius: 3
            }]);
            const funnel = data.applicationFunnel || {};
            drawChart("funnel", "bar", Object.keys(funnel).map(titleCase), [{
                label: "Applications",
                data: Object.keys(funnel).map((key) => funnel[key]),
                backgroundColor: ["#c9a961", "#9d7f3f", "#3f9d72", "#c25c52", "#6b7a8c"]
            }]);
            const collabMonths = Object.keys(data.monthlyCollaborations || {});
            drawChart("collaborations", "bar", collabMonths.map(shortMonth), [{
                label: "Collaborations",
                data: collabMonths.map((key) => data.monthlyCollaborations[key]),
                backgroundColor: "#17395c"
            }]);
            drawChart("budget", "bar", ["Planned budget", "Recorded spending"], [{
                label: data.currency || "USD",
                data: [Number(data.plannedBudget), Number(data.recordedSpending)],
                backgroundColor: ["#c9a961", "#17395c"]
            }], {indexAxis: "y"});
        } else if (role === "CREATOR") {
            const months = Object.keys(data.monthlyApplications || {});
            drawChart("applications", "line", months.map(shortMonth), [{
                label: "Applications",
                data: months.map((key) => data.monthlyApplications[key]),
                borderColor: "#17395c",
                backgroundColor: "rgba(23, 57, 92, 0.12)",
                fill: true,
                tension: 0.35,
                pointRadius: 3
            }]);
            drawChart("funnel", "doughnut", ["Accepted", "Pending", "Other"], [{
                data: [
                    data.acceptedApplications,
                    data.pendingApplications,
                    Math.max(0, data.totalApplications - data.acceptedApplications - data.pendingApplications)
                ],
                backgroundColor: ["#3f9d72", "#c9a961", "#dfe4ea"],
                borderWidth: 0
            }], {cutout: "62%"});
            const earningMonths = Object.keys(data.monthlyEarnings || {});
            drawChart("collaborations", "bar", earningMonths.map(shortMonth), [{
                label: "Recorded earnings (" + (data.currency || "USD") + ")",
                data: earningMonths.map((key) => data.monthlyEarnings[key]),
                backgroundColor: "#c9a961"
            }]);
            drawChart("budget", "bar", ["Recorded earnings", "Outstanding records"], [{
                label: data.currency || "USD",
                data: [Number(data.recordedEarnings), Number(data.outstandingPaymentRecords)],
                backgroundColor: ["#17395c", "#c9a961"]
            }], {indexAxis: "y"});
        } else {
            const campaignStatus = data.campaignStatus || {};
            drawChart("applications", "bar", Object.keys(campaignStatus).map(titleCase), [{
                label: "Campaigns",
                data: Object.keys(campaignStatus).map((key) => campaignStatus[key]),
                backgroundColor: "#17395c"
            }]);
            const collabStatus = data.collaborationStatus || {};
            drawChart("funnel", "doughnut", Object.keys(collabStatus).map(titleCase), [{
                data: Object.keys(collabStatus).map((key) => collabStatus[key]),
                backgroundColor: ["#17395c", "#c9a961", "#3f9d72", "#c25c52"],
                borderWidth: 0
            }], {cutout: "62%"});
            drawChart("collaborations", "bar", ["Applications", "Collaborations", "Completed"], [{
                label: "Count",
                data: [Number(data.totalApplications), Number(data.totalCollaborations),
                    Number(data.completedCollaborations)],
                backgroundColor: ["#17395c", "#c9a961", "#3f9d72"]
            }]);
            drawChart("budget", "bar", ["Scheduled payments", "Paid payments"], [{
                label: "USD",
                data: [Number(data.scheduledPayments), Number(data.paidPayments)],
                backgroundColor: ["#c9a961", "#17395c"]
            }], {indexAxis: "y"});
        }
    }

    /* ------------------------------------------------------------------
       Messaging
       ------------------------------------------------------------------ */

    function showConversationAlert(message) {
        if (!els.conversationAlert) {
            return;
        }
        els.conversationAlert.hidden = false;
        els.conversationAlertText.textContent = message;
    }

    function renderConversations() {
        if (!els.conversationList) {
            return;
        }
        els.conversationList.innerHTML = "";
        state.messages.conversations.forEach((conversation) => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "conversation-item"
                + (conversation.id === state.messages.activeId ? " is-active" : "");
            const name = roleName() === "BRAND" ? conversation.creatorName : conversation.brandName;
            const title = document.createElement("strong");
            title.textContent = name || conversation.campaignTitle;
            const preview = document.createElement("span");
            preview.textContent = conversation.lastMessage || conversation.campaignTitle;
            button.append(title, preview);
            if (conversation.unreadCount > 0) {
                const unread = document.createElement("span");
                unread.className = "conversation-unread";
                unread.textContent = conversation.unreadCount + " unread";
                button.appendChild(unread);
            }
            button.addEventListener("click", () => selectConversation(conversation.id));
            els.conversationList.appendChild(button);
        });
        els.conversationEmpty.hidden = state.messages.conversations.length !== 0;
    }

    async function loadConversations() {
        if (!els.conversationList || state.messages.loading) {
            return;
        }
        state.messages.loading = true;
        if (els.conversationAlert) {
            els.conversationAlert.hidden = true;
        }
        try {
            const response = await api("/api/conversations?size=50");
            state.messages.conversations = response.data.content;
            renderConversations();
            if (state.messages.conversations.length > 0) {
                const stillExists = state.messages.conversations
                    .some((item) => item.id === state.messages.activeId);
                if (!stillExists) {
                    await selectConversation(state.messages.conversations[0].id);
                }
            }
            refreshUnreadCounts();
        } catch (error) {
            showConversationAlert("Conversations could not be loaded: " + error.message);
        } finally {
            state.messages.loading = false;
        }
    }

    async function selectConversation(conversationId) {
        state.messages.activeId = conversationId;
        renderConversations();
        const conversation = state.messages.conversations.find((item) => item.id === conversationId);
        if (conversation) {
            els.threadTitle.textContent = conversation.campaignTitle;
            els.threadSubtitle.textContent = "With " + (roleName() === "BRAND"
                ? conversation.creatorName : conversation.brandName);
            els.threadUnread.hidden = conversation.unreadCount === 0;
            els.threadUnread.textContent = conversation.unreadCount + " unread";
        }
        if (els.messageForm) {
            els.messageForm.hidden = false;
        }
        els.messageList.innerHTML = "";
        try {
            const response = await api("/api/conversations/" + conversationId + "/messages?size=100");
            const messages = response.data.content;
            if (messages.length === 0) {
                els.messageList.innerHTML = '<p class="fine-line">No messages yet. Start the conversation.</p>';
            }
            messages.forEach((message) => {
                const bubble = document.createElement("div");
                bubble.className = "message-bubble"
                    + (message.senderId === state.session.user.id ? " is-mine" : "");
                const body = document.createElement("p");
                body.textContent = message.body;
                const meta = document.createElement("small");
                meta.textContent = message.senderName + " · " + formatDate(message.createdAt);
                bubble.append(body, meta);
                els.messageList.appendChild(bubble);
            });
            els.messageList.scrollTop = els.messageList.scrollHeight;
            if (conversation) {
                conversation.unreadCount = 0;
            }
            els.threadUnread.hidden = true;
            renderConversations();
        } catch (error) {
            showConversationAlert("Messages could not be loaded: " + error.message);
        }
    }

    async function startConversation(collaborationId) {
        try {
            const response = await api("/api/conversations", {
                method: "POST",
                body: JSON.stringify({collaborationId: collaborationId})
            });
            state.messages.activeId = response.data.id;
            showView("messages");
            await loadConversations();
            await selectConversation(response.data.id);
        } catch (error) {
            toast(error.message);
        }
    }

    function setupMessaging() {
        if (!els.messageForm) {
            return;
        }
        els.messageForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const body = els.messageBody.value.trim();
            if (!body || !state.messages.activeId) {
                return;
            }
            els.messageSend.disabled = true;
            els.messageSend.textContent = "Sending...";
            try {
                await api("/api/conversations/" + state.messages.activeId + "/messages", {
                    method: "POST",
                    body: JSON.stringify({body: body})
                });
                els.messageBody.value = "";
                await selectConversation(state.messages.activeId);
            } catch (error) {
                toast(error.message);
            } finally {
                els.messageSend.disabled = false;
                els.messageSend.textContent = "Send";
            }
        });
    }

    /* ------------------------------------------------------------------
       Reviews and reports
       ------------------------------------------------------------------ */

    function starRating(rating) {
        const span = document.createElement("span");
        span.className = "review-stars";
        span.textContent = "★".repeat(Math.max(0, Math.min(5, rating)))
            + "☆".repeat(Math.max(0, 5 - rating));
        span.setAttribute("aria-label", rating + " out of 5 stars");
        return span;
    }

    function reviewCard(review, showStatus) {
        const card = document.createElement("article");
        card.className = "review-card";
        const head = document.createElement("div");
        head.className = "review-head";
        const copy = document.createElement("div");
        const title = document.createElement("h4");
        title.textContent = review.campaignTitle;
        const sub = document.createElement("p");
        sub.textContent = review.authorName + " · " + titleCase(review.authorRole)
            + " · " + formatDate(review.createdAt);
        copy.append(title, sub);
        const right = document.createElement("div");
        right.appendChild(starRating(review.rating));
        if (showStatus) {
            const badge = document.createElement("span");
            badge.className = "badge badge-soft";
            badge.textContent = titleCase(review.status);
            right.appendChild(badge);
        }
        head.append(copy, right);
        card.appendChild(head);
        if (review.comment) {
            const comment = document.createElement("p");
            comment.className = "review-comment";
            comment.textContent = review.comment;
            card.appendChild(comment);
        }
        return card;
    }

    function showReviewAlert(message) {
        if (!els.reviewAlert) {
            return;
        }
        els.reviewAlert.hidden = false;
        els.reviewAlertText.textContent = message;
    }

    async function loadReviews() {
        if (!els.reviewEligible || state.reviews.loading) {
            return;
        }
        state.reviews.loading = true;
        if (els.reviewAlert) {
            els.reviewAlert.hidden = true;
        }
        try {
            const [collaborations, mine, received] = await Promise.all([
                api("/api/collaborations?status=COMPLETED&size=50"),
                api("/api/reviews/mine?size=50"),
                api("/api/reviews/received?size=50")
            ]);
            const mineReviews = mine.data.content;
            const reviewedIds = new Set(mineReviews.map((review) => review.collaborationId));
            state.reviews.eligible = collaborations.data.content
                .filter((collaboration) => !reviewedIds.has(collaboration.id));
            state.reviews.mine = mineReviews;
            state.reviews.received = received.data.content;

            els.reviewEligible.innerHTML = "";
            state.reviews.eligible.forEach((collaboration) => {
                const card = document.createElement("div");
                card.className = "review-eligible-card";
                const copy = document.createElement("div");
                const title = document.createElement("strong");
                title.textContent = collaboration.campaignTitle;
                const sub = document.createElement("p");
                sub.className = "fine-line";
                sub.textContent = "Completed collaboration with "
                    + (roleName() === "BRAND" ? collaboration.creatorName : collaboration.brandName);
                copy.append(title, sub);

                const form = document.createElement("form");
                form.className = "review-form";
                const row = document.createElement("div");
                row.className = "review-form-row";
                const rating = document.createElement("select");
                rating.className = "select";
                rating.setAttribute("aria-label", "Rating");
                [["5", "5 stars"], ["4", "4 stars"], ["3", "3 stars"], ["2", "2 stars"], ["1", "1 star"]]
                    .forEach(([value, label]) => {
                        const option = document.createElement("option");
                        option.value = value;
                        option.textContent = label;
                        rating.appendChild(option);
                    });
                const comment = document.createElement("textarea");
                comment.className = "input textarea";
                comment.rows = 2;
                comment.maxLength = 2000;
                comment.placeholder = "Share a clear, professional review.";
                comment.setAttribute("aria-label", "Review comment");
                row.append(rating, comment);
                form.append(row);
                const submit = document.createElement("button");
                submit.type = "submit";
                submit.className = "button button-navy button-sm";
                submit.textContent = "Publish review";
                form.appendChild(submit);
                form.addEventListener("submit", async (event) => {
                    event.preventDefault();
                    submit.disabled = true;
                    try {
                        await api("/api/collaborations/" + collaboration.id + "/reviews", {
                            method: "POST",
                            body: JSON.stringify({
                                rating: Number(rating.value),
                                comment: comment.value.trim() || null
                            })
                        });
                        toast("Review published.");
                        await loadReviews();
                    } catch (error) {
                        showReviewAlert(error.message);
                        toast("The review could not be published.");
                    } finally {
                        submit.disabled = false;
                    }
                });
                card.append(copy, form);
                els.reviewEligible.appendChild(card);
            });
            els.reviewEligibleEmpty.hidden = state.reviews.eligible.length !== 0;
            els.reviewEligibleCount.textContent = state.reviews.eligible.length
                + (state.reviews.eligible.length === 1 ? " available" : " available");

            els.reviewMine.innerHTML = "";
            mineReviews.forEach((review) => els.reviewMine.appendChild(reviewCard(review, true)));
            if (mineReviews.length === 0) {
                els.reviewMine.innerHTML = '<p class="fine-line">You have not written any reviews yet.</p>';
            }
            els.reviewReceived.innerHTML = "";
            state.reviews.received.forEach((review) =>
                els.reviewReceived.appendChild(reviewCard(review, false)));
            if (state.reviews.received.length === 0) {
                els.reviewReceived.innerHTML = '<p class="fine-line">No published reviews about you yet.</p>';
            }
            els.reviewAverage.textContent = state.reviews.received.length
                ? (state.reviews.received.reduce((sum, review) => sum + review.rating, 0)
                    / state.reviews.received.length).toFixed(1) + " / 5"
                : "No rating";
        } catch (error) {
            showReviewAlert("Reviews could not be loaded: " + error.message);
        } finally {
            state.reviews.loading = false;
        }
    }

    function setupReviews() {
        if (!els.reportForm) {
            return;
        }
        els.reportForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const form = els.reportForm;
            const value = (name) => {
                const field = form.querySelector("[name='" + name + "']");
                return field && field.value.trim() !== "" ? field.value.trim() : null;
            };
            const reason = value("reason");
            if (!reason) {
                showReportAlert("Describe the reason for this report.", false);
                return;
            }
            const targetId = value("targetId");
            const submit = form.querySelector("button[type='submit']");
            submit.disabled = true;
            try {
                await api("/api/reports", {
                    method: "POST",
                    body: JSON.stringify({
                        targetType: value("targetType"),
                        targetId: targetId === null ? null : Number(targetId),
                        reason: reason,
                        details: value("details")
                    })
                });
                form.reset();
                showReportAlert("Report submitted. An administrator will review it.", true);
                toast("Report submitted.");
            } catch (error) {
                showReportAlert(error.message, false);
                toast("The report could not be submitted.");
            } finally {
                submit.disabled = false;
            }
        });
    }

    function showReportAlert(message, success) {
        if (!els.reportAlert) {
            return;
        }
        els.reportAlert.hidden = false;
        els.reportAlert.className = "form-alert " + (success ? "is-success" : "is-error");
        els.reportAlertText.textContent = message;
    }

    /* ------------------------------------------------------------------
       Notifications
       ------------------------------------------------------------------ */

    function notificationItem(notification) {
        const item = document.createElement("article");
        item.className = "notification-item" + (notification.read ? "" : " is-unread");
        const dot = document.createElement("span");
        dot.className = "status-dot " + (notification.read ? "" : "is-up");
        dot.setAttribute("aria-hidden", "true");
        const copy = document.createElement("div");
        const title = document.createElement("h4");
        title.textContent = notification.title;
        const message = document.createElement("p");
        message.textContent = notification.message;
        copy.append(title, message);
        const time = document.createElement("time");
        time.dateTime = notification.createdAt;
        time.textContent = formatDate(notification.createdAt);
        item.append(dot, copy, time);
        item.addEventListener("click", async () => {
            try {
                if (!notification.read) {
                    await api("/api/notifications/" + notification.id + "/read", {method: "POST"});
                }
                await loadNotifications();
                refreshUnreadCounts();
                if (notification.link) {
                    window.location.href = notification.link;
                }
            } catch (error) {
                toast(error.message);
            }
        });
        return item;
    }

    async function loadNotifications() {
        if (!els.notificationList || state.notifications.loading) {
            return;
        }
        state.notifications.loading = true;
        if (els.notificationAlert) {
            els.notificationAlert.hidden = true;
        }
        try {
            const view = state.notifications;
            const response = await api("/api/notifications?page=" + view.page + "&size=12");
            const pageData = response.data;
            view.totalPages = Math.max(1, pageData.totalPages);
            els.notificationList.innerHTML = "";
            pageData.content.forEach((notification) =>
                els.notificationList.appendChild(notificationItem(notification)));
            els.notificationEmpty.hidden = pageData.content.length !== 0;
            els.notificationCount.textContent = pageData.totalElements
                + (pageData.totalElements === 1 ? " notification" : " notifications");
            els.notificationPagination.hidden = view.totalPages <= 1;
            els.notificationPage.textContent = "Page " + (view.page + 1) + " of " + view.totalPages;
            els.notificationPrev.disabled = view.page <= 0;
            els.notificationNext.disabled = view.page >= view.totalPages - 1;
        } catch (error) {
            if (els.notificationAlert) {
                els.notificationAlert.hidden = false;
                els.notificationAlertText.textContent = "Notifications could not be loaded: " + error.message;
            }
        } finally {
            state.notifications.loading = false;
        }
    }

    async function refreshUnreadCounts() {
        if (!els.notificationBadge || !state.session) {
            return;
        }
        if (roleName() === "ADMIN") {
            els.notificationBadge.hidden = true;
            return;
        }
        try {
            const [notifications, messages] = await Promise.all([
                api("/api/notifications/unread-count"),
                api("/api/conversations/unread-count")
            ]);
            const total = Number(notifications.data.count || 0) + Number(messages.data.count || 0);
            els.notificationBadge.hidden = total === 0;
            els.notificationBadge.textContent = String(total);
            els.notificationCount.textContent = Number(notifications.data.count || 0) + " unread";
        } catch (error) {
            // Polling is best-effort and never blocks the workspace.
        }
    }

    function setupNotifications() {
        if (!els.notificationList) {
            return;
        }
        els.notificationPrev.addEventListener("click", () => {
            if (state.notifications.page > 0) {
                state.notifications.page -= 1;
                loadNotifications();
            }
        });
        els.notificationNext.addEventListener("click", () => {
            if (state.notifications.page < state.notifications.totalPages - 1) {
                state.notifications.page += 1;
                loadNotifications();
            }
        });
        const readAll = document.querySelector("[data-notification-read-all]");
        if (readAll) {
            readAll.addEventListener("click", async () => {
                try {
                    await api("/api/notifications/read-all", {method: "POST"});
                    toast("All notifications marked read.");
                    await loadNotifications();
                    await refreshUnreadCounts();
                } catch (error) {
                    toast(error.message);
                }
            });
        }
        if (els.notificationBell) {
            els.notificationBell.addEventListener("click", () => showView("notifications"));
        }
    }

    /* ------------------------------------------------------------------
       Administration
       ------------------------------------------------------------------ */

    function showAdminAlert(kind, message) {
        const map = {
            users: [els.adminUserAlert, els.adminUserAlertText],
            campaigns: [els.adminCampaignAlert, els.adminCampaignAlertText],
            reports: [els.adminReportAlert, els.adminReportAlertText],
            reviews: [els.adminReviewAlert, els.adminReviewAlertText],
            audit: [els.adminAuditAlert, els.adminAuditAlertText]
        };
        const pair = map[kind];
        if (!pair || !pair[0]) {
            return;
        }
        pair[0].hidden = false;
        pair[1].textContent = message;
    }

    function statusBadgeText(status) {
        const badge = document.createElement("span");
        badge.className = status === "ACTIVE" || status === "PUBLISHED" || status === "RESOLVED"
            ? "badge badge-success"
            : status === "SUSPENDED" || status === "HIDDEN" || status === "DISMISSED"
                ? "badge badge-danger"
                : "badge badge-soft";
        badge.textContent = titleCase(status);
        return badge;
    }

    async function loadAdminUsers() {
        if (!els.adminUserList || state.admin.users.loading) {
            return;
        }
        state.admin.users.loading = true;
        els.adminUserList.innerHTML = "";
        try {
            const view = state.admin.users;
            const params = new URLSearchParams({page: String(view.page), size: "10"});
            if (view.role) {
                params.set("role", view.role);
            }
            const response = await api("/api/admin/users?" + params.toString());
            const pageData = response.data;
            view.totalPages = Math.max(1, pageData.totalPages);
            const query = view.q.toLowerCase();
            const users = pageData.content.filter((user) => !query
                || user.fullName.toLowerCase().includes(query)
                || user.email.toLowerCase().includes(query));
            users.forEach((user) => {
                const row = document.createElement("tr");
                const account = document.createElement("td");
                const stack = document.createElement("span");
                stack.className = "table-stack";
                const name = document.createElement("strong");
                name.textContent = user.fullName;
                const email = document.createElement("span");
                email.className = "table-sub";
                email.textContent = user.email;
                stack.append(name, email);
                account.appendChild(stack);

                const role = document.createElement("td");
                const roleBadge = document.createElement("span");
                roleBadge.className = "badge badge-soft";
                roleBadge.textContent = titleCase(user.role);
                role.appendChild(roleBadge);

                const status = document.createElement("td");
                status.appendChild(statusBadgeText(user.status));

                const joined = document.createElement("td");
                joined.textContent = formatDate(user.createdAt);

                const actions = document.createElement("td");
                const wrap = document.createElement("div");
                wrap.className = "table-actions";
                if (user.role !== "ADMIN") {
                    if (user.status === "ACTIVE") {
                        wrap.appendChild(actionButton("Suspend", "button button-ghost button-sm is-danger",
                            () => adminUserAction(user, "suspend", "Account suspended.")));
                    } else {
                        wrap.appendChild(actionButton("Reactivate", "button button-navy button-sm",
                            () => adminUserAction(user, "activate", "Account reactivated.")));
                    }
                }
                actions.appendChild(wrap);
                row.append(account, role, status, joined, actions);
                els.adminUserList.appendChild(row);
            });
            els.adminUserCount.textContent = pageData.totalElements
                + (pageData.totalElements === 1 ? " account" : " accounts");
            els.adminUserPagination.hidden = view.totalPages <= 1;
            els.adminUserPage.textContent = "Page " + (view.page + 1) + " of " + view.totalPages;
            els.adminUserPrev.disabled = view.page <= 0;
            els.adminUserNext.disabled = view.page >= view.totalPages - 1;
        } catch (error) {
            showAdminAlert("users", "Accounts could not be loaded: " + error.message);
        } finally {
            state.admin.users.loading = false;
        }
    }

    async function adminUserAction(user, action, message) {
        if (action === "suspend" && !window.confirm(
            "Suspend " + user.fullName + " (" + user.email + ")? They will be signed out of new requests.")) {
            return;
        }
        try {
            await api("/api/admin/users/" + user.id + "/" + action, {method: "POST"});
            toast(message);
            await loadAdminUsers();
            await loadAdminAudit();
        } catch (error) {
            showAdminAlert("users", error.message);
            toast("The account action failed.");
        }
    }

    async function loadAdminCampaigns() {
        if (!els.adminCampaignList || state.admin.campaigns.loading) {
            return;
        }
        state.admin.campaigns.loading = true;
        els.adminCampaignList.innerHTML = "";
        try {
            const view = state.admin.campaigns;
            const params = new URLSearchParams({page: String(view.page), size: "10"});
            if (view.status) {
                params.set("status", view.status);
            }
            if (view.q) {
                params.set("q", view.q);
            }
            const response = await api("/api/admin/campaigns?" + params.toString());
            const pageData = response.data;
            view.totalPages = Math.max(1, pageData.totalPages);
            pageData.content.forEach((campaign) => {
                const row = document.createElement("tr");

                const titleCell = document.createElement("td");
                const stack = document.createElement("span");
                stack.className = "table-stack";
                const title = document.createElement("strong");
                title.textContent = campaign.title;
                const category = document.createElement("span");
                category.className = "table-sub";
                category.textContent = campaign.category || "Uncategorised";
                stack.append(title, category);
                titleCell.appendChild(stack);

                const brand = document.createElement("td");
                brand.textContent = campaign.brandName;

                const status = document.createElement("td");
                status.appendChild(statusBadgeText(campaign.status));

                const budget = document.createElement("td");
                budget.textContent = formatMoneyAmount(campaign.minBudget, campaign.currency)
                    + " – " + formatMoneyAmount(campaign.maxBudget, campaign.currency);

                const deadline = document.createElement("td");
                deadline.textContent = formatDate(campaign.applicationDeadline);

                const actions = document.createElement("td");
                const wrap = document.createElement("div");
                wrap.className = "table-actions";
                if (campaign.status !== "CLOSED") {
                    wrap.appendChild(actionButton("Close", "button button-ghost button-sm is-danger",
                        () => adminCampaignClose(campaign)));
                }
                actions.appendChild(wrap);

                row.append(titleCell, brand, status, budget, deadline, actions);
                els.adminCampaignList.appendChild(row);
            });
            els.adminCampaignEmpty.hidden = pageData.content.length !== 0;
            els.adminCampaignCount.textContent = pageData.totalElements
                + (pageData.totalElements === 1 ? " campaign" : " campaigns");
            els.adminCampaignPagination.hidden = view.totalPages <= 1;
            els.adminCampaignPage.textContent = "Page " + (view.page + 1) + " of " + view.totalPages;
            els.adminCampaignPrev.disabled = view.page <= 0;
            els.adminCampaignNext.disabled = view.page >= view.totalPages - 1;
        } catch (error) {
            showAdminAlert("campaigns", "Campaigns could not be loaded: " + error.message);
        } finally {
            state.admin.campaigns.loading = false;
        }
    }

    async function adminCampaignClose(campaign) {
        if (!window.confirm("Close \"" + campaign.title + "\" for platform policy reasons? "
            + "The brand will be notified and the action will be recorded in the audit log.")) {
            return;
        }
        try {
            await api("/api/admin/campaigns/" + campaign.id + "/close", {method: "POST"});
            toast("Campaign closed.");
            await loadAdminCampaigns();
            await loadAdminAudit();
        } catch (error) {
            showAdminAlert("campaigns", error.message);
            toast("The campaign could not be closed.");
        }
    }

    async function loadAdminReports() {
        if (!els.adminReportList || state.admin.reports.loading) {
            return;
        }
        state.admin.reports.loading = true;
        els.adminReportList.innerHTML = "";
        try {
            const view = state.admin.reports;
            const params = new URLSearchParams({page: String(view.page), size: "10"});
            if (view.status) {
                params.set("status", view.status);
            }
            const response = await api("/api/admin/reports?" + params.toString());
            const pageData = response.data;
            view.totalPages = Math.max(1, pageData.totalPages);
            pageData.content.forEach((report) => {
                const card = document.createElement("article");
                card.className = "report-card";
                const head = document.createElement("div");
                head.className = "report-head";
                const copy = document.createElement("div");
                const title = document.createElement("h4");
                title.textContent = report.reason;
                const sub = document.createElement("p");
                sub.textContent = titleCase(report.targetType) + " #" + (report.targetId || "—")
                    + " · reported by " + report.reporterName + " · " + formatDate(report.createdAt);
                copy.append(title, sub);
                head.append(copy, statusBadgeText(report.status));
                card.appendChild(head);
                if (report.details) {
                    const details = document.createElement("p");
                    details.className = "report-details";
                    details.textContent = report.details;
                    card.appendChild(details);
                }
                if (report.adminNotes) {
                    const notes = document.createElement("p");
                    notes.className = "fine-line";
                    notes.textContent = "Admin notes: " + report.adminNotes;
                    card.appendChild(notes);
                }
                const actions = document.createElement("div");
                actions.className = "report-actions";
                if (report.status === "OPEN") {
                    actions.appendChild(actionButton("Start review", "button button-ghost button-sm",
                        () => resolveReport(report, "UNDER_REVIEW", null)));
                }
                if (report.status !== "RESOLVED" && report.status !== "DISMISSED") {
                    actions.appendChild(actionButton("Resolve", "button button-navy button-sm",
                        () => resolveReportWithNote(report, "RESOLVED")));
                    actions.appendChild(actionButton("Dismiss", "button button-ghost button-sm is-danger",
                        () => resolveReportWithNote(report, "DISMISSED")));
                }
                card.appendChild(actions);
                els.adminReportList.appendChild(card);
            });
            els.adminReportEmpty.hidden = pageData.content.length !== 0;
            els.adminReportCount.textContent = pageData.totalElements
                + (pageData.totalElements === 1 ? " report" : " reports");
            els.adminReportPagination.hidden = view.totalPages <= 1;
            els.adminReportPage.textContent = "Page " + (view.page + 1) + " of " + view.totalPages;
            els.adminReportPrev.disabled = view.page <= 0;
            els.adminReportNext.disabled = view.page >= view.totalPages - 1;
        } catch (error) {
            showAdminAlert("reports", "Reports could not be loaded: " + error.message);
        } finally {
            state.admin.reports.loading = false;
        }
    }

    function resolveReportWithNote(report, status) {
        const notes = window.prompt("Record the action taken for this report (required):");
        if (!notes || !notes.trim()) {
            return;
        }
        resolveReport(report, status, notes.trim());
    }

    async function resolveReport(report, status, notes) {
        try {
            await api("/api/admin/reports/" + report.id + "/resolve", {
                method: "POST",
                body: JSON.stringify({status: status, adminNotes: notes})
            });
            toast("Report updated.");
            await loadAdminReports();
            await loadAdminAudit();
        } catch (error) {
            showAdminAlert("reports", error.message);
            toast("The report could not be updated.");
        }
    }

    async function loadAdminReviews() {
        if (!els.adminReviewList || state.admin.reviews.loading) {
            return;
        }
        state.admin.reviews.loading = true;
        els.adminReviewList.innerHTML = "";
        try {
            const view = state.admin.reviews;
            const params = new URLSearchParams({page: String(view.page), size: "10"});
            if (view.status) {
                params.set("status", view.status);
            }
            const response = await api("/api/admin/reviews?" + params.toString());
            const pageData = response.data;
            view.totalPages = Math.max(1, pageData.totalPages);
            pageData.content.forEach((review) => {
                const card = reviewCard(review, true);
                const actions = document.createElement("div");
                actions.className = "review-actions";
                if (review.status !== "PUBLISHED") {
                    actions.appendChild(actionButton("Publish", "button button-navy button-sm",
                        () => moderateReview(review, "PUBLISHED")));
                }
                if (review.status !== "UNDER_REVIEW") {
                    actions.appendChild(actionButton("Under review", "button button-ghost button-sm",
                        () => moderateReview(review, "UNDER_REVIEW")));
                }
                if (review.status !== "HIDDEN") {
                    actions.appendChild(actionButton("Hide", "button button-ghost button-sm is-danger",
                        () => moderateReview(review, "HIDDEN")));
                }
                card.appendChild(actions);
                els.adminReviewList.appendChild(card);
            });
            els.adminReviewEmpty.hidden = pageData.content.length !== 0;
            els.adminReviewCount.textContent = pageData.totalElements
                + (pageData.totalElements === 1 ? " review" : " reviews");
            els.adminReviewPagination.hidden = view.totalPages <= 1;
            els.adminReviewPage.textContent = "Page " + (view.page + 1) + " of " + view.totalPages;
            els.adminReviewPrev.disabled = view.page <= 0;
            els.adminReviewNext.disabled = view.page >= view.totalPages - 1;
        } catch (error) {
            showAdminAlert("reviews", "Reviews could not be loaded: " + error.message);
        } finally {
            state.admin.reviews.loading = false;
        }
    }

    async function moderateReview(review, status) {
        if (status === "HIDDEN" && !window.confirm(
            "Hide this review from public profiles? The moderation action is audit logged.")) {
            return;
        }
        try {
            await api("/api/admin/reviews/" + review.id + "/status", {
                method: "POST",
                body: JSON.stringify({status: status})
            });
            toast("Review status updated.");
            await loadAdminReviews();
            await loadAdminAudit();
        } catch (error) {
            showAdminAlert("reviews", error.message);
            toast("The review could not be updated.");
        }
    }

    async function loadAdminAudit() {
        if (!els.adminAuditList || state.admin.audit.loading) {
            return;
        }
        state.admin.audit.loading = true;
        els.adminAuditList.innerHTML = "";
        try {
            const view = state.admin.audit;
            const response = await api("/api/admin/audit-logs?page=" + view.page + "&size=15");
            const pageData = response.data;
            view.totalPages = Math.max(1, pageData.totalPages);
            pageData.content.forEach((entry) => {
                const row = document.createElement("tr");
                const when = document.createElement("td");
                when.textContent = formatDate(entry.createdAt);
                const actor = document.createElement("td");
                actor.textContent = entry.actorName || "System";
                const action = document.createElement("td");
                const badge = document.createElement("span");
                badge.className = "badge badge-soft";
                badge.textContent = titleCase(entry.action);
                action.appendChild(badge);
                const target = document.createElement("td");
                target.textContent = entry.entityType + (entry.entityId ? " #" + entry.entityId : "");
                const details = document.createElement("td");
                details.className = "audit-details";
                details.textContent = entry.details || "—";
                row.append(when, actor, action, target, details);
                els.adminAuditList.appendChild(row);
            });
            els.adminAuditCount.textContent = pageData.totalElements
                + (pageData.totalElements === 1 ? " entry" : " entries");
            els.adminAuditPagination.hidden = view.totalPages <= 1;
            els.adminAuditPage.textContent = "Page " + (view.page + 1) + " of " + view.totalPages;
            els.adminAuditPrev.disabled = view.page <= 0;
            els.adminAuditNext.disabled = view.page >= view.totalPages - 1;
        } catch (error) {
            showAdminAlert("audit", "Audit history could not be loaded: " + error.message);
        } finally {
            state.admin.audit.loading = false;
        }
    }

    function setupAdmin() {
        if (!els.adminUserList) {
            return;
        }
        const reload = (key, loader) => {
            const view = state.admin[key];
            view.page = 0;
            loader();
        };
        if (els.adminUserRole) {
            els.adminUserRole.addEventListener("change", () => {
                state.admin.users.role = els.adminUserRole.value;
                reload("users", loadAdminUsers);
            });
        }
        let userTimer = null;
        if (els.adminUserSearch) {
            els.adminUserSearch.addEventListener("input", () => {
                window.clearTimeout(userTimer);
                userTimer = window.setTimeout(() => {
                    state.admin.users.q = els.adminUserSearch.value.trim();
                    loadAdminUsers();
                }, 300);
            });
        }
        if (els.adminCampaignStatus) {
            els.adminCampaignStatus.addEventListener("change", () => {
                state.admin.campaigns.status = els.adminCampaignStatus.value;
                reload("campaigns", loadAdminCampaigns);
            });
        }
        let campaignTimer = null;
        if (els.adminCampaignSearch) {
            els.adminCampaignSearch.addEventListener("input", () => {
                window.clearTimeout(campaignTimer);
                campaignTimer = window.setTimeout(() => {
                    state.admin.campaigns.q = els.adminCampaignSearch.value.trim();
                    reload("campaigns", loadAdminCampaigns);
                }, 300);
            });
        }
        const bindPage = (name, prev, next, loader) => {
            if (prev) {
                prev.addEventListener("click", () => {
                    if (state.admin[name].page > 0) {
                        state.admin[name].page -= 1;
                        loader();
                    }
                });
            }
            if (next) {
                next.addEventListener("click", () => {
                    if (state.admin[name].page < state.admin[name].totalPages - 1) {
                        state.admin[name].page += 1;
                        loader();
                    }
                });
            }
        };
        bindPage("users", els.adminUserPrev, els.adminUserNext, loadAdminUsers);
        bindPage("campaigns", els.adminCampaignPrev, els.adminCampaignNext, loadAdminCampaigns);
        bindPage("reports", els.adminReportPrev, els.adminReportNext, loadAdminReports);
        bindPage("reviews", els.adminReviewPrev, els.adminReviewNext, loadAdminReviews);
        bindPage("audit", els.adminAuditPrev, els.adminAuditNext, loadAdminAudit);

        document.querySelectorAll("[data-admin-user-refresh]").forEach((button) => {
            button.addEventListener("click", loadAdminUsers);
        });
        document.querySelectorAll("[data-admin-campaign-refresh]").forEach((button) => {
            button.addEventListener("click", loadAdminCampaigns);
        });
        document.querySelectorAll("[data-admin-report-refresh]").forEach((button) => {
            button.addEventListener("click", loadAdminReports);
        });
        document.querySelectorAll("[data-admin-review-refresh]").forEach((button) => {
            button.addEventListener("click", loadAdminReviews);
        });
        document.querySelectorAll("[data-admin-audit-refresh]").forEach((button) => {
            button.addEventListener("click", loadAdminAudit);
        });
        if (els.adminReportStatus) {
            els.adminReportStatus.addEventListener("change", () => {
                state.admin.reports.status = els.adminReportStatus.value;
                state.admin.reports.page = 0;
                loadAdminReports();
            });
        }
        if (els.adminReviewStatus) {
            els.adminReviewStatus.addEventListener("change", () => {
                state.admin.reviews.status = els.adminReviewStatus.value;
                state.admin.reviews.page = 0;
                loadAdminReviews();
            });
        }
    }

    function setupSidebar() {
        const toggle = document.querySelector("[data-sidebar-toggle]");
        if (!toggle) {
            return;
        }
        toggle.addEventListener("click", () => {
            const open = els.sidebar.classList.toggle("is-open");
            toggle.setAttribute("aria-expanded", String(open));
        });
        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape") {
                closeSidebar();
            }
        });
    }

    function setupButtons() {
        document.querySelectorAll("[data-goto-profile]").forEach((button) => {
            button.addEventListener("click", () => showView("profile"));
        });
    }

    async function boot() {
        if (!api) {
            window.location.href = "/login.html";
            return;
        }
        ensureCsrf();
        setupSidebar();
        setupProfileForm();
        setupCampaigns();
        setupApplications();
        setupCollaborations();
        setupDeliverables();
        setupContracts();
        setupPayments();
        setupMessaging();
        setupReviews();
        setupNotifications();
        setupAdmin();
        setupButtons();
        try {
            const response = await api("/api/auth/me");
            state.session = response.data;
            renderAccount();
            renderNavigation();
            renderOverview();
            showView("overview");
            refreshUnreadCounts();
        } catch (error) {
            if (error.status === 401) {
                window.location.href = "/login.html?next=" + encodeURIComponent("/dashboard.html");
                return;
            }
            showAppAlert(error.message);
        }
    }

    document.addEventListener("DOMContentLoaded", boot);
})();
