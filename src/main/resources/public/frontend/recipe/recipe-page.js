/**
 * This script defines the CRUD operations for Recipe objects in the Recipe Management Application.
 */

const BASE_URL = "http://localhost:8081"; // backend URL

let recipes = [];

// Wait for DOM to fully load before accessing elements
window.addEventListener("DOMContentLoaded", () => {

    /* 
     * DONE: Get references to various DOM elements
     * - Recipe name and instructions fields (add, update, delete)
     * - Recipe list container
     * - Admin link and logout button
     * - Search input
    */
    const recipeList = document.getElementById("recipe-list");
    const addName = document.getElementById("add-recipe-name-input");
    const addInstr = document.getElementById("add-recipe-instructions-input");
    const addBtn = document.getElementById("add-recipe-submit-input");
    const updName = document.getElementById("update-recipe-name-input");
    const updInstr = document.getElementById("update-recipe-instructions-input");
    const updBtn = document.getElementById("update-recipe-submit-input");
    const delName = document.getElementById("delete-recipe-name-input");
    const delBtn = document.getElementById("delete-recipe-submit-input");
    const searchInput = document.getElementById("search-input");
    const searchBtn = document.getElementById("search-button");
    const adminLink = document.getElementById("admin-link");
    const logoutBtn = document.getElementById("logout-button");

    /*
     * DONE: Show logout button if auth-token exists in sessionStorage
     */
    if (sessionStorage.getItem("auth-token")) {
      logoutBtn.style.display = "inline-block";
    }


    /*
     * DONE: Show admin link if is-admin flag in sessionStorage is "true"
     */
    if (adminLink) {
      if (sessionStorage.getItem("is-admin") === "true") {
        adminLink.style.display = "inline-block";
      } else {
        adminLink.style.display = "none";
      }
    }
   
  
  

    /*
     * DONE: Attach event handlers
     * - Add recipe button → addRecipe()
     * - Update recipe button → updateRecipe()
     * - Delete recipe button → deleteRecipe()
     * - Search button → searchRecipes()
     * - Logout button → processLogout()
     */
    addBtn.addEventListener("click", addRecipe);
    updBtn.addEventListener("click", updateRecipe);
    delBtn.addEventListener("click", deleteRecipe);
    searchBtn.addEventListener("click", searchRecipes);
    logoutBtn.addEventListener("click", processLogout);

    /*
     * DONE: On page load, call getRecipes() to populate the list
     */
    getRecipes();


    /**
     * DONE: Search Recipes Function
     * - Read search term from input field
     * - Send GET request with name query param
     * - Update the recipe list using refreshRecipeList()
     * - Handle fetch errors and alert user
     */
    async function searchRecipes() {
      const term = (searchInput.value || "").trim();
      try {
        const url = term ? `${BASE_URL}/recipes?name=${encodeURIComponent(term)}` : `${BASE_URL}/recipes`;
        const res = await fetch(url, {
          headers: { "Authorization": "Bearer " + sessionStorage.getItem("auth-token") }
        });
        if (!res.ok) return alert("Failed to search recipes.");
        const data = await res.json();
        refreshRecipeList(Array.isArray(data) ? data : []);
      } catch {
        alert("Network error while searching recipes.");
      }
    }

    /**
     * DONE: Add Recipe Function
     * - Get values from add form inputs
     * - Validate both name and instructions
     * - Send POST request to /recipes
     * - Use Bearer token from sessionStorage
     * - On success: clear inputs, fetch latest recipes, refresh the list
     */
    async function addRecipe() {
      const name = (addName.value || "").trim();
      const instructions = (addInstr.value || "").trim();
      if (!name || !instructions) return alert("Please provide name and instructions.");

      try {
        const res = await fetch(`${BASE_URL}/recipes`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": "Bearer " + sessionStorage.getItem("auth-token")
          },
          body: JSON.stringify({ name, instructions })
        });
        if (res.ok) {
          addName.value = "";
          addInstr.value = "";
          await getRecipes();
        } else alert("Failed to add recipe.");
      } catch {
        alert("Network error while adding recipe.");
      }
    }

    /**
     * DONE: Update Recipe Function
     * - Get values from update form inputs
     * - Validate both name and updated instructions
     * - Fetch current recipes to locate the recipe by name
     * - Send PUT request to update it by ID
     * - On success: clear inputs, fetch latest recipes, refresh the list
     */
    async function updateRecipe() {
      const name = (updName.value || "").trim();
      const instructions = (updInstr.value || "").trim();
      if (!name || !instructions) return alert("Please provide recipe name and new instructions.");
  
      try {
        if (!recipes.length) await getRecipes();
        const match = recipes.find(r => (r.name || "").toLowerCase() === name.toLowerCase());
        if (!match) return alert("Recipe not found.");
  
        const res = await fetch(`${BASE_URL}/recipes/${match.id}`, {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            "Authorization": "Bearer " + sessionStorage.getItem("auth-token")
          },
          body: JSON.stringify({ instructions })
        });
        if (res.ok) {
          updName.value = "";
          updInstr.value = "";
          await getRecipes();
        } else alert("Failed to update recipe.");
      } catch {
        alert("Network error while updating recipe.");
      }
    }

    /**
     * DONE: Delete Recipe Function
     * - Get recipe name from delete input
     * - Find matching recipe in list to get its ID
     * - Send DELETE request using recipe ID
     * - On success: refresh the list
     */
    async function deleteRecipe() {
      const name = (delName.value || "").trim();
      if (!name) return alert("Please provide a recipe name to delete.");

      if (!recipes.length) await getRecipes();
      const match = recipes.find(r => (r.name || "").toLowerCase() === name.toLowerCase());
      if (!match) return alert("Recipe not found.");

      try {
        const res = await fetch(`${BASE_URL}/recipes/${match.id}`, {
          method: "DELETE",
          headers: { "Authorization": "Bearer " + sessionStorage.getItem("auth-token") }
        });

        if (res.ok) {
          delName.value = "";
          await getRecipes();
        } else {
          alert("Not authorized to delete this recipe.");
        }
      } catch {
        alert("Network error while deleting recipe.");
      }
    }

    /**
     * DONE: Get Recipes Function
     * - Fetch all recipes from backend
     * - Store in recipes array
     * - Call refreshRecipeList() to display
     */
    async function getRecipes() {
      try {
        const res = await fetch(`${BASE_URL}/recipes`, {
          headers: { "Authorization": "Bearer " + sessionStorage.getItem("auth-token") }
        });
        if (!res.ok) return alert("Failed to load recipes.");
        recipes = await res.json();
        refreshRecipeList(recipes);
      } catch {
        alert("Network error while loading recipes.");
      }
    }

    /**
     * DONE: Refresh Recipe List Function
     * - Clear current list in DOM
     * - Create <li> elements for each recipe with name + instructions
     * - Append to list container
     */
    function refreshRecipeList(list) {
      const data = Array.isArray(list) ? list : recipes;
      recipeList.innerHTML = "";
      if (!data.length) {
        const li = document.createElement("li");
        li.textContent = "No recipes found.";
        recipeList.appendChild(li);
        return;
      }
      for (const r of data) {
        const li = document.createElement("li");
        li.innerHTML = `<strong>${r.name || ""}</strong><br>${r.instructions || ""}`;
        recipeList.appendChild(li);
      }
    }

    /**
     * DONE: Logout Function
     * - Send POST request to /logout
     * - Use Bearer token from sessionStorage
     * - On success: clear sessionStorage and redirect to login
     * - On failure: alert the user
     */
    async function processLogout() {
      try {
        await fetch(`${BASE_URL}/logout`, {
          method: "POST",
          headers: { "Authorization": "Bearer " + sessionStorage.getItem("auth-token") }
        });
      } catch {}
      sessionStorage.removeItem("auth-token");
      sessionStorage.removeItem("is-admin");
      window.location.href = "../login/login-page.html";
    }

});
