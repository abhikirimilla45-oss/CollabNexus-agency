(function () {
    "use strict";

    const page = document.body.getAttribute("data-marketplace-page");
    if (page !== "campaigns" && page !== "creators") {
        return;
    }

    const isCampaigns = page === "campaigns";
    const grid = document.querySelector(isCampaigns ? "[data-campaign-grid]" : "[data-creator-grid]");
    const form = document.querySelector(isCampaigns ? "[data-campaign-filters]" : "[data-creator-filters]");
    const summary = document.querySelector("[data-results-summary]");
    const empty = document.querySelector("[data-marketplace-empty]");
    const pagination = document.querySelector("[data-pagination]");
    const pageLabel = document.querySelector("[data-page-label]");
    const prev = document.querySelector("[data-page-prev]");
    const next = document.querySelector("[data-page-next]");

    const dateFormatter = new Intl.DateTimeFormat(undefined, {
        day: "2-digit", month: "short", year: "numeric"
    });

    const state = {page: 0, size: 12, totalPages: 1, loading: false, applyCampaign: null, sessionLoaded: false, session: null};

    const applyModal = document.querySelector("[data-apply-modal]");
    const applyForm = document.querySelector("[data-apply-form]");
    const applyAlert = document.querySelector("[data-apply-alert]");
    const applyAlertText = document.querySelector("[data-apply-alert-text]");
    const applySubmit = document.querySelector("[data-apply-submit]");

    function text(value, fallback) {
        return value === null || value === undefined || value === "" ? (fallback || "") : String(value);
    }

    function initials(name) {
        return text(name, "CN").split(/\s+/).filter(Boolean).slice(0, 2)
            .map((part) => part.charAt(0).toUpperCase()).join("") || "CN";
    }

    function titleCase(value) {
        return text(value).toLowerCase().replace(/(^|[_\s])([a-z])/g, (match, prefix, letter) => prefix + letter.toUpperCase())
            .replace(/_/g, " ");
    }

    function truncate(value, length) {
        const clean = text(value).trim();
        return clean.length > length ? clean.slice(0, length - 1).trimEnd() + "…" : clean;
    }

    function formatDate(value) {
        if (!value) {
            return "Not set";
        }
        const date = new Date(value.length === 10 ? value + "T00:00:00" : value);
        return Number.isNaN(date.getTime()) ? "Not set" : dateFormatter.format(date);
    }

    function formatMoney(value, currency) {
        if (value === null || value === undefined || value === "") {
            return "Not stated";
        }
        try {
            return new Intl.NumberFormat(undefined, {
                style: "currency",
                currency: currency || "USD",
                maximumFractionDigits: 0
            }).format(Number(value));
        } catch (error) {
            return text(value);
        }
    }

    function tags(values) {
        const row = document.createElement("div");
        row.className = "tag-row";
        values.filter(Boolean).forEach((value) => {
            const tag = document.createElement("span");
            tag.className = "tag";
            tag.textContent = value;
            row.appendChild(tag);
        });
        return row;
    }

    function meta(entries) {
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

    function link(href, label, className) {
        const anchor = document.createElement("a");
        anchor.href = href;
        anchor.textContent = label;
        anchor.className = className;
        return anchor;
    }

    function campaignCard(campaign) {
        const card = document.createElement("article");
        card.className = "campaign-card";

        const top = document.createElement("div");
        top.className = "campaign-top";
        const brand = document.createElement("span");
        brand.className = "campaign-brand";
        brand.textContent = text(campaign.brandName, "Brand");
        const status = document.createElement("span");
        status.className = "status-badge is-open";
        status.textContent = "Applications open";
        top.append(brand, status);

        const title = document.createElement("h3");
        title.textContent = text(campaign.title, "Untitled campaign");

        const description = document.createElement("p");
        description.textContent = truncate(campaign.description, 180);

        const budget = formatMoney(campaign.minBudget, campaign.currency) + " – "
            + formatMoney(campaign.maxBudget, campaign.currency);

        const foot = document.createElement("div");
        foot.className = "campaign-foot";
        const count = document.createElement("span");
        const applications = Number(campaign.applicationCount || 0);
        count.textContent = applications + (applications === 1 ? " application" : " applications");
        const apply = document.createElement("button");
        apply.type = "button";
        apply.className = "text-link link-button";
        apply.textContent = "Apply as creator";
        apply.addEventListener("click", () => openApply(campaign));
        foot.append(count, apply);

        card.append(
            top,
            title,
            description,
            tags([campaign.niches, campaign.platforms, campaign.contentFormats]
                .flatMap((value) => text(value).split(","))
                .map((value) => value.trim())
                .filter(Boolean)
                .slice(0, 3)),
            meta([
                ["Budget", budget],
                ["Creators", String(campaign.creatorsRequired || 1)],
                ["Deadline", formatDate(campaign.applicationDeadline)]
            ]),
            foot
        );
        return card;
    }

    function creatorCard(creator, index) {
        const card = document.createElement("article");
        card.className = "creator-card";

        const head = document.createElement("div");
        head.className = "creator-head";
        const avatar = document.createElement("span");
        avatar.className = "avatar avatar-" + ((index % 4) + 1);
        avatar.setAttribute("aria-hidden", "true");
        avatar.textContent = initials(creator.displayName);
        const nameWrap = document.createElement("div");
        nameWrap.className = "creator-name";
        const name = document.createElement("h3");
        name.textContent = text(creator.displayName, "Creator");
        const headline = document.createElement("p");
        headline.className = "creator-handle";
        headline.textContent = text(creator.headline, creator.niche || "Creator profile");
        nameWrap.append(name, headline);
        const availability = document.createElement("span");
        const available = creator.availability === "AVAILABLE";
        availability.className = "availability " + (available ? "is-open" : "is-limited");
        availability.textContent = available ? "Available" : titleCase(creator.availability || "Unavailable");
        head.append(avatar, nameWrap, availability);

        const bio = document.createElement("p");
        bio.className = "creator-bio";
        bio.textContent = truncate(creator.bio, 150) || "This creator has not added a biography yet.";

        const stats = document.createElement("dl");
        stats.className = "creator-stats";
        [
            ["Indicative rate", creator.baseRate === null || creator.baseRate === undefined
                ? "Not set" : formatMoney(creator.baseRate, creator.currency)],
            ["Based in", text(creator.country, "Not stated")]
        ].forEach(([term, value]) => {
            const group = document.createElement("div");
            const dt = document.createElement("dt");
            dt.textContent = term;
            const dd = document.createElement("dd");
            dd.textContent = value;
            group.append(dt, dd);
            stats.appendChild(group);
        });

        const foot = document.createElement("div");
        foot.className = "campaign-foot";
        const verification = document.createElement("span");
        verification.textContent = creator.verificationStatus === "IDENTITY_VERIFIED"
            ? "Identity verified" : "Not platform endorsed";
        foot.appendChild(verification);
        if (creator.portfolioUrl) {
            const portfolio = link(creator.portfolioUrl, "Portfolio", "text-link");
            portfolio.target = "_blank";
            portfolio.rel = "noopener noreferrer";
            foot.appendChild(portfolio);
        }

        card.append(
            head,
            bio,
            tags([creator.niche, creator.primaryPlatform, creator.languages]
                .filter(Boolean).slice(0, 3)),
            stats,
            foot
        );
        return card;
    }

    function currentParams() {
        const params = new URLSearchParams();
        new FormData(form).forEach((value, key) => {
            const cleaned = String(value).trim();
            if (cleaned) {
                params.set(key, cleaned);
            }
        });
        params.set("page", String(state.page));
        params.set("size", String(state.size));
        return params;
    }

    function syncUrl(params) {
        const visible = new URLSearchParams(params);
        visible.delete("page");
        visible.delete("size");
        const query = visible.toString();
        const url = window.location.pathname + (query ? "?" + query : "");
        window.history.replaceState({}, "", url);
    }

    function renderPagination(totalPages) {
        state.totalPages = Math.max(1, totalPages);
        if (!pagination) {
            return;
        }
        pagination.hidden = state.totalPages <= 1;
        if (pageLabel) {
            pageLabel.textContent = "Page " + (state.page + 1) + " of " + state.totalPages;
        }
        if (prev) {
            prev.disabled = state.page <= 0;
        }
        if (next) {
            next.disabled = state.page >= state.totalPages - 1;
        }
    }

    async function load() {
        if (state.loading) {
            return;
        }
        state.loading = true;
        grid.setAttribute("aria-busy", "true");
        grid.innerHTML = "";
        if (empty) {
            empty.hidden = true;
        }
        if (summary) {
            summary.textContent = isCampaigns ? "Loading campaigns..." : "Loading creators...";
        }

        const params = currentParams();
        syncUrl(params);

        try {
            const response = await fetch("/api/" + page + "?" + params.toString(), {
                credentials: "same-origin",
                headers: {Accept: "application/json"},
                cache: "no-store"
            });
            const payload = await response.json().catch(() => null);
            if (!response.ok || !payload || payload.success !== true) {
                throw new Error((payload && payload.message) || "The marketplace could not be loaded.");
            }

            const result = payload.data;
            const items = Array.isArray(result.content) ? result.content : [];
            items.forEach((item, index) => {
                grid.appendChild(isCampaigns ? campaignCard(item) : creatorCard(item, state.page * state.size + index));
            });

            if (summary) {
                const label = isCampaigns
                    ? (result.totalElements === 1 ? "campaign" : "campaigns")
                    : (result.totalElements === 1 ? "creator" : "creators");
                summary.textContent = result.totalElements + " " + label + " found";
            }
            if (empty) {
                empty.hidden = items.length !== 0;
            }
            renderPagination(result.totalPages);
        } catch (error) {
            if (summary) {
                summary.textContent = "Marketplace unavailable";
            }
            if (empty) {
                empty.hidden = false;
                const heading = empty.querySelector("h2");
                const copy = empty.querySelector("p");
                if (heading) {
                    heading.textContent = "The marketplace could not be loaded.";
                }
                if (copy) {
                    copy.textContent = error.message;
                }
            }
            renderPagination(1);
        } finally {
            state.loading = false;
            grid.removeAttribute("aria-busy");
        }
    }

    async function currentSession() {
        if (!state.sessionLoaded) {
            try {
                const payload = await window.CollabNexusApi.api("/api/auth/me");
                state.session = payload && payload.data ? payload.data : null;
            } catch (error) {
                state.session = null;
            }
            state.sessionLoaded = true;
        }
        return state.session;
    }

    function showApplyAlert(message) {
        if (!applyAlert || !applyAlertText) {
            return;
        }
        applyAlert.hidden = false;
        applyAlert.className = "form-alert is-error";
        applyAlertText.textContent = message;
    }

    function clearApplyAlert() {
        if (applyAlert) {
            applyAlert.hidden = true;
        }
        if (applyForm) {
            applyForm.querySelectorAll(".field").forEach((field) => field.classList.remove("has-error"));
        }
    }

    function closeApply() {
        if (!applyModal) {
            return;
        }
        applyModal.hidden = true;
        document.body.classList.remove("modal-open");
        state.applyCampaign = null;
        if (applySubmit) {
            applySubmit.disabled = false;
            applySubmit.textContent = "Submit application";
        }
    }

    async function openApply(campaign) {
        const session = await currentSession();
        if (!session || !session.authenticated) {
            window.location.href = "/login.html?next=" + encodeURIComponent("/campaigns.html");
            return;
        }
        if (!session.user || session.user.role !== "CREATOR") {
            window.CollabNexus.toast("Only creator accounts can apply to campaigns.");
            return;
        }
        if (!applyModal || !applyForm) {
            return;
        }
        state.applyCampaign = campaign;
        applyForm.reset();
        clearApplyAlert();
        const name = applyModal.querySelector("[data-apply-campaign]");
        if (name) {
            name.textContent = text(campaign.title, "this campaign");
        }
        const currency = applyForm.querySelector("[name='currency']");
        if (currency && campaign.currency) {
            const existing = Array.from(currency.options).find((item) => item.value === campaign.currency);
            if (existing) {
                currency.value = campaign.currency;
            } else {
                const option = document.createElement("option");
                option.value = campaign.currency;
                option.textContent = campaign.currency;
                currency.appendChild(option);
                currency.value = campaign.currency;
            }
        }
        applyModal.hidden = false;
        document.body.classList.add("modal-open");
        const proposal = applyForm.querySelector("[name='proposal']");
        if (proposal) {
            proposal.focus();
        }
    }

    function readApplyPayload() {
        const value = (name) => {
            const field = applyForm.querySelector("[name='" + name + "']");
            return field ? field.value.trim() : "";
        };
        const price = value("proposedPrice");
        const campaign = state.applyCampaign || {};
        return {
            proposal: value("proposal"),
            proposedPrice: price ? Number(price) : null,
            currency: value("currency") || campaign.currency || "USD",
            availableFrom: value("availableFrom") || null,
            portfolioUrl: value("portfolioUrl") || null
        };
    }

    async function submitApply(event) {
        event.preventDefault();
        if (!state.applyCampaign) {
            return;
        }
        clearApplyAlert();
        const proposalField = applyForm.querySelector("[name='proposal']");
        if (!proposalField || !proposalField.value.trim()) {
            proposalField.closest(".field").classList.add("has-error");
            showApplyAlert("Add a short proposal so the brand can review your application.");
            proposalField.focus();
            return;
        }
        const portfolioField = applyForm.querySelector("[name='portfolioUrl']");
        if (portfolioField && portfolioField.value.trim() && portfolioField.validity.typeMismatch) {
            portfolioField.closest(".field").classList.add("has-error");
            showApplyAlert("Enter a full portfolio URL including https://.");
            portfolioField.focus();
            return;
        }
        applySubmit.disabled = true;
        applySubmit.textContent = "Submitting...";
        try {
            await window.CollabNexusApi.ensureCsrf();
            await window.CollabNexusApi.api("/api/campaigns/" + state.applyCampaign.id + "/applications", {
                method: "POST",
                body: JSON.stringify(readApplyPayload())
            });
            closeApply();
            window.CollabNexus.toast("Application submitted. Track it from your workspace.");
            state.applyCampaign = null;
            load();
        } catch (error) {
            showApplyAlert(error.message);
            applySubmit.disabled = false;
            applySubmit.textContent = "Submit application";
        }
    }

    if (applyForm) {
        applyForm.addEventListener("submit", submitApply);
    }

    if (applyModal) {
        applyModal.querySelectorAll("[data-apply-close], [data-apply-cancel]").forEach((button) => {
            button.addEventListener("click", closeApply);
        });
        applyModal.addEventListener("click", (event) => {
            if (event.target === applyModal) {
                closeApply();
            }
        });
        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && !applyModal.hidden) {
                closeApply();
            }
        });
    }

    function clearFilters() {
        form.reset();
        state.page = 0;
        load();
    }

    function restoreFilters() {
        const params = new URLSearchParams(window.location.search);
        params.forEach((value, key) => {
            const field = form.querySelector("[name='" + key + "']");
            if (field) {
                field.value = value;
            }
        });
    }

    form.addEventListener("submit", (event) => {
        event.preventDefault();
        state.page = 0;
        load();
    });

    document.querySelectorAll("[data-filters-clear]").forEach((button) => {
        button.addEventListener("click", clearFilters);
    });

    if (prev) {
        prev.addEventListener("click", () => {
            if (state.page > 0) {
                state.page -= 1;
                load();
                window.scrollTo({top: 0, behavior: "smooth"});
            }
        });
    }

    if (next) {
        next.addEventListener("click", () => {
            if (state.page < state.totalPages - 1) {
                state.page += 1;
                load();
                window.scrollTo({top: 0, behavior: "smooth"});
            }
        });
    }

    restoreFilters();
    load();
})();
