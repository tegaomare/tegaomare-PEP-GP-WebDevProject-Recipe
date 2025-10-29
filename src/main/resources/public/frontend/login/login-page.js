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
        if (!username || !password) { alert("Please enter username and password."); return; }

        try {
            const res = await fetch(`${BASE_URL}/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password })
            });

            if (res.status === 200) {
            // Backend returns: "<token> <isAdmin>"
            const text = (await res.text()).trim();
            const [token, adminFlag] = text.split(/\s+/);
            console.log("Parsed login:", { token, adminFlag });
            sessionStorage.setItem("auth-token", token || "");
            sessionStorage.setItem("is-admin", String(adminFlag === "true"));
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
