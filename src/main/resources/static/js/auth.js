(function () {
    "use strict";

    function readCookie(name) {
        return document.cookie.split(";").map((part) => part.trim())
            .filter((part) => part.startsWith(name + "="))
            .map((part) => decodeURIComponent(part.substring(name.length + 1)))[0] || null;
    }

    async function ensureCsrf() {
        if (readCookie("XSRF-TOKEN")) {
            return;
        }
        try {
            await fetch("/api/auth/csrf", {
                credentials: "same-origin",
                headers: {Accept: "application/json"},
                cache: "no-store"
            });
        } catch (error) {
            // The login and registration endpoints are rate limited server-side.
        }
    }

    async function api(path, options) {
        const settings = Object.assign({method: "GET"}, options || {});
        const headers = Object.assign({Accept: "application/json"}, settings.headers || {});
        const isFormData = typeof FormData !== "undefined" && settings.body instanceof FormData;
        if (settings.body && !isFormData && !headers["Content-Type"]) {
            headers["Content-Type"] = "application/json";
        }
        if (settings.method !== "GET" && settings.method !== "HEAD") {
            const token = readCookie("XSRF-TOKEN");
            if (token) {
                headers["X-XSRF-TOKEN"] = token;
            }
        }
        const response = await fetch(path, Object.assign({}, settings, {
            headers: headers,
            credentials: "same-origin",
            cache: "no-store"
        }));

        let payload = null;
        try {
            payload = await response.json();
        } catch (error) {
            payload = null;
        }

        if (!response.ok) {
            const failure = new Error((payload && payload.message) || "Something went wrong. Please try again.");
            failure.status = response.status;
            failure.payload = payload;
            throw failure;
        }
        return payload;
    }

    function showAlert(form, message, type) {
        const alert = form.querySelector("[data-auth-alert]");
        if (!alert) {
            if (window.CollabNexus && window.CollabNexus.toast) {
                window.CollabNexus.toast(message);
            }
            return;
        }
        alert.hidden = false;
        alert.className = "form-alert " + (type === "success" ? "is-success" : "is-error");
        const text = alert.querySelector("[data-auth-alert-text]");
        if (text) {
            text.textContent = message;
        }
        alert.scrollIntoView({behavior: "smooth", block: "nearest"});
    }

    function clearAlert(form) {
        const alert = form.querySelector("[data-auth-alert]");
        if (alert) {
            alert.hidden = true;
        }
    }

    function setBusy(button, busy, idleLabel) {
        if (!button) {
            return;
        }
        button.disabled = busy;
        button.classList.toggle("is-busy", busy);
        if (busy) {
            button.dataset.idleLabel = button.textContent;
            button.textContent = "Please wait...";
        } else if (idleLabel || button.dataset.idleLabel) {
            button.textContent = idleLabel || button.dataset.idleLabel;
        }
    }

    function redirectFor(session) {
        const params = new URLSearchParams(window.location.search);
        const next = params.get("next");
        if (next && next.startsWith("/") && !next.startsWith("//")) {
            window.location.href = next;
            return;
        }
        window.location.href = "/dashboard.html";
    }

    function setupLogin() {
        const form = document.querySelector("form[data-auth-form='login']");
        if (!form) {
            return;
        }
        ensureCsrf();

        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            clearAlert(form);
            if (window.CollabNexus && window.CollabNexus.validateForm
                && !window.CollabNexus.validateForm(form, "login")) {
                showAlert(form, "Please review the highlighted fields.", "error");
                return;
            }

            const button = form.querySelector("[type='submit']");
            setBusy(button, true);
            try {
                const session = await api("/api/auth/login", {
                    method: "POST",
                    body: JSON.stringify({
                        email: form.querySelector("[name='email']").value.trim(),
                        password: form.querySelector("[name='password']").value
                    })
                });
                showAlert(form, "Welcome back. Opening your workspace...", "success");
                redirectFor(session);
            } catch (error) {
                showAlert(form, error.message, "error");
                setBusy(button, false);
            }
        });

        form.querySelectorAll("[data-demo-fill]").forEach((button) => {
            button.addEventListener("click", () => {
                form.querySelector("[name='email']").value = button.getAttribute("data-email");
                form.querySelector("[name='password']").value = button.getAttribute("data-password");
                form.querySelector("[name='email']").focus();
            });
        });
    }

    function setupRegister() {
        const form = document.querySelector("form[data-auth-form='register']");
        if (!form) {
            return;
        }
        ensureCsrf();

        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            clearAlert(form);
            if (window.CollabNexus && window.CollabNexus.validateForm
                && !window.CollabNexus.validateForm(form, "register")) {
                showAlert(form, "Please review the highlighted fields.", "error");
                return;
            }

            const role = form.querySelector("[name='role']:checked").value;
            const payload = {
                fullName: form.querySelector("[name='fullName']").value.trim(),
                email: form.querySelector("[name='email']").value.trim(),
                password: form.querySelector("[name='password']").value,
                role: role,
                country: valueOrNull(form, "[name='country']")
            };

            if (role === "brand") {
                payload.companyName = valueOrNull(form, "[name='companyName']");
                payload.companyWebsite = valueOrNull(form, "[name='companyWebsite']");
            } else {
                payload.primaryNiche = valueOrNull(form, "[name='primaryNiche']");
                payload.primaryPlatform = valueOrNull(form, "[name='primaryPlatform']");
            }

            const button = form.querySelector("[type='submit']");
            setBusy(button, true);
            try {
                const session = await api("/api/auth/register", {
                    method: "POST",
                    body: JSON.stringify(payload)
                });
                showAlert(form, "Account created. Preparing your workspace...", "success");
                redirectFor(session);
            } catch (error) {
                showAlert(form, error.message, "error");
                setBusy(button, false);
            }
        });
    }

    function valueOrNull(form, selector) {
        const input = form.querySelector(selector);
        if (!input || !input.value.trim()) {
            return null;
        }
        return input.value.trim();
    }

    function setupLogoutButtons() {
        document.querySelectorAll("[data-logout]").forEach((button) => {
            button.addEventListener("click", async (event) => {
                event.preventDefault();
                try {
                    await ensureCsrf();
                    await api("/api/auth/logout", {method: "POST"});
                } catch (error) {
                    // Signing out locally still clears the browser view.
                } finally {
                    window.location.href = "/login.html";
                }
            });
        });
    }

    function setupHeaderSession() {
        const actions = document.querySelector(".nav-actions[data-auth-nav]");
        if (!actions) {
            return;
        }
        api("/api/auth/me").then((payload) => {
            if (!payload || !payload.data || !payload.data.authenticated) {
                return;
            }
            actions.innerHTML = "";
            const dashboard = document.createElement("a");
            dashboard.className = "nav-login";
            dashboard.href = "/dashboard.html";
            dashboard.textContent = "Dashboard";
            const logout = document.createElement("button");
            logout.type = "button";
            logout.className = "button button-gold button-sm";
            logout.setAttribute("data-logout", "");
            logout.textContent = "Log out";
            actions.append(dashboard, logout);
            setupLogoutButtons();
        }).catch(() => {
            // Anonymous visitors keep the default sign-in actions.
        });
    }

    document.addEventListener("DOMContentLoaded", () => {
        setupLogin();
        setupRegister();
        setupLogoutButtons();
        setupHeaderSession();
    });

    window.CollabNexusApi = {
        api: api,
        ensureCsrf: ensureCsrf,
        readCookie: readCookie,
        showAlert: showAlert
    };
})();
