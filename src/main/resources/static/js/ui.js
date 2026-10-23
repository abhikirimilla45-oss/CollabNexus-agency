(function () {
    "use strict";

    const dateFormatter = new Intl.DateTimeFormat(undefined, {
        day: "2-digit",
        month: "short",
        year: "numeric"
    });

    let toastTimer = null;

    function setText(node, value) {
        if (node) {
            node.textContent = value;
        }
    }

    function showToast(message) {
        const toast = document.querySelector("[data-toast]");
        if (!toast) {
            return;
        }
        toast.textContent = message;
        toast.hidden = false;
        window.clearTimeout(toastTimer);
        toastTimer = window.setTimeout(() => {
            toast.hidden = true;
        }, 3200);
    }

    function setupNavigation() {
        const toggle = document.querySelector(".nav-toggle");
        const nav = document.querySelector("#primaryNav");
        if (!toggle || !nav) {
            return;
        }

        const closeNav = () => {
            nav.classList.remove("is-open");
            toggle.setAttribute("aria-expanded", "false");
            toggle.setAttribute("aria-label", "Open navigation");
        };

        toggle.addEventListener("click", () => {
            const isOpen = nav.classList.toggle("is-open");
            toggle.setAttribute("aria-expanded", String(isOpen));
            toggle.setAttribute("aria-label", isOpen ? "Close navigation" : "Open navigation");
        });

        nav.querySelectorAll("a").forEach((link) => {
            link.addEventListener("click", closeNav);
        });

        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape") {
                closeNav();
            }
        });

        window.addEventListener("resize", () => {
            if (window.innerWidth > 860) {
                closeNav();
            }
        });
    }

    function setupHeader() {
        const header = document.querySelector("#siteHeader");
        if (!header) {
            return;
        }
        const update = () => {
            header.classList.toggle("is-scrolled", window.scrollY > 12);
        };
        update();
        window.addEventListener("scroll", update, {passive: true});
    }

    function setupReveal() {
        const items = document.querySelectorAll(".reveal");
        if (!("IntersectionObserver" in window)) {
            items.forEach((item) => item.classList.add("is-visible"));
            return;
        }

        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    entry.target.classList.add("is-visible");
                    observer.unobserve(entry.target);
                }
            });
        }, {threshold: 0.12, rootMargin: "0px 0px -6% 0px"});

        items.forEach((item) => observer.observe(item));

        window.setTimeout(() => {
            items.forEach((item) => item.classList.add("is-visible"));
        }, 2500);
    }

    function setupYear() {
        document.querySelectorAll("[data-current-year]").forEach((node) => {
            setText(node, String(new Date().getFullYear()));
        });
    }

    function setupPasswordToggles() {
        document.querySelectorAll("[data-password-toggle]").forEach((button) => {
            const target = document.getElementById(button.getAttribute("aria-controls"));
            if (!target) {
                return;
            }

            button.addEventListener("click", () => {
                const show = target.type === "password";
                target.type = show ? "text" : "password";
                button.setAttribute("aria-label", show ? "Hide password" : "Show password");
                button.classList.toggle("is-active", show);
            });
        });
    }

    function strengthScore(password) {
        let score = 0;
        if (password.length >= 8) {
            score += 1;
        }
        if (/[a-z]/.test(password) && /[A-Z]/.test(password)) {
            score += 1;
        }
        if (/\d/.test(password)) {
            score += 1;
        }
        if (/[^A-Za-z0-9]/.test(password) || password.length >= 12) {
            score += 1;
        }
        return score;
    }

    function setupStrengthMeter() {
        const meter = document.querySelector("[data-strength]");
        const password = document.getElementById("registerPassword");
        if (!meter || !password) {
            return;
        }

        const bar = meter.querySelector("[data-strength-bar]");
        const label = meter.querySelector("[data-strength-label]");
        const defaultText = label ? label.textContent : "";
        const states = [
            {width: "10%", color: "#c25c52", text: "Too weak. Add length and a mix of characters."},
            {width: "30%", color: "#c25c52", text: "Weak. Add upper case, lower case, and a number."},
            {width: "55%", color: "#c9a961", text: "Fair. Add another character type for more strength."},
            {width: "80%", color: "#3f9d72", text: "Good. This password meets the minimum requirements."},
            {width: "100%", color: "#2f7d5a", text: "Strong. This password meets the minimum requirements."}
        ];

        password.addEventListener("input", () => {
            if (!bar || !label) {
                return;
            }
            if (!password.value) {
                bar.style.width = "0";
                label.textContent = defaultText;
                return;
            }
            const state = states[strengthScore(password.value)];
            bar.style.width = state.width;
            bar.style.background = state.color;
            label.textContent = state.text;
        });
    }

    function setFieldError(field, message) {
        if (!field) {
            return;
        }
        field.classList.add("has-error");
        const error = field.querySelector("[data-field-error]");
        setText(error, message);
    }

    function clearFieldError(field) {
        if (!field) {
            return;
        }
        field.classList.remove("has-error");
    }

    function fieldContainer(input) {
        return input.closest(".field") || input.closest(".checkbox-field");
    }

    function isHiddenElement(element) {
        return Boolean(element.closest("[hidden]"));
    }

    function validateInput(input, formType) {
        const container = fieldContainer(input);
        if (!container || input.disabled) {
            return true;
        }

        const value = input.type === "checkbox" ? input.checked : input.value.trim();
        const name = input.name;

        if (input.required) {
            if (input.type === "checkbox" && !value) {
                setFieldError(container, container.classList.contains("checkbox-field")
                    ? "Please accept the terms and privacy policy to continue."
                    : "This field is required.");
                return false;
            }
            if (input.type !== "checkbox" && !value) {
                setFieldError(container, "This field is required.");
                return false;
            }
        }

        if (!value) {
            clearFieldError(container);
            return true;
        }

        if (input.type === "email" && !/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(value)) {
            setFieldError(container, "Enter a valid email address, such as name@company.com.");
            return false;
        }

        if (input.type === "url") {
            try {
                const url = new URL(value);
                if (url.protocol !== "http:" && url.protocol !== "https:") {
                    throw new Error("Unsupported protocol");
                }
            } catch (error) {
                setFieldError(container, "Enter a full URL including https://.");
                return false;
            }
        }

        if (name === "password") {
            if (formType === "register") {
                if (value.length < 8 || !/[a-z]/.test(value) || !/[A-Z]/.test(value) || !/\d/.test(value)) {
                    setFieldError(container, "Use at least 8 characters with upper case, lower case, and a number.");
                    return false;
                }
            } else if (value.length < 8) {
                setFieldError(container, "Passwords are at least 8 characters.");
                return false;
            }
        }

        if (name === "confirmPassword") {
            const password = input.form.querySelector("[name='password']");
            if (password && value !== password.value) {
                setFieldError(container, "Passwords must match.");
                return false;
            }
        }

        clearFieldError(container);
        return true;
    }

    function setupFormValidation() {
        document.querySelectorAll(".js-validate-form").forEach((form) => {
            const formType = form.getAttribute("data-form-type") || "form";

            form.querySelectorAll("input, select, textarea").forEach((input) => {
                const eventName = input.type === "checkbox" || input.tagName === "SELECT" ? "change" : "input";
                input.addEventListener(eventName, () => {
                    if (fieldContainer(input)?.classList.contains("has-error")) {
                        validateInput(input, formType);
                    }
                });
            });

            const termsError = form.querySelector("[data-terms-error]");
            const terms = form.querySelector("[name='acceptTerms']");
            if (terms) {
                terms.addEventListener("change", () => {
                    if (termsError) {
                        termsError.style.display = terms.checked ? "none" : "block";
                    }
                });
            }

            form.addEventListener("submit", (event) => {
                event.preventDefault();

                if (!validateForm(form, formType)) {
                    showToast("Please review the highlighted fields.");
                    return;
                }

                if (form.hasAttribute("data-auth-form") || form.hasAttribute("data-profile-form")) {
                    return;
                }

                const success = form.querySelector("[data-form-success]");
                if (success) {
                    success.hidden = false;
                    success.scrollIntoView({behavior: "smooth", block: "nearest"});
                }
                showToast("Your details look correct.");
            });
        });
    }

    function validateForm(form, formType) {
        let firstInvalid = null;
        form.querySelectorAll("input, select, textarea").forEach((input) => {
            if (isHiddenElement(input)) {
                clearFieldError(fieldContainer(input));
                return;
            }
            const valid = validateInput(input, formType);
            if (!valid && !firstInvalid) {
                firstInvalid = input;
            }
        });

        const terms = form.querySelector("[name='acceptTerms']");
        const termsError = form.querySelector("[data-terms-error]");
        if (terms && !terms.checked) {
            if (termsError) {
                termsError.style.display = "block";
            }
            if (!firstInvalid) {
                firstInvalid = terms;
            }
        } else if (termsError) {
            termsError.style.display = "none";
        }

        if (firstInvalid) {
            firstInvalid.focus({preventScroll: false});
            return false;
        }
        return true;
    }

    function setupRoleSelector() {
        const form = document.querySelector("form[data-form-type='register']");
        if (!form) {
            return;
        }

        const radios = Array.from(form.querySelectorAll("[name='role']"));
        const panels = Array.from(form.querySelectorAll("[data-role-panel]"));
        const submitLabel = form.querySelector("[data-submit-label]");
        const params = new URLSearchParams(window.location.search);
        const requestedRole = params.get("role");

        if (requestedRole === "brand" || requestedRole === "creator") {
            const requested = radios.find((radio) => radio.value === requestedRole);
            if (requested) {
                requested.checked = true;
            }
        }

        const selectedRole = () => {
            const selected = radios.find((radio) => radio.checked);
            return selected ? selected.value : "creator";
        };

        const apply = () => {
            const role = selectedRole();
            radios.forEach((radio) => {
                const option = radio.closest("[data-role-option]");
                if (option) {
                    option.classList.toggle("is-selected", radio.checked);
                }
            });

            panels.forEach((panel) => {
                const matches = panel.getAttribute("data-role-panel") === role;
                panel.hidden = !matches;
                panel.querySelectorAll("input, select").forEach((input) => {
                    input.disabled = !matches;
                    if (!matches) {
                        clearFieldError(fieldContainer(input));
                    }
                });
            });

            const company = document.getElementById("companyName");
            const niche = document.getElementById("primaryNiche");
            const platform = document.getElementById("primaryPlatform");

            if (company) {
                company.required = role === "brand";
            }
            if (niche) {
                niche.required = role === "creator";
            }
            if (platform) {
                platform.required = role === "creator";
            }

            setText(submitLabel, role === "brand" ? "Create brand account" : "Create creator account");
        };

        radios.forEach((radio) => radio.addEventListener("change", apply));
        apply();
    }

    function setupFaq() {
        const items = document.querySelectorAll(".faq-item");
        items.forEach((item) => {
            item.addEventListener("toggle", () => {
                if (!item.open) {
                    return;
                }
                items.forEach((other) => {
                    if (other !== item) {
                        other.open = false;
                    }
                });
            });
        });
    }

    function init() {
        setupNavigation();
        setupHeader();
        setupReveal();
        setupYear();
        setupPasswordToggles();
        setupStrengthMeter();
        setupRoleSelector();
        setupFormValidation();
        setupFaq();
    }

    window.CollabNexus = {
        toast: showToast,
        validateForm: validateForm,
        formatDate: (value) => {
            const date = new Date(value);
            return Number.isNaN(date.getTime()) ? "Unknown" : dateFormatter.format(date);
        }
    };

    document.addEventListener("DOMContentLoaded", init);
})();
