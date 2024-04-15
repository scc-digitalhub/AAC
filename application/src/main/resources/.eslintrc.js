module.exports = {
    "env": {
        "browser": true,
        "es2021": true,
        "jquery": true,
    },
    "plugins": [
        "prettier",
        "security"
    ],
    "extends": [
        "eslint:recommended",
        "plugin:security/recommended",
        // "plugin:prettier/recommended",
    ],
    "parserOptions": {
        "ecmaVersion": 12,
        "sourceType": "module"
    },
    "rules": {
    }
};
