/**
 * This script handles the login functionality for the Recipe Management Application.
 * It manages user authentication by sending login requests to the server and handling responses.
*/
const BASE_URL = "http://localhost:8081"; // backend URL

/* 
 * DONE: Get references to DOM elements
 * - username input
 * - password input
 * - login button
 * - logout button (optional, for token testing)
 */
window.addEventListener("DOMContentLoaded", () => {
    const loginInput    = document.getElementById("login-input");
    const passwordInput = document.getElementById("password-input");
    const loginButton   = document.getElementById("login-button");

/* 
 * DONE: Add click event listener to login button
 * - Call processLogin on click
 */
    loginButton.addEventListener("click", processLogin);


/**
 * DONE: Process Login Function
 * 
 * Requirements:
 * - Retrieve values from username and password input fields
 * - Construct a request body with { username, password }
 * - Configure request options for fetch (POST, JSON headers)
 * - Send request to /login endpoint
 * - Handle responses:
 *    - If 200: extract token and isAdmin from response text
 *      - Store both in sessionStorage
 *      - Redirect to recipe-page.html
 *    - If 401: alert user about incorrect login
 *    - For others: show generic alert
 * - Add try/catch to handle fetch/network errors
 * 
 * Hints:
 * - Use fetch with POST method and JSON body
 * - Use sessionStorage.setItem("key", value) to store auth token and admin flag
 * - Use `window.location.href` for redirection
 */
    async function processLogin() {
        const username = (loginInput.value || "").trim();
        const password = (passwordInput.value || "").trim();
        if (!username || !password) {
        alert("Please enter username and password.");
        return;
        }

        try {
        const res = await fetch(`${BASE_URL}/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password })
        });

        if (res.status === 200) {
            // Support both JSON and plain-text formats
            let token = null;
            let isAdmin = "false";

            const ct = (res.headers.get("content-type") || "").toLowerCase();
            if (ct.includes("application/json")) {
            const j = await res.json();
            token   = j["auth-token"] || j["token"] || null;
            const adminVal = j["is-admin"] ?? j["isAdmin"] ?? j["admin"];
            if (typeof adminVal === "boolean") isAdmin = adminVal ? "true" : "false";
            if (typeof adminVal === "string")  isAdmin = adminVal === "true" ? "true" : "false";
            } else {
            const text = (await res.text()).trim(); // e.g. "abcd123 true"
            const parts = text.split(/\s+/);
            token = parts[0] || null;
            if (parts.length > 1) isAdmin = parts[1] === "true" ? "true" : "false";
            }

            if (!token) { alert("Login succeeded but no token was returned."); return; }

            sessionStorage.setItem("auth-token", token);
            sessionStorage.setItem("is-admin", isAdmin);

            // go to recipe page
            window.location.href = "../recipe/recipe-page.html";
        } else if (res.status === 401) {
            alert("Incorrect login!");
        } else {
            alert("Unknown issue during login.");
        }
        } catch (e) {
        console.error(e);
        alert("Network error while logging in.");
        }
    }
});
