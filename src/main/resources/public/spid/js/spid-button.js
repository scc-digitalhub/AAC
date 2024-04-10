function closeOpenItems(provider) {
  // close the dropdown if the user clicks on another dropdown
  let dropdowns = document.querySelectorAll(".spid-idp-button");
  dropdowns.forEach(function(openDropdown) {
    if (!openDropdown.id.includes(provider)){
      openDropdown.classList.remove("spid-idp-button-open");
    }
  });
}

function expandDropdown(provider) {
  closeOpenItems(provider);
  let dropdownMenu = document.getElementById("spid-dropdown-" + provider);
  dropdownMenu.classList.toggle("spid-idp-button-open");
}

window.onclick = function (event) {
  // close the dropdown if the user clicks outside of it
  if (!event.target.closest(".button-spid")) {
    let dropdowns = document.querySelectorAll(".spid-idp-button");
    dropdowns.forEach(function(openDropdown) {
      openDropdown.classList.remove("spid-idp-button-open");
    });
  }
};
