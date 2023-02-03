import { TranslationMessages } from 'ra-core';
import englishMessages from 'ra-language-english';

const messages: TranslationMessages = {
    ...englishMessages,
    admin: 'Admin',
    developer: 'Developer',
    user: 'User',
    security: 'Security',
    resources: {
        accounts: {
            name: 'Account |||| Accounts',
        },
        credentials: {
            name: 'Credential |||| Credentials',
            fields: {
                curPassword: 'Current password',
            },
        },
        profiles: {
            name: 'Personal info',
        },
        connections: {
            name: 'Connected App |||| Connected Apps',
        },
    },
    dashboard: {
        welcome: 'Welcome, %{name}',
        description:
            'Manage you personal information, accounts and review your security settings.',
        review_manage: 'Review and manage your accounts',
        manage_accounts: 'Manage Accounts',
        view_update: 'View and update your credentials',
        edit_profile: 'Edit profile',
        active_sessions:
            'You have currently active sessions on one or more devices.',
        view_sessions: 'View sessions',
        view_and: 'view connections in applications',
        manage_connections: 'Manage Connections',
        credentials: 'Credentials',
        accounts: 'Accounts',
        third_party: '3rd party apps',
        edit_credentials: 'Edit credentials',
    },
    accounts_page: {
        header: 'Accounts',
        description:
            'View, manage and connect your local, social and external accounts',
        registered_user: 'Registered user',
        delete_user: {
            title: 'Delete user',
            text: 'Remove your user and all the associated information',
            action: 'Delete user',
            confirm: 'Delete your user %{id}?',
            content:
                'Are you sure you want to delete your account? This action cannot be undone!',
        },
        delete_account: {
            content:
                'Are you sure you want to delete your account? This action cannot be undone!',
        },
    },
    profiles_page: {
        header: 'Personal info',
        description:
            'Review and inspect your personal information, as available to applications and services.',
    },
    connections_page: {
        header: 'Connected applications',
        description:
            'Manage and control application permissions. You gave access to some of your personal information to applications and sites. Review and remove access to applications you no longer use or trust.',
        permissions: 'Has access to %{num} permissions',
    },
    credentials_page: {
        password: {
            edit: {
                title: 'Update password',
                description: 'Change your old password and set a new one',
            },
        },
    },
    password: {
        policy: {
            description: 'The password must satisfy the following requisites:',
            strength: 'New password strength',
            passwordMinLength: 'Minimum length: %{value}',
            passwordMaxLength: 'Maximum length: %{value}',
            passwordRequireAlpha: 'Require letters',
            passwordRequireUppercaseAlpha: 'Require an uppercase character',
            passwordRequireNumber: 'Require a number',
            passwordRequireSpecial: 'Require a special character',
            passwordSupportWhitespace: 'Supports white-spaces',
        },
    },
    edit_password: {
        password: 'Password',
        change_password: 'Change password',
        replace_current: 'Replace current password with a new one',
        current_password: 'Current password',
        verify_password: 'Verify password',
        length: 'Length',
        policy: 'Password policy',
        min_requirement: 'Minimum password length: ',
        max_requirement: 'Maximun password length: ',
        alpha_requirement: 'Requires a letter',
        numeric_requirement: 'Requires a number',
        special_requirement: 'Requires a special char',
        uppercase_requirement: 'Requires a uppercase letter',
    },
    error: {
        already_registered: 'Already registered.',
        authentication_service:
            'Authentication problem. Please try again later.',
        bad_credentials: 'Invalid user or password',
        duplicated_data: 'Duplicated data',
        internal_error: 'Internal error. Please try again.',
        invalid_data: 'Invalid field data.',
        invalid_email: 'Invalid email',
        //invalid_password: 'Invalid user or password',
        invalid_password: {
            contains_whitespace: 'Invalid password: contains whitespaces',
            empty: 'Invalid password: empty password',
            max_length: 'Invalid password: too long',
            min_length: 'Invalid password: too short',
            password_reuse: 'Invalid password: password reuse',
            require_alpha: 'Invalid password: alpha char required',
            require_number: 'Invalid password: number required',
            require_uppercase_alpha:
                'Invalid password: uppercase alpha char required',
            require_special: 'Invalid password: special character required',
            not_match: 'Invalid password: the passwords do not match',
            policy: 'Invalid password: the passwords does not satisfy the policy',
        },
        invalid_user: 'Invalid user',
        locked: 'User locked',
        mismatch_passwords: 'Passwords does not match',
        missing_data: 'Missing data',
        not_confirmed: 'Registration not confirmed.',
        not_registered: 'Invalid user or password',
        // registration: 'Registration problem. Please try again later.',
        invalid_field: 'Invalid field data.',
        unsupported_operation: 'Unsupported operation.',
        wrong_password: 'Wrong password',
        unauthenticated_user: 'User must be authenticated.',
        registration: {
            invalid_password: 'Invalid password',
        },
    },
};

messages.ra.page.dashboard = 'Home';

export default messages;
