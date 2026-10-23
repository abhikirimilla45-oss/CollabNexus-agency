(function () {
    "use strict";

    const HEALTH_ENDPOINT = "/api/health";
    const dots = document.querySelectorAll("[data-status-dot]");
    const labels = document.querySelectorAll("[data-status-label]");
    const updated = document.querySelector("[data-status-updated]");

    function setLabel(value) {
        labels.forEach((label) => {
            label.textContent = value;
        });
    }

    function setDots(state) {
        dots.forEach((dot) => {
            dot.classList.remove("is-up", "is-down");
            if (state) {
                dot.classList.add(state);
            }
        });
    }

    function formatTime(value) {
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return "Unknown";
        }
        return new Intl.DateTimeFormat(undefined, {
            day: "2-digit",
            month: "short",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        }).format(date);
    }

    async function fetchHealth() {
        const controller = new AbortController();
        const timeout = window.setTimeout(() => controller.abort(), 8000);

        try {
            const response = await fetch(`${HEALTH_ENDPOINT}?t=${Date.now()}`, {
                headers: {Accept: "application/json"},
                cache: "no-store",
                signal: controller.signal
            });

            if (!response.ok) {
                throw new Error(`Health endpoint returned HTTP ${response.status}`);
            }

            const payload = await response.json();
            if (!payload || payload.success !== true || !payload.data) {
                throw new Error("Unexpected health response");
            }

            const health = payload.data;
            const isUp = health.status === "UP" && health.database === "UP";
            setDots(isUp ? "is-up" : "is-down");
            setLabel(isUp ? "All systems up" : "Platform degraded");

            if (updated) {
                updated.textContent = `Checked ${formatTime(health.timestamp)}`;
            }
            document.documentElement.setAttribute("data-platform-status", isUp ? "up" : "degraded");
        } catch (error) {
            setDots("is-down");
            setLabel("Status unavailable");
            if (updated) {
                updated.textContent = "No response from the health endpoint";
            }
            document.documentElement.setAttribute("data-platform-status", "down");
        } finally {
            window.clearTimeout(timeout);
        }
    }

    document.addEventListener("DOMContentLoaded", fetchHealth);
})();
