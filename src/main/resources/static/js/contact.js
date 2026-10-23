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
        await fetch("/api/auth/csrf", {
            credentials: "same-origin",
            headers: {Accept: "application/json"},
            cache: "no-store"
        });
    }

    function fieldOf(control) {
        return control.closest(".field") || control.closest("[data-contact-consent]");
    }

    function clearErrors(form) {
        form.querySelectorAll(".has-error").forEach((element) => element.classList.remove("has-error"));
    }

    function markInvalid(control) {
        const container = fieldOf(control);
        if (container) {
            container.classList.add("has-error");
        }
    }

    function validate(form) {
        clearErrors(form);
        let firstInvalid = null;
        const controls = Array.from(form.querySelectorAll("input, select, textarea"));
        controls.forEach((control) => {
            if (control.type === "checkbox" ? !control.checked : !control.checkValidity()) {
                markInvalid(control);
                if (!firstInvalid) {
                    firstInvalid = control;
                }
            }
        });
        if (firstInvalid) {
            firstInvalid.focus({preventScroll: false});
        }
        return !firstInvalid;
    }

    function showAlert(form, message, type) {
        const alert = form.parentElement.querySelector("[data-contact-alert]");
        if (!alert) {
            return;
        }
        alert.hidden = false;
        alert.classList.toggle("is-error", type === "error");
        alert.classList.toggle("is-success", type !== "error");
        alert.querySelector("[data-contact-alert-text]").textContent = message;
        alert.scrollIntoView({behavior: "smooth", block: "nearest"});
    }

    function hideAlert(form) {
        const alert = form.parentElement.querySelector("[data-contact-alert]");
        if (alert) {
            alert.hidden = true;
        }
    }

    function setup() {
        const form = document.querySelector("[data-contact-form]");
        if (!form) {
            return;
        }
        const submit = form.querySelector("[data-contact-submit]");
        const idleLabel = submit ? submit.textContent : "Send message";

        form.addEventListener("input", (event) => {
            const container = fieldOf(event.target);
            if (container && container.classList.contains("has-error")
                && (event.target.type === "checkbox" ? event.target.checked : event.target.checkValidity())) {
                container.classList.remove("has-error");
            }
        });

        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            hideAlert(form);
            if (!validate(form)) {
                showAlert(form, "Please review the highlighted fields.", "error");
                return;
            }

            const data = new FormData(form);
            const payload = {
                name: String(data.get("name") || "").trim(),
                email: String(data.get("email") || "").trim(),
                role: String(data.get("role") || ""),
                topic: String(data.get("topic") || ""),
                message: String(data.get("message") || "").trim(),
                consent: data.get("consent") === "on"
            };

            if (submit) {
                submit.disabled = true;
                submit.classList.add("is-busy");
                submit.textContent = "Sending...";
            }

            try {
                await ensureCsrf();
                const headers = {Accept: "application/json", "Content-Type": "application/json"};
                const token = readCookie("XSRF-TOKEN");
                if (token) {
                    headers["X-XSRF-TOKEN"] = token;
                }
                const response = await fetch("/api/public/contact", {
                    method: "POST",
                    credentials: "same-origin",
                    headers: headers,
                    body: JSON.stringify(payload)
                });
                let body = null;
                try {
                    body = await response.json();
                } catch (error) {
                    body = null;
                }
                if (!response.ok) {
                    const message = (body && body.message) || "The message could not be sent. Please try again.";
                    showAlert(form, message, "error");
                    return;
                }
                form.reset();
                clearErrors(form);
                showAlert(form, (body && body.message)
                    || "Thanks for reaching out. A platform administrator will review your message.", "success");
            } catch (error) {
                showAlert(form, "The platform could not be reached. Check your connection and try again.", "error");
            } finally {
                if (submit) {
                    submit.disabled = false;
                    submit.classList.remove("is-busy");
                    submit.textContent = idleLabel;
                }
            }
        });
    }

    document.addEventListener("DOMContentLoaded", setup);
})();
