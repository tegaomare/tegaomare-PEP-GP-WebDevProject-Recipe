/**
 * This script defines the registration functionality for the Registration page in the Recipe Management Application.
 */

const BASE_URL = "http://localhost:8081"; // backend URL

/* 
 * DONE: Get references to various DOM elements
 * - usernameInput, emailInput, passwordInput, repeatPasswordInput, registerButton
 */
window.addEventListener("DOMContentLoaded", () => {
    const usernameInput       = document.getElementById("username-input");
    const emailInput          = document.getElementById("email-input");
    const passwordInput       = document.getElementById("password-input");
    const repeatPasswordInput = document.getElementById("repeat-password-input");
    const registerButton      = document.getElementById("register-button");
  


/* 
 * DONE: Ensure the register button calls processRegistration when clicked
 */
    registerButton.addEventListener("click", processRegistration);


/**
 * DONE: Process Registration Function
 * 
 * Requirements:
 * - Retrieve username, email, password, and repeat password from input fields
 * - Validate all fields are filled
 * - Check that password and repeat password match
 * - Create a request body with username, email, and password
 * - Define requestOptions using method POST and proper headers
 * 
 * Fetch Logic:
 * - Send POST request to `${BASE_URL}/register`
 * - If status is 201:
 *      - Redirect user to login page
 * - If status is 409:
 *      - Alert that user/email already exists
 * - Otherwise:
 *      - Alert generic registration error
 * 
 * Error Handling:
 * - Wrap in try/catch
 * - Log error and alert user
 */
async function processRegistration() {
    try {
        const username = (usernameInput.value || "").trim();
        const email    = (emailInput.value || "").trim();
        const password = (passwordInput.value || "").trim();
        const repeat   = (repeatPasswordInput.value || "").trim();
  
        if (!username || !email || !password || !repeat) {
          alert("Please fill out all fields.");
          return;
        }
        if (password !== repeat) {
          alert("Passwords do not match.");
          return;
        }
  
        const res = await fetch(`${BASE_URL}/register`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ username, email, password })
        });
  
        if (res.status === 201) {
          // success => go to login
          window.location.href = "../login/login-page.html";
        } else if (res.status === 409) {
          // duplicate
          alert("Username or email already exists.");
        } else {
          alert("Registration failed. Please try again.");
        }
      } catch (err) {
        console.error(err);
        alert("Network error while registering.");
      }
    }
});
