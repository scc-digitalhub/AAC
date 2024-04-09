function openDropdown(dropdownMenu) {
  dropdownMenu.style.display = "block";
}

function closeDropdown(dropdownMenu) {
  dropdownMenu.style.display = "none";
}

function toggleDropdown(dropdownMenu) {
  dropdownMenu.classList.toggle("spid-idp-button-open");
  if (dropdownMenu.matches(".spid-idp-button-open")) {
    openDropdown(dropdownMenu);
  } else {
    closeDropdown(dropdownMenu);
  }
}

function closeOpenItems(provider) {
  let openMenus = document.querySelectorAll(".spid-idp-button");
  openMenus.forEach(function(openMenu) {
    if (!openMenu.id.includes(provider)){
      openMenu.classList.remove("spid-idp-button-open");
      closeDropdown(openMenu);
    }
  });
}

function expandDropdown(provider) {
  closeOpenItems(provider);
  let dropdownMenu = document.getElementById("spid-dropdown-" + provider);
  toggleDropdown(dropdownMenu);
}

window.onclick = function (event) {
  // close the dropdown is the user clicks outside of it
  if (!event.target.matches(".spid-display-button")) {
    let dropdowns = document.getElementsByClassName("spid-idp-button");
    for (let i = 0; i < dropdowns.length; i++) {
      let openDropdown = dropdowns[i];
      if (openDropdown.classList.contains("spid-idp-button-open")) {
        openDropdown.classList.remove("spid-idp-button-open");
        closeDropdown(openDropdown);
      }
    }
  }
};
