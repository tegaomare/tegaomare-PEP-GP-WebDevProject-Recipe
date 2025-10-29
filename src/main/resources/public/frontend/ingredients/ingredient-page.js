/**
 * This script defines the add, view, and delete operations for Ingredient objects in the Recipe Management Application.
 */

const BASE_URL = "http://localhost:8081"; // backend URL

/*
 * DONE: Create an array to keep track of ingredients
 */
let ingredients = [];

/* 
 * DONE: Get references to various DOM elements
 * - addIngredientNameInput
 * - deleteIngredientNameInput
 * - ingredientListContainer
 * - searchInput (optional for future use)
 * - adminLink (if visible conditionally)
 */
window.addEventListener("DOMContentLoaded", () => {
  const addInput = document.getElementById("add-ingredient-name-input");
  const delInput = document.getElementById("delete-ingredient-name-input");
  const addBtn   = document.getElementById("add-ingredient-submit-button");
  const delBtn   = document.getElementById("delete-ingredient-submit-button");
  const listEl   = document.getElementById("ingredient-list");
  
    // Admin gate: redirect non-admins back to recipes
    if (sessionStorage.getItem("is-admin") !== "true") {
      window.location.href = "../recipe/recipe-page.html";
      return;
    }

    /* 
    * DONE: Attach 'onclick' events to:
    * - "add-ingredient-submit-button" → addIngredient()
    * - "delete-ingredient-submit-button" → deleteIngredient()
    */
    addBtn.addEventListener("click", addIngredient);
    delBtn.addEventListener("click", deleteIngredient);



    /* 
    * DONE: On page load, call getIngredients()
    */
    getIngredients();


    /**
     * DONE: Add Ingredient Function
     * 
     * Requirements:
     * - Read and trim value from addIngredientNameInput
     * - Validate input is not empty
     * - Send POST request to /ingredients
     * - Include Authorization token from sessionStorage
     * - On success: clear input, call getIngredients() and refreshIngredientList()
     * - On failure: alert the user
     */
    async function addIngredient() {
        const name = (addInput.value || "").trim();
        if (!name) { alert("Please enter an ingredient name."); return; }
        try {
          const res = await fetch(`${BASE_URL}/ingredients`, {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              "Authorization": "Bearer " + sessionStorage.getItem("auth-token")
            },
            body: JSON.stringify({ name })
          });
          if (res.ok) {
            addInput.value = "";
            await getIngredients(); // list includes "salt" for the test
          } else {
            alert("Failed to add ingredient.");
          }
        } catch (e) {
          console.error(e); alert("Network error while adding ingredient.");
        }
    }


    /**
     * DONE: Get Ingredients Function
     * 
     * Requirements:
     * - Fetch all ingredients from backend
     * - Store result in `ingredients` array
     * - Call refreshIngredientList() to display them
     * - On error: alert the user
     */
    async function getIngredients() {
        try {
          const res = await fetch(`${BASE_URL}/ingredients`, {
            headers: { "Authorization": "Bearer " + sessionStorage.getItem("auth-token") }
          });
          if (!res.ok) { alert("Failed to fetch ingredients."); return; }
          ingredients = await res.json();
          refreshIngredientList();   
        } catch (e) {
          console.error(e); alert("Network error while fetching ingredients.");
        }
    
    }


    /**
     * DONE: Delete Ingredient Function
     * 
     * Requirements:
     * - Read and trim value from deleteIngredientNameInput
     * - Search ingredientListContainer's <li> elements for matching name
     * - Determine ID based on index (or other backend logic)
     * - Send DELETE request to /ingredients/{id}
     * - On success: call getIngredients() and refreshIngredientList(), clear input
     * - On failure or not found: alert the user
     */
    async function deleteIngredient() {
        const name = (delInput.value || "").trim();
        if (!name) { alert("Please enter an ingredient name to delete."); return; }
    
        const match = ingredients.find(i => (i.name || "").toLowerCase() === name.toLowerCase());
        if (!match) { alert("Ingredient not found."); return; }
    
        try {
          const res = await fetch(`${BASE_URL}/ingredients/${match.id}`, {
            method: "DELETE",
            headers: { "Authorization": "Bearer " + sessionStorage.getItem("auth-token") }
          });
          if (res.ok) {
            delInput.value = "";
            await getIngredients(); // list should not include deleted item
          } else {
            alert("Failed to delete ingredient.");
          }
        } catch (e) {
          console.error(e); alert("Network error while deleting ingredient.");
        }
    }


    /**
     * DONE: Refresh Ingredient List Function
     * 
     * Requirements:
     * - Clear ingredientListContainer
     * - Loop through `ingredients` array
     * - For each ingredient:
     *   - Create <li> and inner <p> with ingredient name
     *   - Append to container
     */
    function refreshIngredientList() {
        listEl.innerHTML = "";
        if (!ingredients.length) {
          const li = document.createElement("li");
          li.textContent = "No ingredients found.";
          listEl.appendChild(li);
          return;
        }
        for (const ing of ingredients) {
          const li = document.createElement("li");
          li.textContent = ing.name;
          listEl.appendChild(li);
        }
    
    }
});